package giis.socialnetwork.e2e.functional.common;

import giis.selema.framework.junit5.LifecycleJunit5;
import giis.selema.manager.SeleManager;
import giis.selema.manager.SelemaConfig;
import giis.selema.services.browser.DynamicGridBrowserService;
import giis.selema.services.impl.WatermarkService;
import giis.socialnetwork.e2e.functional.pages.LoginPage;
import giis.socialnetwork.e2e.functional.pages.MainPage;
import giis.socialnetwork.e2e.functional.pages.SignupPage;
import giis.socialnetwork.e2e.functional.utils.Waiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ExtendWith(LifecycleJunit5.class)
public class BaseLoggedClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseLoggedClass.class);

    private static final SeleManager seleManager = new SeleManager(new SelemaConfig()
            .setReportSubdir("target/containerlogs/"
                    + (System.getProperty("TJOB_NAME") == null ? "" : System.getProperty("TJOB_NAME")))
            .setName(System.getProperty("TJOB_NAME") == null ? "locallogs" : System.getProperty("TJOB_NAME")));

    protected static String sutUrl;
    protected WebDriver driver;
    protected Waiter waiter;

    @BeforeAll
    static void setupAll() throws IOException {
        log.info("Starting global browser setup");
        sutUrl = SutConfig.resolveSutUrl();
        setupBrowser();
        log.info("Global browser setup complete. SUT: {}", sutUrl);
    }

    protected static void setupBrowser() {
        seleManager.setBrowser("chrome").setArguments(new String[]{"--start-maximized", "--incognito"});
        if (System.getenv("SELENOID_PRESENT") != null) {
            log.debug("Setting up Selenium WebDriver with Selenium-hub");
            seleManager.setDriverUrl("http://selenium-hub:4444/wd/hub")
                    .add(new DynamicGridBrowserService().setVideo())
                    .add(new WatermarkService().setDelayOnFailure(3));
        }
    }

    @BeforeEach
    void setup(TestInfo testInfo) {
        log.info("Starting setup for test: {}", testInfo.getDisplayName());
        driver = seleManager.getDriver();
        waiter = new Waiter(driver);
        driver.get(sutUrl + "/index.html");
        log.info("Setup complete, starting: {}", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("Tearing down test: {}", testInfo.getDisplayName());
        driver.get(sutUrl + "/index.html");
    }

    // ── UI user fixture ───────────────────────────────────────────────────────

    /** Immutable credentials for a UI test user, unique per {@link #newUser(String)} call. */
    public static final class TestUser {
        public final String username;
        public final String password;

        TestUser(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /** Creates unique credentials derived from {@code label} and the current timestamp. */
    protected TestUser newUser(String label) {
        long ts = System.currentTimeMillis();
        return new TestUser(label + ts, "pwd" + ts);
    }

    /** Registers {@code user} through the signup page, landing on the login page. */
    protected LoginPage registerUser(TestUser user) {
        return new SignupPage(driver, waiter, sutUrl).open()
                .register("E2E", "User", user.username, user.password);
    }

    /** Registers {@code user} and logs in, landing on the main feed page. */
    protected MainPage registerAndLogin(TestUser user) {
        return registerUser(user).login(user.username, user.password);
    }
}
