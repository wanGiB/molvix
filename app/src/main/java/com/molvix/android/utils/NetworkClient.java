package com.molvix.android.utils;

import okhttp3.OkHttpClient;

public class NetworkClient {

    public static OkHttpClient getOkHttpClient(boolean retry) {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(retry)
                .build();
    }

}
