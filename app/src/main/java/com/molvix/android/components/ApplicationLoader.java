package com.molvix.android.components;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.molvix.android.R;
import com.molvix.android.database.MolvixDB;
import com.raizlabs.android.dbflow.config.DatabaseConfig;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowLog;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;

public class ApplicationLoader extends MultiDexApplication {

    @SuppressLint("StaticFieldLeak")
    private static Context _INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        initPRDownloadManager();
        initAdMob();
        initContext();
        MultiDex.install(getBaseContext());
        setupDatabase();
    }

    private void initPRDownloadManager() {
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .build();
        PRDownloader.initialize(this, config);
    }

    private void initAdMob() {
        MobileAds.initialize(this, getString(R.string.admob_app_id));
    }

    /***
     * Sets up the Local Database. DBFlow is used here
     * ***/
    private void setupDatabase() {
        FlowManager.init(new FlowConfig.Builder(this)
                .addDatabaseConfig(new DatabaseConfig.Builder(MolvixDB.class)
                        .modelNotifier(DirectModelNotifier.get())
                        .build()).build());
        FlowLog.setMinimumLoggingLevel(FlowLog.Level.V); // set to verbose logging
    }

    private void initContext() {
        if (_INSTANCE == null) {
            _INSTANCE = this;
        }
    }

    public static Context getInstance() {
        return _INSTANCE;
    }

}
