package giis.socialnetwork.e2e.functional.tests;

import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestPosts extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Post compose form is visible on the main feed page after login")
    void testComposeFormVisible() throws ElementNotFoundException {
        long ts = System.currentTimeMillis();
        MainPage main = new SignupPage(driver, waiter, sutUrl).open()
                .register("Post", "Er", "poster" + ts, "pwd" + ts)
                .login("poster" + ts, "pwd" + ts);
        Assertions.assertTrue(main.isComposeFormVisible(), "Post compose form must be visible on the main page");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Composing a post via the form makes it appear in the user's own timeline")
    void testComposePostAppearsInTimeline() throws ElementNotFoundException {
        long ts = System.currentTimeMillis();
        String username = "timelineui" + ts;
        String postText = "UI test post " + ts;
        MainPage main = new SignupPage(driver, waiter, sutUrl).open()
                .register("Timeline", "Ui", username, "pwd" + ts)
                .login(username, "pwd" + ts);
        // profile.html shows the user's own user-timeline; main.html shows home-timeline (followed users only)
        main.composePost(postText).openProfile().waitForPost(postText);
        Assertions.assertTrue(main.hasPostText(postText),
                "Composed post must appear in the user's profile timeline after creation");
    }
}
