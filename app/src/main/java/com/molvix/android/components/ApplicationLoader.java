package com.molvix.android.components;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.ads.MobileAds;
import com.molvix.android.R;

import io.realm.Realm;
import io.realm.RealmConfiguration;

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
     * Sets up the Local Database. Realm is used here
     * ***/
    private void setupDatabase() {
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().name("molvix.realm").build();
        Realm.setDefaultConfiguration(config);
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
