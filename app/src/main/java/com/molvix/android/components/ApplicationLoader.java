package com.molvix.android.components;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.ads.MobileAds;
import com.huxq17.download.config.DownloadConfig;
import com.huxq17.download.core.DownloadTaskExecutor;
import com.huxq17.download.core.SimpleDownloadTaskExecutor;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.ObjectBox;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.utils.AuthorizationHeaderConnection;
import com.molvix.android.utils.NetworkClient;

public class ApplicationLoader extends MultiDexApplication {

    @SuppressLint("StaticFieldLeak")
    private static Context _INSTANCE;

    public static DownloadTaskExecutor videoDownloadDispatcher = new SimpleDownloadTaskExecutor() {

        @Override
        public int getMaxDownloadNumber() {
            return AppConstants.MAXIMUM_RUNNABLE_TASK;
        }

        @Override
        public String getName() {
            return "MolvixVideoDownloadDispatcher";
        }

        @Override
        public String getTag() {
            return "video";
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(getBaseContext());
        initContext();
        initDownloadManager();
        initDataBase();
        initAdMob();
        initPresets();
    }

    private void initDownloadManager() {
        DownloadConfig.newBuilder()
                .setMaxRunningTaskNum(AppConstants.MAXIMUM_RUNNABLE_TASK)
                .setMinUsableStorageSpace(getMinUsableStorageSpace())
                .setDownloadConnectionFactory(new AuthorizationHeaderConnection
                        .Factory(NetworkClient.getIgnoreCertificateOkHttpClient()))
                .build();
    }

    private long getMinUsableStorageSpace() {
        return 10 * 1024L * 1024;
    }

    private void initPresets() {
        ContentManager.fetchPresets();
    }

    private void initDataBase() {
        ObjectBox.init(this);
    }

    private void initAdMob() {
        MobileAds.initialize(this, getString(R.string.admob_app_id));
    }

    private void initContext() {
        if (_INSTANCE == null) {
            _INSTANCE = this;
        }
    }

    public static Context getInstance() {
        return _INSTANCE;
    }

    public static DownloadTaskExecutor getVideoDownloadDispatcher() {
        return videoDownloadDispatcher;
    }

}
