package com.molvix.android.utils;

import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;

public class NetworkClient {

    public static OkHttpClient getOkHttpClient(boolean retry) {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(retry)
                .build();
    }

    private static OkHttpClient OK_HTTP_CLIENT;

    public static OkHttpClient getIgnoreCertificateOkHttpClient() {
        if (OK_HTTP_CLIENT == null) {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                    .followRedirects(true)
                    .retryOnConnectionFailure(true)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .connectTimeout(15, TimeUnit.SECONDS);
            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            OK_HTTP_CLIENT = builder.build();
        }
        return OK_HTTP_CLIENT;
    }

}
