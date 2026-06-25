package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.name("username");
    private static final By PASSWORD = By.name("password");
    private static final By LOGIN_BTN = By.cssSelector("input[name='login']");
    private static final By SIGN_UP = By.linkText("Sign Up");

    public LoginPage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    public LoginPage open() {
        navigate("/index.html");
        waiter.waitForLoginPage();
        return this;
    }

    public MainPage login(String username, String password) throws ElementNotFoundException {
        waiter.waitForLoginPage();
        fill(USERNAME, username);
        fill(PASSWORD, password);
        click(LOGIN_BTN);
        waiter.waitForMainPage();
        return new MainPage(driver, waiter, sutUrl);
    }

    public boolean isUsernameDisplayed() {return isDisplayed(USERNAME);}
    public boolean isPasswordDisplayed() {return isDisplayed(PASSWORD);}
    public boolean isSignUpLinkDisplayed() {return isDisplayed(SIGN_UP);}
}