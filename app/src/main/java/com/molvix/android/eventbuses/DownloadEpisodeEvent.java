package com.molvix.android.eventbuses;

import com.molvix.android.models.Episode;

public class DownloadEpisodeEvent {

    public Episode episode;

    public DownloadEpisodeEvent(Episode episode) {
        this.episode = episode;
    }

    public Episode getEpisode() {
        return episode;
    }

}
