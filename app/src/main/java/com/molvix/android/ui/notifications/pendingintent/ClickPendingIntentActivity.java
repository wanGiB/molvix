package com.molvix.android.ui.notifications.pendingintent;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.molvix.android.ui.notifications.constants.BroadcastActions;
import com.molvix.android.ui.notifications.interfaces.PendingIntentNotification;
import com.molvix.android.ui.notifications.notification.MolvixNotification;

public class ClickPendingIntentActivity implements PendingIntentNotification {
    private final Class<?> mActivity;
    private final Bundle mBundle;
    private final int mIdentifier;

    public ClickPendingIntentActivity(Class<?> activity, Bundle bundle, int identifier) {
        this.mActivity = activity;
        this.mBundle = bundle;
        this.mIdentifier = identifier;
    }

    @Override
    public PendingIntent onSettingPendingIntent() {
        Intent clickIntentActivity = new Intent(MolvixNotification.mSingleton.mContext, mActivity);
        clickIntentActivity.setAction(BroadcastActions.ACTION_CLICK_INTENT);
        clickIntentActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        clickIntentActivity.setPackage(MolvixNotification.mSingleton.mContext.getPackageName());

        if (mBundle != null) {
            clickIntentActivity.putExtras(mBundle);
        }
        return PendingIntent.getActivity(MolvixNotification.mSingleton.mContext, mIdentifier, clickIntentActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
