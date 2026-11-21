package giis.socialnetwork.e2e.functional.utils;

import giis.socialnetwork.e2e.functional.common.SutConfig;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class Waiter {

    private static final Logger log = LoggerFactory.getLogger(Waiter.class);
    private final WebDriverWait genericWaiter;
    private final WebDriverWait navWaiter;

    public Waiter(WebDriver driver) {
        genericWaiter = new WebDriverWait(driver, Duration.ofSeconds(SutConfig.waitSeconds()));
        navWaiter = new WebDriverWait(driver, Duration.ofSeconds(SutConfig.navWaitSeconds()));
    }

    /** Waits with the default (short) timeout. */
    public void waitUntil(ExpectedCondition<?> condition, String errorMessage) {until(genericWaiter, condition, errorMessage);}
    /** Waits with the longer navigation timeout. */
    public void navWaitUntil(ExpectedCondition<?> condition, String errorMessage) {until(navWaiter, condition, errorMessage);}

    private static void until(WebDriverWait waiter, ExpectedCondition<?> condition, String errorMessage) {
        try {
            waiter.until(condition);
        } catch (TimeoutException timeout) {
            log.error(errorMessage);
            throw new TimeoutException("\"" + errorMessage + "\" > " + timeout.getMessage(), timeout);
        }
    }
}
