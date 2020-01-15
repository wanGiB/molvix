package com.molvix.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.reflect.TypeToken;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.GsonUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class AppPrefs {

    private static SharedPreferences appSharedPreferences;

    private static SharedPreferences getAppPreferences() {
        if (appSharedPreferences == null) {
            appSharedPreferences = ApplicationLoader.getInstance()
                    .getSharedPreferences(AppConstants.APP_PREFS_NAME, Context.MODE_PRIVATE);
        }
        return appSharedPreferences;
    }

    @SuppressLint("ApplySharedPref")
    public static void persistDownloadOptionsForEpisodeLink(List<String> downloadOptions, String episodeLink) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        String downloadOptionsStringMarshall = GsonUtils.getGSON().toJson(downloadOptions, listType);
        getAppPreferences().edit().putString(CryptoUtils.getSha256Digest(episodeLink), downloadOptionsStringMarshall).commit();
    }

    public static List<String> getDownloadOptionsForEpisodeLink(String episodeLink) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> downloadOptionsResult = new ArrayList<>();
        String episodeLinkHash = CryptoUtils.getSha256Digest(episodeLink);
        String result = getAppPreferences().getString(episodeLink, null);
        if (result != null) {
            downloadOptionsResult = GsonUtils.getGSON().fromJson(result, listType);
        }
        return downloadOptionsResult;
    }

    @SuppressLint("ApplySharedPref")
    public static void persistDownloadableEpisodeTargetLink(String targetLink, String episodeLink) {
        getAppPreferences().edit().putString(CryptoUtils.getSha256Digest(episodeLink), targetLink).commit();
    }

    public static String getTargetLinkOfEpisode(String episodeLink) {
        return getAppPreferences().getString(CryptoUtils.getSha256Digest(episodeLink), null);
    }

}
