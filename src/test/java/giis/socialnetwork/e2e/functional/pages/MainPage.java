package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class MainPage extends TimelinePage {

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
        waitUntilLoaded();
        return this;
    }

    public boolean hasNavLink(String label) {return driver.findElements(NAV_LINKS).stream().anyMatch(e -> e.getText().contains(label));}
    public ProfilePage openProfile() {
        return new ProfilePage(driver, waiter, sutUrl).open();
    }
    public ContactPage goToContacts() {
        return new ContactPage(driver, waiter, sutUrl).open();
    }
    public String getBrandText() {
        return text(NAVBAR_BRAND);
    }

    /** Opens the compose panel and waits until the post textarea is visible. */
    public MainPage openComposeForm() {
        click(SHOW_POST);
        waiter.waitUntil(ExpectedConditions.visibilityOfElementLocated(POST_CONTENT), "Post textarea not visible");
        return this;
    }

    public boolean isComposeFormVisible() {return isDisplayed(POST_CONTENT) && isDisplayed(CREATE_POST);}

    public MainPage composePost(String postText) {
        openComposeForm();
        fill(POST_CONTENT, postText);
        click(CREATE_POST);
        return this;
    }

}