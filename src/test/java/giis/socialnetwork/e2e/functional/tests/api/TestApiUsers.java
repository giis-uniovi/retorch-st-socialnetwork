package giis.socialnetwork.e2e.functional.tests.api;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
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
    @DisplayName("TestAPIRegisterUser")
    void testAPIRegisterUser() throws IOException {
        long ts = unique();
        String username = "reg" + ts;

        int status = registerUser("Alice", "Reg", username, "pwd" + ts);
        Assertions.assertEquals(200, status, "Registration must return HTTP 200 (redirect followed)");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIRegisterMissingField")
    void testAPIRegisterMissingField() throws IOException {
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
    @DisplayName("TestAPILoginUser")
    void testAPILoginUser() throws IOException {
        long ts = unique();
        String username = "log" + ts;
        String password = "pwd" + ts;

        registerUser("Bob", "Login", username, password);

        long userId = loginUser(username, password);
        Assertions.assertTrue(userId > 0, "Parsed user_id from JWT must be a positive integer");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIRegisterDuplicateUsername")
    void testAPIRegisterDuplicateUsername() throws IOException {
        long ts = unique();
        String username = "dup" + ts;
        String originalPassword = "pwd" + ts;
        String otherPassword = "other" + ts;

        int first = registerUser("Dave", "Dup", username, originalPassword);
        // user-service rejects the duplicate with a ServiceException, but the Lua
        // gateway swallows exceptions of void Thrift methods (generated bindings
        // never re-raise result.se), so the HTTP status is still a 200 redirect.
        // The observable contract is that the existing account is left untouched.
        int second = registerUser("Dave", "Dup", username, otherPassword);
        Assertions.assertAll(
                () -> Assertions.assertEquals(200, first, "First registration must succeed"),
                () -> Assertions.assertEquals(200, second,
                        "Duplicate registration redirects like a success (gateway swallows the rejection)"),
                () -> Assertions.assertTrue(loginSetsToken(username, originalPassword),
                        "The original password must still authenticate after a duplicate attempt"),
                () -> Assertions.assertFalse(loginSetsToken(username, otherPassword),
                        "The duplicate attempt must not overwrite the account password")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPILoginWrongPasswordStatus")
    void testAPILoginWrongPasswordStatus() throws IOException {
        long ts = unique();
        String username = "wrongst" + ts;
        registerUser("Eve", "Status", username, "pwd" + ts);

        // login.lua surfaces the user-service authentication failure as HTTP 500.
        int status = postFormStatus(userUrl("/login"), loginPayload(username, "definitely-wrong"));
        Assertions.assertEquals(500, status, "Login with a wrong password must return HTTP 500");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestLoginWrongPassword")
    void testLoginWrongPassword() throws IOException {
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
