package giis.socialnetwork.e2e.functional.utils;

import giis.socialnetwork.e2e.functional.common.ElementNotClickableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Click {

    private static final Logger log = LoggerFactory.getLogger(Click.class);

    private Click() {}

    /** Selenium click, falling back to a JS click if it fails (e.g. obscured element). */
    public static void element(WebDriver driver, WebElement element) {
        try {
            log.debug("Clicking element: {}", element);
            element.click();
        } catch (Exception e) {
            log.debug("Standard click failed ({}), retrying via JavaScript", e.getMessage());
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception jsEx) {
                throw new ElementNotClickableException("Could not click element: " + jsEx.getMessage(), jsEx);
            }
        }
    }
}
