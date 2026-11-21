package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

/** Entry of the {@code get_follower} list. */
public class Follower extends ApiDto {

    @SerializedName("follower_id")
    private long followerId;

    public long getFollowerId() {return followerId;}
}