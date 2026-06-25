package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.common.ElementNotFoundException;
import giis.socialnetwork.e2e.functional.utils.Click;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Shared infrastructure for every page object: holds the {@link WebDriver}, {@link Waiter}
 * and SUT base URL, and exposes the common navigation/interaction helpers so concrete pages
 * only declare their own locators and page-specific behaviour.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Waiter waiter;
    protected final String sutUrl;

    protected BasePage(WebDriver driver, Waiter waiter, String sutUrl) {
        this.driver = driver;
        this.waiter = waiter;
        this.sutUrl = sutUrl;
    }

    /**Navigates to {@code sutUrl + path}; callers follow with a page-specific wait.*/
    protected void navigate(String path) {
        driver.get(sutUrl + path);
    }

    protected void fill(By locator, String value) {
        waiter.waitUntil(ExpectedConditions.visibilityOfElementLocated(locator), "Field not visible: " + locator);
        try {
            type(locator, value);
        } catch (WebDriverException domChangedMidInteraction) {
            // The document changed while typing (stale node during a page transition); re-locate once.
            type(locator, value);
        }
    }

    private void type(By locator, String value) {
        WebElement field = driver.findElement(locator);
        field.clear();
        field.sendKeys(value);
    }

    /**Clicks once the element is clickable, falling back to a JS click if it is obscured.*/
    protected void click(By locator) throws ElementNotFoundException {
        waiter.waitUntil(ExpectedConditions.elementToBeClickable(locator), "Element not clickable: " + locator);
        Click.element(driver, driver.findElement(locator));
    }

    protected String text(By locator) {
        return driver.findElement(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        return driver.findElement(locator).isDisplayed();
    }

    /*** True if at least one element matches — use for containers whose CSS visibility is unreliable.*/
    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }
}