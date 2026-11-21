package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

/** A link in a {@link Post}'s text. */
public class Url extends ApiDto {

    @SerializedName("shortened_url")
    private String shortenedUrl;
    @SerializedName("expanded_url")
    private String expandedUrl;

    public String getShortenedUrl() {return shortenedUrl;}
    public String getExpandedUrl() {return expandedUrl;}
}