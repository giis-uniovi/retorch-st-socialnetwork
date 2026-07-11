package giis.socialnetwork.e2e.functional.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Resolves the SUT base URL shared by the API and UI base classes:
 * {@code SUT_URL} system property, then {@code SUT_URL} environment variable,
 * then {@code LOCALHOST_URL} from {@code src/test/resources/test.properties}.
 */
public final class SutConfig {

    private SutConfig() {}

    public static String resolveSutUrl() throws IOException {
        String url = System.getProperty("SUT_URL") != null
                ? System.getProperty("SUT_URL")
                : System.getenv("SUT_URL");
        if (url != null) {
            return url;
        }
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("src/test/resources/test.properties")));
        return properties.getProperty("LOCALHOST_URL");
    }
}
