package com.molvix.android.eventbuses;

import com.molvix.android.models.Season;

public class UpdateSeason {
    private Season season;

    public UpdateSeason(Season season) {
        this.season = season;
    }

    public Season getSeason() {
        return season;
    }

}
