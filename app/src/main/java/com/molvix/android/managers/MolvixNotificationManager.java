package com.molvix.android.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
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
import com.molvix.android.ui.activities.ResumeUnFinishedDownloadsActivity;
import com.molvix.android.ui.notifications.notification.Load;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.Set;

public class MolvixNotificationManager {

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

    public static void showEpisodeDownloadProgressNotification(String movieName, String movieDescription, String seasonId, String episodeId, String title, int progress, String progressMessage) {
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
            mLoad.message("Downloading..." + progressMessage);
            mLoad.progress().value(progress, 100, false).percentage(progress + "%").build();
        }
    }

    static void recommendMovieToUser(String movieId, Bitmap bitmap) {
        Movie recommendableMovie = MolvixDB.getMovie(movieId);
        if (recommendableMovie != null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "About to display Notifications for first found recommendable Movie");
            Intent movieDetailsIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
            movieDetailsIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.DISPLAY_MOVIE);
            movieDetailsIntent.putExtra(AppConstants.MOVIE_ID, recommendableMovie.getMovieId());
            PendingIntent movieDetailsPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, movieDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            createNotificationChannel("Next Rate Movie", "Check this out", "Molvix Next Rated Movie");
            MolvixNotification.with(ApplicationLoader.getInstance())
                    .load()
                    .notificationChannelId("Molvix Next Rated Movie")
                    .title("Have you seen this Movie?")
                    .message(UiUtils.fromHtml("\"<b>" + WordUtils.capitalize(recommendableMovie.getMovieName()) + "</b>\""))
                    .autoCancel(true)
                    .click(movieDetailsPendingIntent)
                    .smallIcon(R.drawable.ic_stat_molvix_logo)
                    .largeIcon(R.mipmap.ic_launcher)
                    .custom()
                    .background(bitmap)
                    .setPlaceholder(R.drawable.ic_placeholder)
                    .build();
            AppPrefs.setLastMovieRecommendationTime(System.currentTimeMillis());
            recommendableMovie.setRecommendedToUser(true);
            MolvixDB.updateMovie(recommendableMovie);
        }
    }

    private static void displayUpdatedMovieNotification(Bitmap movieBitmap, Movie movie, String displayMessage, Notification notification, String checkKey) {
        Intent movieDetailsIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
        movieDetailsIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.DISPLAY_MOVIE);
        movieDetailsIntent.putExtra(AppConstants.MOVIE_ID, notification.getDestinationKey());
        PendingIntent movieDetailsPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, movieDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        createNotificationChannel("Next Rated Movie", "Check this out", "Molvix Next Rated Movie");
        MolvixNotification.with(ApplicationLoader.getInstance())
                .load()
                .notificationChannelId("Molvix Next Rated Movie")
                .title(WordUtils.capitalize(movie.getMovieName()))
                .message(UiUtils.fromHtml(displayMessage))
                .priority(NotificationCompat.PRIORITY_HIGH)
                .ticker("Molvix")
                .autoCancel(true)
                .click(movieDetailsPendingIntent)
                .smallIcon(R.drawable.ic_stat_molvix_logo)
                .largeIcon(R.mipmap.ic_launcher)
                .custom()
                .background(movieBitmap)
                .setPlaceholder(R.drawable.ic_placeholder)
                .buildCustomWithDefaults();
        AppPrefs.setHasBeenNotified(checkKey);
    }

    public static void displayNewMovieNotification(Movie updatedMovie, String displayMessage, Notification newMovieAvailableNotification, String checkKey) {
        String movieArtUrl = updatedMovie.getMovieArtUrl();
        if (movieArtUrl != null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Movie Art Url for next notification is not null.About to display notification for the movie");
            new BitmapLoadTask(newMovieAvailableNotification, displayMessage, checkKey).execute(updatedMovie);
        } else {
            new MovieContentsExtractionTask(newMovieAvailableNotification, displayMessage, checkKey).execute(updatedMovie);
        }
    }

    public static void checkAndResumeUnFinishedDownloads() {
        if (!AppConstants.MAIN_ACTIVITY_IN_FOCUS.get() && !ApplicationLoader.globalDownloadListener.isEnable()) {
            Set<String> unFinishedDownloads = AppPrefs.getInProgressDownloads();
            if (!unFinishedDownloads.isEmpty()) {

                int sizeOfUnFinishedDownloads = unFinishedDownloads.size();
                String quantifier = sizeOfUnFinishedDownloads == 1 ? "download" : "downloads";
                String message = "You have " + sizeOfUnFinishedDownloads + " unfinished " + quantifier;

                Intent unFinishedDownloadsIntent = new Intent(ApplicationLoader.getInstance(), ResumeUnFinishedDownloadsActivity.class);
                PendingIntent unFinishedDownloadsPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, unFinishedDownloadsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Intent mainIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
                mainIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.SHOW_UNFINISHED_DOWNLOADS);

                PendingIntent mainPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 100, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                createNotificationChannel("UnFinished Molvix Downloads", "UnFinished Molvix Downloads", "UnFinished Molvix Downloads");

                MolvixNotification.with(ApplicationLoader.getInstance())
                        .load()
                        .identifier(Math.abs(AppConstants.SHOW_UNFINISHED_DOWNLOADS.hashCode()))
                        .notificationChannelId("UnFinished Molvix Downloads")
                        .title("Molvix")
                        .message(message)
                        .autoCancel(true)
                        .click(mainPendingIntent)
                        .button(android.R.drawable.stat_sys_download, "RESUME " + quantifier.toUpperCase(), unFinishedDownloadsPendingIntent)
                        .smallIcon(R.drawable.ic_stat_molvix_logo)
                        .largeIcon(R.mipmap.ic_launcher)
                        .simple()
                        .build();
            }
        }
    }

    private static class MovieContentsExtractionTask extends AsyncTask<Movie, Void, Void> {

        private Notification notification;
        private String checkKey;
        private String displayMessage;

        MovieContentsExtractionTask(Notification notification, String displayMessage, String checkKey) {
            this.notification = notification;
            this.checkKey = checkKey;
            this.displayMessage = displayMessage;
        }

        @Override
        protected Void doInBackground(Movie... movies) {
            ContentManager.extractMovieMetaData(movies[0], (result, e) -> {
                if (e == null && result != null) {
                    String movieArtUrl = result.getMovieArtUrl();
                    if (movieArtUrl != null) {
                        new BitmapLoadTask(notification, displayMessage, checkKey).execute(result);
                    }
                }
            });
            return null;
        }

    }

    private static class BitmapLoadTask extends AsyncTask<Movie, Void, Void> {

        private Notification notification;
        private String checkKey;
        private String displayMessage;

        BitmapLoadTask(Notification notification, String displayMessage, String checkKey) {
            this.notification = notification;
            this.checkKey = checkKey;
            this.displayMessage = displayMessage;
        }

        @Override
        protected Void doInBackground(Movie... movies) {
            String artUrl = movies[0].getMovieArtUrl();
            RequestOptions imageLoadRequestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
            Glide.with(ApplicationLoader.getInstance())
                    .asBitmap()
                    .load(artUrl)
                    .apply(imageLoadRequestOptions)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            UiUtils.runOnMain(() -> MolvixNotificationManager.displayUpdatedMovieNotification(resource, movies[0], displayMessage, notification, checkKey));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            //Most times the url has issues
                            Bitmap backUpDrawable = BitmapFactory.decodeResource(ApplicationLoader.getInstance().getResources(), R.mipmap.ic_launcher);
                            if (backUpDrawable != null) {
                                UiUtils.runOnMain(() -> MolvixNotificationManager.displayUpdatedMovieNotification(backUpDrawable, movies[0], displayMessage, notification, checkKey));
                            }
                        }

                    });
            return null;
        }
    }
}