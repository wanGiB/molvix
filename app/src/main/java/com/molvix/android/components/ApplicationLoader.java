package com.molvix.android.components;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.ads.MobileAds;
import com.molvix.android.R;
import com.molvix.android.receivers.MovieRecommendationReceiver;

import java.util.Calendar;

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
        initMovieRecommendation();
    }

    private void initMovieRecommendation() {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.HOUR_OF_DAY, 12);
        mCalendar.set(Calendar.MINUTE, 30);
        mCalendar.set(Calendar.SECOND, 30);
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //Recommend a new movie every 12 Noon!
        Intent movieRecommendationIntent = new Intent(this, MovieRecommendationReceiver.class);
        PendingIntent movieRecommendationPendingIntent = PendingIntent.getBroadcast(this, 100, movieRecommendationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (mAlarmManager != null) {
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, movieRecommendationPendingIntent);
        }
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
