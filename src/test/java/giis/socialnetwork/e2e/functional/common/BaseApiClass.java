package giis.socialnetwork.e2e.functional.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Base class for Social Network API test suite. Provides HTTP infrastructure for
 * form-encoded POST and GET requests, cookie-based JWT session management, and
 * fixture helpers to register/login users, compose posts and manage social graph.
 */
public class BaseApiClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseApiClass.class);
    protected static final String READPATH = "/read";
    protected static CloseableHttpClient httpClient;
    protected static BasicCookieStore cookieStore;
    protected static String sutUrl;

    @BeforeAll
    static void setupAll() throws IOException {
        log.info("Starting API test global setup");
        sutUrl = SutConfig.resolveSutUrl();
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

    // ── HTTP primitives ───────────────────────────────────────────────────────

    /** Executes the request, discards the body, and returns the HTTP status code. */
    private static int executeForStatus(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            EntityUtils.consume(response.getEntity());
            int status = response.getStatusLine().getStatusCode();
            log.debug("{} {} -> {}", request.getMethod(), request.getURI(), status);
            return status;
        }
    }

    /** Executes the request and returns the response body (empty string if none). */
    private static String executeForBody(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity) : "";
            log.debug("{} {} -> {} ({} chars)", request.getMethod(), request.getURI(),
                    response.getStatusLine().getStatusCode(), body.length());
            return body;
        }
    }

    private static HttpPost buildFormPost(String url, List<NameValuePair> params) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return request;
    }

    protected int getStatus(String url) throws IOException {
        return executeForStatus(new HttpGet(url));
    }

    protected String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");
        return executeForBody(request);
    }

    protected int postFormStatus(String url, List<NameValuePair> params) throws IOException {
        return executeForStatus(buildFormPost(url, params));
    }

    protected String postForm(String url, List<NameValuePair> params) throws IOException {
        return executeForBody(buildFormPost(url, params));
    }

    /** Parses the GET response as a JSON array, tolerating non-array bodies (e.g. an
     *  empty home timeline serialises as {@code {}}) by returning an empty array. */
    protected JsonArray getJsonArray(String url) throws IOException {
        JsonElement element = JsonParser.parseString(get(url));
        return element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    // ── URL builders ──────────────────────────────────────────────────────────

    protected String userUrl(String path) {
        return sutUrl + "/api/user" + path;
    }

    protected String wrk2PostUrl(String path) {
        return sutUrl + "/wrk2-api/post" + path;
    }

    protected String wrk2UserTimelineUrl(String path) {
        return sutUrl + "/wrk2-api/user-timeline" + path;
    }

    protected String wrk2HomeTimelineUrl(String path) {
        return sutUrl + "/wrk2-api/home-timeline" + path;
    }

    /** Builds a timeline read URL ({@code base} is a user- or home-timeline read endpoint). */
    protected String timelineReadUrl(String base, long userId, int start, int stop) {
        return base + "?user_id=" + userId + "&start=" + start + "&stop=" + stop;
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

    /** Shared by follow and unfollow — both endpoints take the same form fields. */
    protected static List<NameValuePair> followFormPayload(String userName, String followeeName) {
        return Arrays.asList(
                new BasicNameValuePair("user_name", userName),
                new BasicNameValuePair("followee_name", followeeName)
        );
    }

    protected static List<NameValuePair> composePostPayload(String username, long userId, String text,
                                                            String mediaIds, String mediaTypes) {
        return Arrays.asList(
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("user_id", String.valueOf(userId)),
                new BasicNameValuePair("text", text),
                new BasicNameValuePair("media_ids", mediaIds),
                new BasicNameValuePair("media_types", mediaTypes),
                new BasicNameValuePair("post_type", "0")
        );
    }

    // ── Fixture helpers ───────────────────────────────────────────────────────

    protected static long unique() {
        return System.currentTimeMillis();
    }

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
     * Clears the cookie store, posts the login form, and returns the
     * {@code login_token} cookie issued by the server, or {@code null} when
     * authentication failed and no token was set.
     */
    private static Cookie loginAndGetToken(String username, String password) throws IOException {
        cookieStore.clear();
        executeForBody(buildFormPost(sutUrl + "/api/user/login", loginPayload(username, password)));
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("login_token".equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * Logs in as the given user via {@code POST /api/user/login}. Stores the
     * {@code login_token} JWT cookie in the shared cookie store. Returns the
     * numeric {@code user_id} extracted from the JWT payload.
     */
    protected long loginUser(String username, String password) throws IOException {
        Cookie token = loginAndGetToken(username, password);
        if (token == null) {
            throw new IllegalStateException("No login_token cookie after login for user: " + username);
        }
        long userId = parseUserIdFromJwt(token.getValue());
        log.debug("Logged in as '{}', user_id={}", username, userId);
        return userId;
    }

    /**
     * Attempts a login and reports whether the server issued a {@code login_token}
     * cookie, without throwing when it did not. Used to assert authentication failures.
     */
    protected boolean loginSetsToken(String username, String password) throws IOException {
        return loginAndGetToken(username, password) != null;
    }

    /**
     * Convenience fixture: derives a unique username/password from {@code label},
     * registers the user and logs in. Returns {@code {username, userId, password}}
     * so the caller can reference them in social-graph operations.
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
        return composePost(username, userId, text, "[]", "[]");
    }

    /**
     * Composes a post with media attachments. {@code mediaIds}/{@code mediaTypes}
     * are JSON arrays as the wrk2 API expects them, e.g.
     * {@code ["123456789012345678"]} / {@code ["png"]}.
     */
    protected int composePost(String username, long userId, String text,
                              String mediaIds, String mediaTypes) throws IOException {
        int status = postFormStatus(wrk2PostUrl("/compose"),
                composePostPayload(username, userId, text, mediaIds, mediaTypes));
        log.debug("Composed post for user '{}': HTTP {}", username, status);
        return status;
    }

    /**
     * Follows {@code followeeName} as {@code userName} via
     * {@code POST /api/user/follow}. No authentication required.
     * Returns HTTP 200 on success (server redirects to contact.html).
     */
    protected int followUser(String userName, String followeeName) throws IOException {
        int status = postFormStatus(userUrl("/follow"), followFormPayload(userName, followeeName));
        log.debug("'{}' follows '{}': HTTP {}", userName, followeeName, status);
        return status;
    }

    /**
     * Unfollows {@code followeeName} as {@code userName} via
     * {@code POST /api/user/unfollow}. No authentication required.
     * Returns HTTP 200 on success.
     */
    protected int unfollowUser(String userName, String followeeName) throws IOException {
        int status = postFormStatus(userUrl("/unfollow"), followFormPayload(userName, followeeName));
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

    // ── Assertion helpers ─────────────────────────────────────────────────────

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
