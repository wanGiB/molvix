package com.molvix.android.eventbuses;

import com.molvix.android.models.Episode;

public class UpdateEpisode {
    private Episode episode;

    public UpdateEpisode(Episode episode) {
        this.episode = episode;
    }

    public Episode getEpisode() {
        return episode;
    }

}
