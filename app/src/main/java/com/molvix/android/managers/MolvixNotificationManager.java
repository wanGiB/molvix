package com.molvix.android.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;

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
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.CancelDownloadActivity;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.ui.notifications.notification.Load;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

class MolvixNotificationManager {

    private static void createNotificationChannel(String channelName, String channelDescription, String channelId) {
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
    }

    static void showEpisodeDownloadProgressNotification(String movieName, String movieDescription, String seasonId, String episodeId, String title, int progress, String progressMessage) {
        createNotificationChannel(movieName, movieDescription, seasonId);

        int identifier = Math.abs(episodeId.hashCode());

        Intent cancelIntent = new Intent(ApplicationLoader.getInstance(), CancelDownloadActivity.class);
        cancelIntent.putExtra(AppConstants.EPISODE_ID, episodeId);
        PendingIntent cancelPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), identifier, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent mainIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
        mainIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.NAVIGATE_TO_SECOND_FRAGMENT);

        PendingIntent mainPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), identifier, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Load mLoad = MolvixNotification.with(ApplicationLoader.getInstance()).load();
        mLoad.notificationChannelId(seasonId)
                .title(title)
                .largeIcon(R.mipmap.ic_launcher);
        mLoad.identifier(identifier);

        if (progress == 100) {
            mLoad.click(mainPendingIntent);
            mLoad.smallIcon(android.R.drawable.stat_sys_download_done);
            mLoad.message("Downloaded" + "..." + progressMessage);
            mLoad.autoCancel(true);
            mLoad.simple().build();
        } else {
            mLoad.button(R.drawable.cancel, "CANCEL", cancelPendingIntent);
            mLoad.smallIcon(android.R.drawable.stat_sys_download);
            mLoad.message("Downloading" + "..." + progressMessage);
            mLoad.progress().value(progress, 100, false).build();
        }
    }

    static void recommendMovieToUser(String movieId, Bitmap bitmap) {
        Movie recommendableMovie = MolvixDB.getMovie(movieId);
        if (recommendableMovie != null) {
            Intent movieDetailsIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
            movieDetailsIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.DISPLAY_MOVIE);
            movieDetailsIntent.putExtra(AppConstants.MOVIE_ID, recommendableMovie.getMovieId());
            PendingIntent movieDetailsPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, movieDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            createNotificationChannel("Next Rate Movie", "Check this out", "Molvix Next Rated Movie");
            MolvixNotification.with(ApplicationLoader.getInstance())
                    .load()
                    .notificationChannelId("Molvix Next Rated Movie")
                    .title("Molvix")
                    .message("Have you seen the Movie \"" + WordUtils.capitalize(recommendableMovie.getMovieName()) + "\"")
                    .autoCancel(true)
                    .click(movieDetailsPendingIntent)
                    .bigTextStyle("Have you seen the Movie \"" + WordUtils.capitalize(recommendableMovie.getMovieName()) + "\"", "Recommended For You")
                    .smallIcon(R.drawable.ic_stat_molvix_logo)
                    .largeIcon(R.mipmap.ic_launcher)
                    .color(android.R.color.background_dark)
                    .custom()
                    .background(bitmap)
                    .setPlaceholder(R.drawable.ic_placeholder)
                    .build();
            AppPrefs.setLastMovieRecommendationTime(System.currentTimeMillis());
            recommendableMovie.setRecommendedToUser(true);
            MolvixDB.updateMovie(recommendableMovie);
        }
    }

    static void displayNewMovieNotification(Movie updatedMovie, Notification newMovieAvailableNotification, String checkKey) {
        String movieArtUrl = updatedMovie.getMovieArtUrl();
        if (movieArtUrl != null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Movie Art Url for next notification is not null.About to display notification for the movie");
            new BitmapLoadTask(newMovieAvailableNotification, checkKey).execute(movieArtUrl);
        } else {
            new MovieContentsExtractionTask(newMovieAvailableNotification, checkKey).execute(updatedMovie);
        }
    }

    private static class MovieContentsExtractionTask extends AsyncTask<Movie, Void, Void> {

        private Notification notification;
        private String checkKey;

        MovieContentsExtractionTask(Notification notification, String checkKey) {
            this.notification = notification;
            this.checkKey = checkKey;
        }

        @Override
        protected Void doInBackground(Movie... movies) {
            ContentManager.extractMovieMetaData(movies[0], (result, e) -> {
                if (e == null && result != null) {
                    String movieArtUrl = result.getMovieArtUrl();
                    if (movieArtUrl != null) {
                        new BitmapLoadTask(notification, checkKey).execute(movieArtUrl);
                    }
                }
            });
            return null;
        }

    }

    private static class BitmapLoadTask extends AsyncTask<String, Void, Void> {

        private Notification notification;
        private String checkKey;

        BitmapLoadTask(Notification notification, String checkKey) {
            this.notification = notification;
            this.checkKey = checkKey;
        }

        @Override
        protected Void doInBackground(String... strings) {
            String artUrl = strings[0];
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
                            UiUtils.runOnMain(() -> MolvixNotificationManager.displayUpdatedMovieNotification(resource, notification, checkKey));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                    });
            return null;

        }

    }

    private static void displayUpdatedMovieNotification(Bitmap movieBitmap, Notification notification, String checkKey) {
        String messageDisplay = notification.getMessage();
        Intent movieDetailsIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
        movieDetailsIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.DISPLAY_MOVIE);
        movieDetailsIntent.putExtra(AppConstants.MOVIE_ID, notification.getDestinationKey());
        PendingIntent movieDetailsPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, movieDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        createNotificationChannel("Next Rated Movie", "Check this out", "Molvix Next Rated Movie");
        MolvixNotification.with(ApplicationLoader.getInstance())
                .load()
                .notificationChannelId("Molvix Next Rated Movie")
                .title("Molvix")
                .message(UiUtils.fromHtml(messageDisplay))
                .autoCancel(true)
                .click(movieDetailsPendingIntent)
                .bigTextStyle(UiUtils.fromHtml(messageDisplay), "From Your Downloads")
                .smallIcon(R.drawable.ic_stat_molvix_logo)
                .largeIcon(R.mipmap.ic_launcher)
                .color(android.R.color.background_dark)
                .custom()
                .background(movieBitmap)
                .setPlaceholder(R.drawable.ic_placeholder)
                .build();
        AppPrefs.setHasBeenNotified(checkKey);

    }

}