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
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.DateUtils;
import com.molvix.android.utils.UiUtils;

import java.util.Locale;
import java.util.Random;

import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;

public class MovieTracker {

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
        long lastRecommendationTimeStamp = AppPrefs.getLastMovieRecommendationTime();
        if (lastRecommendationTimeStamp != -1) {
            String lastTime = DateUtils.getRelativeDate(ApplicationLoader.getInstance(), Locale.getDefault(), lastRecommendationTimeStamp);
            if (lastTime.equals(ApplicationLoader.getInstance().getString(R.string.DateUtils_today))) {
                return;
            }
        }
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
    }

}
