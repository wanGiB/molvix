package com.molvix.android.models;

import com.orm.dsl.Table;
import com.orm.dsl.Unique;

@Table
public class DownloadableEpisode {

    @Unique
    private String episodeId;
    private Episode downloadableEpisode;
    private long id;

    public DownloadableEpisode() {

    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

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
