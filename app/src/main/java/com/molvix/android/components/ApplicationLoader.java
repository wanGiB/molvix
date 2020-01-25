package com.molvix.android.components;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.ads.MobileAds;
import com.molvix.android.R;
import com.molvix.android.database.ObjectBox;

public class ApplicationLoader extends MultiDexApplication {

    @SuppressLint("StaticFieldLeak")
    private static Context _INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(getBaseContext());
        initContext();
        initDataBase();
        initPRDownloadManager();
        initAdMob();
    }

    private void initDataBase() {
        ObjectBox.init(this);
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

    private void initContext() {
        if (_INSTANCE == null) {
            _INSTANCE = this;
        }
    }

    public static Context getInstance() {
        return _INSTANCE;
    }

}
