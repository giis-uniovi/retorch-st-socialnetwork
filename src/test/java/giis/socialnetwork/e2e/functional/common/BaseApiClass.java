package giis.socialnetwork.e2e.functional.common;

import giis.socialnetwork.e2e.functional.dto.Post;
import giis.socialnetwork.e2e.functional.utils.ApiAssert;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertStatus;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

/** Base class for API tests: tracks sessions, closes them after the class, and provides fixtures. */
public class BaseApiClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseApiClass.class);
    protected static final long HOME_TIMELINE_TIMEOUT_MS = 15_000;
    protected static final long HOME_TIMELINE_POLL_MS = 500;
    private static final int DEFAULT_PAGE_STOP = 10;
    private static final List<SocialNetworkApi> SESSIONS = new ArrayList<SocialNetworkApi>();

    protected static String sutUrl;
    /** Cookie-less session for unauthenticated calls. */
    protected static SocialNetworkApi anonymous;

    @BeforeAll
    static void setupAll() throws IOException {
        log.info("Starting API test global setup");
        sutUrl = SutConfig.resolveSutUrl();
        log.info("API base URL: {}", sutUrl);
        anonymous = newSession();
    }

    @AfterAll
    static void tearDownAll() {
        synchronized (SESSIONS) {
            for (SocialNetworkApi session : SESSIONS) {
                session.close();
            }
            SESSIONS.clear();
        }
        log.info("API sessions closed");
    }

    protected static long unique() {
        return Unique.next();
    }

    protected static SocialNetworkApi newSession() {
        SocialNetworkApi session = new SocialNetworkApi(sutUrl);
        synchronized (SESSIONS) {
            SESSIONS.add(session);
        }
        return session;
    }

    /** Registers a unique user and logs in with a dedicated session. */
    protected ApiUser createUser(String label) throws IOException {
        TestUser user = TestUser.generate(label);
        SocialNetworkApi session = newSession();
        registerOrFail(session, user, label, label + "Ln");
        long userId = session.login(user.username, user.password);
        return new ApiUser(user.username, user.password, userId, session);
    }

    /** Registers {@code user} (anonymous session) and fails the arrangement unless HTTP 200. */
    protected void registerOrFail(TestUser user, String firstName, String lastName) throws IOException {
        registerOrFail(anonymous, user, firstName, lastName);
    }

    private static void registerOrFail(SocialNetworkApi session, TestUser user, String firstName, String lastName)
            throws IOException {
        assertStatus(HTTP_OK, session.register(firstName, lastName, user.username, user.password),
                "Arrange: registering user '" + user.username + "'");
    }

    protected ApiUser addFollower(ApiUser author) throws IOException {
        ApiUser follower = createUser("follower");
        followOrFail(follower, author);
        return follower;
    }

    /** Author with one follower: compose returns HTTP 500 (empty ZADD) unless the author has a follower first. */
    protected ApiUser createAuthorWithFollower(String label) throws IOException {
        ApiUser author = createUser(label);
        addFollower(author);
        return author;
    }

    protected void followOrFail(ApiUser follower, ApiUser followee) throws IOException {
        assertStatus(HTTP_OK, followUser(follower.username, followee.username),
                "Arrange: '" + follower.username + "' following '" + followee.username + "'");
    }

    protected void composePostOrFail(ApiUser author, String text) throws IOException {
        assertStatus(HTTP_OK, composePost(author, text),
                "Arrange: composing a post as '" + author.username + "'");
    }

    protected void composePostOrFail(ApiUser author, String text, String mediaIds, String mediaTypes)
            throws IOException {
        assertStatus(HTTP_OK, composePost(author, text, mediaIds, mediaTypes),
                "Arrange: composing a post with media as '" + author.username + "'");
    }

    /** Polls the home timeline until it contains a post with {@code text}; fails with the last read on timeout. */
    protected List<Post> awaitHomeTimelinePost(long userId, String text) {return awaitHomeTimeline(userId, "a post with text '" + text + "'",
                timeline -> ApiAssert.anyMatch(timeline, p -> text.equals(p.getText())));
    }

    /** Polls the home timeline until {@code done} holds; fails with the last read on timeout. */
    protected List<Post> awaitHomeTimeline(long userId, String expectation, Predicate<List<Post>> done) {
        AtomicReference<List<Post>> last = new AtomicReference<List<Post>>();
        try {
            await().atMost(Duration.ofMillis(HOME_TIMELINE_TIMEOUT_MS))
                    .pollInterval(Duration.ofMillis(HOME_TIMELINE_POLL_MS))
                    .until(() -> {
                        last.set(readHomeTimeline(userId));
                        return done.test(last.get());
                    });
        } catch (ConditionTimeoutException e) {
            fail("Home timeline of user " + userId + " must contain " + expectation + " within "
                    + HOME_TIMELINE_TIMEOUT_MS + " ms, last read: " + last.get());
        }
        return last.get();
    }

    protected int composePost(ApiUser author, String text) throws IOException {
        return author.api.compose(author.username, author.userId, text);
    }

    protected int composePost(ApiUser author, String text, String mediaIds, String mediaTypes) throws IOException {
        return author.api.compose(author.username, author.userId, text, mediaIds, mediaTypes);
    }

    protected int followUser(String userName, String followeeName) throws IOException {return anonymous.follow(userName, followeeName);}
    protected int unfollowUser(String userName, String followeeName) throws IOException {return anonymous.unfollow(userName, followeeName);}
    protected int registerUser(String firstName, String lastName, String username, String password) throws IOException {return anonymous.register(firstName, lastName, username, password);}
    protected long loginUser(String username, String password) throws IOException {return newSession().login(username, password);}
    protected boolean loginSetsToken(String username, String password) throws IOException {return newSession().tryLogin(username, password).isPresent();}

    protected List<Post> readUserTimeline(long userId) throws IOException {return readUserTimeline(userId, 0, DEFAULT_PAGE_STOP);}
    protected List<Post> readUserTimeline(long userId, int start, int stop) throws IOException {return anonymous.userTimeline(userId, start, stop);}
    protected List<Post> readHomeTimeline(long userId) throws IOException {return readHomeTimeline(userId, 0, DEFAULT_PAGE_STOP);}
    protected List<Post> readHomeTimeline(long userId, int start, int stop) throws IOException {return anonymous.homeTimeline(userId, start, stop);}
}
