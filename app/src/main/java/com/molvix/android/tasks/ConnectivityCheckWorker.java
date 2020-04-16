package com.molvix.android.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.molvix.android.managers.ContentManager;
import com.molvix.android.receivers.ConnectivityChangeReceiver;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.MolvixLogger;

public class ConnectivityCheckWorker extends Worker {

    private boolean running = false;

    public ConnectivityCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!running) {
            if (ConnectivityUtils.isConnected()) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Connection found for Network Manager");
                ConnectivityChangeReceiver.spinAllNetworkRelatedJobs();
            } else {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "No Connection found for Network Manager");
            }
            running = true;
        }
        return Result.success();
    }

    @Override
    public void onStopped() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "WorkManager has Stopped");
        super.onStopped();
    }

}
