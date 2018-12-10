package nl.arnhem.flash.model;

import io.realm.RealmObject;

/**
 * Created by meeera on 6/10/17.Edited by Robin Bouwman for this app on 1/1/18
 **/

public class BookmarkModel extends RealmObject {

    private String title;
    private String url;
    private String bookMark;
    //public Icon favicon;

    public String getBookMark() {
        return bookMark;
    }

    public void setBookMark(String bookMark) {
        this.bookMark = bookMark;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    //public Icon getFavicon() {return favicon;}

    //public void setFavicon(Icon favicon) {this.favicon = favicon;}
}
