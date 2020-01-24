package com.molvix.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.models.Episode;

public class EmptyContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String episodeId = intent.getStringExtra(AppConstants.EPISODE_ID);
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            if (episode != null) {
                FileDownloadManager.cancelDownload(episode);
            }
        }
        finish();
    }

}
