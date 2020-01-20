package com.molvix.android.ui.notifications.pendingintent;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.molvix.android.ui.notifications.constants.BroadcastActions;
import com.molvix.android.ui.notifications.interfaces.PendingIntentNotification;
import com.molvix.android.ui.notifications.notification.MolvixNotification;

public class DismissPendingIntentActivity implements PendingIntentNotification {
    private final Class<?> mActivity;
    private final Bundle mBundle;
    private final int mIdentifier;

    public DismissPendingIntentActivity(Class<?> activity, Bundle bundle, int identifier) {
        this.mActivity = activity;
        this.mBundle = bundle;
        this.mIdentifier = identifier;
    }

    @Override
    public PendingIntent onSettingPendingIntent() {
        Intent dismissIntentActivity = new Intent(MolvixNotification.mSingleton.mContext, mActivity);
        dismissIntentActivity.setAction(BroadcastActions.ACTION_DISMISS_INTENT);
        dismissIntentActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        dismissIntentActivity.setPackage(MolvixNotification.mSingleton.mContext.getPackageName());
        if (mBundle != null) {
            dismissIntentActivity.putExtras(mBundle);
        }

        return PendingIntent.getActivity(MolvixNotification.mSingleton.mContext, mIdentifier, dismissIntentActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
