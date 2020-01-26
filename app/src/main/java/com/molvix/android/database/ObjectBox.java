package com.molvix.android.database;

import android.content.Context;

import com.molvix.android.BuildConfig;
import com.molvix.android.models.MyObjectBox;
import com.molvix.android.utils.MolvixLogger;

import io.objectbox.BoxStore;

public class ObjectBox {

    private static BoxStore boxStore;

    public static void init(Context context) {
        boxStore = MyObjectBox.builder()
                .androidContext(context.getApplicationContext())
                .build();
        if (BuildConfig.DEBUG) {
            MolvixLogger.d(ObjectBox.class.getSimpleName(), String.format("Using ObjectBox %s (%s)",
                    BoxStore.getVersion(), BoxStore.getVersionNative()));
        }
    }

    public static BoxStore get() {
        return boxStore;
    }

}
