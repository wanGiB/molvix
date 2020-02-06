package com.molvix.android.utils;

import okhttp3.OkHttpClient;

public class NetworkClient {

    public static OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .build();
    }

}
