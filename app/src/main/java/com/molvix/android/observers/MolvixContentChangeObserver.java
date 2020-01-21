package com.molvix.android.observers;

import android.content.SharedPreferences;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.contracts.OnContentChangedListener;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.MolvixDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MolvixContentChangeObserver {

    private static final String NOTIFICATIONS_KEY = "NOTIFICATIONS";
    private static final String DOWNLOADABLES_KEY = "DOWNLOADABLES";
    private static HashMap<String, SharedPreferences.OnSharedPreferenceChangeListener> prefObserverMap = new HashMap<>();
    private static String MOVIES_KEY = "ALL_MOVIES";

    public static void addMoviesChangedListener(OnContentChangedListener<List<Movie>> moviesChanged) {
        removeMoviesChangeListener();
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(AppConstants.MOVIE)) {
                String movieId = key.split(AppConstants.MOVIE)[1];
                Movie movie = MolvixDB.getMovie(movieId);
                if (movie != null) {
                    List<Movie> updatedMovies = new ArrayList<>();
                    updatedMovies.add(movie);
                    moviesChanged.onDataChanged(updatedMovies);
                }
            }
        };
        prefObserverMap.put(MOVIES_KEY, onSharedPreferenceChangeListener);
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(prefObserverMap.get(MOVIES_KEY));
    }

    public static void removeMoviesChangeListener() {
        if (prefObserverMap.containsKey(MOVIES_KEY)) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(prefObserverMap.get(MOVIES_KEY));
            prefObserverMap.remove(MOVIES_KEY);
        }
    }

    public static void addMovieChangedListener(String movieId, OnContentChangedListener<Movie> movieOnContentChangedListener) {
        removeMovieChangeListener(movieId);
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(movieId)) {
                Movie updatedMovie = MolvixDB.getMovie(movieId);
                if (updatedMovie != null) {
                    movieOnContentChangedListener.onDataChanged(updatedMovie);
                }
            }
        };
        prefObserverMap.put(movieId, onSharedPreferenceChangeListener);
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(prefObserverMap.get(movieId));
    }

    public static void removeMovieChangeListener(String movieId) {
        if (prefObserverMap.containsKey(movieId)) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(prefObserverMap.get(movieId));
            prefObserverMap.remove(movieId);
        }
    }

    public static void addNotificationsChangeListener(OnContentChangedListener<Notification> onContentChangedListener) {
        removeNotificationsChangeListener();
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(AppConstants.NOTIFICATION)) {
                String notificationKey = key.split(AppConstants.NOTIFICATION)[1];
                Notification notification = MolvixDB.getNotification(notificationKey);
                if (notification != null) {
                    onContentChangedListener.onDataChanged(notification);
                }
            }
        };
        prefObserverMap.put(NOTIFICATIONS_KEY, onSharedPreferenceChangeListener);
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(prefObserverMap.get(NOTIFICATIONS_KEY));
    }

    public static void removeNotificationsChangeListener() {
        if (prefObserverMap.containsKey(NOTIFICATIONS_KEY)) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(prefObserverMap.get(NOTIFICATIONS_KEY));
            prefObserverMap.remove(NOTIFICATIONS_KEY);
        }
    }

    public static void addEpisodeChangeListener(Episode episode, OnContentChangedListener<Episode> episodeOnContentChangedListener) {
        removeEpisodeChangeListener(episode);
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(AppConstants.EPISODE)) {
                String episodeKey = key.split(AppConstants.EPISODE)[1];
                if (episodeKey.equals(episode.getEpisodeId())) {
                    Episode updatedEpisode = MolvixDB.getEpisode(episodeKey);
                    if (updatedEpisode != null) {
                        episodeOnContentChangedListener.onDataChanged(updatedEpisode);
                    }
                }
            }
        };
        prefObserverMap.put(episode.getEpisodeId(), onSharedPreferenceChangeListener);
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(prefObserverMap.get(episode.getEpisodeId()));
    }

    public static void removeEpisodeChangeListener(Episode episode) {
        if (prefObserverMap.containsKey(episode.getEpisodeId())) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(prefObserverMap.get(episode.getEpisodeId()));
            prefObserverMap.remove(episode.getEpisodeId());
        }
    }

    public static void addSeasonChangeListener(Season season, OnContentChangedListener<Season> seasonOnContentChangedListener) {
        removeSeasonChangeListener(season);
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(AppConstants.SEASON)) {
                String seasonId = key.split(AppConstants.SEASON)[1];
                if (seasonId.equals(season.getSeasonId())) {
                    Season updatedSeason = MolvixDB.getSeason(seasonId);
                    if (updatedSeason != null) {
                        seasonOnContentChangedListener.onDataChanged(updatedSeason);
                    }
                }
            }
        };
        prefObserverMap.put(season.getSeasonId(), onSharedPreferenceChangeListener);
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(prefObserverMap.get(season.getSeasonId()));
    }

    public static void removeSeasonChangeListener(Season season) {
        if (prefObserverMap.containsKey(season.getSeasonId())) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(prefObserverMap.get(season.getSeasonId()));
            prefObserverMap.remove(season.getSeasonId());
        }
    }

    public static void addDownloadableEpisodesChangeListener(OnContentChangedListener<List<DownloadableEpisode>> downloadableContentChangedListener) {
        removeDownloadableEpisodesChangeListener();
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(AppConstants.DOWNLOADABLE)) {
                MolvixDB.fetchDownloadableEpisodes((result, e) -> downloadableContentChangedListener.onDataChanged(result));
            }
        };
        prefObserverMap.put(DOWNLOADABLES_KEY, onSharedPreferenceChangeListener);
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(prefObserverMap.get(DOWNLOADABLES_KEY));
    }

    private static void removeDownloadableEpisodesChangeListener() {
        if (prefObserverMap.containsKey(DOWNLOADABLES_KEY)) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(prefObserverMap.get(DOWNLOADABLES_KEY));
            prefObserverMap.remove(DOWNLOADABLES_KEY);
        }
    }

}
