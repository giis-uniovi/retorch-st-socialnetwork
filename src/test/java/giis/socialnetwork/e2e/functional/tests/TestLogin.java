package giis.socialnetwork.e2e.functional.tests;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseLoggedClass;
import giis.socialnetwork.e2e.functional.pages.LoginPage;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestLogin extends BaseLoggedClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestRegister")
    void testRegister() {
        TestUser user = newUser("alice");
        LoginPage loginPage = new SignupPage(driver, waiter, sutUrl).open()
                .register("Alice", "Test", user.username, user.password);
        Assertions.assertTrue(loginPage.isUsernameDisplayed(), "After registration the login page must be shown");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend", concurrency = 10, sharing = true, accessMode = "READONLY")
    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestLogin")
    void testLogin() {
        TestUser user = newUser("bob");
        MainPage main = new SignupPage(driver, waiter, sutUrl).open()
                .register("Bob", "Test", user.username, user.password)
                .login(user.username, user.password);
        Assertions.assertTrue(main.getBrandText().contains("DeathStar"),
                "After login the main feed page with 'DeathStar' brand must be shown");
    }
}
