package com.molvix.android.managers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.ui.activities.EmptyContentActivity;
import com.molvix.android.ui.activities.MainActivity;

import ir.zadak.zadaknotify.notification.Load;
import ir.zadak.zadaknotify.notification.ZadakNotification;

public class MolvixNotificationManager {
    private static String createNotificationChannel(String channelName, String channelDescription, String channelId) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = channelName;
            String description = channelDescription;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
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

}
