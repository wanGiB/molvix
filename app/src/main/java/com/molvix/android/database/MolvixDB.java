package com.molvix.android.database;

import android.util.Pair;

import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.DownloadableEpisode_;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Episode_;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Notification_;
import com.molvix.android.models.Season;
import com.molvix.android.models.Season_;
import com.molvix.android.utils.CryptoUtils;

import java.util.List;

import io.objectbox.Box;

public class MolvixDB {

    private static Box<DownloadableEpisode> getDownloadableEpisodeBox() {
        return ObjectBox.get().boxFor(DownloadableEpisode.class);
    }

    private static Box<Episode> getEpisodeBox() {
        return ObjectBox.get().boxFor(Episode.class);
    }

    private static Box<Season> getSeasonBox() {
        return ObjectBox.get().boxFor(Season.class);
    }

    private static Box<Movie> getMovieBox() {
        return ObjectBox.get().boxFor(Movie.class);
    }

    private static Box<Notification> getNotificationBox() {
        return ObjectBox.get().boxFor(Notification.class);
    }

    public static Movie getMovie(String movieId) {
        return getMovieBox().query().equal(Movie_.movieId, movieId).build().findFirst();
    }

    public static Season getSeason(String seasonId) {
        return getSeasonBox().query().equal(Season_.seasonId, seasonId).build().findFirst();
    }

    public static Episode getEpisode(String episodeId) {
        return getEpisodeBox().query().equal(Episode_.episodeId, episodeId).build().findFirst();
    }

    public static Notification getNotification(String notificationObjectId) {
        return getNotificationBox()
                .query()
                .equal(Notification_.notificationObjectId, notificationObjectId)
                .build()
                .findFirst();
    }

    public static void fetchNotifications(DoneCallback<List<Notification>> notificationsFetchDoneCallBack) {
        List<Notification> notifications = getNotificationBox()
                .query()
                .build()
                .find();
        notificationsFetchDoneCallBack.done(notifications, null);
    }

    public static void searchMovies(String searchString, DoneCallback<List<Movie>> searchDoneCallBack) {
        List<Movie> result = getMovieBox()
                .query().contains(Movie_.movieName, searchString)
                .or()
                .contains(Movie_.movieDescription, searchString)
                .build()
                .find();
        searchDoneCallBack.done(result, null);
    }

    private static void saveMovie(Movie newMovie) {
        getMovieBox().put(newMovie);
    }

    public static void updateMovie(Movie updatableMovie) {
        getMovieBox().put(updatableMovie);
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
            newMovie.setRecommendedToUser(false);
            newMovie.setSeenByUser(false);
            saveMovie(newMovie);
        }
    }

    public static void createNewSeason(Season season) {
        getSeasonBox().put(season);
    }

    public static void createNewEpisode(Episode episode) {
        getEpisodeBox().put(episode);
    }

    public static void updateSeason(Season updatableSeason) {
        getSeasonBox().put(updatableSeason);
    }

    public static DownloadableEpisode getDownloadableEpisode(String episodeId) {
        return getDownloadableEpisodeBox()
                .query()
                .equal(DownloadableEpisode_.downloadableEpisodeId, episodeId)
                .build()
                .findFirst();
    }

    public static void createNewDownloadableEpisode(DownloadableEpisode newDownloadableEpisode) {
        Box<DownloadableEpisode> downloadableEpisodesBox = getDownloadableEpisodeBox();
        downloadableEpisodesBox.put(newDownloadableEpisode);
    }

    public static void deleteDownloadableEpisode(DownloadableEpisode downloadableEpisode) {
        getDownloadableEpisodeBox().remove(downloadableEpisode);
    }

    public static void updateEpisode(Episode episode) {
        getEpisodeBox().put(episode);
    }

    public static void createNewNotification(Notification newNotification) {
        getNotificationBox().put(newNotification);
    }

    public static void fetchAllAvailableMovies(DoneCallback<List<Movie>> fetchDoneCallBack) {
        List<Movie> availableMovies = getMovieBox().query().build().find();
        fetchDoneCallBack.done(availableMovies, null);
    }

    public static void fetchRecommendableMovies(DoneCallback<List<Movie>> fetchDoneCallBack) {
        List<Movie> recommendableMovies = getMovieBox().query().equal(Movie_.recommendedToUser, false).build().find();
        fetchDoneCallBack.done(recommendableMovies, null);
    }

    public static void updateNotification(Notification notification) {
        getNotificationBox().put(notification);
    }

    @SuppressWarnings("unused")
    public static void fetchDownloadableEpisodes(DoneCallback<List<DownloadableEpisode>> downloadableEpisodeDoneCallback) {
        List<DownloadableEpisode> downloadableEpisodes = getDownloadableEpisodeBox().query().build().find();
        downloadableEpisodeDoneCallback.done(downloadableEpisodes, null);
    }

}