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
import java.io.UncheckedIOException;

@ExtendWith(LifecycleJunit5.class)
public class BaseLoggedClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseLoggedClass.class);

    private static final SeleManager seleManager = createSeleManager();

    protected static String sutUrl;
    protected WebDriver driver;
    protected Waiter waiter;

    private static SeleManager createSeleManager() {
        String tjob = SutConfig.tjobName();
        return new SeleManager(new SelemaConfig()
                .setReportSubdir("target/containerlogs/" + (tjob == null ? "" : tjob))
                .setName(tjob == null ? "locallogs" : tjob));
    }

    @BeforeAll
    static void setupAll() throws IOException {
        sutUrl = SutConfig.resolveSutUrl();
        setupBrowser();
        log.debug("Global browser setup complete. SUT: {}", sutUrl);
    }

    protected static void setupBrowser() throws IOException {
        boolean remote = System.getenv("SELENOID_PRESENT") != null;
        // locally, skip Chrome's proxy auto-detection (adds ~20 s to the first navigation of every new browser)
        String[] args = remote
                ? new String[]{"--start-maximized", "--incognito"}
                : new String[]{"--start-maximized", "--incognito", "--no-proxy-server"};
        seleManager.setBrowser(SutConfig.resolveBrowser()).setArguments(args);
        if (remote) {
            log.debug("Setting up Selenium WebDriver with Selenium-hub");
            seleManager.setDriverUrl("http://selenium-hub:4444/wd/hub")
                    .add(new DynamicGridBrowserService().setVideo())
                    .add(new WatermarkService().setDelayOnFailure(3));
        }
    }

    @BeforeEach
    void setup(TestInfo testInfo) {
        log.debug("Starting setup for test: {}", testInfo.getDisplayName());
        driver = seleManager.getDriver();
        waiter = new Waiter(driver);
        driver.get(sutUrl + "/index.html");
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.debug("Tearing down test: {}", testInfo.getDisplayName());
        driver.get(sutUrl + "/index.html");
    }


    protected LoginPage loginPage() {return new LoginPage(driver, waiter, sutUrl);}
    protected SignupPage signupPage() {return new SignupPage(driver, waiter, sutUrl);}


    /** Registers {@code user} through the HTTP API (fast fixture) and returns the numeric user id. */
    protected long registerViaApi(TestUser user) {
        try (SocialNetworkApi api = new SocialNetworkApi(sutUrl)) {
            int status = api.register("E2E", "User", user.username, user.password);
            if (status != 200) {
                throw new IllegalStateException("API registration of " + user.username + " failed with HTTP " + status);
            }
            return api.login(user.username, user.password);
        } catch (IOException e) {
            throw new UncheckedIOException("API fixture failed for " + user.username, e);
        }
    }

    /** Registers {@code user} via the API and logs in through the UI, landing on the main feed page. */
    protected MainPage registerAndLogin(TestUser user) {
        registerViaApi(user);
        return loginPage().open().login(user.username, user.password);
    }
}
