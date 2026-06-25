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

    private static final String READPATH = "/read";

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

        String url = wrk2UserTimelineUrl(READPATH) + "?user_id=" + userId + "&start=0&stop=10";
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

        String url = wrk2UserTimelineUrl(READPATH) + "?user_id=" + authorId + "&start=0&stop=10";
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
}
