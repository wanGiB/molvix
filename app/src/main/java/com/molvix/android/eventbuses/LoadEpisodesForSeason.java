package com.molvix.android.eventbuses;

public class LoadEpisodesForSeason {
    private String seasonId;

    public LoadEpisodesForSeason(String seasonId) {
        this.seasonId = seasonId;
    }

    public String getSeasonId() {
        return seasonId;
    }
}
