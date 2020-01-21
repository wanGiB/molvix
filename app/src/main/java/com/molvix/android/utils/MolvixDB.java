package com.molvix.android.utils;

import android.util.Pair;

import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.contracts.OnContentChangedListener;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;

import java.util.ArrayList;
import java.util.List;

public class MolvixDB {

    public static Movie getMovie(String movieId) {
        return null;
    }

    public static void searchMovies(String searchString, DoneCallback<List<Movie>> searchDoneCallBack) {

    }

    private static void insertNewMovie(Movie newMovie) {

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
        }
    }

    public static Season getSeason(String seasonId) {
        return null;
    }

    public static void createNewSeason(Season season) {

    }

    public static Episode getEpisode(String episodeId) {
        return null;
    }

    public static void createNewEpisode(Episode episode) {

    }

    public static void updateSeason(Season updatableSeason) {

    }

    public static void updateMovie(Movie updatableMovie) {

    }

    public static DownloadableEpisode getDownloadableEpisode(String episodeId) {
        return null;
    }

    public static void createNewDownloadableEpisode(DownloadableEpisode newDownloadableEpisode) {

    }

    public static void deleteDownloadableEpisode(DownloadableEpisode downloadableEpisode) {

    }

    public static void updateEpisode(Episode episode) {

    }

    public static void createNewNotification(Notification newNotification) {

    }

    public static void fetchAllAvailableMovies(DoneCallback<List<Movie>> fetchDoneCallBack) {

    }

    public static void listenToIncomingDownloadableEpisodes(OnContentChangedListener<List<DownloadableEpisode>> downloadableContentChangedListener) {

    }

    public static void fetchNotifications(DoneCallback<List<Notification>>notificationsFetchDoneCallBack) {

    }

    public static void updateNotification(Notification notification) {

    }
}