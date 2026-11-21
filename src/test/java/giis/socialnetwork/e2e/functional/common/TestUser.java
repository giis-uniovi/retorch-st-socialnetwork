package giis.socialnetwork.e2e.functional.common;

/** Immutable test-user credentials (lowercase alphanumeric username), unique per {@link #generate(String)} call. */
public final class TestUser {

    public final String username;
    public final String password;

    private TestUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static TestUser generate(String label) {
        long id = Unique.next();
        String username = (label + id).toLowerCase().replaceAll("[^a-z0-9]", "");
        return new TestUser(username, "pwd" + id);
    }
}
