package com.molvix.android.eventbuses;

import com.molvix.android.models.Season;

public class LoadEpisodesForSeason {
    private Season season;
    public LoadEpisodesForSeason(Season season) {
        this.season = season;
    }

    public Season getSeason() {
        return season;
    }

}
