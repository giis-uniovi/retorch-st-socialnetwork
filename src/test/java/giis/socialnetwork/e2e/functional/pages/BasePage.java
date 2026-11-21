package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.utils.Click;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Base page object: driver, waiter, SUT URL and common navigation/interaction helpers. */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Waiter waiter;
    protected final String sutUrl;

    protected BasePage(WebDriver driver, Waiter waiter, String sutUrl) {
        this.driver = driver;
        this.waiter = waiter;
        this.sutUrl = sutUrl;
    }

    /** Locator of the element whose visibility marks this page as loaded. */
    protected abstract By loadedMarker();

    /** Waits (navigation timeout) until this page's loaded marker is visible. */
    public void waitUntilLoaded() {
        waiter.navWaitUntil(ExpectedConditions.visibilityOfElementLocated(loadedMarker()),
                getClass().getSimpleName() + " did not load");
    }

    /**Navigates to {@code sutUrl + path}; callers follow with {@link #waitUntilLoaded()}.*/
    protected void navigate(String path) {
        driver.get(sutUrl + path);
    }

    protected void fill(By locator, String value) {
        waiter.waitUntil(ExpectedConditions.visibilityOfElementLocated(locator), "Field not visible: " + locator);
        try {
            type(locator, value);
        } catch (WebDriverException domChangedMidInteraction) {
            // stale node during a page transition: re-locate once
            type(locator, value);
        }
    }

    private void type(By locator, String value) {
        WebElement field = driver.findElement(locator);
        field.clear();
        field.sendKeys(value);
    }

    /**Clicks once the element is clickable, falling back to a JS click if it is obscured.*/
    protected void click(By locator) {
        waiter.waitUntil(ExpectedConditions.elementToBeClickable(locator), "Element not clickable: " + locator);
        Click.element(driver, driver.findElement(locator));
    }

    protected String text(By locator) {
        return driver.findElement(locator).getText();
    }

    /** True if the element exists and is displayed; false if it is absent. */
    protected boolean isDisplayed(By locator) {
        java.util.List<WebElement> found = driver.findElements(locator);
        return !found.isEmpty() && found.get(0).isDisplayed();
    }

    /*** True if at least one element matches — use for containers whose CSS visibility is unreliable.*/
    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }
}