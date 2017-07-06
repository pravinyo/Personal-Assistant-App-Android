package Utils.DataStructure;

/**
 * Created by Pravinyo on 3/28/2017.
 */

public class News {
    private String mAuthor;
    private String mTitle;
    private String mDescription;
    private String mUrl;
    private String mImageUrl;
    private String mPublishedAt;

    public News(String author, String title, String desc, String url, String image, String time){
        mAuthor=author;
        mTitle=title;
        mDescription=desc;
        mUrl=url;
        mImageUrl=image;
        mPublishedAt=time;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getPublishedAt() {
        return mPublishedAt;
    }

}
