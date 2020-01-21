package com.molvix.android.managers;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.observers.MolvixContentChangeObserver;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.DateUtils;
import com.molvix.android.utils.MolvixDB;
import com.molvix.android.utils.UiUtils;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MovieTracker {

    private static BitmapLoadTask bitmapLoadTask;

    @SuppressWarnings("ConstantConditions")
    static void recordEpisodeAsDownloaded(String episodeId) {
        //Add to user notifications pane
        Episode episode = MolvixDB.getEpisode(episodeId);
        Movie movie = MolvixDB.getMovie(episode.getMovieId());
        Season season = MolvixDB.getSeason(episode.getSeasonId());
        Notification newNotification = new Notification();
        newNotification.setNotificationObjectId(String.valueOf(System.currentTimeMillis() + new Random().nextInt(256)));
        newNotification.setDestination(AppConstants.DESTINATION_EPISODE);
        newNotification.setMessage("<b>" + episode.getEpisodeName() + "</b>/<b>" + season.getSeasonName() + "</b> of <b>" + movie.getMovieName() + "</b> successfully downloaded");
        newNotification.setDestinationKey(episodeId);
        newNotification.setTimeStamp(System.currentTimeMillis());
        MolvixDB.createNewNotification(newNotification);
    }

    public static void recommendUnWatchedMoviesToUser() {
        long lastRecommendationTimeStamp = AppPrefs.getLastMovieRecommendationTime();
        if (lastRecommendationTimeStamp != -1) {
            String lastTime = DateUtils.getRelativeDate(ApplicationLoader.getInstance(), Locale.getDefault(), lastRecommendationTimeStamp);
            if (lastTime.equals(ApplicationLoader.getInstance().getString(R.string.DateUtils_today))) {
                return;
            }
        }
        MolvixDB.fetchAllAvailableMovies((recommendableMovies, e) -> {
            if (!recommendableMovies.isEmpty()) {
                Collections.shuffle(recommendableMovies, new SecureRandom());
                Movie firstMovie = recommendableMovies.get(0);
                if (firstMovie != null) {
                    String movieArtUrl = firstMovie.getMovieArtUrl();
                    String movieId = firstMovie.getMovieId();
                    String movieLink = firstMovie.getMovieLink();
                    List<Season> movieSeasons = firstMovie.getMovieSeasons();
                    if (movieArtUrl == null || movieSeasons.isEmpty()) {
                        MolvixContentChangeObserver.addMovieChangedListener(firstMovie.getMovieId(), changedData -> loadBitmapAndRecommendVideo(changedData.getMovieId(), changedData.getMovieArtUrl()));
                        new MovieContentsExtractionTask().execute(movieLink, movieId);
                    } else {
                        loadBitmapAndRecommendVideo(firstMovie.getMovieId(), firstMovie.getMovieArtUrl());
                    }
                }
            }

        });
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
