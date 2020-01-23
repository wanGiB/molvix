package com.molvix.android.database;

import android.os.AsyncTask;
import android.util.Pair;

import com.molvix.android.beans.MoviesToSave;
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
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.CryptoUtils;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;

public class MolvixDB {

    public static Box<DownloadableEpisode> getDownloadableEpisodeBox() {
        return ObjectBox.get().boxFor(DownloadableEpisode.class);
    }

    private static Box<Episode> getEpisodeBox() {
        return ObjectBox.get().boxFor(Episode.class);
    }

    public static Box<Season> getSeasonBox() {
        return ObjectBox.get().boxFor(Season.class);
    }

    public static Box<Movie> getMovieBox() {
        return ObjectBox.get().boxFor(Movie.class);
    }

    public static Box<Notification> getNotificationBox() {
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

    public static void searchMovies(String searchString, DoneCallback<List<Movie>> searchDoneCallBack) {
        new SearchMoviesTask(searchDoneCallBack).execute(searchString);
    }

    public static void updateMovie(Movie updatableMovie) {
        getMovieBox().put(updatableMovie);
    }

    public static void performBulkInsertionOfMovies(List<Pair<String, String>> movies) {
        new BulkSaveTask().execute(new MoviesToSave(movies));
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
        AppPrefs.setEpisodeUpdated(episode.getEpisodeId());
    }

    public static void createNewNotification(Notification newNotification) {
        getNotificationBox().put(newNotification);
    }

    public static void fetchRecommendableMovies(DoneCallback<List<Movie>> fetchDoneCallBack) {
        new FetchRecommendableMoviesTask(fetchDoneCallBack).execute();
    }

    public static void updateNotification(Notification notification) {
        getNotificationBox().put(notification);
    }

    @SuppressWarnings("unused")
    public static void fetchDownloadableEpisodes(DoneCallback<List<DownloadableEpisode>> downloadableEpisodeDoneCallback) {
        new FetchDownloadableEpisodesTask(downloadableEpisodeDoneCallback).execute();
    }

    static class FetchDownloadableEpisodesTask extends AsyncTask<Void, Void, List<DownloadableEpisode>> {

        private DoneCallback<List<DownloadableEpisode>> downloadableEpisodesFetchDoneCallBack;

        FetchDownloadableEpisodesTask(DoneCallback<List<DownloadableEpisode>> downloadableEpisodesFetchDoneCallBack) {
            this.downloadableEpisodesFetchDoneCallBack = downloadableEpisodesFetchDoneCallBack;
        }

        @Override
        protected List<DownloadableEpisode> doInBackground(Void... voids) {
            return getDownloadableEpisodeBox().query().build().find();
        }

        @Override
        protected void onPostExecute(List<DownloadableEpisode> episodes) {
            super.onPostExecute(episodes);
            downloadableEpisodesFetchDoneCallBack.done(episodes, null);
        }

    }

    static class SearchMoviesTask extends AsyncTask<String, Void, List<Movie>> {

        private DoneCallback<List<Movie>> searchDoneCallBack;

        SearchMoviesTask(DoneCallback<List<Movie>> searchDoneCallBack) {
            this.searchDoneCallBack = searchDoneCallBack;
        }

        @Override
        protected List<Movie> doInBackground(String... searchString) {
            return getMovieBox()
                    .query().contains(Movie_.movieName, searchString[0])
                    .or()
                    .contains(Movie_.movieDescription, searchString[0])
                    .build()
                    .find();
        }

        @Override
        protected void onPostExecute(List<Movie> movies) {
            super.onPostExecute(movies);
            searchDoneCallBack.done(movies, null);
        }
    }

    static class FetchRecommendableMoviesTask extends AsyncTask<Void, Void, List<Movie>> {

        private DoneCallback<List<Movie>> moviesFetchDoneCallBack;

        FetchRecommendableMoviesTask(DoneCallback<List<Movie>> moviesFetchDoneCallBack) {
            this.moviesFetchDoneCallBack = moviesFetchDoneCallBack;
        }

        @Override
        protected List<Movie> doInBackground(Void... voids) {
            return getMovieBox().query().equal(Movie_.recommendedToUser, false).build().find();
        }

        @Override
        protected void onPostExecute(List<Movie> movies) {
            super.onPostExecute(movies);
            moviesFetchDoneCallBack.done(movies, null);
        }

    }

    static class BulkSaveTask extends AsyncTask<MoviesToSave, Void, Void> {

        @Override
        protected final Void doInBackground(MoviesToSave... moviesToSaves) {
            List<Pair<String, String>> movies = moviesToSaves[0].getMovies();
            List<Movie> moviesList = new ArrayList<>();
            for (Pair<String, String> movieItem : movies) {
                String movieName = movieItem.first;
                String movieLink = movieItem.second;
                String movieId = CryptoUtils.getSha256Digest(movieLink);
                Movie newMovie = new Movie();
                newMovie.setMovieId(movieId);
                newMovie.setMovieName(movieName.toLowerCase());
                newMovie.setMovieLink(movieLink);
                newMovie.setRecommendedToUser(false);
                newMovie.setSeenByUser(false);
                moviesList.add(newMovie);
            }
            long lastMoviesSize = AppPrefs.getLastMoviesSize();
            if (lastMoviesSize < moviesList.size()) {
                getMovieBox().removeAll();
                getMovieBox().put(moviesList);
                AppPrefs.setLastMoviesSize(moviesList.size());
            }
            return null;
        }
    }

}