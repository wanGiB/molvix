package com.molvix.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;

@SuppressWarnings("unused")
public class AppPrefs {

    private static SharedPreferences appSharedPreferences;

    public static SharedPreferences getAppPreferences() {
        if (appSharedPreferences == null) {
            appSharedPreferences = ApplicationLoader.getInstance()
                    .getSharedPreferences(AppConstants.APP_PREFS_NAME, Context.MODE_PRIVATE);
        }
        return appSharedPreferences;
    }

    @SuppressLint("ApplySharedPref")
    public static void lockCaptchaSolver(String episodeId) {
        getAppPreferences().edit().putString(AppConstants.CAPTCHA_SOLVING, episodeId).commit();
    }

    public static boolean isCaptchaSolvable() {
        String existingProcess = getAppPreferences().getString(AppConstants.CAPTCHA_SOLVING, null);
        return existingProcess == null;
    }

    @SuppressLint("ApplySharedPref")
    public static void unLockCaptchaSolver(String episodeId) {
        getAppPreferences().edit().putString(AppConstants.CAPTCHA_SOLVING, null).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void prepareSeasonEpisodes(String seasonId, boolean prepare) {
        getAppPreferences().edit().putBoolean(seasonId + AppConstants.EPISODES, prepare).commit();
    }

    public static boolean wasSeasonEpisodesUnderPreparation(String seasonId) {
        return getAppPreferences().getBoolean(seasonId + AppConstants.EPISODES, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void fireSeasonUpdated(String seasonId, boolean value) {
        getAppPreferences().edit().putBoolean(seasonId, value).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void fireEpisodeUpdated(String episodeId, boolean value) {
        getAppPreferences().edit().putBoolean(episodeId, value).commit();
    }

}
