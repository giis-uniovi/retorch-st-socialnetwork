package giis.socialnetwork.e2e.functional.tests.api;

import giis.retorch.annotations.AccessMode;
import giis.socialnetwork.e2e.functional.common.BaseApiClass;
import giis.socialnetwork.e2e.functional.common.SocialNetworkApi;
import giis.socialnetwork.e2e.functional.common.TestUser;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static giis.socialnetwork.e2e.functional.utils.ApiAssert.assertStatus;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_OK;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_BAD_REQUEST;
import static giis.socialnetwork.e2e.functional.utils.HttpStatus.HTTP_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** API tests for {@code POST /api/user/register} and {@code /api/user/login}. */
@Tag("api")
class TestApiUsers extends BaseApiClass {

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIRegisterUser")
    void testAPIRegisterUser() throws IOException {
        TestUser user = TestUser.generate("reg");

        int status = registerUser("Alice", "Reg", user.username, user.password);
        assertStatus(HTTP_OK, status, "Registration (redirect followed)");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPIRegisterMissingField")
    void testAPIRegisterMissingField() throws IOException {
        // first_name omitted: register.lua rejects incomplete arguments.
        List<NameValuePair> incomplete = Arrays.<NameValuePair>asList(
                new BasicNameValuePair("last_name", "NoFirst"),
                new BasicNameValuePair("username", "missing" + unique()),
                new BasicNameValuePair("password", "pwd"));
        int status = anonymous.register(incomplete);
        assertStatus(HTTP_BAD_REQUEST, status, "Registration with a missing field");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPILoginUser")
    void testAPILoginUser() throws IOException {
        TestUser user = TestUser.generate("log");

        registerOrFail(user, "Bob", "Login");

        long userId = loginUser(user.username, user.password);
        assertTrue(userId > 0, "Parsed user_id from JWT must be a positive integer");
    }

    @Tag("known-quirk")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPIRegisterDuplicateUsername")
    void testAPIRegisterDuplicateUsername() throws IOException {
        // Known quirk: a duplicate register is a 200.
        TestUser user = TestUser.generate("dup");
        String otherPassword = "other" + unique();

        int first = registerUser("Dave", "Dup", user.username, user.password);
        // The Lua gateway swallows the user-service rejection of void Thrift methods, hence the 200.
        int second = registerUser("Dave", "Dup", user.username, otherPassword);
        assertAll(
                () -> assertStatus(HTTP_OK, first, "First registration"),
                () -> assertStatus(HTTP_OK, second,
                        "Duplicate registration (redirects like a success, gateway swallows the rejection)"),
                () -> assertTrue(loginSetsToken(user.username, user.password),
                        "The original password must still authenticate after a duplicate attempt"),
                () -> assertFalse(loginSetsToken(user.username, otherPassword),
                        "The duplicate attempt must not overwrite the account password")
        );
    }

    @Tag("known-quirk")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestAPILoginWrongPasswordStatus")
    void testAPILoginWrongPasswordStatus() throws IOException {
        // Known quirk: login.lua surfaces an authentication failure as HTTP 500.
        TestUser user = TestUser.generate("wrongst");
        registerOrFail(user, "Eve", "Status");

        int status = newSession().loginStatus(user.username, "definitely-wrong");
        assertStatus(HTTP_SERVER_ERROR, status, "Login with a wrong password");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("TestLoginWrongPassword")
    void testLoginWrongPassword() throws IOException {
        TestUser user = TestUser.generate("wrongpw");
        registerOrFail(user, "Carol", "Wrong");

        assertAll(
                () -> assertTrue(loginSetsToken(user.username, user.password),
                        "Login with the correct password must issue a login_token cookie"),
                () -> assertFalse(loginSetsToken(user.username, "definitely-wrong"),
                        "Login with a wrong password must not issue a login_token cookie")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("TestAPILoginUnknownUserIssuesNoToken")
    void testAPILoginUnknownUserIssuesNoToken() throws IOException {
        SocialNetworkApi session = newSession();
        assertFalse(session.tryLogin("neverregistered" + unique(), "pwd").isPresent(),
                "Login for a never-registered user must not issue a login_token cookie");
    }
}
