package com.molvix.android.managers;

import com.molvix.android.preferences.AppPrefs;

public class SeasonsManager {

    static void addToRefreshedSeasons(String seasonId) {
        AppPrefs.addToRefreshedSeasons(seasonId);
    }

    public static boolean canFetchSeasonDetails(String seasonId) {
        return AppPrefs.canRefreshSeasonDetails(seasonId);
    }

    public static void setSeasonRefreshable(String seasonId){
        AppPrefs.setSeasonRefreshable(seasonId);
    }
}
