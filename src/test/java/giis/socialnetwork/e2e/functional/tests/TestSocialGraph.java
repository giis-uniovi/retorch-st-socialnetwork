package giis.socialnetwork.e2e.functional.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.TestUser;
import giis.socialnetwork.e2e.functional.pages.ContactPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

@Tag("ui")
class TestSocialGraph extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestFollowAndUnfollow")
    void testFollowAndUnfollow() {
        TestUser userA = TestUser.generate("sga");
        TestUser userB = TestUser.generate("sgb");
        TestUser userC = TestUser.generate("sgc");

        String idB = String.valueOf(registerViaApi(userB));
        String idC = String.valueOf(registerViaApi(userC));

        ContactPage contact = registerAndLogin(userA).goToContacts();

        // Follow both: an empty get_followee returns HTTP 500
        contact.followUser(userB.username).followUser(userC.username);
        contact.waitForFolloweeCount(2);
        assertEquals(new HashSet<>(Arrays.asList(idB, idC)), new HashSet<>(contact.followeeIds()),
                "The followees must be exactly B and C");

        contact.unfollowUser(userB.username);
        contact.waitForFolloweeCount(1);
        assertEquals(Collections.singletonList(idC), contact.followeeIds(),
                "Only C must remain after unfollowing B");
    }
}