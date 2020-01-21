package com.molvix.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;

import java.util.HashSet;
import java.util.Set;

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

    @SuppressLint("ApplySharedPref")
    public static void setDailyMoviesRecommendability(boolean newValue) {
        getAppPreferences().edit().putBoolean(AppConstants.DAILY_MOVIES_RECOMMENDABILITY, newValue).commit();
    }

    public static boolean canDailyMoviesBeRecommended() {
        return getAppPreferences().getBoolean(AppConstants.DAILY_MOVIES_RECOMMENDABILITY, true);
    }

    public static boolean isAdAlreadyConsumed() {
        return getAppPreferences().getBoolean(AppConstants.AD_CONSUMED, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setAdConsumed(boolean value) {
        getAppPreferences().edit().putBoolean(AppConstants.AD_CONSUMED, value).commit();
    }

    public static long getLastMovieRecommendationTime() {
        return getAppPreferences().getLong(AppConstants.LAST_MOVIES_RECOMMENDATION_TIME, -1);
    }

    @SuppressLint("ApplySharedPref")
    public static void setLastMovieRecommendationTime(long time) {
        getAppPreferences().edit().putLong(AppConstants.LAST_MOVIES_RECOMMENDATION_TIME, time).commit();
    }

    public static boolean canRefreshMovieDetails(String movieId) {
        Set<String> refreshed = getAppPreferences().getStringSet(AppConstants.REFRESHED_MOVIES, new HashSet<>());
        return !refreshed.contains(movieId);
    }

    @SuppressLint("ApplySharedPref")
    public static void addToRefreshedMovies(String movieId) {
        Set<String> refreshedMovies = getAppPreferences().getStringSet(AppConstants.REFRESHED_MOVIES, new HashSet<>());
        refreshedMovies.add(movieId);
        getAppPreferences().edit().putStringSet(AppConstants.REFRESHED_MOVIES, refreshedMovies).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void clearAllRefreshedMovies() {
        Set<String> refreshedMovies = getAppPreferences().getStringSet(AppConstants.REFRESHED_MOVIES, new HashSet<>());
        refreshedMovies.clear();
        getAppPreferences().edit().putStringSet(AppConstants.REFRESHED_MOVIES, refreshedMovies).commit();
    }

    public static boolean canRefreshSeasonDetails(String seasonId) {
        Set<String> refreshed = getAppPreferences().getStringSet(AppConstants.REFRESHED_SEASONS, new HashSet<>());
        return !refreshed.contains(seasonId);
    }

    @SuppressLint("ApplySharedPref")
    public static void addToRefreshedSeasons(String seasonId) {
        Set<String> refreshedSeasons = getAppPreferences().getStringSet(AppConstants.REFRESHED_SEASONS, new HashSet<>());
        refreshedSeasons.add(seasonId);
        getAppPreferences().edit().putStringSet(AppConstants.REFRESHED_SEASONS, refreshedSeasons).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void clearAllRefreshedSeasons() {
        Set<String> refreshedSeasons = getAppPreferences().getStringSet(AppConstants.REFRESHED_SEASONS, new HashSet<>());
        refreshedSeasons.clear();
        getAppPreferences().edit().putStringSet(AppConstants.REFRESHED_SEASONS, refreshedSeasons).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void setSeasonRefreshable(String seasonId) {
        Set<String> refreshedSeasons = getAppPreferences().getStringSet(AppConstants.REFRESHED_SEASONS, new HashSet<>());
        refreshedSeasons.remove(seasonId);
        getAppPreferences().edit().putStringSet(AppConstants.REFRESHED_SEASONS, refreshedSeasons).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void setMovieRefreshable(String movieId) {
        Set<String> refreshedMovies = getAppPreferences().getStringSet(AppConstants.REFRESHED_MOVIES, new HashSet<>());
        refreshedMovies.remove(movieId);
        getAppPreferences().edit().putStringSet(AppConstants.REFRESHED_MOVIES, refreshedMovies).commit();
    }
}