package com.molvix.android.eventbuses;

import com.molvix.android.models.Episode;
import com.tonyodev.fetch2.Error;

public class EpisodeDownloadErrorException {
    private Episode episode;
    private Error error;

    public EpisodeDownloadErrorException(Episode episode, Error error) {
        this.episode = episode;
        this.error = error;
    }

    public Episode getEpisode() {
        return episode;
    }

    public Error getError() {
        return error;
    }

}
