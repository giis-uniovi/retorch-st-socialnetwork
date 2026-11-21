package giis.socialnetwork.e2e.functional.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.common.TestUser;
import giis.socialnetwork.e2e.functional.pages.LoginPage;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class TestLogin extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestRegister")
    void testRegister() {
        TestUser user = TestUser.generate("alice");
        LoginPage login = signupPage().open().register("Alice", "Test", user.username, user.password);
        assertTrue(login.isUsernameDisplayed(), "After registration the login page must be shown");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestLogin")
    void testLogin() {
        TestUser user = TestUser.generate("bob");
        MainPage main = signupPage().open().register("Bob", "Test", user.username, user.password)
                .login(user.username, user.password);
        assertTrue(main.getBrandText().contains("DeathStar"),
                "After login the main feed page with 'DeathStar' brand must be shown");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @Tag("known-quirk")
    @DisplayName("TestLoginWithWrongPasswordDoesNotReachMainPage")
    void testLoginWithWrongPasswordDoesNotReachMainPage() {
        TestUser user = TestUser.generate("carol");
        registerViaApi(user);
        // the form posts to the API: the browser is left on a plain-text error response (HTTP 500)
        LoginPage login = loginPage().open().attemptLogin(user.username, "wrong-" + user.password);
        String failure = login.waitForLoginFailureText();
        assertTrue(failure.contains("User login failure"), "A rejected login must show the failure message");
        assertFalse(login.isMainPageShown(), "A rejected login must not reach the main page");
    }
}