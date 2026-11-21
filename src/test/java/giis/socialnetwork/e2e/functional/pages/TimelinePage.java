package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Base for pages that render a timeline into {@code #card-block} (main = home-timeline, profile = user-timeline). */
public abstract class TimelinePage extends BasePage {

    private static final int MAX_REFRESH_RETRIES = 5;
    private static final By CARD_BLOCK = By.cssSelector("#card-block");

    protected TimelinePage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    @Override
    protected By loadedMarker() {return By.cssSelector("a.navbar-brand");}

    /** Waits for a post with the given text, refreshing and retrying (timeline is eventually consistent). */
    public void waitForPost(String text) {
        for (int attempt = 1; attempt <= MAX_REFRESH_RETRIES; attempt++) {
            try {
                waiter.waitUntil(ExpectedConditions.textToBePresentInElementLocated(CARD_BLOCK, text),
                        "Post text '" + text + "' not yet in timeline");
                return;
            } catch (TimeoutException e) {
                if (attempt == MAX_REFRESH_RETRIES) {
                    throw new TimeoutException(
                            "Post '" + text + "' not found after " + MAX_REFRESH_RETRIES + " retries: " + e.getMessage(), e);
                }
                driver.navigate().refresh();
                waitUntilLoaded();
            }
        }
    }

    public boolean hasPost(String text) {return isPresent(CARD_BLOCK) && text(CARD_BLOCK).contains(text);}
    public boolean hasMentionLink(String username) {return isPresent(By.cssSelector("#card-block a[href*='profile.html?username=" + username + "']"));}
}
