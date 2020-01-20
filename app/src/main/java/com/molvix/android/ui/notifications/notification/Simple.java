package com.molvix.android.ui.notifications.notification;


import androidx.core.app.NotificationCompat;

public class Simple extends Builder {
    public Simple(NotificationCompat.Builder builder, int identifier, String tag) {
        super(builder, identifier, tag);
    }

    @Override
    public void build() {
        super.build();
        super.notificationNotify();
    }
}
