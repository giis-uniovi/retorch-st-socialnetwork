package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

/** Entry of the {@code get_followee} list. */
public class Followee extends ApiDto {

    @SerializedName("followee_id")
    private long followeeId;

    public long getFolloweeId() {return followeeId;}
}