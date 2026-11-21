package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

/** A media attachment of a {@link Post}. */
public class Media extends ApiDto {

    @SerializedName("media_id")
    private String mediaId;
    @SerializedName("media_type")
    private String mediaType;

    public String getMediaId() {return mediaId;}
    public String getMediaType() {return mediaType;}
}