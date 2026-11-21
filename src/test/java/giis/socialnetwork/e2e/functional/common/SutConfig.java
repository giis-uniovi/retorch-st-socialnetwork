package giis.socialnetwork.e2e.functional.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Test configuration; {@code SUT_URL}: system property, then env var, then {@code LOCALHOST_URL} in test.properties. */
public final class SutConfig {

    private static final String PROPERTIES_FILE = "test.properties";
    private static final String DEFAULT_BROWSER = "chrome";

    private SutConfig() {}

    public static String resolveSutUrl() throws IOException {
        String url = System.getProperty("SUT_URL") != null
                ? System.getProperty("SUT_URL")
                : System.getenv("SUT_URL");
        return url != null ? url : loadProperties().getProperty("LOCALHOST_URL");
    }

    /** Lowercase browser name from {@code BROWSER_USER}; defaults to chrome. */
    public static String resolveBrowser() throws IOException {
        String browser = loadProperties().getProperty("BROWSER_USER");
        return browser == null || browser.trim().isEmpty() ? DEFAULT_BROWSER : browser.trim().toLowerCase();
    }

    /** {@code TJOB_NAME} system property, or {@code null} outside CI. */
    public static String tjobName() {
        return System.getProperty("TJOB_NAME");
    }

    /** Default element wait in seconds ({@code -DWAIT_SECONDS}, default 10). */
    public static int waitSeconds() {
        return intProperty("WAIT_SECONDS", 10);
    }

    /** Page navigation wait in seconds ({@code -DNAV_WAIT_SECONDS}, default 30). */
    public static int navWaitSeconds() {
        return intProperty("NAV_WAIT_SECONDS", 30);
    }

    private static int intProperty(String name, int defaultValue) {
        String value = System.getProperty(name);
        return value == null || value.trim().isEmpty() ? defaultValue : Integer.parseInt(value.trim());
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = SutConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IOException(PROPERTIES_FILE + " not found on the test classpath");
            }
            properties.load(in);
        }
        return properties;
    }
}
