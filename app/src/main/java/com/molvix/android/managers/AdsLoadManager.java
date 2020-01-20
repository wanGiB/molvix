package com.molvix.android.managers;

import android.content.Context;
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
import com.molvix.android.eventbuses.LoadAds;
import com.molvix.android.preferences.AppPrefs;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class AdsLoadManager {

    private static final int NUMBER_OF_ADS = 5;
    public static List<UnifiedNativeAd> nativeAds = new ArrayList<>();

    public static void loadAds(Context context) {
        AdLoader.Builder builder = new AdLoader.Builder(context, context.getString(R.string.native_ad_unit_id));
        builder.forUnifiedNativeAd(unifiedNativeAd -> {
            Log.d("AdsLoadManager", "AdsLoaded");
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            destroyAds();
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

    public static void destroyAds() {
        if (!nativeAds.isEmpty()) {
            for (UnifiedNativeAd unifiedNativeAd : nativeAds) {
                unifiedNativeAd.destroy();
            }
            nativeAds.clear();
        }
        AdsLoadManager.setAdConsumed(false);
        EventBus.getDefault().post(new LoadAds(true));
    }

    public static boolean adConsumed() {
        return AppPrefs.isAdAlreadyConsumed();
    }

    public static void setAdConsumed(boolean value) {
        AppPrefs.setAdConsumed(value);
    }
}
