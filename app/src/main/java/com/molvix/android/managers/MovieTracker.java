package com.molvix.android.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Pair;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.UiUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;

public class MovieTracker {

    private static BitmapLoadTask bitmapLoadTask;

    @SuppressWarnings("ConstantConditions")
    static void recordEpisodeAsDownloaded(String episodeId) {
        //Add to user notifications pane
        try (Realm realm = Realm.getDefaultInstance()) {
            Episode episode = realm.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episodeId).findFirst();
            Movie movie = realm.where(Movie.class).equalTo(AppConstants.MOVIE_ID, episode.getMovieId()).findFirst();
            Season season = realm.where(Season.class).equalTo(AppConstants.SEASON_ID, episode.getSeasonId()).findFirst();
            Notification notification = realm.where(Notification.class)
                    .equalTo(AppConstants.NOTIFICATION_RESOLUTION_KEY, episodeId)
                    .equalTo(AppConstants.NOTIFICATION_DESTINATION, AppConstants.DESTINATION_EPISODE)
                    .findFirst();
            if (notification != null) {
                return;
            }
            realm.executeTransaction(r -> {
                Notification newNotification = r.createObject(Notification.class, CryptoUtils.getSha256Digest(String.valueOf(System.currentTimeMillis() + new Random().nextInt(256))));
                newNotification.setDestination(AppConstants.DESTINATION_EPISODE);
                newNotification.setMessage("<b>" + episode.getEpisodeName() + "</b>/<b>" + season.getSeasonName() + "</b> of <b>" + movie.getMovieName() + "</b> successfully downloaded");
                newNotification.setResolutionKey(episodeId);
                newNotification.setTimeStamp(System.currentTimeMillis());
                r.copyToRealmOrUpdate(newNotification, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
            });
        }
    }

    public static void recommendUnWatchedMoviesToUser() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Movie> recommendableMovies = realm.where(Movie.class).equalTo(AppConstants.MOVIE_RECOMMENDED_TO_USER, false).findAll();
            if (recommendableMovies != null && recommendableMovies.isLoaded() && !recommendableMovies.isEmpty()) {
                //Get the First Movie
                Movie firstMovie = recommendableMovies.get(0);
                if (firstMovie != null) {
                    String movieArtUrl = firstMovie.getMovieArtUrl();
                    String movieId = firstMovie.getMovieId();
                    String movieLink = firstMovie.getMovieLink();
                    RealmList<Season> movieSeasons = firstMovie.getMovieSeasons();
                    if (movieArtUrl == null || movieSeasons == null || movieSeasons.isEmpty()) {
                        firstMovie.addChangeListener((RealmChangeListener<Movie>) realmModel -> {
                            loadBitmapAndRecommendVideo(realmModel.getMovieId(), realmModel.getMovieArtUrl());
                            firstMovie.removeAllChangeListeners();
                        });
                        new MovieContentsExtractionTask().execute(movieLink, movieId);
                    } else {
                        loadBitmapAndRecommendVideo(firstMovie.getMovieId(), firstMovie.getMovieArtUrl());
                    }
                }
            }
        }
    }

    private static class MovieContentsExtractionTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... movieIds) {
            ContentManager.extractMetaDataFromMovieLink(movieIds[0], movieIds[1]);
            return null;
        }

    }

    private static void loadBitmapAndRecommendVideo(String videoId, String artUrl) {
        if (bitmapLoadTask != null) {
            bitmapLoadTask.cancel(true);
            bitmapLoadTask = null;
        }
        bitmapLoadTask = new BitmapLoadTask();
        bitmapLoadTask.execute(videoId, artUrl);
    }

    static class BitmapLoadTask extends AsyncTask<String, Void, Pair<String, Bitmap>> {

        @Override
        protected Pair<String, Bitmap> doInBackground(String... strings) {
            String videoId = strings[0];
            String artUrl = strings[1];
            Bitmap artBitmap = getBitmapFromURL(artUrl);
            return new Pair<>(videoId, artBitmap);
        }

        @Override
        protected void onPostExecute(Pair<String, Bitmap> stringBitmapPair) {
            super.onPostExecute(stringBitmapPair);
            if (stringBitmapPair.first != null && stringBitmapPair.second != null) {
                UiUtils.runOnMain(() -> MolvixNotificationManager.recommendMovieToUser(stringBitmapPair.first, stringBitmapPair.second));
            }
        }
    }

    private static Bitmap getBitmapFromURL(final String strURL) {
        Callable<Bitmap> bitmapCallable = () -> {
            try {
                URL url = new URL(strURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        };
        FutureTask<Bitmap> bitmapFutureTask = new FutureTask<>(bitmapCallable);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(bitmapFutureTask);
        try {
            return bitmapFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

}
