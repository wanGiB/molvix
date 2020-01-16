package com.molvix.android.managers;

import com.molvix.android.preferences.AppPrefs;

public class SeasonsManager {

    public static void prepareSeason(String seasonId, boolean prepare) {
        AppPrefs.prepareSeason(seasonId, prepare);
    }

    public static boolean wasSeasonUnderPreparation(String seasonId) {
        return AppPrefs.wasSeasonUnderPreparation(seasonId);
    }

}
