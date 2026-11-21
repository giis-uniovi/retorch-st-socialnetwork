package giis.socialnetwork.e2e.functional.tests.api;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.ApiUser;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import giis.socialnetwork.e2e.functional.dto.Post;
import giis.socialnetwork.e2e.functional.dto.Url;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertAnyMatch;
import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertNoneMatch;
import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertStatus;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_OK;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** API tests for {@code POST /wrk2-api/post/compose} and {@code GET /wrk2-api/user-timeline/read}. */
@Tag("api")
class TestApiPosts extends BaseApiClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIComposePost")
    void testAPIComposePost() throws IOException {
        ApiUser author = createAuthorWithFollower("compose");

        int status = composePost(author, "Hello from API test " + unique());
        assertStatus(HTTP_OK, status, "Compose post");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIReadUserTimeline")
    void testAPIReadUserTimeline() throws IOException {
        ApiUser user = createUser("timeline");
        String postText = "Timeline test post " + unique();

        // No social-graph resource in @AccessMode, so no follower is arranged and the compose status is ignored.
        composePost(user, postText);

        List<Post> timeline = readUserTimeline(user.userId);
        assertFalse(timeline.isEmpty(), "User timeline must contain at least one post after composing");

        Post post = timeline.get(0);
        assertAll(
                () -> assertNotNull(post.getPostId(), "Post must have 'post_id'"),
                () -> assertNotNull(post.getCreator(), "Post must have 'creator'"),
                () -> assertNotNull(post.getText(), "Post must have 'text'"),
                () -> assertNotNull(post.getTimestamp(), "Post must have 'timestamp'")
        );
        assertEquals(user.username, post.getCreator().getUsername(),
                "Post creator username must match the author");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostWithMentionPopulatesUserMentions")
    void testAPIComposePostWithMentionPopulatesUserMentions() throws IOException {
        ApiUser author = createUser("mentionauthor");
        ApiUser mentioned = createUser("mentioned");

        // No social-graph resource in @AccessMode, so no follower is arranged and the compose status is ignored.
        composePost(author, "hi @" + mentioned.username + " " + unique());

        List<Post> timeline = readUserTimeline(author.userId);
        assertFalse(timeline.isEmpty(), "Author timeline must contain the composed post");

        assertAnyMatch(timeline.get(0).getUserMentions(), m -> mentioned.username.equals(m.getUsername()),
                "Mentioned user '" + mentioned.username + "' must appear in the post's user_mentions");
    }

    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIReadUserTimelineBadRequest")
    void testAPIReadUserTimelineBadRequest() throws IOException {
        int status = anonymous.userTimelineStatusWithoutParams();
        assertStatus(HTTP_BAD_REQUEST, status, "Timeline read without required params");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIUserTimelinePagination")
    void testAPIUserTimelinePagination() throws IOException {
        ApiUser author = createAuthorWithFollower("paging");

        for (int i = 1; i <= 3; i++) {
            composePostOrFail(author, "Page post " + i + " " + author.username);
        }

        List<Post> all = readUserTimeline(author.userId, 0, 10);
        List<Post> firstPage = readUserTimeline(author.userId, 0, 2);
        List<Post> secondPage = readUserTimeline(author.userId, 2, 10);

        assertAll(
                () -> assertEquals(3, all.size(), "Full read must return all three posts"),
                () -> assertEquals(2, firstPage.size(),
                        "start=0&stop=2 must return exactly two posts (stop is exclusive)"),
                () -> assertEquals(1, secondPage.size(),
                        "start=2&stop=10 must return the remaining post"),
                () -> assertEquals("Page post 3 " + author.username,
                        all.get(0).getText(),
                        "The timeline must be ordered newest first"),
                () -> assertEquals("Page post 1 " + author.username,
                        secondPage.get(0).getText(),
                        "The second page must contain the oldest post only")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostWithMediaAttachesMedia")
    void testAPIComposePostWithMediaAttachesMedia() throws IOException {
        ApiUser author = createAuthorWithFollower("media");

        String mediaId = "123456789012345678";
        int status = composePost(author, "Post with media " + unique(),
                "[\"" + mediaId + "\"]", "[\"png\"]");
        assertStatus(HTTP_OK, status, "Compose with media");

        List<Post> timeline = readUserTimeline(author.userId);
        assertFalse(timeline.isEmpty(), "Timeline must contain the composed post");
        assertAnyMatch(timeline.get(0).getMedia(), m -> mediaId.equals(m.getMediaId()), "The post's media list must contain the attached media id");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostShortensUrls")
    void testAPIComposePostShortensUrls() throws IOException {
        ApiUser author = createAuthorWithFollower("urls");

        composePostOrFail(author, "check http://example.com/page" + unique() + " out");

        List<Post> timeline = readUserTimeline(author.userId);
        assertFalse(timeline.isEmpty(), "Timeline must contain the composed post");
        Post post = timeline.get(0);
        List<Url> urls = post.getUrls();
        assertFalse(urls.isEmpty(), "The post must contain a shortened URL entry");
        String shortened = urls.get(0).getShortenedUrl();
        assertAll(
                () -> assertTrue(shortened.startsWith("https://short-url/"),
                        "url-shorten-service must rewrite the link to the short-url host, got: " + shortened),
                () -> assertTrue(post.getText().contains(shortened),
                        "The post text must contain the shortened URL instead of the original")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIReadUserTimelineWithoutPostsIsEmpty")
    void testAPIReadUserTimelineWithoutPostsIsEmpty() throws IOException {
        ApiUser user = createUser("noposts");

        assertTrue(readUserTimeline(user.userId).isEmpty(), "A user without posts must have an empty timeline");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostWithEmptyTextBadRequest")
    void testAPIComposePostWithEmptyTextBadRequest() throws IOException {
        ApiUser author = createAuthorWithFollower("empty");

        assertStatus(HTTP_BAD_REQUEST, composePost(author, ""), "Compose with empty text");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Tag("known-quirk")
    @Test
    @DisplayName("TestAPIComposePostWithMismatchedMediaIsSilentlyDropped")
    void testAPIComposePostWithMismatchedMediaIsSilentlyDropped() throws IOException {
        // Known quirk: media_ids/media_types of different length answer HTTP 200 but the post is never stored.
        ApiUser author = createAuthorWithFollower("badmedia");
        String lostText = "Lost " + unique();
        String keptText = "Kept " + unique();

        assertStatus(HTTP_OK, composePost(author, lostText, "[\"123\"]", "[]"), "Compose with mismatched media");
        composePostOrFail(author, keptText);

        List<Post> timeline = readUserTimeline(author.userId);
        assertAll(
                () -> assertAnyMatch(timeline, p -> keptText.equals(p.getText()), "The valid post must be stored"),
                () -> assertNoneMatch(timeline, p -> lostText.equals(p.getText()),
                        "The post with mismatched media must not be stored")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIUserTimelinePaginationBeyondSizeIsEmpty")
    void testAPIUserTimelinePaginationBeyondSizeIsEmpty() throws IOException {
        ApiUser author = createAuthorWithFollower("beyond");
        composePostOrFail(author, "Only post " + unique());

        assertTrue(readUserTimeline(author.userId, 5, 10).isEmpty(),
                "A page that starts beyond the timeline size must be empty");
    }
}
