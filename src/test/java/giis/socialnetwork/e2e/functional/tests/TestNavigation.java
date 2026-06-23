package giis.socialnetwork.e2e.functional.tests;

import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.pages.ContactPage;
import giis.socialnetwork.e2e.functional.pages.LoginPage;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestNavigation extends BaseLoggedClass {

    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Login page loads and displays the login form with a Sign Up link")
    void testLoginPageStructure() {
        LoginPage page = new LoginPage(driver, waiter, sutUrl).open();
        Assertions.assertAll(
                () -> Assertions.assertTrue(page.isUsernameDisplayed(), "Username field must be visible"),
                () -> Assertions.assertTrue(page.isPasswordDisplayed(), "Password field must be visible"),
                () -> Assertions.assertTrue(page.isSignUpLinkDisplayed(), "Sign Up link must be visible")
        );
    }

    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Signup page loads and displays the registration form with a Login link")
    void testSignupPageStructure() {
        SignupPage page = new SignupPage(driver, waiter, sutUrl).open();
        Assertions.assertAll(
                () -> Assertions.assertTrue(page.isFirstNameDisplayed(), "first_name field must be visible"),
                () -> Assertions.assertTrue(page.isLastNameDisplayed(), "last_name field must be visible"),
                () -> Assertions.assertTrue(page.isUsernameDisplayed(), "username field must be visible"),
                () -> Assertions.assertTrue(page.isPasswordDisplayed(), "password field must be visible"),
                () -> Assertions.assertTrue(page.isLoginLinkDisplayed(), "Login link must be visible")
        );
    }

    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Main page navbar contains Post and Contacts links after login")
    void testMainPageNavbar() throws ElementNotFoundException {
        long ts = System.currentTimeMillis();
        String username = "nav" + ts;
        MainPage main = new SignupPage(driver, waiter, sutUrl).open()
                .register("Nav", "User", username, "pwd" + ts)
                .login(username, "pwd" + ts);
        Assertions.assertAll(
                () -> Assertions.assertTrue(main.hasNavLink("Post"), "Navbar must contain a 'Post' link"),
                () -> Assertions.assertTrue(main.hasNavLink("Contacts"), "Navbar must contain a 'Contacts' link")
        );
    }

    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Contact page loads with Follower List and Followee List sections after login")
    void testContactPageSections() throws ElementNotFoundException {
        long ts = System.currentTimeMillis();
        String username = "cnt" + ts;
        ContactPage contact = new SignupPage(driver, waiter, sutUrl).open()
                .register("Cnt", "User", username, "pwd" + ts)
                .login(username, "pwd" + ts)
                .goToContacts();
        Assertions.assertAll(
                () -> Assertions.assertTrue(contact.isFollowerListDisplayed(), "follower-list section must be visible"),
                () -> Assertions.assertTrue(contact.isFolloweeListDisplayed(), "followee-list section must be visible")
        );
    }
}
