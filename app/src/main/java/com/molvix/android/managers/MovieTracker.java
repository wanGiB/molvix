package com.molvix.android.managers;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.RandomStringUtils;
import com.molvix.android.utils.UiUtils;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

public class MovieTracker {

    private static BitmapLoadTask bitmapLoadTask;

    static void recordEpisodeAsDownloaded(Episode episode) {
        //Add to user notifications pane
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String notificationId = CryptoUtils.getSha256Digest(RandomStringUtils.random(256) + System.currentTimeMillis());
        Notification existingNotification = MolvixDB.getNotification(notificationId);
        if (existingNotification != null) {
            return;
        }
        Notification newNotification = new Notification();
        newNotification.setNotificationObjectId(notificationId);
        newNotification.setDestination(AppConstants.DESTINATION_DOWNLOADED_EPISODE);
        newNotification.setMessage("<b>" + episode.getEpisodeName() + "</b>/<b>" + season.getSeasonName() + "</b> of <b>" + movie.getMovieName() + "</b> successfully downloaded");
        newNotification.setDestinationKey(episode.getEpisodeId());
        newNotification.setTimeStamp(System.currentTimeMillis());
        MolvixDB.createNewNotification(newNotification);
    }

    public static void recommendUnWatchedMoviesToUser() {
        long lastRecommendationTimeStamp = AppPrefs.getLastMovieRecommendationTime();
        if (lastRecommendationTimeStamp != -1) {
            if (DateUtils.isToday(lastRecommendationTimeStamp)) {
                return;
            }
        }
        new Thread(() -> {
            List<Movie> recommendableMovies = MolvixDB.
                    getMovieBox()
                    .query()
                    .equal(Movie_.recommendedToUser, false)
                    .build()
                    .find();
            if (!recommendableMovies.isEmpty()) {
                Collections.shuffle(recommendableMovies, new SecureRandom());
                Movie firstMovie = recommendableMovies.get(0);
                if (firstMovie != null) {
                    String movieArtUrl = firstMovie.getMovieArtUrl();
                    List<Season> movieSeasons = firstMovie.getSeasons();
                    if (movieArtUrl == null || movieSeasons == null || movieSeasons.isEmpty()) {
                        new MovieContentsExtractionTask().execute(firstMovie);
                    } else {
                        loadBitmapAndRecommendVideo(firstMovie.getMovieId(), firstMovie.getMovieArtUrl());
                    }
                }
            }
        }).start();
    }

    private static class MovieContentsExtractionTask extends AsyncTask<Movie, Void, Void> {

        @Override
        protected Void doInBackground(Movie... movies) {
            ContentManager.extractMovieMetaData(movies[0], (result, e) -> {
                if (e == null && result != null) {
                    loadBitmapAndRecommendVideo(result.getMovieId(), result.getMovieArtUrl());
                }
            });
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

    private static class BitmapLoadTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String videoId = strings[0];
            String artUrl = strings[1];
            RequestOptions imageLoadRequestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
            Glide.with(ApplicationLoader.getInstance())
                    .asBitmap()
                    .load(artUrl)
                    .apply(imageLoadRequestOptions)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }

                    })
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            UiUtils.runOnMain(() -> MolvixNotificationManager.recommendMovieToUser(videoId, resource));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                    });
            return null;
        }
    }

}
