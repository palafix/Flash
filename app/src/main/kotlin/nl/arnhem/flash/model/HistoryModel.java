package nl.arnhem.flash.model;

import android.media.Image;
import io.realm.RealmObject;

/**
 * Created by meeera on 6/10/17.Edited by Robin Bouwman for this app on 1/1/18
 **/

public class HistoryModel extends RealmObject {

    private String title;
    private String url;
    public String history;
    public String time;

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {this.history = history;}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
