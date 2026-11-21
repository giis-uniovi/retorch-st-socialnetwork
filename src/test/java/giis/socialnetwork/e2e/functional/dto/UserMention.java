package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

/** An {@code @mention} extracted from a {@link Post}'s text. */
public class UserMention extends ApiDto {

    @SerializedName("user_id")
    private long userId;
    private String username;

    public long getUserId() {return userId;}
    public String getUsername() {return username;}
}