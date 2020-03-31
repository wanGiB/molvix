package com.molvix.android.managers;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.MolvixLogger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AdsLoadManager {

    private static final int NUMBER_OF_ADS = 5;
    private static AdsLoadTask adsLoadTask;
    private static Timer adsLoadTimer = new Timer();

    private static void loadAds() {
        if (adsLoadTask != null) {
            adsLoadTask.cancel(true);
            adsLoadTask = null;
        }
        adsLoadTask = new AdsLoadTask();
        adsLoadTask.execute();
    }

    public static void spin() {
        try {
            //Load ads every 20seconds
            adsLoadTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long lastAdLoadTime = AppPrefs.getLastAdLoadTime();
                    long currentTime = System.currentTimeMillis();
                    long timeDiff = Math.abs(currentTime - lastAdLoadTime);
                    long timeDiffInSecs = TimeUnit.MILLISECONDS.toSeconds(timeDiff);
                    if (ConnectivityUtils.isDeviceConnectedToTheInternet()
                            && timeDiffInSecs >= 60) {
                        loadAds();
                    }
                }
            }, 0, 20000);
        } catch (Exception ex) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "An error has occurred while spinning ads load manager.Error message = " + ex.getMessage());
        }
    }

    public static void destroy() {
        adsLoadTimer.cancel();
        adsLoadTimer.purge();
    }

    static class AdsLoadTask extends AsyncTask<Void, Void, Void> {

        private void loadAds(Context context) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Loading ads");
            AdLoader.Builder builder = new AdLoader.Builder(context, context.getString(R.string.native_release_ad_unit_id));
            builder.forUnifiedNativeAd(unifiedNativeAd -> {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Native Ads Loaded");
                UnifiedNativeAd existingAd = AppConstants.unifiedNativeAdAtomicReference.get();
                if (existingAd != null) {
                    existingAd.destroy();
                }
                AppConstants.unifiedNativeAdAtomicReference.set(unifiedNativeAd);
                AppPrefs.persistLastAdLoadTime(System.currentTimeMillis());
            });
            VideoOptions videoOptions = new VideoOptions.Builder()
                    .setStartMuted(true)
                    .build();
            NativeAdOptions adOptions = new NativeAdOptions.Builder()
                    .setVideoOptions(videoOptions)
                    .build();
            builder.withNativeAdOptions(adOptions);
            AdLoader adLoader = builder.withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Error loading native ads due to " + errorCode);
                    AppPrefs.persistLastAdLoadTime(System.currentTimeMillis());
                }
            }).build();
            AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            adLoader.loadAds(adRequestBuilder.build(), NUMBER_OF_ADS);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            loadAds(ApplicationLoader.getInstance());
            return null;
        }

    }

}
