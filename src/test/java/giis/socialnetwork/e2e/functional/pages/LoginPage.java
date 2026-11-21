package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.name("username");
    private static final By PASSWORD = By.name("password");
    private static final By LOGIN_BTN = By.cssSelector("input[name='login']");
    private static final By SIGN_UP = By.linkText("Sign Up");
    private static final By NAVBAR_BRAND = By.cssSelector("a.navbar-brand");
    private static final By BODY = By.tagName("body");

    public LoginPage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    @Override
    protected By loadedMarker() {return USERNAME;}

    public boolean isUsernameDisplayed() {return isDisplayed(USERNAME);}
    public boolean isPasswordDisplayed() {return isDisplayed(PASSWORD);}
    public boolean isSignUpLinkDisplayed() {return isDisplayed(SIGN_UP);}

    public LoginPage open() {
        navigate("/index.html");
        waitUntilLoaded();
        return this;
    }

    public MainPage login(String username, String password) {
        waitUntilLoaded();
        fill(USERNAME, username);
        fill(PASSWORD, password);
        click(LOGIN_BTN);
        MainPage main = new MainPage(driver, waiter, sutUrl);
        main.waitUntilLoaded();
        return main;
    }

    /** Submits the credentials without waiting for the main page (for rejected logins). */
    public LoginPage attemptLogin(String username, String password) {
        waitUntilLoaded();
        fill(USERNAME, username);
        fill(PASSWORD, password);
        click(LOGIN_BTN);
        return this;
    }

    /** Waits (default timeout) until the browser lands on the login endpoint response and returns its text. */
    public String waitForLoginFailureText() {
        waiter.waitUntil(ExpectedConditions.urlContains("/api/user/login"), "Login endpoint response not shown");
        return text(BODY);
    }

    public boolean isMainPageShown() {return isPresent(NAVBAR_BRAND);}
}