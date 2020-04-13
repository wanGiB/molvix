package com.molvix.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.ConnectivityChangedEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.MolvixNotificationManager;
import com.molvix.android.managers.MovieTracker;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.FileUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private DeletedContentCleanUpTask deletedContentCleanUpTask;

    @Override
    public void onReceive(Context context, Intent intent) {
        performDeletedContentsCleanUp();
        if (intent.getAction() == null) {
            return;
        }
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            performAllPossibleNetworkRelatedJobs();
        }
    }

    public static void performAllPossibleNetworkRelatedJobs() {
        if (AppPrefs.canDailyMoviesBeRecommended()) {
            MovieTracker.recommendUnWatchedMoviesToUser();
        }
        MolvixNotificationManager.checkAndResumeUnFinishedDownloads();
        EventBus.getDefault().post(new ConnectivityChangedEvent());
        fetchNotifications();
        ContentManager.fetchPresets();
    }

    public static void fetchNotifications() {
        if (!AppPrefs.canBeUpdatedOnDownloadedMovies()) {
            return;
        }
        checkForUpdatesOnSeenMovies();
    }

    private static void checkForUpdatesOnSeenMovies() {
        List<Movie> allSeenMovies = new ArrayList<>();
        List<Movie> seenMoviesInDb = MolvixDB.getMovieBox().query().equal(Movie_.seenByUser, true).build().find();
        if (!seenMoviesInDb.isEmpty()) {
            allSeenMovies.addAll(seenMoviesInDb);
        }
        String[] seenMoviesInFile = loadDownloadedVideos(FileUtils.getVideosDir());
        if (seenMoviesInFile.length > 0) {
            List<Movie> moviesInFileCheck = MolvixDB.getMovieBox().query().in(Movie_.movieName, seenMoviesInFile).build().find();
            if (!moviesInFileCheck.isEmpty()) {
                for (Movie movie : moviesInFileCheck) {
                    if (!allSeenMovies.contains(movie)) {
                        allSeenMovies.add(movie);
                    }
                }
            }
        }
        if (!allSeenMovies.isEmpty()) {
            for (Movie movie : allSeenMovies) {
                List<Season> existingMoviesSeasons = movie.getSeasons();
                if (!existingMoviesSeasons.isEmpty()) {
                    Season lastSeason = existingMoviesSeasons.get(existingMoviesSeasons.size() - 1);
                    if (lastSeason != null) {
                        List<Episode> existingEpisodes = lastSeason.getEpisodes();
                        if (!existingEpisodes.isEmpty()) {
                            checkForNewUpdates(movie, lastSeason, existingEpisodes);
                        }
                    }
                }
            }
        }
    }

    private static void checkForNewUpdates(Movie movie, Season lastSeason, List<Episode> existingEpisodes) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                ContentManager.extractMovieSeasonMetaData(lastSeason, (result, e) -> {
                    if (e == null && result != null) {
                        List<Episode> episodesUpdate = result.getEpisodes();
                        if (episodesUpdate.size() > existingEpisodes.size()) {
                            Episode latestEpisode = episodesUpdate.get(episodesUpdate.size() - 1);
                            String message = EpisodesManager.getEpisodeFullName(latestEpisode) + " is out.";
                            String displayMessage = EpisodesManager.getEpisodeAndSeasonDescr(latestEpisode) + " is out.";
                            //Blow Notification, a new episode is available
                            String checkKey = CryptoUtils.getSha256Digest(EpisodesManager.getEpisodeFullName(latestEpisode));
                            Notification newMovieAvailableNotification = new Notification();
                            newMovieAvailableNotification.setNotificationObjectId(checkKey);
                            newMovieAvailableNotification.setMessage(message);
                            newMovieAvailableNotification.setTimeStamp(System.currentTimeMillis());
                            newMovieAvailableNotification.setDestination(AppConstants.DESTINATION_NEW_EPISODE_AVAILABLE);
                            newMovieAvailableNotification.setDestinationKey(movie.getMovieId());
                            MolvixDB.createNewNotification(newMovieAvailableNotification);
                            MolvixNotificationManager.displayNewMovieNotification(movie, displayMessage, newMovieAvailableNotification, checkKey);
                        }
                    }
                });
                return null;
            }
        }.execute();
    }

    private static String[] loadDownloadedVideos(File dir) {
        List<String> downloadedVideos = new ArrayList<>();
        if (dir.exists()) {
            File[] children = dir.listFiles();
            if (children != null && children.length > 0) {
                Arrays.sort(children);
                for (File file : children) {
                    if (!file.isHidden()) {
                        downloadedVideos.add(file.getName().toLowerCase());
                    }
                }
            }
        }
        String[] retrievedMovies = new String[downloadedVideos.size()];
        for (int i = 0; i < downloadedVideos.size(); i++) {
            retrievedMovies[i] = downloadedVideos.get(i);
        }
        return retrievedMovies;
    }

    private void performDeletedContentsCleanUp() {
        if (deletedContentCleanUpTask != null) {
            deletedContentCleanUpTask.cancel(true);
            deletedContentCleanUpTask = null;
        }
        deletedContentCleanUpTask = new DeletedContentCleanUpTask();
        deletedContentCleanUpTask.execute();
    }

    static class DeletedContentCleanUpTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.cleanUpDeletedContents();
            return null;
        }
    }

}
