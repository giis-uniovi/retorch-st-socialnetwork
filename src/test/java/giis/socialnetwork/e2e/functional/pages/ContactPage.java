package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ContactPage extends BasePage {

    private static final By FOLLOWER_LIST   = By.id("follower-list");
    private static final By FOLLOWEE_LIST   = By.id("followee-list");
    // The follow and unfollow forms share duplicate element ids, so locate each by its form action.
    private static final By FOLLOW_INPUT    = By.cssSelector("form[action='api/user/follow'] input[name='followee_name']");
    private static final By FOLLOW_SUBMIT   = By.cssSelector("form[action='api/user/follow'] input[value='Follow']");
    private static final By UNFOLLOW_INPUT  = By.cssSelector("form[action='api/user/unfollow'] input[name='followee_name']");
    private static final By UNFOLLOW_SUBMIT = By.cssSelector("form[action='api/user/unfollow'] input[value='Unfollow']");
    private static final By FOLLOWEE_IDS    = By.cssSelector("#followee-list .followee-id");

    public ContactPage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    public ContactPage open() {
        navigate("/contact.html");
        waiter.waitForContactPage();
        return this;
    }

    public boolean isFollowerListDisplayed() { return isPresent(FOLLOWER_LIST); }
    public boolean isFolloweeListDisplayed() { return isPresent(FOLLOWEE_LIST); }

    /**
     * Submits the follow form. Both follow.lua and unfollow.lua redirect to contact.html, so the
     * submit click drives the navigation itself — we only wait for the contact page to settle.
     * Adding an explicit re-navigation here would race (and cancel) the in-flight POST under load.
     */
    public ContactPage followUser(String username) throws ElementNotFoundException {
        fill(FOLLOW_INPUT, username);
        click(FOLLOW_SUBMIT);
        waiter.waitForContactPage();
        return this;
    }

    public ContactPage unfollowUser(String username) throws ElementNotFoundException {
        fill(UNFOLLOW_INPUT, username);
        click(UNFOLLOW_SUBMIT);
        waiter.waitForContactPage();
        return this;
    }

    /** Number of followee entries populated with a numeric id by get-followee.js. */
    public int followeeCount() {
        return (int) driver.findElements(FOLLOWEE_IDS).stream()
                .map(WebElement::getText)
                .filter(t -> t.matches("\\d+"))
                .count();
    }

    /** Waits (with the longer navigation timeout) for the asynchronous get-followee XHR to render. */
    public void waitForFolloweeCount(int expected) {
        waiter.navWaitUntil(d -> followeeCount() == expected,
                "Followee list did not reach " + expected + " entries");
    }
}
