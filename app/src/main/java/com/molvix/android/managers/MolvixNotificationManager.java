package com.molvix.android.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.Movie;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.EmptyContentActivity;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.ui.notifications.notification.Load;
import com.molvix.android.ui.notifications.notification.MolvixNotification;

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

        Intent cancelIntent = new Intent(ApplicationLoader.getInstance(), EmptyContentActivity.class);
        cancelIntent.putExtra(AppConstants.EPISODE_ID, episodeId);
        PendingIntent cancelPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent mainIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
        mainIntent.putExtra(AppConstants.INVOCATION_TYPE, AppConstants.NAVIGATE_TO_SECOND_FRAGMENT);

        PendingIntent mainPendingIntent = PendingIntent.getActivity(ApplicationLoader.getInstance(), identifier, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Load mLoad = MolvixNotification.with(ApplicationLoader.getInstance()).load();
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

    static void recommendMovieToUser(String movieId, Bitmap bitmap) {
        Movie recommendableMovie = MolvixDB.getMovie(movieId);
        if (recommendableMovie != null) {
            Intent movieDetailsIntent = new Intent(ApplicationLoader.getInstance(), MainActivity.class);
            movieDetailsIntent.putExtra(AppConstants.INVOCATION_TYPE,AppConstants.DISPLAY_MOVIE);
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
                    .bigTextStyle("Have you seen the Movie \"" + WordUtils.capitalize(recommendableMovie.getMovieName()) + "\"")
                    .smallIcon(R.drawable.ic_launcher)
                    .largeIcon(R.drawable.ic_launcher)
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
}