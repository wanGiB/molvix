package com.molvix.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.molvix.android.managers.MovieTracker;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;

public class MovieRecommendationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppPrefs.canDailyMoviesBeRecommended()&& ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            MovieTracker.recommendUnWatchedMoviesToUser();
        }
    }
}
