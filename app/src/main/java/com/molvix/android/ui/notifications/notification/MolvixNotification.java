package com.molvix.android.ui.notifications.notification;

import android.app.NotificationManager;
import android.content.Context;

public class MolvixNotification {
    public static MolvixNotification mSingleton = null;
    public final Context mContext;
    public boolean shutdown;

    public MolvixNotification(Context context) {
        this.mContext = context;
    }

    public static MolvixNotification with(Context context) {
        if (mSingleton == null) {
            synchronized (MolvixNotification.class) {
                if (mSingleton == null) {
                    mSingleton = new Contractor(context).build();
                }
            }
        }
        return mSingleton;
    }

    public Load load() {
        return new Load();
    }

    public void cancel(int identifier) {
        NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notifyManager.cancel(identifier);
    }

    public void cancel(String tag, int identifier) {
        NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notifyManager.cancel(tag, identifier);
    }

    public void shutdown() {
        if (this == mSingleton) {
            throw new UnsupportedOperationException("Default singleton instance cannot be shutdown.");
        }
        if (shutdown) {
            return;
        }
        shutdown = true;
    }

    private static class Contractor {
        private final Context mContext;

        public Contractor(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            this.mContext = context.getApplicationContext();
        }

        public MolvixNotification build() {
            return new MolvixNotification(mContext);
        }
    }
}
