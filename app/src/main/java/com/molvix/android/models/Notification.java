package com.molvix.android.models;

import com.orm.dsl.Table;
import com.orm.dsl.Unique;

@Table
public class Notification {

    @Unique
    private String notificationObjectId;
    private String destinationKey;
    private int destination;
    private String message;
    private long timeStamp;
    private boolean seen;
    private long id;

    public Notification() {

    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

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

    public String getDestinationKey() {
        return destinationKey;
    }

    public void setDestinationKey(String resolutionKey) {
        this.destinationKey = resolutionKey;
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
