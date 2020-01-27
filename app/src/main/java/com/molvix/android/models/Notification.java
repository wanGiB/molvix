package com.molvix.android.models;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
public class Notification {

    @Id
    public long id;
    public String notificationObjectId;
    public String destinationKey;
    public int destination;
    public String message;
    public long timeStamp;
    public boolean seen;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public void setDestinationKey(String destinationKey) {
        this.destinationKey = destinationKey;
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

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    @Override
    public int hashCode() {
        int result;
        result = this.notificationObjectId.hashCode();
        final String name = getClass().getName();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Notification another = (Notification) obj;
        return this.getNotificationObjectId().equals(another.getNotificationObjectId());
    }
}
