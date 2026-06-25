package giis.socialnetwork.e2e.functional.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class Waiter {

    private static final Logger log = LoggerFactory.getLogger(Waiter.class);
    private static final int NAV_WAIT_SECONDS = 30;
    private static final int WAIT_DEFAULT = 10;
    private final WebDriverWait genericWaiter;
    private final WebDriverWait navWaiter;

    public Waiter(WebDriver driver) {
        genericWaiter = new WebDriverWait(driver, Duration.ofSeconds(WAIT_DEFAULT));
        navWaiter = new WebDriverWait(driver, Duration.ofSeconds(NAV_WAIT_SECONDS));
    }

    public void waitUntil(ExpectedCondition<?> condition, String errorMessage) {
        try {
            this.genericWaiter.until(condition);
        } catch (org.openqa.selenium.TimeoutException timeout) {
            log.error(errorMessage);
            throw new org.openqa.selenium.TimeoutException(
                    "\"" + errorMessage + "\" > " + timeout.getMessage());
        }
    }

    public void navWaitUntil(ExpectedCondition<?> condition, String errorMessage) {
        try {
            this.navWaiter.until(condition);
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new org.openqa.selenium.TimeoutException(
                    "\"" + errorMessage + "\" > " + e.getMessage());
        }
    }

    /** Login page (index.html) — login form username field. */
    public void waitForLoginPage() {
        log.debug("Waiting for login page to load");
        navWaitUntil(ExpectedConditions.visibilityOfElementLocated(By.name("username")),
                "Login page did not load");
    }

    /** Signup page — first_name field present. */
    public void waitForSignupPage() {
        log.debug("Waiting for signup page to load");
        navWaitUntil(ExpectedConditions.visibilityOfElementLocated(By.name("first_name")),
                "Signup page did not load");
    }

    /** Main feed page (main.html) — navbar brand link. */
    public void waitForMainPage() {
        log.debug("Waiting for main page to load");
        navWaitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a.navbar-brand")),
                "Main page did not load");
    }

    /** Contact page — follow form input (always visible). */
    public void waitForContactPage() {
        log.debug("Waiting for contact page to load");
        navWaitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("followee-name")),
                "Contact page did not load");
    }

    /**
     * Waits for a post with the given text to appear among the post cards on the
     * main page, retrying with a full refresh up to {@code maxRetries} times to
     * handle eventual consistency in the timeline service.
     */
    public void waitForPostText(String expectedText, WebDriver driver) {
        log.debug("Waiting for post text '{}' in timeline", expectedText);
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                waitUntil(ExpectedConditions.textToBePresentInElementLocated(
                        By.cssSelector("#card-block"), expectedText),
                        "Post text '" + expectedText + "' not yet in timeline");
                return;
            } catch (org.openqa.selenium.TimeoutException e) {
                if (attempt < maxRetries) {
                    log.debug("Post not found (attempt {}), refreshing...", attempt);
                    driver.navigate().refresh();
                    waitForMainPage();
                } else {
                    throw new org.openqa.selenium.TimeoutException(
                            "Post '" + expectedText + "' not found after " + maxRetries + " retries: "
                                    + e.getMessage());
                }
            }
        }
    }
}
