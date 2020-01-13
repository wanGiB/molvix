package com.molvix.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.molvix.android.components.ApplicationLoader;

public class ConnectivityUtils {
    public static boolean isDeviceConnectedToTheInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ApplicationLoader.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
