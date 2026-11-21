package giis.socialnetwork.e2e.functional.tests.api;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.ApiUser;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import giis.socialnetwork.e2e.functional.dto.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertNoneMatch;
import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertStatus;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_OK;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** API tests for {@code GET /wrk2-api/home-timeline/read}; the feed is filled asynchronously, so tests poll. */
@Tag("api")
class TestApiTimeline extends BaseApiClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelineReturnsArray")
    void testAPIHomeTimelineReturnsArray() throws IOException {
        ApiUser reader = createUser("htreader");

        List<Post> timeline = readHomeTimeline(reader.userId);
        assertEquals(0, timeline.size(), "A fresh user's home timeline must be empty");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelineShowsFollowedUserPost")
    void testAPIHomeTimelineShowsFollowedUserPost() throws IOException {
        ApiUser author = createUser("htauthor");
        ApiUser follower = createUser("htfollower");

        followOrFail(follower, author);

        String uniqueText = "HTpost" + unique();
        composePostOrFail(author, uniqueText);

        awaitHomeTimelinePost(follower.userId, uniqueText);
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelineExcludesOwnPosts")
    void testAPIHomeTimelineExcludesOwnPosts() throws IOException {
        ApiUser author = createUser("ownauthor");
        ApiUser follower = addFollower(author);

        String uniqueText = "OwnPost" + unique();
        composePostOrFail(author, uniqueText);

        awaitHomeTimelinePost(follower.userId, uniqueText);

        List<Post> authorTimeline = readHomeTimeline(author.userId);
        assertNoneMatch(authorTimeline, p -> uniqueText.equals(p.getText()),
                "The home timeline must not include the reader's own posts");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelinePagination")
    void testAPIHomeTimelinePagination() throws IOException {
        ApiUser author = createUser("pgauthor");
        ApiUser follower = addFollower(author);

        for (int i = 1; i <= 3; i++) {
            composePostOrFail(author, "HomePage post " + i + " " + author.username);
        }

        List<Post> all = awaitHomeTimeline(follower.userId, "all three posts", timeline -> timeline.size() >= 3);
        assertEquals(3, all.size(), "The home timeline must contain exactly the three posts");

        List<Post> firstPage = readHomeTimeline(follower.userId, 0, 2);
        List<Post> secondPage = readHomeTimeline(follower.userId, 2, 10);
        assertAll(
                () -> assertEquals(2, firstPage.size(),
                        "start=0&stop=2 must return exactly two posts (stop is exclusive)"),
                () -> assertEquals(1, secondPage.size(), "start=2&stop=10 must return the remaining post"),
                () -> assertEquals("HomePage post 3 " + author.username,
                        all.get(0).getText(),
                        "The home timeline must be ordered newest first"),
                () -> assertEquals("HomePage post 1 " + author.username, secondPage.get(0).getText(),
                        "The second page must contain the oldest post only")
        );
    }

    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelineBadRequest")
    void testAPIHomeTimelineBadRequest() throws IOException {
        int status = anonymous.homeTimelineStatusWithoutParams();
        assertStatus(HTTP_BAD_REQUEST, status, "Home timeline read without required params");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelineStopsAfterUnfollow")
    void testAPIHomeTimelineStopsAfterUnfollow() throws IOException {
        ApiUser author = createUser("unfauthor");
        ApiUser leaver = addFollower(author);
        ApiUser stayer = addFollower(author);

        assertStatus(HTTP_OK, unfollowUser(leaver.username, author.username), "Unfollow");

        String uniqueText = "AfterUnfollow" + unique();
        composePostOrFail(author, uniqueText);

        // Wait for the remaining follower first, so the negative check on the leaver is not a blind wait.
        awaitHomeTimelinePost(stayer.userId, uniqueText);
        assertNoneMatch(readHomeTimeline(leaver.userId), p -> uniqueText.equals(p.getText()),
                "A post composed after the unfollow must not reach the former follower");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "home-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIHomeTimelineMergesTwoAuthorsNewestFirst")
    void testAPIHomeTimelineMergesTwoAuthorsNewestFirst() throws IOException {
        ApiUser first = createUser("mergea");
        ApiUser second = createUser("mergeb");
        ApiUser reader = createUser("mergereader");
        followOrFail(reader, first);
        followOrFail(reader, second);

        String firstText = "FromFirst" + unique();
        String secondText = "FromSecond" + unique();
        composePostOrFail(first, firstText);
        awaitHomeTimelinePost(reader.userId, firstText);
        composePostOrFail(second, secondText);

        List<Post> timeline = awaitHomeTimeline(reader.userId, "both posts", t -> t.size() >= 2);
        assertAll(
                () -> assertEquals(secondText, timeline.get(0).getText(), "The newest post must come first"),
                () -> assertEquals(firstText, timeline.get(1).getText(), "The older post must come second")
        );
    }
}
