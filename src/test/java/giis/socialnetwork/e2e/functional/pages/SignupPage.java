package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SignupPage extends BasePage {

    private static final By FIRST_NAME = By.name("first_name");
    private static final By LAST_NAME = By.name("last_name");
    private static final By USERNAME = By.name("username");
    private static final By PASSWORD = By.name("password");
    private static final By SIGNUP_BTN = By.cssSelector("input[name='signup']");
    private static final By LOGIN_LINK = By.linkText("Login");

    public SignupPage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    public SignupPage open() {
        navigate("/signup.html");
        waiter.waitForSignupPage();
        return this;
    }

    public LoginPage register(String firstName, String lastName, String username, String password)
            throws ElementNotFoundException {
        waiter.waitForSignupPage();
        fill(FIRST_NAME, firstName);
        fill(LAST_NAME, lastName);
        fill(USERNAME, username);
        fill(PASSWORD, password);
        click(SIGNUP_BTN);
        waiter.waitForLoginPage();
        return new LoginPage(driver, waiter, sutUrl);
    }

    public boolean isFirstNameDisplayed() {return isDisplayed(FIRST_NAME);}
    public boolean isLastNameDisplayed() {return isDisplayed(LAST_NAME);}
    public boolean isUsernameDisplayed() {return isDisplayed(USERNAME);}
    public boolean isPasswordDisplayed() {return isDisplayed(PASSWORD);}
    public boolean isLoginLinkDisplayed() {return isDisplayed(LOGIN_LINK);}
}