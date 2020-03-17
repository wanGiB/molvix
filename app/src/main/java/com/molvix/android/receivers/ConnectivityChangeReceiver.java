package com.molvix.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

import com.molvix.android.eventbuses.ConnectivityChangedEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MolvixNotificationManager;
import com.molvix.android.managers.MovieTracker;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;

import org.greenrobot.eventbus.EventBus;

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private NotificationsPullTask notificationsPullTask;
    private DeletedContentCleanUpTask deletedContentCleanUpTask;

    @Override
    public void onReceive(Context context, Intent intent) {
        performDeletedContentsCleanUp();
        if (intent.getAction() == null) {
            return;
        }
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            if (AppPrefs.canDailyMoviesBeRecommended()) {
                MovieTracker.recommendUnWatchedMoviesToUser();
            }
            MolvixNotificationManager.checkAndResumeUnFinishedDownloads();
            EventBus.getDefault().post(new ConnectivityChangedEvent());
            fetchNotifications();
            ContentManager.fetchPresets();
        }
    }

    public void fetchNotifications() {
        if (notificationsPullTask != null) {
            notificationsPullTask.cancel(true);
            notificationsPullTask = null;
        }
        notificationsPullTask = new NotificationsPullTask();
        notificationsPullTask.execute();
    }

    private void performDeletedContentsCleanUp() {
        if (deletedContentCleanUpTask != null) {
            deletedContentCleanUpTask.cancel(true);
            deletedContentCleanUpTask = null;
        }
        deletedContentCleanUpTask = new DeletedContentCleanUpTask();
        deletedContentCleanUpTask.execute();
    }

    static class NotificationsPullTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.fetchNotifications();
            return null;
        }
    }

    static class DeletedContentCleanUpTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.cleanUpDeletedContents();
            return null;
        }
    }

}
