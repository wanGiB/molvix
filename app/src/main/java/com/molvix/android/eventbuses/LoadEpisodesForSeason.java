package com.molvix.android.eventbuses;

import com.molvix.android.models.Season;

public class LoadEpisodesForSeason {
    private Season season;
    private boolean showLoadingProgress;

    public LoadEpisodesForSeason(Season season, boolean showLoadingProgress) {
        this.season = season;
        this.showLoadingProgress = showLoadingProgress;
    }

    public boolean canShowLoadingProgress() {
        return showLoadingProgress;
    }

    public Season getSeason() {
        return season;
    }

}
