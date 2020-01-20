package com.molvix.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

import com.molvix.android.managers.ContentManager;
import com.molvix.android.utils.ConnectivityUtils;

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private NotificationsPullTask notificationsPullTask;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            fetchNotifications();
        }
    }

    private void fetchNotifications() {
        if (notificationsPullTask != null) {
            notificationsPullTask.cancel(true);
            notificationsPullTask = null;
        }
        notificationsPullTask = new NotificationsPullTask();
        notificationsPullTask.execute();
    }

    static class NotificationsPullTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.fetchNotifications();
            return null;
        }
    }

}
