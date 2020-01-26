package com.molvix.android.utils;

import android.util.Log;

import com.molvix.android.BuildConfig;

public class MolvixLogger {

    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }
}
