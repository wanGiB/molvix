package com.molvix.android.ui.notifications.notification;

import android.app.Notification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

@SuppressWarnings("WeakerAccess")
public abstract class Builder {
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
