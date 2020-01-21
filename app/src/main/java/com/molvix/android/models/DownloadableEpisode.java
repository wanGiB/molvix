package com.molvix.android.models;

public class DownloadableEpisode {

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
