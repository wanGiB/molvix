package com.molvix.android.utils;

import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;

import okhttp3.OkHttpClient;

public class AuthorizationHeaderConnection extends OkHttpDownloadConnection {
    private AuthorizationHeaderConnection(OkHttpClient okHttpClient, String url) {
        super(okHttpClient, url);
    }
    public static class Factory implements DownloadConnection.Factory {
        private OkHttpClient okHttpClient;

        public Factory(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
        }

        @Override
        public DownloadConnection create(String url) {
            return new AuthorizationHeaderConnection(okHttpClient, url);
        }
    }
}
