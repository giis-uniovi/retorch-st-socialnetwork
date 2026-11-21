package giis.socialnetwork.e2e.functional.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.TestUser;
import giis.socialnetwork.e2e.functional.pages.ContactPage;
import giis.socialnetwork.e2e.functional.pages.LoginPage;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class TestNavigation extends BaseLoggedClass {

    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("testNavigation")
    void testNavigation() {
        TestUser user = TestUser.generate("e2eUser");

        LoginPage login = loginPage().open();
        assertAll("Login Page Checks",
                () -> assertTrue(login.isUsernameDisplayed(), "Username field must be visible"),
                () -> assertTrue(login.isPasswordDisplayed(), "Password field must be visible"),
                () -> assertTrue(login.isSignUpLinkDisplayed(), "Sign Up link must be visible")
        );

        SignupPage signup = signupPage().open();
        assertAll("Signup Page Checks",
                () -> assertTrue(signup.isFirstNameDisplayed(), "first_name field must be visible"),
                () -> assertTrue(signup.isLastNameDisplayed(), "last_name field must be visible"),
                () -> assertTrue(signup.isUsernameDisplayed(), "username field must be visible"),
                () -> assertTrue(signup.isPasswordDisplayed(), "password field must be visible"),
                () -> assertTrue(signup.isLoginLinkDisplayed(), "Login link must be visible")
        );

        MainPage main = signup
                .register("EndToEnd", "User", user.username, user.password)
                .login(user.username, user.password);

        assertAll("Main Page Navbar Checks",
                () -> assertTrue(main.hasNavLink("Post"), "Navbar must contain a 'Post' link"),
                () -> assertTrue(main.hasNavLink("Contacts"), "Navbar must contain a 'Contacts' link")
        );

        ContactPage contact = main.goToContacts();
        assertAll("Contact Page Checks",
                () -> assertTrue(contact.isFollowerListDisplayed(), "follower-list section must be visible"),
                () -> assertTrue(contact.isFolloweeListDisplayed(), "followee-list section must be visible")
        );
    }
}