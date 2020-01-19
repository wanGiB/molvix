package com.molvix.android.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.models.Movie;
import com.molvix.android.ui.activities.EmptyContentActivity;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.ui.activities.MovieDetailsActivity;

import org.apache.commons.lang3.text.WordUtils;

import io.realm.ImportFlag;
import io.realm.Realm;
import ir.zadak.zadaknotify.interfaces.ImageLoader;
import ir.zadak.zadaknotify.interfaces.OnImageLoadingCompleted;
import ir.zadak.zadaknotify.notification.Load;
import ir.zadak.zadaknotify.notification.ZadakNotification;

public class MolvixNotificationManager {

    private static String createNotificationChannel(String channelName, String channelDescription, String channelId) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = ApplicationLoader.getInstance().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        return channelId;
    }

    public static void showEpisodeDownloadProgressNotification(String movieName, String movieDescription, String seasonId, String episodeId, String title, int progress, String progressMessage) {
        createNotificationChannel(movieName, movieDescription, seasonId);

        int identifier = episodeId.hashCode();

        Intent cancelIntent = new Intent(ApplicationLoader.getInstance(), EmptyContentActivity.class);
        cancelIntent.putExtra(AppConstants.EPISODE_ID, episodeId);
        PendingIntent cancelPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent mainIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
        mainIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.NAVIGATE_TO_SECOND_FRAGMENT);

        PendingIntent mainPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), identifier, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Load mLoad = ZadakNotification.with(ApplicationLoader.getInstance()).load();
        mLoad.notificationChannelId(seasonId)
                .title(title)
                .autoCancel(true)
                .largeIcon(R.drawable.ic_launcher);
        mLoad.identifier(Math.abs(identifier));

        if (progress == 100) {
            mLoad.click(mainPendingIntent);
            mLoad.smallIcon(android.R.drawable.stat_sys_download_done);
            mLoad.message("Downloaded" + "..." + progressMessage);
            mLoad.simple().build();
        } else {
            mLoad.button(R.drawable.cancel, "CANCEL", cancelPendingIntent);
            mLoad.smallIcon(android.R.drawable.stat_sys_download);
            mLoad.message("Downloading" + "..." + progressMessage);
            mLoad.progress().value(progress, 100, false).build();
        }
    }

    private static Target getViewTarget(final OnImageLoadingCompleted onCompleted) {
        return new Target<Bitmap>() {

            @Override
            public void onStart() {

            }

            @Override
            public void onStop() {

            }

            @Override
            public void onDestroy() {

            }

            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {

            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {

            }

            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                onCompleted.imageLoadingCompleted(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }

            @Override
            public void getSize(@NonNull SizeReadyCallback cb) {

            }

            @Override
            public void removeCallback(@NonNull SizeReadyCallback cb) {

            }

            @Override
            public void setRequest(@Nullable Request request) {

            }

            @Nullable
            @Override
            public Request getRequest() {
                return null;
            }
        };
    }

    public static void recommendMovieToUser(String movieId) {
        RequestOptions imageLoadRequestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        try (Realm realm = Realm.getDefaultInstance()) {
            Movie recommendableMovie = realm.where(Movie.class).equalTo(AppConstants.MOVIE_ID, movieId).findFirst();
            if (recommendableMovie != null) {
                Intent movieDetailsIntent = new Intent(ApplicationLoader.getInstance(), MovieDetailsActivity.class);
                movieDetailsIntent.putExtra(AppConstants.MOVIE_ID, recommendableMovie.getMovieId());
                PendingIntent movieDetailsPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, movieDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                createNotificationChannel("Next Rate Movie", "Check this out", "Molvix Next Rated Movie");
                ZadakNotification.with(ApplicationLoader.getInstance())
                        .load()
                        .notificationChannelId("Molvix Next Rated Movie")
                        .title("Molvix")
                        .message("Recommended For You")
                        .autoCancel(true)
                        .click(movieDetailsPendingIntent)
                        .bigTextStyle("Have you seen the Movie \"" + WordUtils.capitalize(recommendableMovie.getMovieName()) + "\"")
                        .smallIcon(R.drawable.ic_launcher)
                        .largeIcon(R.drawable.ic_launcher)
                        .color(android.R.color.background_dark)
                        .custom()
                        .setImageLoader(new ImageLoader() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public void load(String photoUrl, OnImageLoadingCompleted onCompleted) {
                                Glide.with(ApplicationLoader.getInstance())
                                        .load(photoUrl)
                                        .apply(imageLoadRequestOptions)
                                        .into(getViewTarget(onCompleted));
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public void load(int imageResId, OnImageLoadingCompleted onCompleted) {
                                Glide.with(ApplicationLoader.getInstance())
                                        .load(imageResId)
                                        .apply(imageLoadRequestOptions)
                                        .into(getViewTarget(onCompleted));
                            }
                        })
                        .background(recommendableMovie.getMovieArtUrl())
                        .setPlaceholder(R.drawable.ic_placeholder)
                        .build();
                realm.executeTransaction(r -> {
                    recommendableMovie.setRecommendedToUser(true);
                    r.copyToRealmOrUpdate(recommendableMovie, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                });
            }
        }
    }
    
}