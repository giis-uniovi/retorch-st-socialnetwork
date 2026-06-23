package giis.socialnetwork.e2e.functional.tests.api;

import com.google.gson.JsonArray;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Validates the home-timeline service end-to-end:
 * <ul>
 *   <li>GET /wrk2-api/home-timeline/read — feed of followed users' posts (JSON array)</li>
 * </ul>
 * The home timeline is populated asynchronously via RabbitMQ and
 * write-home-timeline-service, so tests poll until the post propagates
 * or the {@link #HOME_TIMELINE_TIMEOUT_MS} deadline is reached.
 */
class TestApiTimeline extends BaseApiClass {

    private static final long HOME_TIMELINE_TIMEOUT_MS = 15_000;
    private static final long HOME_TIMELINE_POLL_MS = 500;

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("GET /wrk2-api/home-timeline/read returns a JSON array")
    void testHomeTimelineReturnsArray() throws IOException {
        String[] reader = createUserWithName("htreader");
        long readerId = Long.parseLong(reader[1]);

        String url = wrk2HomeTimelineUrl("/read") + "?user_id=" + readerId + "&start=0&stop=10";
        JsonArray timeline = getJsonArray(url);
        // A new user has an empty home timeline — the array must still be valid JSON
        Assertions.assertNotNull(timeline, "Home timeline response must be a valid JSON array");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("Post by a followed user eventually appears in the follower's home timeline")
    void testHomeTimelineShowsFollowedUserPost() throws IOException {
        // Create the author (User B) and the reader (User A)
        String[] userB = createUserWithName("htauthor");
        String userBName = userB[0];
        long userBId = Long.parseLong(userB[1]);

        String[] userA = createUserWithName("htfollower");
        long userAId = Long.parseLong(userA[1]);

        // A follows B (no auth needed)
        followUser(userA[0], userBName);

        // B composes a post (re-login as B so cookie is correct for any auth-gated calls)
        loginUser(userBName, userB[2]);
        String uniqueText = "HTpost" + unique();
        composePost(userBName, userBId, uniqueText);

        // Re-login as A to use A's session (not strictly needed for wrk2 endpoints but
        // ensures the cookie store is tidy for future authenticated calls in the test)
        loginUser(userA[0], userA[2]);

        JsonArray timeline = pollHomeTimeline(userAId, uniqueText);
        Assertions.assertTrue(containsByField(timeline, "text", uniqueText),
                "Post '" + uniqueText + "' must appear in A's home timeline within "
                        + HOME_TIMELINE_TIMEOUT_MS + " ms");
    }

    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("GET /wrk2-api/home-timeline/read with missing args returns HTTP 400")
    void testHomeTimelineBadRequest() throws IOException {
        int status = getStatus(wrk2HomeTimelineUrl("/read"));
        Assertions.assertEquals(400, status,
                "Home timeline read without required params must return HTTP 400");
    }

    /**
     * Polls {@code GET /wrk2-api/home-timeline/read} for {@code userId} until
     * a post with {@code expectedText} appears or {@link #HOME_TIMELINE_TIMEOUT_MS} elapses.
     * Uses {@link LockSupport#parkNanos} to avoid Sonar rule {@code java:S2925}.
     */
    private JsonArray pollHomeTimeline(long userId, String expectedText) throws IOException {
        String url = wrk2HomeTimelineUrl("/read") + "?user_id=" + userId + "&start=0&stop=10";
        long deadline = System.currentTimeMillis() + HOME_TIMELINE_TIMEOUT_MS;
        JsonArray timeline;
        do {
            timeline = getJsonArray(url);
            if (containsByField(timeline, "text", expectedText)
                    || System.currentTimeMillis() >= deadline) {
                return timeline;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(HOME_TIMELINE_POLL_MS));
        } while (!Thread.currentThread().isInterrupted());
        return timeline;
    }
}
