package giis.socialnetwork.e2e.functional.common;

import com.google.gson.JsonParser;
import giis.socialnetwork.e2e.functional.dto.ApiJson;
import giis.socialnetwork.e2e.functional.dto.Followee;
import giis.socialnetwork.e2e.functional.dto.Follower;
import giis.socialnetwork.e2e.functional.dto.Post;
import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.OptionalLong;

/** Typed facade over the Social Network HTTP API; one instance is one session (the login cookie is per instance). */
public class SocialNetworkApi implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(SocialNetworkApi.class);

    private static final String REGISTER = "/api/user/register";
    private static final String LOGIN = "/api/user/login";
    private static final String FOLLOW = "/api/user/follow";
    private static final String UNFOLLOW = "/api/user/unfollow";
    private static final String GET_FOLLOWER = "/api/user/get_follower";
    private static final String GET_FOLLOWEE = "/api/user/get_followee";
    private static final String COMPOSE = "/wrk2-api/post/compose";
    private static final String USER_TIMELINE_READ = "/wrk2-api/user-timeline/read";
    private static final String HOME_TIMELINE_READ = "/wrk2-api/home-timeline/read";
    private static final String LOGIN_COOKIE = "login_token";

    private final ApiClient client;

    public SocialNetworkApi(String sutUrl) {
        this.client = new ApiClient(sutUrl);
    }

    /** {@code POST /api/user/register}: 200 on success, 400 when a field is missing. */
    public int register(String firstName, String lastName, String username, String password) throws IOException {
        int status = register(registerPayload(firstName, lastName, username, password));
        log.debug("Registered user '{}': HTTP {}", username, status);
        return status;
    }

    public int register(List<NameValuePair> form) throws IOException {
        return client.postFormStatus(REGISTER, form);
    }

    /** Logs in and returns the {@code user_id} from the JWT; throws {@link IllegalStateException} if no token is issued. */
    public long login(String username, String password) throws IOException {
        OptionalLong userId = tryLogin(username, password);
        if (!userId.isPresent()) {
            throw new IllegalStateException("No login_token cookie after login for user: " + username);
        }
        log.debug("Logged in as '{}', user_id={}", username, userId.getAsLong());
        return userId.getAsLong();
    }

    /** Login attempt (clears cookies first); empty when no {@code login_token} cookie is set. */
    public OptionalLong tryLogin(String username, String password) throws IOException {
        client.clearCookies();
        client.postFormStatus(LOGIN, loginPayload(username, password));
        Cookie token = client.findCookie(LOGIN_COOKIE);
        return token == null ? OptionalLong.empty() : OptionalLong.of(parseUserIdFromJwt(token.getValue()));
    }

    /** Login status only; login.lua reports an authentication failure as HTTP 500. */
    public int loginStatus(String username, String password) throws IOException {return client.postFormStatus(LOGIN, loginPayload(username, password));}

    /** {@code POST /api/user/follow} (no auth required): 200 on success. */
    public int follow(String userName, String followeeName) throws IOException {
        int status = client.postFormStatus(FOLLOW, followFormPayload(userName, followeeName));
        log.debug("'{}' follows '{}': HTTP {}", userName, followeeName, status);
        return status;
    }

    /** {@code POST /api/user/unfollow} (no auth required): 200, also when the user was not followed. */
    public int unfollow(String userName, String followeeName) throws IOException {
        int status = client.postFormStatus(UNFOLLOW, followFormPayload(userName, followeeName));
        log.debug("'{}' unfollows '{}': HTTP {}", userName, followeeName, status);
        return status;
    }

    /** {@code GET /api/user/get_follower} for the logged-in user (needs the login cookie). */
    public List<Follower> followers() throws IOException {return ApiJson.parseList(client.getJsonArray(GET_FOLLOWER), Follower.class);}

    /** {@code GET /api/user/get_followee} for the logged-in user (needs the login cookie). */
    public List<Followee> followees() throws IOException {return ApiJson.parseList(client.getJsonArray(GET_FOLLOWEE), Followee.class);}

    /** Status of get_followee for the logged-in user; 500 when the user has no followee (known quirk). */
    public int followeesStatus() throws IOException {return client.getStatus(GET_FOLLOWEE);}

    /** Status of get_follower; 401 without a login cookie. */
    public int followersStatus() throws IOException {return client.getStatus(GET_FOLLOWER);}

    /** {@code POST /wrk2-api/post/compose}: 200 on success (HTTP 500 if the author has no follower). */
    public int compose(String username, long userId, String text) throws IOException {return compose(username, userId, text, "[]", "[]");}

    /** Compose with media; {@code mediaIds}/{@code mediaTypes} are JSON array strings, e.g. {@code ["png"]}. */
    public int compose(String username, long userId, String text, String mediaIds, String mediaTypes)
            throws IOException {
        int status = client.postFormStatus(COMPOSE, composePayload(username, userId, text, mediaIds, mediaTypes));
        log.debug("Composed post for user '{}': HTTP {}", username, status);
        return status;
    }

    /** User timeline, newest first, {@code stop} exclusive. */
    public List<Post> userTimeline(long userId, int start, int stop) throws IOException {
        return ApiJson.parseList(client.getJsonArray(timelinePath(USER_TIMELINE_READ, userId, start, stop)), Post.class);
    }

    /** Home timeline (followed users' posts), newest first; an empty-object body yields an empty list. */
    public List<Post> homeTimeline(long userId, int start, int stop) throws IOException {
        return ApiJson.parseList(client.getJsonArray(timelinePath(HOME_TIMELINE_READ, userId, start, stop)), Post.class);
    }

    /** Status without params (HTTP 400). */
    public int userTimelineStatusWithoutParams() throws IOException {return client.getStatus(USER_TIMELINE_READ);}

    /** Status without params (HTTP 400). */
    public int homeTimelineStatusWithoutParams() throws IOException {return client.getStatus(HOME_TIMELINE_READ);}

    @Override
    public void close() {
        client.close();
    }

    private static String timelinePath(String base, long userId, int start, int stop) {
        return base + "?user_id=" + userId + "&start=" + start + "&stop=" + stop;
    }

    public static List<NameValuePair> registerPayload(String firstName, String lastName,
                                                      String username, String password) {
        return Arrays.<NameValuePair>asList(
                new BasicNameValuePair("first_name", firstName),
                new BasicNameValuePair("last_name", lastName),
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("password", password));
    }

    private static List<NameValuePair> loginPayload(String username, String password) {
        return Arrays.<NameValuePair>asList(
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("password", password));
    }

    private static List<NameValuePair> followFormPayload(String userName, String followeeName) {
        return Arrays.<NameValuePair>asList(
                new BasicNameValuePair("user_name", userName),
                new BasicNameValuePair("followee_name", followeeName));
    }

    private static List<NameValuePair> composePayload(String username, long userId, String text,
                                                      String mediaIds, String mediaTypes) {
        return Arrays.<NameValuePair>asList(
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("user_id", String.valueOf(userId)),
                new BasicNameValuePair("text", text),
                new BasicNameValuePair("media_ids", mediaIds),
                new BasicNameValuePair("media_types", mediaTypes),
                new BasicNameValuePair("post_type", "0"));
    }

    /** Reads {@code user_id} from the JWT payload (signature not verified). */
    static long parseUserIdFromJwt(String jwt) {
        String payload = jwt.split("\\.")[1];
        int padding = (4 - payload.length() % 4) % 4;
        payload = payload + "===".substring(0, padding);
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        return JsonParser.parseString(json).getAsJsonObject().get("user_id").getAsLong();
    }
}
