package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class ContactPage extends BasePage {

    private static final By FOLLOWER_LIST = By.id("follower-list");
    private static final By FOLLOWEE_LIST = By.id("followee-list");
    // The follow and unfollow forms share duplicate element ids, so locate each by its form action.
    private static final By FOLLOW_INPUT = By.cssSelector("form[action='api/user/follow'] input[name='followee_name']");
    private static final By FOLLOW_SUBMIT = By.cssSelector("form[action='api/user/follow'] input[value='Follow']");
    private static final By UNFOLLOW_INPUT = By.cssSelector("form[action='api/user/unfollow'] input[name='followee_name']");
    private static final By UNFOLLOW_SUBMIT = By.cssSelector("form[action='api/user/unfollow'] input[value='Unfollow']");
    private static final By FOLLOWEE_IDS = By.cssSelector("#followee-list .followee-id");

    public ContactPage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    @Override
    protected By loadedMarker() {return By.id("followee-name");}

    public ContactPage open() {
        navigate("/contact.html");
        waitUntilLoaded();
        return this;
    }

    public boolean isFollowerListDisplayed() {return isPresent(FOLLOWER_LIST);}
    public boolean isFolloweeListDisplayed() {return isPresent(FOLLOWEE_LIST);}

    /** Submits a follow/unfollow form; no re-navigation, it would race the in-flight POST. */
    private ContactPage submitFollowForm(By input, By submit, String username) {
        fill(input, username);
        click(submit);
        waitUntilLoaded();
        return this;
    }

    public ContactPage followUser(String username) {return submitFollowForm(FOLLOW_INPUT, FOLLOW_SUBMIT, username);}
    public ContactPage unfollowUser(String username) {return submitFollowForm(UNFOLLOW_INPUT, UNFOLLOW_SUBMIT, username);}

    /*** Waits (with the longer navigation timeout) for the asynchronous get-followee XHR to render.*/
    public void waitForFolloweeCount(int expected) {
        waiter.navWaitUntil(d -> followeeCount() == expected,
                "Followee list did not reach " + expected + " entries");
    }

    public int followeeCount() {
        return followeeIds().size();
    }

    /*** Numeric ids of the followees rendered by get-followee.js, in page order.*/
    public List<String> followeeIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement e : driver.findElements(FOLLOWEE_IDS)) {
            String t = e.getText().trim();
            if (t.matches("\\d+")) {
                ids.add(t);
            }
        }
        return ids;
    }
}
