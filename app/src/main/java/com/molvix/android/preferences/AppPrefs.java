package com.molvix.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;

@SuppressWarnings("unused")
public class AppPrefs {

    private static SharedPreferences appSharedPreferences;

    private static SharedPreferences getAppPreferences() {
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
    public static void prepareSeason(String seasonId, boolean prepare) {
        getAppPreferences().edit().putBoolean(seasonId, prepare).commit();
    }

    public static boolean wasSeasonUnderPreparation(String seasonId) {
        return getAppPreferences().getBoolean(seasonId, false);
    }

}
