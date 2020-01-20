package com.molvix.android.ui.notifications.notification;

import android.app.Notification;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public abstract class Builder {
    private static final String TAG = Builder.class.getSimpleName();
    protected String tag;
    protected Notification notification;
    protected NotificationCompat.Builder builder;
    protected int notificationId;

    public Builder(NotificationCompat.Builder builder, int identifier, String tag) {
        this.builder = builder;
        this.notificationId = identifier;
        this.tag = tag;
    }

    public void build() {
        notification = builder.build();
    }

    public void setBigContentView(RemoteViews views) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            notification.bigContentView = views;
            return;
        }
        Log.w(TAG, "Version does not support big content view");
    }

    protected Notification notificationNotify() {
        if (tag != null) {
            return notificationNotify(tag, notificationId);
        }
        return notificationNotify(notificationId);
    }

    protected Notification notificationNotify(int identifier) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MolvixNotification.mSingleton.mContext);
        notificationManager.notify(identifier, notification);
        return notification;
    }

    protected Notification notificationNotify(String tag, int identifier) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MolvixNotification.mSingleton.mContext);
        notificationManager.notify(tag, identifier, notification);
        return notification;
    }

}
