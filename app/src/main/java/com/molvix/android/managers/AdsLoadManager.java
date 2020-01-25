package com.molvix.android.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AdsLoadManager {

    private static final int NUMBER_OF_ADS = 5;
    private static List<UnifiedNativeAd> nativeAds = new ArrayList<>();
    private static AdsLoadTask adsLoadTask;

    public static void loadAds() {
        if (adsLoadTask != null) {
            adsLoadTask.cancel(true);
            adsLoadTask = null;
        }
        adsLoadTask = new AdsLoadTask();
        adsLoadTask.execute();
    }

    public static List<UnifiedNativeAd> getNativeAds() {
        return nativeAds;
    }

    public static UnifiedNativeAd getAvailableAd() {
        UnifiedNativeAd currentAd = nativeAds.get(0);
        nativeAds.clear();
        return currentAd;
    }

    static class AdsLoadTask extends AsyncTask<Void, Void, Void> {

        private void loadAds(Context context) {
            AdLoader.Builder builder = new AdLoader.Builder(context, context.getString(R.string.native_ad_unit_id));
            builder.forUnifiedNativeAd(unifiedNativeAd -> {
                Log.d("AdsLoadManager", "AdsLoaded");
                nativeAds.add(unifiedNativeAd);
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
                    Log.d("AdsLoadManager", "Error loading ads due to " + errorCode);
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
