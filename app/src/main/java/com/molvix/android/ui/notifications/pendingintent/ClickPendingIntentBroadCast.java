package com.molvix.android.ui.notifications.pendingintent;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.molvix.android.ui.notifications.constants.BroadcastActions;
import com.molvix.android.ui.notifications.interfaces.PendingIntentNotification;
import com.molvix.android.ui.notifications.notification.MolvixNotification;

public class ClickPendingIntentBroadCast implements PendingIntentNotification {
    private final Bundle mBundle;
    private final int mIdentifier;

    public ClickPendingIntentBroadCast(Bundle bundle, int identifier) {
        this.mBundle = bundle;
        this.mIdentifier = identifier;
    }

    @Override
    public PendingIntent onSettingPendingIntent() {
        Intent clickIntentBroadcast = new Intent(BroadcastActions.ACTION_CLICK_INTENT);
        clickIntentBroadcast.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        clickIntentBroadcast.setPackage(MolvixNotification.mSingleton.mContext.getPackageName());
        if (mBundle != null) {
            clickIntentBroadcast.putExtras(mBundle);
        }

        return PendingIntent.getBroadcast(MolvixNotification.mSingleton.mContext, mIdentifier, clickIntentBroadcast,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
