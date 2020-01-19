package com.molvix.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.molvix.android.managers.MovieTracker;

public class MovieRecommendationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MovieTracker.recommendUnWatchedMoviesToUser();
    }
}
