package com.molvix.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.models.Episode;
import com.molvix.android.utils.CryptoUtils;

import java.util.HashSet;
import java.util.Set;

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
    public static void unLockCaptchaSolver() {
        getAppPreferences().edit().putString(AppConstants.CAPTCHA_SOLVING, null).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void setDailyMoviesRecommendability(boolean newValue) {
        getAppPreferences().edit().putBoolean(AppConstants.DAILY_MOVIES_RECOMMENDABILITY, newValue).commit();
    }

    public static boolean canDailyMoviesBeRecommended() {
        return getAppPreferences().getBoolean(AppConstants.DAILY_MOVIES_RECOMMENDABILITY, true);
    }

    public static boolean canBeUpdatedOnDownloadedMovies() {
        return getAppPreferences().getBoolean(AppConstants.DOWNLOADED_MOVIES_UPDATE, true);
    }

    @SuppressLint("ApplySharedPref")
    public static void setDownloadedMoviesUpdatable(boolean newValue) {
        getAppPreferences().edit().putBoolean(AppConstants.DOWNLOADED_MOVIES_UPDATE, newValue).commit();
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

    public static long getLastMoviesSize() {
        return getAppPreferences().getLong(AppConstants.LAST_MOVIES_SIZE, 0L);
    }

    @SuppressLint("ApplySharedPref")
    public static void setLastMoviesSize(long newSize) {
        getAppPreferences().edit().putLong(AppConstants.LAST_MOVIES_SIZE, newSize).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void addToInProgressDownloads(Episode episode) {
        Set<String> inProgressDownloads = getAppPreferences().getStringSet(AppConstants.IN_PROGRESS_DOWNLOADS, new HashSet<>());
        inProgressDownloads.add(episode.getEpisodeId());
        getAppPreferences().edit().putStringSet(AppConstants.IN_PROGRESS_DOWNLOADS, inProgressDownloads).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void removeFromInProgressDownloads(Episode episode) {
        Set<String> inProgressDownloads = getAppPreferences().getStringSet(AppConstants.IN_PROGRESS_DOWNLOADS, new HashSet<>());
        inProgressDownloads.remove(episode.getEpisodeId());
        getAppPreferences().edit().putStringSet(AppConstants.IN_PROGRESS_DOWNLOADS, inProgressDownloads).commit();
    }


    public static Set<String> getInProgressDownloads() {
        return getAppPreferences().getStringSet(AppConstants.IN_PROGRESS_DOWNLOADS, new HashSet<>());
    }

    public static int getDownloadIdFromEpisodeId(String episodeId) {
        return getAppPreferences().getInt(AppConstants.DOWNLOAD + CryptoUtils.getSha256Digest(episodeId), 0);
    }

    public static void mapEpisodeIdToDownloadId(String episodeId, int downloadId) {
        getAppPreferences().edit().putInt(AppConstants.DOWNLOAD + CryptoUtils.getSha256Digest(episodeId), downloadId).apply();
        mapDownloadIdToEpisodeId(downloadId, episodeId);
    }

    private static void mapDownloadIdToEpisodeId(int downloadId, String episodeId) {
        getAppPreferences().edit().putString(AppConstants.DOWNLOAD_ID_KEY + downloadId, episodeId).apply();
    }

    public static String getEpisodeIdFromDownloadId(int downloadId) {
        return getAppPreferences().getString(AppConstants.DOWNLOAD_ID_KEY + downloadId, null);
    }

    public static boolean hasBeenNotified(String checkKey) {
        return getAppPreferences().getBoolean(AppConstants.NOTIFICATION + checkKey, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setHasBeenNotified(String checkKey) {
        getAppPreferences().edit().putBoolean(AppConstants.NOTIFICATION + checkKey, true).commit();
    }

    public static void updateEpisodeDownloadProgress(String episodeId, int progress) {
        getAppPreferences().edit().putInt(AppConstants.EPISODE_DOWNLOAD_PROGRESS + episodeId, progress).apply();
    }

    public static void updateEpisodeDownloadProgressMsg(String episodeId, String progressText) {
        getAppPreferences().edit().putString(AppConstants.EPISODE_DOWNLOAD_PROGRESS_TEXT + episodeId, progressText).apply();
    }

    public static String getEpisodeDownloadProgressText(String episodeId) {
        return getAppPreferences().getString(AppConstants.EPISODE_DOWNLOAD_PROGRESS_TEXT + episodeId, "");
    }

    public static int getEpisodeDownloadProgress(String episodeId) {
        return getAppPreferences().getInt(AppConstants.EPISODE_DOWNLOAD_PROGRESS + episodeId, -1);
    }

    public static void removeKey(String key) {
        getAppPreferences().edit().remove(key).apply();
    }

    public static boolean isPaused(String episodeId) {
        return getAppPreferences().getBoolean(AppConstants.DOWNLOAD_PAUSED + episodeId, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setPaused(String episodeId, boolean value) {
        getAppPreferences().edit().putBoolean(AppConstants.DOWNLOAD_PAUSED + episodeId, value).commit();
    }

    public static long getEstimatedFileLengthForEpisode(String episodeId) {
        return getAppPreferences().getLong(AppConstants.ESTIMATED_FILE_LENGTH + episodeId, 0);
    }

    public static void saveEstimatedFileLengthForEpisode(String episodeId, long fileLength) {
        getAppPreferences().edit().putLong(AppConstants.ESTIMATED_FILE_LENGTH + episodeId, fileLength).apply();
    }

    public static void persistLastAdLoadTime(long lastAdLoadTime) {
        getAppPreferences().edit().putLong(AppConstants.LAST_AD_LOAD_TIME, lastAdLoadTime).apply();
    }

    public static long getLastAdLoadTime() {
        return getAppPreferences().getLong(AppConstants.LAST_AD_LOAD_TIME, 0);
    }

}