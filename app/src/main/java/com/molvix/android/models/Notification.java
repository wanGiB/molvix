package com.molvix.android.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Notification extends RealmObject {
    @PrimaryKey
    private String notificationObjectId;
    private String resolutionKey;
    private int destination;
    private String message;
    private long timeStamp;
    private boolean seen;

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getNotificationObjectId() {
        return notificationObjectId;
    }

    public void setNotificationObjectId(String notificationObjectId) {
        this.notificationObjectId = notificationObjectId;
    }

    public String getResolutionKey() {
        return resolutionKey;
    }

    public void setResolutionKey(String resolutionKey) {
        this.resolutionKey = resolutionKey;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
