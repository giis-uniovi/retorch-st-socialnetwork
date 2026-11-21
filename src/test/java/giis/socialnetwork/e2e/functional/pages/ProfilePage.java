package giis.socialnetwork.e2e.functional.pages;

import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.openqa.selenium.WebDriver;

/** profile.html: renders the logged-in user's own user-timeline. */
public class ProfilePage extends TimelinePage {

    public ProfilePage(WebDriver driver, Waiter waiter, String sutUrl) {
        super(driver, waiter, sutUrl);
    }

    public ProfilePage open() {
        navigate("/profile.html");
        waitUntilLoaded();
        return this;
    }
}