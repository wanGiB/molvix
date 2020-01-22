package com.molvix.android.utils;

import android.util.Pair;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.List;

public class MolvixDB {

    public static Movie getMovie(String movieId) {
        return Select.from(Movie.class).where(Condition.prop(AppConstants.MOVIE_ID).eq(movieId)).first();
    }

    public static Season getSeason(String seasonId) {
        return Select.from(Season.class).where(Condition.prop(AppConstants.SEASON_ID).eq(seasonId)).first();
    }

    public static Episode getEpisode(String episodeId) {
        return Select.from(Episode.class).where(Condition.prop(AppConstants.EPISODE_ID).eq(episodeId)).first();
    }

    public static void searchMovies(String searchString, DoneCallback<List<Movie>> searchDoneCallBack) {
        List<Movie> results = Select.from(Movie.class)
                .where(Condition.prop(AppConstants.MOVIE_NAME).like("%" + searchString + "%"))
                .or(Condition.prop(AppConstants.MOVIE_DESCRIPTION).like("%" + searchString + "%"))
                .list();
        searchDoneCallBack.done(results, null);
    }

    private static void saveMovie(Movie newMovie) {
        SugarRecord.save(newMovie);
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

    public static void createNewSeason(Season season) {
        SugarRecord.save(season);
        AppPrefs.seasonUpdated(season.getSeasonId());
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
        return Select.from(DownloadableEpisode.class).where(Condition.prop(AppConstants.EPISODE_ID).eq(episodeId)).first();
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
        fetchDoneCallBack.done(Select.from(Movie.class).list(), null);
    }

    public static void fetchRecommendableMovies(DoneCallback<List<Movie>> fetchDoneCallBack) {
        List<Movie> recommendableMovies = Select.from(Movie.class).where(Condition.prop(AppConstants.MOVIE_RECOMMENDED_TO_USER).eq(false)).list();
        fetchDoneCallBack.done(recommendableMovies, null);
    }

    public static void fetchNotifications(DoneCallback<List<Notification>> notificationsFetchDoneCallBack) {
        notificationsFetchDoneCallBack.done(Select.from(Notification.class).list(), null);
    }

    public static void updateNotification(Notification notification) {
        SugarRecord.update(notification);
        AppPrefs.notificationsUpdated(notification.getNotificationObjectId());
    }

    public static Notification getNotification(String notificationObjectId) {
        return Select.from(Notification.class).where(Condition.prop(AppConstants.NOTIFICATION_OBJECT_ID).eq(notificationObjectId)).first();
    }

    public static void fetchDownloadableEpisodes(DoneCallback<List<DownloadableEpisode>> downloadableEpisodeDoneCallback) {
        downloadableEpisodeDoneCallback.done(Select.from(DownloadableEpisode.class).list(), null);
    }

}