package com.molvix.android.ui.notifications.notification;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Looper;
import android.text.Spanned;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import com.molvix.android.R;

public class Custom extends Builder {
    private String mTitle;
    private String mMessage;
    private Spanned mMessageSpanned;
    private int mSmallIcon;
    private Bitmap backgroundBitmap;

    Custom(NotificationCompat.Builder builder, int identifier, String title, String message, Spanned messageSpanned, int smallIcon, String tag) {
        super(builder, identifier, tag);
        this.mTitle = title;
        this.mMessage = message;
        this.mMessageSpanned = messageSpanned;
        this.mSmallIcon = smallIcon;
        this.init();
    }

    private void init() {
        this.setTitle();
        this.setMessage();
        this.setSmallIcon();
    }

    private void setTitle() {
        builder.setContentTitle(mTitle);
    }

    private void setMessage() {
        if (mMessageSpanned != null) {
            builder.setContentText(mMessageSpanned);
        } else {
            builder.setContentText(mMessage);
        }
    }

    private void setSmallIcon() {
        if (mSmallIcon <= 0) {
            builder.setSmallIcon(R.mipmap.ic_launcher);
        } else {
            builder.setSmallIcon(mSmallIcon);
        }
    }

    @SuppressLint("ResourceType")
    public Custom background(@DrawableRes int resource) {
        if (resource <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }
        return this;
    }

    public Custom background(Bitmap bitmap) {
        this.backgroundBitmap = bitmap;
        return this;
    }

    @SuppressLint("ResourceType")
    public Custom setPlaceholder(@DrawableRes int resource) {
        if (resource <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }
        return this;
    }

    @Override
    public void build() {
        if (!(Looper.getMainLooper().getThread() == Thread.currentThread())) {
            throw new IllegalStateException("Method call should happen from the main thread.");
        }
        loadImageBackground();
        super.build();
        super.notificationNotify();
    }

    private void loadImageBackground() {
        builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(backgroundBitmap).setBigContentTitle(mTitle).setSummaryText(mMessageSpanned != null ? mMessageSpanned : mMessage));
    }

}
