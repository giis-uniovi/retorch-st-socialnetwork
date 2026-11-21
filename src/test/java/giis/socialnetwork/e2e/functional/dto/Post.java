package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** A post as returned by the timeline read endpoints. */
public class Post extends ApiDto {

    @SerializedName("post_id")
    private String postId;
    private Creator creator;
    private String text;
    private Long timestamp;
    @SerializedName("user_mentions")
    private List<UserMention> userMentions;
    private List<Media> media;
    private List<Url> urls;

    public String getPostId() {return postId;}
    public Creator getCreator() {return creator;}
    public String getText() {return text;}
    public Long getTimestamp() {return timestamp;}
    public List<UserMention> getUserMentions() {return orEmpty(userMentions);}
    public List<Media> getMedia() {return orEmpty(media);}
    public List<Url> getUrls() {return orEmpty(urls);}
}