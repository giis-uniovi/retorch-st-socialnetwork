package giis.socialnetwork.e2e.functional.tests;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.pages.ContactPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestSocialGraph extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "social-graph", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestFollowAndUnfollow")
    void testFollowAndUnfollow() throws ElementNotFoundException {
        long ts = System.currentTimeMillis();
        String userA = "sga" + ts;
        String userB = "sgb" + ts;
        String userC = "sgc" + ts;
        String password = "pwd" + ts;

        // Register the two followees first (no login needed to be followed by username)
        new SignupPage(driver, waiter, sutUrl).open().register("B", "Followee", userB, password);
        new SignupPage(driver, waiter, sutUrl).open().register("C", "Followee", userC, password);

        ContactPage contact = new SignupPage(driver, waiter, sutUrl).open()
                .register("A", "Follower", userA, password)
                .login(userA, password)
                .goToContacts();

        // Follow both so the followee list never empties (an empty get_followee returns HTTP 500)
        contact.followUser(userB).followUser(userC);
        contact.waitForFolloweeCount(2);
        Assertions.assertEquals(2, contact.followeeCount(), "Both followees must appear after following B and C");

        contact.unfollowUser(userB);
        contact.waitForFolloweeCount(1);
        Assertions.assertEquals(1, contact.followeeCount(), "One followee must remain after unfollowing B");
    }
}
