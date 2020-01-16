package com.molvix.android.managers;

import com.molvix.android.preferences.AppPrefs;

public class SeasonsManager {

    public static void prepareSeasonEpisodes(String seasonId, boolean prepare) {
        AppPrefs.prepareSeasonEpisodes(seasonId, prepare);
    }

    public static boolean wasSeasonEpisodesUnderPreparation(String seasonId) {
        return AppPrefs.wasSeasonEpisodesUnderPreparation(seasonId);
    }

    public static void fireSeasonUpdated(String seasonId, boolean value) {
        AppPrefs.fireSeasonUpdated(seasonId,value);
    }

}
