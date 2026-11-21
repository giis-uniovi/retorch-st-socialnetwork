package giis.socialnetwork.e2e.functional.tests.api;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.ApiUser;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import giis.socialnetwork.e2e.functional.dto.Followee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertAnyMatch;
import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertNoneMatch;
import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertStatus;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_OK;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_UNAUTHORIZED;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** API tests for {@code /api/user/follow}, {@code unfollow}, {@code get_follower} and {@code get_followee}. */
@Tag("api")
class TestApiSocialGraph extends BaseApiClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIFollowUser")
    void testAPIFollowUser() throws IOException {
        ApiUser userA = createUser("follower");
        ApiUser userB = createUser("followee");

        int status = followUser(userA.username, userB.username);
        assertStatus(HTTP_OK, status, "Follow (redirect to contact.html followed)");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestaAPIGetFollowers")
    void testAPIGetFollowers() throws IOException {
        ApiUser userA = createUser("fa");
        ApiUser userB = createUser("fb");

        followOrFail(userA, userB);

        assertAnyMatch(userB.followers(), f -> f.getFollowerId() == userA.userId,
                "User A (id=" + userA.userId + ") must appear in B's follower list");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIGetFolloweesWithToken")
    void testAPIGetFolloweesWithToken() throws IOException {
        ApiUser userA = createUser("ga");
        ApiUser userB = createUser("gb");

        followOrFail(userA, userB);

        assertAnyMatch(userA.followees(), f -> f.getFolloweeId() == userB.userId,
                "User B (id=" + userB.userId + ") must appear in A's followee list");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIUnfollowNotFollowedIsIdempotent")
    void testAPIUnfollowNotFollowedIsIdempotent() throws IOException {
        ApiUser userA = createUser("ia");
        ApiUser userB = createUser("ib");
        ApiUser userC = createUser("ic");

        followOrFail(userA, userC);

        int status = unfollowUser(userA.username, userB.username);
        assertStatus(HTTP_OK, status, "Unfollowing a non-followed user");

        List<Followee> followees = userA.followees();
        assertAll(
                () -> assertAnyMatch(followees, f -> f.getFolloweeId() == userC.userId,
                        "C must still be followed after the no-op unfollow"),
                () -> assertEquals(1, followees.size(),
                        "The followee list must be unchanged by the no-op unfollow")
        );
    }

    @AccessMode(resID = "social-graph", concurrency = 10, sharing = true, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIGetFollowersWithoutSessionUnauthorized")
    void testAPIGetFollowersWithoutSessionUnauthorized() throws IOException {
        int status = anonymous.followersStatus();
        assertStatus(HTTP_UNAUTHORIZED, status, "get_follower without a login_token cookie");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIUnfollowRemovesFollowee")
    void testAPIUnfollowRemovesFollowee() throws IOException {
        ApiUser userA = createUser("ua");
        ApiUser userB = createUser("ub");
        ApiUser userC = createUser("uc");

        // C keeps the followee set non-empty (see createAuthorWithFollower for the empty-ZADD quirk).
        followOrFail(userA, userB);
        followOrFail(userA, userC);

        List<Followee> before = userA.followees();
        assertAll(
                () -> assertAnyMatch(before, f -> f.getFolloweeId() == userB.userId, "B must be followed initially"),
                () -> assertAnyMatch(before, f -> f.getFolloweeId() == userC.userId, "C must be followed initially")
        );

        int status = unfollowUser(userA.username, userB.username);
        assertStatus(HTTP_OK, status, "Unfollow");

        List<Followee> after = userA.followees();
        assertAll(
                () -> assertNoneMatch(after, f -> f.getFolloweeId() == userB.userId, "B must be gone after unfollow"),
                () -> assertAnyMatch(after, f -> f.getFolloweeId() == userC.userId, "C must still be followed")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Tag("known-quirk")
    @Test
    @DisplayName("TestAPIGetFolloweesWithoutFolloweesReturnsServerError")
    void testAPIGetFolloweesWithoutFolloweesReturnsServerError() throws IOException {
        // Known quirk: an empty followee set makes the empty ZADD fail, so get_followee answers HTTP 500.
        ApiUser loner = createUser("loner");

        assertStatus(HTTP_SERVER_ERROR, loner.api.followeesStatus(), "get_followee for a user without followees");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Tag("known-quirk")
    @Test
    @DisplayName("TestAPIFollowSelf")
    void testAPIFollowSelf() throws IOException {
        // Known quirk: self-follow is accepted and the user becomes its own follower and followee.
        ApiUser user = createUser("selfie");

        assertStatus(HTTP_OK, followUser(user.username, user.username), "Following oneself");
        assertAll(
                () -> assertAnyMatch(user.followees(), f -> f.getFolloweeId() == user.userId,
                        "The user must appear in its own followee list"),
                () -> assertAnyMatch(user.followers(), f -> f.getFollowerId() == user.userId,
                        "The user must appear in its own follower list")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Tag("known-quirk")
    @Test
    @DisplayName("TestAPIFollowUnknownUserFails")
    void testAPIFollowUnknownUserFails() throws IOException {
        // Known quirk: an unregistered user on either side is reported as HTTP 500 rather than a 4xx.
        ApiUser user = createUser("fu");
        String ghost = "ghost" + unique();

        assertAll(
                () -> assertStatus(HTTP_SERVER_ERROR, followUser(user.username, ghost), "Following an unknown user"),
                () -> assertStatus(HTTP_SERVER_ERROR, followUser(ghost, user.username), "Following as an unknown user")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIFollowTwiceIsIdempotent")
    void testAPIFollowTwiceIsIdempotent() throws IOException {
        ApiUser userA = createUser("ta");
        ApiUser userB = createUser("tb");

        followOrFail(userA, userB);
        followOrFail(userA, userB);

        List<Followee> followees = userA.followees();
        assertEquals(1, followees.size(), "A repeated follow must keep a single followee entry, got: " + followees);
    }
}
