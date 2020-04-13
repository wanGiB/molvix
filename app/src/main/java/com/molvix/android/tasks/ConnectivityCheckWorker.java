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

    public ConnectivityCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "ConnectivityCheckWorker is currently running");
        if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            ConnectivityChangeReceiver.performAllPossibleNetworkRelatedJobs();
        }
        return Result.success();
    }

    @Override
    public void onStopped() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "ConnectivityCheckWorker is Stopped");
        super.onStopped();
    }

}
