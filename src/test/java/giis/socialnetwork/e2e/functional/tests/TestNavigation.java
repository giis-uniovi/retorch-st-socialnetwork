package giis.socialnetwork.e2e.functional.tests;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.pages.ContactPage;
import giis.socialnetwork.e2e.functional.pages.LoginPage;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestNavigation extends BaseLoggedClass {

    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("testNavigation")
    void testNavigation() throws ElementNotFoundException {
        long ts = System.currentTimeMillis();
        String username = "e2eUser" + ts;
        String password = "pwd" + ts;

        // 1. Verify Login Page Structure
        LoginPage loginPage = new LoginPage(driver, waiter, sutUrl).open();
        Assertions.assertAll("Login Page Checks",
                () -> Assertions.assertTrue(loginPage.isUsernameDisplayed(), "Username field must be visible"),
                () -> Assertions.assertTrue(loginPage.isPasswordDisplayed(), "Password field must be visible"),
                () -> Assertions.assertTrue(loginPage.isSignUpLinkDisplayed(), "Sign Up link must be visible")
        );

        // 2. Verify Signup Page Structure
        SignupPage signupPage = new SignupPage(driver, waiter, sutUrl).open();
        Assertions.assertAll("Signup Page Checks",
                () -> Assertions.assertTrue(signupPage.isFirstNameDisplayed(), "first_name field must be visible"),
                () -> Assertions.assertTrue(signupPage.isLastNameDisplayed(), "last_name field must be visible"),
                () -> Assertions.assertTrue(signupPage.isUsernameDisplayed(), "username field must be visible"),
                () -> Assertions.assertTrue(signupPage.isPasswordDisplayed(), "password field must be visible"),
                () -> Assertions.assertTrue(signupPage.isLoginLinkDisplayed(), "Login link must be visible")
        );

        // 3. Register, Login, and Verify Main Page Navbar
        MainPage mainPage = signupPage
                .register("EndToEnd", "User", username, password)
                .login(username, password);

        Assertions.assertAll("Main Page Navbar Checks",
                () -> Assertions.assertTrue(mainPage.hasNavLink("Post"), "Navbar must contain a 'Post' link"),
                () -> Assertions.assertTrue(mainPage.hasNavLink("Contacts"), "Navbar must contain a 'Contacts' link")
        );

        // 4. Navigate to Contacts Page and Verify Sections
        ContactPage contactPage = mainPage.goToContacts();
        Assertions.assertAll("Contact Page Checks",
                () -> Assertions.assertTrue(contactPage.isFollowerListDisplayed(), "follower-list section must be visible"),
                () -> Assertions.assertTrue(contactPage.isFolloweeListDisplayed(), "followee-list section must be visible")
        );
    }
}
