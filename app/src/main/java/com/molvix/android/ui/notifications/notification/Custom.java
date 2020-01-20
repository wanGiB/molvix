package com.molvix.android.ui.notifications.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Looper;
import android.text.Spanned;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import com.molvix.android.R;
import com.molvix.android.managers.MovieTracker;

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN)
public class Custom extends Builder {
    private RemoteViews mRemoteView;
    private String mTitle;
    private String mMessage;
    private Spanned mMessageSpanned;
    private int mSmallIcon;
    private int mPlaceHolderResourceId;
    private Bitmap backgroundBitmap;

    public Custom(NotificationCompat.Builder builder, int identifier, String title, String message, Spanned messageSpanned, int smallIcon, String tag) {
        super(builder, identifier, tag);
        this.mRemoteView = new RemoteViews(MolvixNotification.mSingleton.mContext.getPackageName(), R.layout.notification_custom);
        this.mTitle = title;
        this.mMessage = message;
        this.mMessageSpanned = messageSpanned;
        this.mSmallIcon = smallIcon;
        this.mPlaceHolderResourceId = R.drawable.ic_launcher;
        this.init();
    }

    private void init() {
        this.setTitle();
        this.setMessage();
        this.setSmallIcon();
    }

    private void setTitle() {
        mRemoteView.setTextViewText(R.id.notification_text_title, mTitle);
    }

    private void setMessage() {
        if (mMessageSpanned != null) {
            mRemoteView.setTextViewText(R.id.notification_text_message, mMessageSpanned);
        } else {
            mRemoteView.setTextViewText(R.id.notification_text_message, mMessage);
        }
    }

    private void setSmallIcon() {
        if (mSmallIcon <= 0) {
            mRemoteView.setImageViewResource(R.id.notification_img_icon, R.drawable.ic_launcher);
        }
        mRemoteView.setImageViewResource(R.id.notification_img_icon, mSmallIcon);
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
        this.mPlaceHolderResourceId = resource;
        return this;
    }

    @Override
    public void build() {
        if (!(Looper.getMainLooper().getThread() == Thread.currentThread())) {
            throw new IllegalStateException("Method call should happen from the main thread.");
        }
        super.build();
        setBigContentView(mRemoteView);
        loadImageBackground();
        super.notificationNotify();
    }

    private void loadImageBackground() {
        mRemoteView.setImageViewResource(R.id.notification_img_background, mPlaceHolderResourceId);
        if (backgroundBitmap != null) {
            mRemoteView.setImageViewBitmap(R.id.notification_img_background, backgroundBitmap);
        }
    }

}
