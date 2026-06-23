package giis.socialnetwork.e2e.functional.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Base class for Social Network API test suite. Provides HTTP infrastructure for
 * form-encoded POST and GET requests, cookie-based JWT session management, and
 * fixture helpers to register/login users, compose posts and manage social graph.
 */
public class BaseApiClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseApiClass.class);

    protected static CloseableHttpClient httpClient;
    protected static BasicCookieStore cookieStore;
    protected static String sutUrl;
    protected static String tJobName;

    @BeforeAll
    static void setupAll() throws IOException {
        log.info("Starting API test global setup");
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("src/test/resources/test.properties")));
        tJobName = System.getProperty("TJOB_NAME");
        String envUrl = System.getProperty("SUT_URL") != null
                ? System.getProperty("SUT_URL")
                : System.getenv("SUT_URL");
        sutUrl = envUrl != null ? envUrl : properties.getProperty("LOCALHOST_URL");
        log.info("API base URL: {}", sutUrl);
        cookieStore = new BasicCookieStore();
        httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            log.info("Shared HTTP client closed");
        }
    }

    // ── URL builders ──────────────────────────────────────────────────────────

    protected String userUrl(String path) { return sutUrl + "/api/user" + path; }
    protected String wrk2PostUrl(String path) { return sutUrl + "/wrk2-api/post" + path; }
    protected String wrk2UserTimelineUrl(String path) { return sutUrl + "/wrk2-api/user-timeline" + path; }
    protected String wrk2HomeTimelineUrl(String path) { return sutUrl + "/wrk2-api/home-timeline" + path; }

    // ── HTTP primitives ───────────────────────────────────────────────────────

    protected String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String body = entity != null ? EntityUtils.toString(entity) : "";
        log.debug("GET {} -> {} ({} chars)", url, response.getStatusLine().getStatusCode(), body.length());
        return body;
    }

    protected int getStatus(String url) throws IOException {
        return statusOf(new HttpGet(url));
    }

    protected String postForm(String url, List<NameValuePair> params) throws IOException {
        HttpPost request = buildFormPost(url, params);
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String body = entity != null ? EntityUtils.toString(entity) : "";
        log.debug("POST {} -> {} ({} chars)", url, response.getStatusLine().getStatusCode(), body.length());
        return body;
    }

    protected int postFormStatus(String url, List<NameValuePair> params) throws IOException {
        return statusOf(buildFormPost(url, params));
    }

    private static HttpPost buildFormPost(String url, List<NameValuePair> params) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return request;
    }

    private int statusOf(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            EntityUtils.consume(response.getEntity());
            int status = response.getStatusLine().getStatusCode();
            log.debug("{} {} -> {}", request.getMethod(), request.getURI(), status);
            return status;
        }
    }

    protected JsonObject getJsonObject(String url) throws IOException {
        return JsonParser.parseString(get(url)).getAsJsonObject();
    }

    protected JsonArray getJsonArray(String url) throws IOException {
        JsonElement element = JsonParser.parseString(get(url));
        return element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    /**
     * Returns {@code true} if any element of {@code array} is an object whose
     * {@code fieldName} property equals {@code expected}.
     */
    protected static boolean containsByField(JsonArray array, String fieldName, String expected) {
        for (JsonElement element : array) {
            if (element.isJsonObject()
                    && expected.equals(element.getAsJsonObject().get(fieldName).getAsString())) {
                return true;
            }
        }
        return false;
    }

    protected static long unique() {
        return System.currentTimeMillis();
    }

    // ── Payload builders (form params) ────────────────────────────────────────

    protected static List<NameValuePair> registerPayload(String firstName, String lastName,
                                                          String username, String password) {
        return Arrays.asList(
                new BasicNameValuePair("first_name", firstName),
                new BasicNameValuePair("last_name", lastName),
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("password", password)
        );
    }

    protected static List<NameValuePair> loginPayload(String username, String password) {
        return Arrays.asList(
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("password", password)
        );
    }

    protected static List<NameValuePair> composePostPayload(String username, long userId, String text) {
        return Arrays.asList(
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("user_id", String.valueOf(userId)),
                new BasicNameValuePair("text", text),
                new BasicNameValuePair("media_ids", "[]"),
                new BasicNameValuePair("media_types", "[]"),
                new BasicNameValuePair("post_type", "0")
        );
    }

    protected static List<NameValuePair> followPayload(String userName, String followeeName) {
        return Arrays.asList(
                new BasicNameValuePair("user_name", userName),
                new BasicNameValuePair("followee_name", followeeName)
        );
    }

    protected static List<NameValuePair> unfollowPayload(String userName, String followeeName) {
        return Arrays.asList(
                new BasicNameValuePair("user_name", userName),
                new BasicNameValuePair("followee_name", followeeName)
        );
    }

    // ── Fixture helpers ───────────────────────────────────────────────────────

    /**
     * Registers a new user via {@code POST /api/user/register}. Returns HTTP 200
     * on success (server redirects to index.html after registration).
     */
    protected int registerUser(String firstName, String lastName,
                                String username, String password) throws IOException {
        int status = postFormStatus(userUrl("/register"),
                registerPayload(firstName, lastName, username, password));
        log.debug("Registered user '{}': HTTP {}", username, status);
        return status;
    }

    /**
     * Logs in as the given user via {@code POST /api/user/login}. Stores the
     * {@code login_token} JWT cookie in the shared cookie store. Returns the
     * numeric {@code user_id} extracted from the JWT payload.
     */
    protected long loginUser(String username, String password) throws IOException {
        cookieStore.clear();
        postForm(userUrl("/login"), loginPayload(username, password));
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("login_token".equals(cookie.getName())) {
                long userId = parseUserIdFromJwt(cookie.getValue());
                log.debug("Logged in as '{}', user_id={}", username, userId);
                return userId;
            }
        }
        throw new IllegalStateException("No login_token cookie after login for user: " + username);
    }

    /**
     * Convenience fixture: derives username/password from {@code label + unique()},
     * registers the user, logs in, and returns the assigned {@code user_id}.
     */
    protected long createUser(String label) throws IOException {
        long ts = unique();
        String username = (label + ts).toLowerCase().replaceAll("[^a-z0-9]", "");
        String password = "pwd" + ts;
        registerUser(label, label + "Ln", username, password);
        return loginUser(username, password);
    }

    /**
     * Same as {@link #createUser(String)} but also returns the generated username
     * so the caller can reference it in social-graph operations.
     */
    protected String[] createUserWithName(String label) throws IOException {
        long ts = unique();
        String username = (label + ts).toLowerCase().replaceAll("[^a-z0-9]", "");
        String password = "pwd" + ts;
        registerUser(label, label + "Ln", username, password);
        long userId = loginUser(username, password);
        return new String[]{username, String.valueOf(userId), password};
    }

    /**
     * Composes a post via {@code POST /wrk2-api/post/compose}. Returns HTTP 200
     * on success with body "Successfully upload post".
     */
    protected int composePost(String username, long userId, String text) throws IOException {
        int status = postFormStatus(wrk2PostUrl("/compose"),
                composePostPayload(username, userId, text));
        log.debug("Composed post for user '{}': HTTP {}", username, status);
        return status;
    }

    /**
     * Follows {@code followeeName} as {@code userName} via
     * {@code POST /api/user/follow}. No authentication required.
     * Returns HTTP 200 on success (server redirects to contact.html).
     */
    protected int followUser(String userName, String followeeName) throws IOException {
        int status = postFormStatus(userUrl("/follow"), followPayload(userName, followeeName));
        log.debug("'{}' follows '{}': HTTP {}", userName, followeeName, status);
        return status;
    }

    /**
     * Unfollows {@code followeeName} as {@code userName} via
     * {@code POST /api/user/unfollow}. No authentication required.
     * Returns HTTP 200 on success.
     */
    protected int unfollowUser(String userName, String followeeName) throws IOException {
        int status = postFormStatus(userUrl("/unfollow"), unfollowPayload(userName, followeeName));
        log.debug("'{}' unfollows '{}': HTTP {}", userName, followeeName, status);
        return status;
    }

    /**
     * Reads the follower id list of the currently logged-in user via
     * {@code GET /api/user/get_follower} (requires a valid {@code login_token} cookie).
     */
    protected JsonArray getFollowers() throws IOException {
        return getJsonArray(userUrl("/get_follower"));
    }

    /**
     * Reads the followee id list of the currently logged-in user via
     * {@code GET /api/user/get_followee} (requires a valid {@code login_token} cookie).
     */
    protected JsonArray getFollowees() throws IOException {
        return getJsonArray(userUrl("/get_followee"));
    }

    /**
     * Attempts a login and reports whether the server issued a {@code login_token}
     * cookie, without throwing when it did not. Used to assert authentication failures.
     */
    protected boolean loginSetsToken(String username, String password) throws IOException {
        cookieStore.clear();
        postForm(userUrl("/login"), loginPayload(username, password));
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("login_token".equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decodes the {@code user_id} field from a JWT token payload without verifying
     * the signature — safe for test fixtures where the secret is known.
     */
    private static long parseUserIdFromJwt(String jwt) {
        String payload = jwt.split("\\.")[1];
        int padding = (4 - payload.length() % 4) % 4;
        payload = payload + "===".substring(0, padding);
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        return JsonParser.parseString(json).getAsJsonObject().get("user_id").getAsLong();
    }
}
