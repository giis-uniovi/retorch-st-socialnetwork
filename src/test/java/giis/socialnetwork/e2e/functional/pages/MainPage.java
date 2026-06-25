package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class MainPage extends BasePage {

    private static final By NAVBAR_BRAND = By.cssSelector("a.navbar-brand");
    private static final By NAV_LINKS = By.cssSelector(".navbar-nav .nav-link");
    private static final By SHOW_POST = By.id("show-post");
    private static final By POST_CONTENT = By.id("post-content");
    private static final By CREATE_POST = By.id("create-post");

    public MainPage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    public MainPage open() {
        navigate("/main.html");
        waiter.waitForMainPage();
        return this;
    }

    public MainPage composePost(String postText) throws ElementNotFoundException {
        click(SHOW_POST);
        waiter.waitUntil(ExpectedConditions.visibilityOfElementLocated(POST_CONTENT), "Post textarea not visible");
        fill(POST_CONTENT, postText);
        click(CREATE_POST);
        return this;
    }

    public void waitForPost(String postText) {
        waiter.waitForPostText(postText, driver);
    }

    /*** Navigates to profile.html, which renders the logged-in user's own user-timeline.*/
    public MainPage openProfile() {
        navigate("/profile.html");
        waiter.waitForMainPage();
        return this;
    }

    public ContactPage goToContacts() {
        navigate("/contact.html");
        waiter.waitForContactPage();
        return new ContactPage(driver, waiter, sutUrl);
    }


    public boolean isComposeFormVisible() throws ElementNotFoundException {
        click(SHOW_POST);
        waiter.waitUntil(ExpectedConditions.visibilityOfElementLocated(POST_CONTENT), "Post textarea not visible");
        return isDisplayed(POST_CONTENT) && isDisplayed(CREATE_POST);
    }

    public String getBrandText() {
        return text(NAVBAR_BRAND);
    }
    public boolean hasNavLink(String label) {return driver.findElements(NAV_LINKS).stream().anyMatch(e -> e.getText().contains(label));}
    public boolean hasPostText(String postText) {return driver.getPageSource().contains(postText);}
}