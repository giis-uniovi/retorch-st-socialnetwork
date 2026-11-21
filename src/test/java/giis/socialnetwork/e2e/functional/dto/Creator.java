package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

/** The author of a {@link Post}. */
public class Creator extends ApiDto {

    @SerializedName("user_id")
    private long userId;
    private String username;

    public long getUserId() {return userId;}
    public String getUsername() {return username;}
}