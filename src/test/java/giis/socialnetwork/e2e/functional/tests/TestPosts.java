package giis.socialnetwork.e2e.functional.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.TestUser;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.ProfilePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class TestPosts extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestVisibilityComposeForm")
    void testVisibilityComposeForm() {
        MainPage main = registerAndLogin(TestUser.generate("poster"));
        main.openComposeForm();
        assertTrue(main.isComposeFormVisible(), "Post compose form must be visible on the main page");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestComposePostAppearsInTimeline")
    void testComposePostAppearsInTimeline() {
        TestUser user = TestUser.generate("timelineui");
        String postText = "UI test post " + user.username;
        MainPage main = registerAndLogin(user);
        // profile.html shows the user-timeline, main.html the home-timeline
        ProfilePage profile = main.composePost(postText).openProfile();
        profile.waitForPost(postText);
        assertTrue(profile.hasPost(postText),
                "Composed post must appear in the user's profile timeline after creation");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestComposePostWithMentionShowsProfileLink")
    void testComposePostWithMentionShowsProfileLink() {
        TestUser mentioned = TestUser.generate("mentioned");
        registerViaApi(mentioned);

        MainPage main = registerAndLogin(TestUser.generate("mauthor"));

        String postText = "hi @" + mentioned.username;
        ProfilePage profile = main.composePost(postText).openProfile();
        profile.waitForPost(postText);
        assertTrue(profile.hasMentionLink(mentioned.username),
                "The rendered post must link the @mention to the mentioned user's profile page");
    }
}