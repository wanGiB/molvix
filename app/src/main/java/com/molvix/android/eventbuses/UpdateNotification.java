package com.molvix.android.eventbuses;

import com.molvix.android.models.Notification;

public class UpdateNotification {

    private Notification notification;

    public UpdateNotification(Notification notification) {
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }

}
