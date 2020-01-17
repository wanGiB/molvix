package com.molvix.android.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DownloadableEpisode extends RealmObject {

    @PrimaryKey
    private String episodeId;
    private Episode downloadableEpisode;

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public Episode getDownloadableEpisode() {
        return downloadableEpisode;
    }

    public void setDownloadableEpisode(Episode downloadableEpisode) {
        this.downloadableEpisode = downloadableEpisode;
    }

}
