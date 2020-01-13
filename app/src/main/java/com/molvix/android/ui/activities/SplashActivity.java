package com.molvix.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.ui.services.MoviesDownloadService;

public class SplashActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Intent moviesDownloadIntent = new Intent(this, MoviesDownloadService.class);
        MoviesDownloadService.enqueueWork(this, moviesDownloadIntent);
    }
}
