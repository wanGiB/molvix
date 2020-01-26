package com.molvix.android.ui.notifications.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.molvix.android.R;
import com.molvix.android.ui.notifications.interfaces.PendingIntentNotification;

import java.util.List;

public class Wear extends Builder {
    private NotificationCompat.WearableExtender wearableExtender;
    private RemoteInput remoteInput;

    Wear(NotificationCompat.Builder builder, int identifier, String tag) {
        super(builder, identifier, tag);
        this.wearableExtender = new NotificationCompat.WearableExtender();
    }

    public Wear hideIcon(boolean hideIcon) {
        wearableExtender.setHintHideIcon(hideIcon);
        return this;
    }

    public Wear contentIcon(@DrawableRes int contentIcon) {
        wearableExtender.setContentIcon(contentIcon);
        return this;
    }

    public Wear addPages(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification Must Not Be Null.");
        }

        wearableExtender.addPage(notification);
        return this;
    }

    public Wear addPages(List<Notification> notificationList) {
        if (notificationList == null || notificationList.isEmpty()) {
            throw new IllegalArgumentException("List Notitifcation Must Not Be Null And Empty!");
        }

        wearableExtender.addPages(notificationList);
        return this;
    }

    @SuppressLint("ResourceType")
    public Wear button(@DrawableRes int icon, String title, PendingIntent pendingIntent) {
        if (icon < 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        if (title == null) {
            throw new IllegalStateException("Title Must Not Be Null!");
        }
        if (pendingIntent == null) {
            throw new IllegalArgumentException("PendingIntent Must Not Be Null.");
        }

        this.wearableExtender.addAction(new NotificationCompat.Action(icon, title, pendingIntent));
        return this;
    }


    public Wear remoteInput(@DrawableRes int icon, @StringRes int title, PendingIntentNotification pendingIntentNotification, RemoteInput remoteInput) {
        remoteInput(icon, MolvixNotification.mSingleton.mContext.getString(title), pendingIntentNotification.onSettingPendingIntent(), remoteInput);
        return this;
    }

    public Wear remoteInput(@DrawableRes int icon, String title, PendingIntentNotification pendingIntentNotification, RemoteInput remoteInput) {
        remoteInput(icon, title, pendingIntentNotification.onSettingPendingIntent(), remoteInput);
        return this;
    }

    public Wear remoteInput(@DrawableRes int icon, @StringRes int title, PendingIntent pendingIntent, RemoteInput remoteInput) {
        remoteInput(icon, MolvixNotification.mSingleton.mContext.getString(title), pendingIntent, remoteInput);
        return this;
    }

    @SuppressLint("ResourceType")
    public Wear remoteInput(@DrawableRes int icon, String title, PendingIntent pendingIntent, RemoteInput remoteInput) {
        if (icon <= 0) {
            throw new IllegalArgumentException("Resource ID Icon Should Not Be Less Than Or Equal To Zero!");
        }

        if (title == null) {
            throw new IllegalArgumentException("Title Must Not Be Null!");
        }

        if (pendingIntent == null) {
            throw new IllegalArgumentException("PendingIntent Must Not Be Null!");
        }

        if (remoteInput == null) {
            throw new IllegalArgumentException("RemoteInput Must Not Be Null!");
        }

        this.remoteInput = remoteInput;
        wearableExtender.addAction(new NotificationCompat.Action.Builder(icon,
                title, pendingIntent)
                .addRemoteInput(remoteInput)
                .build());
        return this;
    }

    @SuppressLint("ResourceType")
    public Wear remoteInput(@DrawableRes int icon, String title, PendingIntent pendingIntent) {
        if (icon <= 0) {
            throw new IllegalArgumentException("Resource ID Icon Should Not Be Less Than Or Equal To Zero!");
        }

        if (title == null) {
            throw new IllegalArgumentException("Title Must Not Be Null!");
        }

        if (pendingIntent == null) {
            throw new IllegalArgumentException("PendingIntent Must Not Be Null!");
        }

        this.remoteInput = new RemoteInput.Builder(MolvixNotification.mSingleton.mContext.getString(R.string.key_voice_reply))
                .setLabel(MolvixNotification.mSingleton.mContext.getString(R.string.label_voice_reply))
                .setChoices(MolvixNotification.mSingleton.mContext.getResources().getStringArray(R.array.reply_choices))
                .build();
        wearableExtender.addAction(new NotificationCompat.Action.Builder(icon,
                title, pendingIntent)
                .addRemoteInput(remoteInput)
                .build());
        return this;
    }

    public Wear remoteInput(@DrawableRes int icon, @StringRes int title, PendingIntent pendingIntent, String replyLabel, String[] replyChoices) {
        return remoteInput(icon, MolvixNotification.mSingleton.mContext.getString(title), pendingIntent, replyLabel, replyChoices);
    }

    @SuppressLint("ResourceType")
    public Wear remoteInput(@DrawableRes int icon, String title, PendingIntent pendingIntent, String replyLabel, String[] replyChoices) {
        if (icon <= 0) {
            throw new IllegalArgumentException("Resource ID Icon Should Not Be Less Than Or Equal To Zero!");
        }

        if (title == null) {
            throw new IllegalArgumentException("Title Must Not Be Null!");
        }

        if (replyChoices == null) {
            throw new IllegalArgumentException("Reply Choices Must Not Be Null!");
        }

        if (pendingIntent == null) {
            throw new IllegalArgumentException("PendingIntent Must Not Be Null!");
        }
        if (replyLabel == null) {
            throw new IllegalArgumentException("Reply Label Must Not Be Null!");
        }

        this.remoteInput = new RemoteInput.Builder(MolvixNotification.mSingleton.mContext.getString(R.string.key_voice_reply))
                .setLabel(replyLabel)
                .setChoices(replyChoices)
                .build();
        wearableExtender.addAction(new NotificationCompat.Action.Builder(icon,
                title, pendingIntent)
                .addRemoteInput(remoteInput)
                .build());
        return this;
    }

    public Wear background(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap Must Not Be Null.");
        }

        this.wearableExtender.setBackground(bitmap);
        return this;
    }

    @SuppressLint("ResourceType")
    public Wear background(@DrawableRes int background) {
        if (background <= 0) {
            throw new IllegalArgumentException("Resource ID Background Should Not Be Less Than Or Equal To Zero!");
        }

        Bitmap bitmap = BitmapFactory.decodeResource(MolvixNotification.mSingleton.mContext.getResources(), background);
        this.wearableExtender.setBackground(bitmap);
        return this;
    }

    public Wear startScrollBottom(boolean startScrollBottom) {
        this.wearableExtender.setStartScrollBottom(startScrollBottom);
        return this;
    }

    @Override
    public void build() {
        builder.extend(wearableExtender);
        super.build();
        super.notificationNotify();
    }
}
