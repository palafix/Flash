package nl.arnhem.flash.model;

import android.app.FragmentManager;
import android.graphics.pdf.PdfDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.RealmObject;

/**
 * Created by meeera on 6/10/17.
 **/

public class BookmarkModel extends RealmObject {

    private String title;
    private String url;
    private String bookMark;

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
}
