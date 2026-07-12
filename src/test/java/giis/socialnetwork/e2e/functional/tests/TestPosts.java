package giis.socialnetwork.e2e.functional.tests;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestPosts extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestVisibilityComposeForm")
    void testVisibilityComposeForm() {
        MainPage main = registerAndLogin(newUser("poster"));
        Assertions.assertTrue(main.isComposeFormVisible(), "Post compose form must be visible on the main page");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "post", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user-timeline", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestComposePostAppearsInTimeline")
    void testComposePostAppearsInTimeline() {
        TestUser user = newUser("timelineui");
        String postText = "UI test post " + user.username;
        MainPage main = registerAndLogin(user);
        // profile.html shows the user's own user-timeline; main.html shows home-timeline (followed users only)
        main.composePost(postText).openProfile().waitForPost(postText);
        Assertions.assertTrue(main.hasPostText(postText),
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
        TestUser mentioned = newUser("mentioned");
        registerUser(mentioned);

        TestUser author = newUser("mauthor");
        MainPage main = registerAndLogin(author);

        String postText = "hi @" + mentioned.username;
        main.composePost(postText).openProfile().waitForPost("hi @" + mentioned.username);
        Assertions.assertTrue(main.hasMentionLink(mentioned.username),
                "The rendered post must link the @mention to the mentioned user's profile page");
    }
}
