package giis.socialnetwork.e2e.functional.tests.api;

import com.google.gson.JsonArray;
import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Validates the social-graph endpoints exposed through the Nginx gateway:
 * <ul>
 *   <li>POST /api/user/follow       — follow a user by username (HTTP 200 after redirect)</li>
 *   <li>POST /api/user/unfollow     — unfollow a user by username (HTTP 200)</li>
 *   <li>GET  /api/user/get_follower — list followers of the logged-in user (JWT auth)</li>
 *   <li>GET  /api/user/get_followee — list followees of the logged-in user (JWT auth)</li>
 * </ul>
 */
class TestApiSocialGraph extends BaseApiClass {

    private static final String FOLLOWEEID = "followee_id";

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIFollowUser")
    void testAPIFollowUser() throws IOException {
        String[] userA = createUserWithName("follower");
        String[] userB = createUserWithName("followee");

        int status = followUser(userA[0], userB[0]);
        Assertions.assertEquals(200, status, "Follow must return HTTP 200 (redirect to contact.html followed)");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestaAPIGetFollowers")
    void testAPIGetFollowers() throws IOException {
        String[] userA = createUserWithName("fa");
        String userAId = userA[1];
        String[] userB = createUserWithName("fb");

        // A follows B by username — no auth required for this endpoint
        followUser(userA[0], userB[0]);

        // Re-login as B so the cookie store holds B's JWT (get_follower reads user_id from the token)
        loginUser(userB[0], userB[2]);

        JsonArray followers = getFollowers();
        Assertions.assertTrue(containsByField(followers, "follower_id", userAId),
                "User A (id=" + userAId + ") must appear in B's follower list");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIGetFolloweesWithToken")
    void testAPIGetFolloweesWithToken() throws IOException {
        String[] userA = createUserWithName("ga");
        String[] userB = createUserWithName("gb");
        String userBId = userB[1];

        followUser(userA[0], userB[0]);

        // Re-login as A so the cookie store holds A's JWT (get_followee reads user_id from the token)
        loginUser(userA[0], userA[2]);

        JsonArray followees = getFollowees();
        Assertions.assertTrue(containsByField(followees, FOLLOWEEID, userBId),
                "User B (id=" + userBId + ") must appear in A's followee list");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIUnfollowRemovesFollowee")
    void testAPIUnfollowRemovesFollowee() throws IOException {
        String[] userA = createUserWithName("ua");
        String[] userB = createUserWithName("ub");
        String[] userC = createUserWithName("uc");
        String userBId = userB[1];
        String userCId = userC[1];

        // A follows both B and C; keeping C means get_followee never reads an empty set
        // (social-graph-service rejects ZADD with an empty member set, returning HTTP 500).
        followUser(userA[0], userB[0]);
        followUser(userA[0], userC[0]);
        loginUser(userA[0], userA[2]);

        JsonArray before = getFollowees();
        Assertions.assertAll(
                () -> Assertions.assertTrue(containsByField(before, FOLLOWEEID, userBId), "B must be followed initially"),
                () -> Assertions.assertTrue(containsByField(before, FOLLOWEEID, userCId), "C must be followed initially")
        );

        int status = unfollowUser(userA[0], userB[0]);
        Assertions.assertEquals(200, status, "Unfollow must return HTTP 200");

        JsonArray after = getFollowees();
        Assertions.assertAll(
                () -> Assertions.assertFalse(containsByField(after, FOLLOWEEID, userBId), "B must be gone after unfollow"),
                () -> Assertions.assertTrue(containsByField(after, FOLLOWEEID, userCId), "C must still be followed")
        );
    }
}