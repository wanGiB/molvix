package com.molvix.android.utils;

import android.util.Pair;

import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.orm.SugarRecord;

import java.util.List;

public class MolvixDB {

    public static Movie getMovie(String movieId) {
        return null;
    }

    public static void searchMovies(String searchString, DoneCallback<List<Movie>> searchDoneCallBack) {

    }

    private static void saveMovie(Movie newMovie) {
        long id = SugarRecord.save(newMovie);
        newMovie.setId(id);
        SugarRecord.update(newMovie);
        AppPrefs.movieUpdated(newMovie.getMovieId());
    }

    public static void updateMovie(Movie updatableMovie) {
        SugarRecord.update(updatableMovie);
        AppPrefs.movieUpdated(updatableMovie.getMovieId());
    }

    public static void performBulkInsertionOfMovies(List<Pair<String, String>> movies) {
        for (Pair<String, String> movieItem : movies) {
            String movieName = movieItem.first;
            String movieLink = movieItem.second;
            String movieId = CryptoUtils.getSha256Digest(movieLink);
            Movie existingMovie = getMovie(movieId);
            if (existingMovie != null) {
                return;
            }
            Movie newMovie = new Movie();
            newMovie.setMovieId(movieId);
            newMovie.setMovieName(movieName.toLowerCase());
            newMovie.setMovieLink(movieLink);
            saveMovie(newMovie);
        }
    }

    public static Season getSeason(String seasonId) {
        return null;
    }

    public static void createNewSeason(Season season) {
        SugarRecord.save(season);
        AppPrefs.seasonUpdated(season.getSeasonId());
    }

    public static Episode getEpisode(String episodeId) {
        return null;
    }

    public static void createNewEpisode(Episode episode) {
        SugarRecord.save(episode);
        AppPrefs.episodeUpdated(episode.getEpisodeId());
    }

    public static void updateSeason(Season updatableSeason) {
        SugarRecord.update(updatableSeason);
        AppPrefs.seasonUpdated(updatableSeason.getSeasonId());
    }

    public static DownloadableEpisode getDownloadableEpisode(String episodeId) {
        return null;
    }

    public static void createNewDownloadableEpisode(DownloadableEpisode newDownloadableEpisode) {
        SugarRecord.save(newDownloadableEpisode);
        AppPrefs.downloadableEpisodeUpdated(newDownloadableEpisode.getEpisodeId());
    }

    public static void deleteDownloadableEpisode(DownloadableEpisode downloadableEpisode) {
        SugarRecord.delete(downloadableEpisode);
    }

    public static void updateEpisode(Episode episode) {
        SugarRecord.update(episode);
        AppPrefs.episodeUpdated(episode.getEpisodeId());
    }

    public static void createNewNotification(Notification newNotification) {
        SugarRecord.save(newNotification);
        AppPrefs.notificationsUpdated(newNotification.getNotificationObjectId());
    }

    public static void fetchAllAvailableMovies(DoneCallback<List<Movie>> fetchDoneCallBack) {

    }

    public static void fetchNotifications(DoneCallback<List<Notification>> notificationsFetchDoneCallBack) {

    }

    public static void updateNotification(Notification notification) {
        SugarRecord.update(notification);
        AppPrefs.notificationsUpdated(notification.getNotificationObjectId());
    }

    public static Notification getNotification(String notificationKey) {
        return null;
    }

    public static void fetchDownloadableEpisodes(DoneCallback<List<DownloadableEpisode>> downloadableEpisodeDoneCallback) {

    }


}