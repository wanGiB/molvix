package com.molvix.android.ui.notifications.notification;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.molvix.android.ui.notifications.interfaces.PendingIntentNotification;
import com.molvix.android.ui.notifications.pendingintent.ClickPendingIntentActivity;
import com.molvix.android.ui.notifications.pendingintent.ClickPendingIntentBroadCast;
import com.molvix.android.ui.notifications.pendingintent.DismissPendingIntentActivity;
import com.molvix.android.ui.notifications.pendingintent.DismissPendingIntentBroadCast;

public class Load {

    private NotificationCompat.Builder builder;

    private String message;
    private Spanned messageSpanned;
    private String notificationChannelId;
    private int notificationId;
    private int smallIcon;
    private String tag;
    private String title;

    public Load() {
        builder = new NotificationCompat.Builder(MolvixNotification.mSingleton.mContext);
        builder.setContentIntent(PendingIntent.getBroadcast(MolvixNotification.mSingleton.mContext, 0, new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public Load addPerson(@NonNull String uri) {
        if (uri.length() == 0) {
            throw new IllegalArgumentException("URI Must Not Be Empty!");
        }

        this.builder.addPerson(uri);
        return this;
    }

    public Load autoCancel(boolean autoCancel) {
        this.builder.setAutoCancel(autoCancel);
        return this;
    }

    @SuppressLint("ResourceType")
    public Load bigTextStyle(@StringRes int bigTextStyle) {
        if (bigTextStyle <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        return bigTextStyle(MolvixNotification.mSingleton.mContext.getResources().getString(

                bigTextStyle), null);
    }

    @SuppressLint("ResourceType")
    public Load bigTextStyle(@StringRes int bigTextStyle, @StringRes int summaryText) {
        if (bigTextStyle <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        return bigTextStyle(MolvixNotification.mSingleton.mContext.getResources().getString(
                bigTextStyle), MolvixNotification.mSingleton.mContext.getResources().getString(
                summaryText));
    }

    public Load bigTextStyle(@NonNull String bigTextStyle) {
        if (bigTextStyle.trim().length() == 0) {
            throw new IllegalArgumentException("Big Text Style Must Not Be Empty!");
        }

        return bigTextStyle(bigTextStyle, null);


    }

    public Load bigTextStyle(@NonNull String bigTextStyle, String summaryText) {
        if (bigTextStyle.trim().length() == 0) {
            throw new IllegalArgumentException("Big Text Style Must Not Be Empty!");
        }

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(bigTextStyle);
        if (summaryText != null) {
            bigStyle.setSummaryText(summaryText);
        }


        this.builder.setStyle(bigStyle);
        return this;
    }

    public Load bigTextStyle(@NonNull Spanned bigTextStyle, String summaryText) {
        if (bigTextStyle.length() == 0) {
            throw new IllegalArgumentException("Big Text Style Must Not Be Empty!");
        }

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(bigTextStyle);
        if (summaryText != null) {
            bigStyle.setSummaryText(summaryText);
        }
        this.builder.setStyle(bigStyle);
        return this;
    }

    public Load button(@DrawableRes int icon, @NonNull String title, @NonNull PendingIntent pendingIntent) {
        this.builder.addAction(icon, title, pendingIntent);
        return this;
    }

    public Load button(@DrawableRes int icon, @NonNull String title,
                       @NonNull PendingIntentNotification pendingIntentNotification) {
        this.builder.addAction(icon, title, pendingIntentNotification.onSettingPendingIntent());

        return this;
    }

    public Load button(@NonNull NotificationCompat.Action action) {
        this.builder.addAction(action);
        return this;
    }

    public Load click(@NonNull Class<?> activity, Bundle bundle) {
        this.builder.setContentIntent(new ClickPendingIntentActivity(activity, bundle, notificationId)
                .onSettingPendingIntent());
        return this;
    }

    public Load click(@NonNull Class<?> activity) {
        click(activity, null);
        return this;
    }

    public Load click(@NonNull Bundle bundle) {
        this.builder.setContentIntent(new ClickPendingIntentBroadCast(bundle, notificationId).onSettingPendingIntent());
        return this;
    }

    public Load click(@NonNull PendingIntentNotification pendingIntentNotification) {
        this.builder.setContentIntent(pendingIntentNotification.onSettingPendingIntent());
        return this;
    }

    public Load click(@NonNull PendingIntent pendingIntent) {
        this.builder.setContentIntent(pendingIntent);
        return this;
    }

    public Custom custom() {
        notificationShallContainAtLeastThoseSmallIconValid();
        return new Custom(builder, notificationId, title, message, messageSpanned, smallIcon, tag);
    }

    public Load dismiss(@NonNull Class<?> activity, Bundle bundle) {
        this.builder.setDeleteIntent(new DismissPendingIntentActivity(activity, bundle, notificationId)
                .onSettingPendingIntent());
        return this;
    }

    public Load dismiss(@NonNull Class<?> activity) {
        dismiss(activity, null);
        return this;
    }

    public Load dismiss(@NonNull Bundle bundle) {
        this.builder.setDeleteIntent(new DismissPendingIntentBroadCast(bundle, notificationId).onSettingPendingIntent
                ());
        return this;
    }

    public Load dismiss(@NonNull PendingIntentNotification pendingIntentNotification) {
        this.builder.setDeleteIntent(pendingIntentNotification.onSettingPendingIntent());
        return this;
    }

    public Load dismiss(@NonNull PendingIntent pendingIntent) {
        this.builder.setDeleteIntent(pendingIntent);
        return this;
    }

    public Load flags(int defaults) {
        this.builder.setDefaults(defaults);
        return this;
    }

    public Load group(@NonNull String groupKey) {
        if (groupKey.trim().length() == 0) {

            throw new IllegalArgumentException("Group Key Must Not Be Empty!");
        }

        this.builder.setGroup(groupKey);
        return this;
    }

    public Load groupSummary(boolean groupSummary) {
        this.builder.setGroupSummary(groupSummary);
        return this;
    }

    public Load identifier(int identifier) {
        if (identifier <= 0) {

            throw new IllegalStateException("Identifier Should Not Be Less Than Or Equal To Zero!");
        }

        this.notificationId = identifier;
        return this;
    }

    public Load inboxStyle(@NonNull String[] inboxLines, @NonNull String title, String summary) {
        if (inboxLines.length <= 0) {
            throw new IllegalArgumentException("Inbox Lines Must Have At Least One Text!");
        }

        if (title.trim().length() == 0) {
            throw new IllegalArgumentException("Title Must Not Be Empty!");
        }

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (String inboxLine : inboxLines) {
            inboxStyle.addLine(inboxLine);
        }
        inboxStyle.setBigContentTitle(title);
        if (summary != null) {
            inboxStyle.setSummaryText(summary);
        }
        this.builder.setStyle(inboxStyle);
        return this;
    }

    public Load largeIcon(@NonNull Bitmap bitmap) {
        this.builder.setLargeIcon(bitmap);
        return this;
    }

    @SuppressLint("ResourceType")
    public Load largeIcon(@DrawableRes int largeIcon) {
        if (largeIcon <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        Bitmap bitmap = BitmapFactory.decodeResource(MolvixNotification.mSingleton.mContext.getResources(), largeIcon);
        this.builder.setLargeIcon(bitmap);
        return this;
    }

    public Load lights(int color, int ledOnMs, int ledOfMs) {
        if (ledOnMs < 0) {
            throw new IllegalStateException("Led On Milliseconds Invalid!");
        }

        if (ledOfMs < 0) {
            throw new IllegalStateException("Led Off Milliseconds Invalid!");
        }

        this.builder.setLights(color, ledOnMs, ledOfMs);

        return this;
    }

    @SuppressLint("ResourceType")
    public Load message(@StringRes int message) {
        if (message <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        this.message = MolvixNotification.mSingleton.mContext.getResources().getString(message);
        this.builder.setContentText(this.message);
        return this;
    }

    public Load message(@NonNull String message) {
        if (message.trim().length() == 0) {
            throw new IllegalArgumentException("Message Must Not Be Empty!");
        }
        this.message = message;
        this.builder.setContentText(message);
        return this;
    }

    public Load message(@NonNull Spanned messageSpanned) {
        if (messageSpanned.length() == 0) {
            throw new IllegalArgumentException("Message Must Not Be Empty!");
        }

        this.messageSpanned = messageSpanned;
        this.builder.setContentText(messageSpanned);
        return this;
    }

    public Load notificationChannelId(final String channelId) {
        this.notificationChannelId = channelId;
        this.builder.setChannelId(channelId);
        return this;
    }

    public Load number(int number) {
        this.builder.setNumber(number);
        return this;
    }

    public Load ongoing(boolean ongoing) {
        this.builder.setOngoing(ongoing);
        return this;
    }

    public Load onlyAlertOnce(boolean onlyAlertOnce) {
        this.builder.setOnlyAlertOnce(onlyAlertOnce);
        return this;
    }

    public Load priority(int priority) {
        if (priority <= 0) {
            throw new IllegalArgumentException("Priority Should Not Be Less Than Or Equal To Zero!");
        }
        this.builder.setPriority(priority);
        return this;
    }

    public Progress progress() {
        notificationShallContainAtLeastThoseSmallIconValid();
        return new Progress(builder, notificationId, tag);
    }

    public Simple simple() {
        notificationShallContainAtLeastThoseSmallIconValid();
        return new Simple(builder, notificationId, tag);
    }

    public Load smallIcon(@DrawableRes int smallIcon) {
        this.smallIcon = smallIcon;
        this.builder.setSmallIcon(smallIcon);
        return this;
    }

    public Load sound(@NonNull Uri sound) {
        this.builder.setSound(sound);
        return this;
    }

    public Load tag(@NonNull String tag) {
        this.tag = tag;
        return this;
    }

    @SuppressLint("ResourceType")
    public Load ticker(@StringRes int ticker) {
        if (ticker <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        this.builder.setTicker(MolvixNotification.mSingleton.mContext.getResources().getString(ticker));

        return this;
    }

    public Load ticker(String ticker) {
        if (ticker == null) {
            throw new IllegalStateException("Ticker Must Not Be Null!");



        }

        if (ticker.trim().length() == 0) {
            throw new IllegalArgumentException("Ticker Must Not Be Empty!");

        }

        this.builder.setTicker(ticker);

        return this;
    }

    @SuppressLint("ResourceType")
    public Load title(@StringRes int title) {
        if (title <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        this.title = MolvixNotification.mSingleton.mContext.getResources().getString(title);
        this.builder.setContentTitle(this.title);
        return this;
    }

    public Load title(String title) {
        if (title == null) {
            throw new IllegalStateException("Title Must Not Be Null!");
        }

        if (title.trim().length() == 0) {
            throw new IllegalArgumentException("Title Must Not Be Empty!");

        }

        this.title = title;
        this.builder.setContentTitle(this.title);
        return this;
    }

    public Load vibrate(@NonNull long[] vibrate) {
        for (long aVibrate : vibrate) {
            if (aVibrate <= 0) {
                throw new IllegalArgumentException("Vibrate Time " + aVibrate + " Invalid!");
            }
        }

        this.builder.setVibrate(vibrate);

        return this;
    }

    public Wear wear() {
        notificationShallContainAtLeastThoseSmallIconValid();
        return new Wear(builder, notificationId, tag);
    }

    public Load when(long when) {
        if (when <= 0) {
            throw new IllegalArgumentException("Resource ID Should Not Be Less Than Or Equal To Zero!");
        }

        this.builder.setWhen(when);
        return this;
    }

    private void notificationShallContainAtLeastThoseSmallIconValid() {
        if (smallIcon <= 0) {
            throw new IllegalArgumentException("This is required. Notifications with an invalid icon resource will " +
                    "not be shown.");
        }
    }
}