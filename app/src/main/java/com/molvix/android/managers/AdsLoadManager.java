package com.molvix.android.managers;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.molvix.android.BuildConfig;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.eventbuses.AttachLoadedAd;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.MolvixLogger;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.Timer;
import java.util.TimerTask;

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
        //Load ads every 15seconds
        adsLoadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                    loadAds();
                }
            }
        }, 0, 15000);
    }

    static class AdsLoadTask extends AsyncTask<Void, Void, Void> {

        private void loadAds(Context context) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Loading ads");
            AdLoader.Builder builder = new AdLoader.Builder(context, context.getString(R.string.native_ad_unit_id));
            builder.forUnifiedNativeAd(unifiedNativeAd -> {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Native Ads Loaded");
                UnifiedNativeAd existingAd = AppConstants.unifiedNativeAdAtomicReference.get();
                if (existingAd != null) {
                    existingAd.destroy();
                }
                AppConstants.unifiedNativeAdAtomicReference.set(unifiedNativeAd);
                EventBus.getDefault().post(new AttachLoadedAd(true));
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
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Error loading ads due to " + errorCode);
                }
            }).build();
            AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            if (StringUtils.isNotEmpty(AppConstants.TEST_DEVICE_ID) && BuildConfig.DEBUG) {
                adRequestBuilder.addTestDevice(AppConstants.TEST_DEVICE_ID);
            }
            adLoader.loadAds(adRequestBuilder.build(), NUMBER_OF_ADS);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            loadAds(ApplicationLoader.getInstance());
            return null;
        }

    }

}
