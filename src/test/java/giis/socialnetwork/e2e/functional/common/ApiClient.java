package giis.socialnetwork.e2e.functional.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Thin HTTP client with its own cookie store (one instance = one session); paths are relative to the base URL. */
public class ApiClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int SOCKET_TIMEOUT_MS = 30_000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 10_000;

    /** Status and body of one HTTP exchange. */
    public static final class Response {
        public final int status;
        public final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private final String baseUrl;
    private final BasicCookieStore cookieStore = new BasicCookieStore();
    private final CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(CONNECT_TIMEOUT_MS)
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
                    .build())
            .setDefaultCookieStore(cookieStore)
            .setRedirectStrategy(new LaxRedirectStrategy())
            .build();

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private Response execute(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity) : "";
            int status = response.getStatusLine().getStatusCode();
            log.debug("{} {} -> {} ({} chars)", request.getMethod(), request.getURI(), status, body.length());
            return new Response(status, body);
        }
    }

    private HttpPost buildFormPost(String path, List<NameValuePair> params) {
        HttpPost request = new HttpPost(baseUrl + path);
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return request;
    }

    public int getStatus(String path) throws IOException {
        return execute(new HttpGet(baseUrl + path)).status;
    }

    public Response get(String path) throws IOException {
        HttpGet request = new HttpGet(baseUrl + path);
        request.addHeader("Accept", "application/json");
        return execute(request);
    }

    public int postFormStatus(String path, List<NameValuePair> params) throws IOException {
        return execute(buildFormPost(path, params)).status;
    }

    /**
     * GET as a JSON array; throws on a non-2xx status. Only for HTTP 200, a non-array body
     * (the gateway sends {@code {}} for empty arrays) yields an empty array.
     */
    public JsonArray getJsonArray(String path) throws IOException {
        Response response = get(path);
        if (response.status < 200 || response.status > 299) {
            throw new IllegalStateException("GET " + path + " returned HTTP " + response.status
                    + ": " + response.body.trim());
        }
        JsonElement element = JsonParser.parseString(response.body);
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        if (response.status == 200) {
            return new JsonArray();
        }
        throw new IllegalStateException("GET " + path + " returned HTTP " + response.status
                + " with a non-array body: " + response.body.trim());
    }

    /** Cookie with the given name, or {@code null}. */
    public Cookie findCookie(String name) {
        for (Cookie cookie : cookieStore.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    public void clearCookies() {
        cookieStore.clear();
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
