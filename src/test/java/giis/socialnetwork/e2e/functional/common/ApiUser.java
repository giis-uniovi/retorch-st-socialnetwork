package giis.socialnetwork.e2e.functional.common;

import giis.socialnetwork.e2e.functional.dto.Followee;
import giis.socialnetwork.e2e.functional.dto.Follower;

import java.io.IOException;
import java.util.List;

/** A registered and logged-in test user together with its own API session. */
public final class ApiUser {

    public final String username;
    public final String password;
    public final long userId;
    public final SocialNetworkApi api;

    public ApiUser(String username, String password, long userId, SocialNetworkApi api) {
        this.username = username;
        this.password = password;
        this.userId = userId;
        this.api = api;
    }

    public List<Follower> followers() throws IOException {return api.followers();}

    public List<Followee> followees() throws IOException {return api.followees();}
}
