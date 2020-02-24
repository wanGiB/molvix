package com.molvix.android.database;

import android.os.AsyncTask;
import android.util.Pair;

import com.molvix.android.beans.MoviesToSave;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.DownloadableEpisode_;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Episode_;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Notification_;
import com.molvix.android.models.Presets;
import com.molvix.android.models.Season;
import com.molvix.android.models.Season_;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.MolvixLogger;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;

public class MolvixDB {

    public static Box<DownloadableEpisode> getDownloadableEpisodeBox() {
        return ObjectBox.get().boxFor(DownloadableEpisode.class);
    }

    public static Box<Presets> getPresetsBox() {
        return ObjectBox.get().boxFor(Presets.class);
    }

    public static Box<Episode> getEpisodeBox() {
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

    public static Presets getPresets() {
        return getPresetsBox().query().build().findFirst();
    }

    public static void updatePreset(Presets presets) {
        getPresetsBox().put(presets);
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
    }

    public static void createNewNotification(Notification newNotification) {
        getNotificationBox().put(newNotification);
    }

    public static void updateNotification(Notification associatedNotification) {
        getNotificationBox().put(associatedNotification);
    }

    static class BulkSaveTask extends AsyncTask<MoviesToSave, Void, Void> {

        @Override
        protected final Void doInBackground(MoviesToSave... moviesToSaves) {
            List<Pair<String, String>> movies = moviesToSaves[0].getMovies();
            List<Movie> newMovieList = new ArrayList<>();
            List<Movie> existingMoviesList = getMovieBox().query().build().find();
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
                newMovieList.add(newMovie);
            }
            long lastMoviesSize = AppPrefs.getLastMoviesSize();
            if (lastMoviesSize < newMovieList.size()) {
                noteNewMovies(newMovieList, existingMoviesList);
                getMovieBox().removeAll();
                getMovieBox().put(newMovieList);
                AppPrefs.setLastMoviesSize(newMovieList.size());
            }
            //This is a hack to update all downloaded episodes of movies
            //That the movies might have changed positions
            updateNotifications();
            return null;
        }

        private void noteNewMovies(List<Movie> newMovieList, List<Movie> existingMoviesList) {
            List<Movie> newlyAddedMovies = intersectionOf(newMovieList, existingMoviesList);
            if (!newlyAddedMovies.isEmpty()) {
                for (Movie newMovie : newlyAddedMovies) {
                    newMovie.setNewMovie(true);
                    if (newMovieList.contains(newMovie)) {
                        int indexOfNewMovie = newMovieList.indexOf(newMovie);
                        newMovieList.set(indexOfNewMovie,newMovie);
                    }
                }
            }
        }

        private List<Movie> intersectionOf(List<Movie> updatedMovieList, List<Movie> existingMoviesList) {
            List<Movie> intersection = new ArrayList<>();
            if (existingMoviesList.isEmpty()) {
                return intersection;
            }
            for (Movie updatedMovie : updatedMovieList) {
                if (!existingMoviesList.contains(updatedMovie)) {
                    intersection.add(updatedMovie);
                }
            }
            return intersection;
        }

        private void updateNotifications() {
            List<Notification> notifications = MolvixDB.getNotificationBox().query().equal(Notification_.destination, AppConstants.DESTINATION_DOWNLOADED_EPISODE).build().find();
            if (!notifications.isEmpty()) {
                for (Notification notification : notifications) {
                    String destinationKey = notification.getDestinationKey();
                    if (destinationKey != null) {
                        Episode episode = MolvixDB.getEpisode(destinationKey);
                        if (episode != null) {
                            Season season = episode.getSeason();
                            if (season != null) {
                                String seasonLink = season.getSeasonLink();
                                String seasonName = season.getSeasonName();
                                if (seasonLink != null && seasonName != null) {
                                    String moviePart = StringUtils.substringAfterLast(StringUtils.removeEnd(seasonLink, "/" + seasonName + "/index.html"), "/");
                                    if (StringUtils.isNotEmpty(moviePart)) {
                                        Movie movie = MolvixDB.getMovieBox().query().contains(Movie_.movieLink, moviePart).build().findFirst();
                                        if (movie != null) {
                                            Movie existingMovie = season.getMovie();
                                            if (existingMovie == null) {
                                                season.movie.setTarget(movie);
                                                MolvixDB.updateSeason(season);
                                                MolvixLogger.d(ContentManager.class.getSimpleName(), "Updating recently downloaded episodes with latest movie positions");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}