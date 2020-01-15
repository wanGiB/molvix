package com.molvix.android.eventbuses;

import com.molvix.android.models.Episode;

public class EpisodeResolutionEvent {
    private Episode episode;

    public EpisodeResolutionEvent(Episode episode) {
        this.episode = episode;
    }

    public Episode getEpisode() {
        return episode;
    }

}
