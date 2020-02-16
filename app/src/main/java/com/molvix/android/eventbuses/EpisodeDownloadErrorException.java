package com.molvix.android.eventbuses;

import com.molvix.android.models.Episode;

public class EpisodeDownloadErrorException {
    private Episode episode;

    public EpisodeDownloadErrorException(Episode episode) {
        this.episode = episode;
    }

    public Episode getEpisode() {
        return episode;
    }

}
