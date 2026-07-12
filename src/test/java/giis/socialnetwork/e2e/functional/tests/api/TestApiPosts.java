package giis.socialnetwork.e2e.functional.tests.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Validates post composition and user-timeline retrieval:
 * <ul>
 *   <li>POST /wrk2-api/post/compose            — create a post (HTTP 200)</li>
 *   <li>GET  /wrk2-api/user-timeline/read      — read back the composed post, incl. @mention extraction</li>
 * </ul>
 */
class TestApiPosts extends BaseApiClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIComposePost")
    void testAPIComposePost() throws IOException {
        String[] author = createUserWithName("compose");
        String username = author[0];
        long userId = Long.parseLong(author[1]);

        // social-graph-service rejects ZADD with an empty member set; author must have ≥1 follower.
        String[] follower = createUserWithName("composefollower");
        followUser(follower[0], username);

        int status = composePost(username, userId, "Hello from API test " + unique());
        Assertions.assertEquals(200, status, "Compose post must return HTTP 200");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIReadUserTimeline")
    void testAPIReadUserTimeline() throws IOException {
        String[] user = createUserWithName("timeline");
        String username = user[0];
        long userId = Long.parseLong(user[1]);
        String postText = "Timeline test post " + unique();

        composePost(username, userId, postText);

        String url = timelineReadUrl(wrk2UserTimelineUrl(READPATH), userId, 0, 10);
        JsonArray timeline = getJsonArray(url);
        Assertions.assertFalse(timeline.isEmpty(), "User timeline must contain at least one post after composing");

        JsonObject post = timeline.get(0).getAsJsonObject();
        Assertions.assertAll(
                () -> Assertions.assertTrue(post.has("post_id"), "Post must have 'post_id'"),
                () -> Assertions.assertTrue(post.has("creator"), "Post must have 'creator'"),
                () -> Assertions.assertTrue(post.has("text"), "Post must have 'text'"),
                () -> Assertions.assertTrue(post.has("timestamp"), "Post must have 'timestamp'")
        );

        JsonObject creator = post.get("creator").getAsJsonObject();
        Assertions.assertEquals(username, creator.get("username").getAsString(),
                "Post creator username must match the author");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostWithMentionPopulatesUserMentions")
    void testAPIComposePostWithMentionPopulatesUserMentions() throws IOException {
        String[] author = createUserWithName("mentionauthor");
        String authorName = author[0];
        long authorId = Long.parseLong(author[1]);
        String[] mentioned = createUserWithName("mentioned");
        String mentionedName = mentioned[0];

        composePost(authorName, authorId, "hi @" + mentionedName + " " + unique());

        String url = timelineReadUrl(wrk2UserTimelineUrl(READPATH), authorId, 0, 10);
        JsonArray timeline = getJsonArray(url);
        Assertions.assertFalse(timeline.isEmpty(), "Author timeline must contain the composed post");

        JsonArray mentions = timeline.get(0).getAsJsonObject().getAsJsonArray("user_mentions");
        Assertions.assertTrue(containsByField(mentions, "username", mentionedName),
                "Mentioned user '" + mentionedName + "' must appear in the post's user_mentions");
    }

    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIReadUserTimelineBadRequest")
    void testAPIReadUserTimelineBadRequest() throws IOException {
        int status = getStatus(wrk2UserTimelineUrl(READPATH));
        Assertions.assertEquals(400, status, "Timeline read without required params must return HTTP 400");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIUserTimelinePagination")
    void testAPIUserTimelinePagination() throws IOException {
        String[] author = createUserWithName("paging");
        String username = author[0];
        long userId = Long.parseLong(author[1]);
        String[] follower = createUserWithName("pagingfollower");
        followUser(follower[0], username);

        // Three posts, composed oldest to newest; the timeline serves newest first
        for (int i = 1; i <= 3; i++) {
            composePost(username, userId, "Page post " + i + " " + username);
        }

        JsonArray all = getJsonArray(timelineReadUrl(wrk2UserTimelineUrl(READPATH), userId, 0, 10));
        JsonArray firstPage = getJsonArray(timelineReadUrl(wrk2UserTimelineUrl(READPATH), userId, 0, 2));
        JsonArray secondPage = getJsonArray(timelineReadUrl(wrk2UserTimelineUrl(READPATH), userId, 2, 10));

        Assertions.assertAll(
                () -> Assertions.assertEquals(3, all.size(), "Full read must return all three posts"),
                () -> Assertions.assertEquals(2, firstPage.size(),
                        "start=0&stop=2 must return exactly two posts (stop is exclusive)"),
                () -> Assertions.assertEquals(1, secondPage.size(),
                        "start=2&stop=10 must return the remaining post"),
                () -> Assertions.assertEquals("Page post 3 " + username,
                        all.get(0).getAsJsonObject().get("text").getAsString(),
                        "The timeline must be ordered newest first")
        );
        // The two pages must not overlap: the second page's post is the oldest one
        Assertions.assertEquals("Page post 1 " + username,
                secondPage.get(0).getAsJsonObject().get("text").getAsString(),
                "The second page must contain the oldest post only");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostWithMediaAttachesMedia")
    void testAPIComposePostWithMediaAttachesMedia() throws IOException {
        String[] author = createUserWithName("media");
        String username = author[0];
        long userId = Long.parseLong(author[1]);
        String[] follower = createUserWithName("mediafollower");
        followUser(follower[0], username);

        String mediaId = "123456789012345678";
        int status = composePost(username, userId, "Post with media " + unique(),
                "[\"" + mediaId + "\"]", "[\"png\"]");
        Assertions.assertEquals(200, status, "Compose with media must return HTTP 200");

        JsonArray timeline = getJsonArray(timelineReadUrl(wrk2UserTimelineUrl(READPATH), userId, 0, 10));
        Assertions.assertFalse(timeline.isEmpty(), "Timeline must contain the composed post");
        JsonArray media = timeline.get(0).getAsJsonObject().getAsJsonArray("media");
        Assertions.assertTrue(containsByField(media, "media_id", mediaId),
                "The post's media list must contain the attached media id");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIComposePostShortensUrls")
    void testAPIComposePostShortensUrls() throws IOException {
        String[] author = createUserWithName("urls");
        String username = author[0];
        long userId = Long.parseLong(author[1]);
        String[] follower = createUserWithName("urlsfollower");
        followUser(follower[0], username);

        composePost(username, userId, "check http://example.com/page" + unique() + " out");

        JsonArray timeline = getJsonArray(timelineReadUrl(wrk2UserTimelineUrl(READPATH), userId, 0, 10));
        Assertions.assertFalse(timeline.isEmpty(), "Timeline must contain the composed post");
        JsonObject post = timeline.get(0).getAsJsonObject();
        JsonArray urls = post.getAsJsonArray("urls");
        Assertions.assertFalse(urls.isEmpty(), "The post must contain a shortened URL entry");
        String shortened = urls.get(0).getAsJsonObject().get("shortened_url").getAsString();
        Assertions.assertAll(
                () -> Assertions.assertTrue(shortened.startsWith("https://short-url/"),
                        "url-shorten-service must rewrite the link to the short-url host, got: " + shortened),
                () -> Assertions.assertTrue(post.get("text").getAsString().contains(shortened),
                        "The post text must contain the shortened URL instead of the original")
        );
    }
}
