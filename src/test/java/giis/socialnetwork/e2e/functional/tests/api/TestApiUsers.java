package giis.socialnetwork.e2e.functional.tests.api;

import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Validates the user-service endpoints exposed through the Nginx gateway:
 * <ul>
 *   <li>POST /api/user/register — register a new user (HTTP 200 after redirect); 400 on missing fields</li>
 *   <li>POST /api/user/login   — authenticate and receive JWT cookie; no cookie on bad credentials</li>
 * </ul>
 */
class TestApiUsers extends BaseApiClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("POST /api/user/register returns HTTP 200 and accepts the new user")
    void testRegisterUser() throws IOException {
        long ts = unique();
        String username = "reg" + ts;

        int status = registerUser("Alice", "Reg", username, "pwd" + ts);
        Assertions.assertEquals(200, status, "Registration must return HTTP 200 (redirect followed)");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/user/register with a missing field returns HTTP 400")
    void testRegisterMissingFieldReturns400() throws IOException {
        // Omit first_name; register.lua rejects incomplete arguments before reaching the user service.
        List<NameValuePair> incomplete = Arrays.asList(
                new BasicNameValuePair("last_name", "NoFirst"),
                new BasicNameValuePair("username", "missing" + unique()),
                new BasicNameValuePair("password", "pwd"));
        int status = postFormStatus(userUrl("/register"), incomplete);
        Assertions.assertEquals(400, status, "Registration with a missing field must return HTTP 400");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("POST /api/user/login returns HTTP 200 and sets the login_token JWT cookie")
    void testLoginUser() throws IOException {
        long ts = unique();
        String username = "log" + ts;
        String password = "pwd" + ts;

        registerUser("Bob", "Login", username, password);

        long userId = loginUser(username, password);
        Assertions.assertTrue(userId > 0, "Parsed user_id from JWT must be a positive integer");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("POST /api/user/login with a wrong password sets no login_token cookie")
    void testLoginWrongPasswordSetsNoToken() throws IOException {
        long ts = unique();
        String username = "wrongpw" + ts;
        registerUser("Carol", "Wrong", username, "pwd" + ts);

        Assertions.assertAll(
                () -> Assertions.assertTrue(loginSetsToken(username, "pwd" + ts),
                        "Login with the correct password must issue a login_token cookie"),
                () -> Assertions.assertFalse(loginSetsToken(username, "definitely-wrong"),
                        "Login with a wrong password must not issue a login_token cookie")
        );
    }
}
