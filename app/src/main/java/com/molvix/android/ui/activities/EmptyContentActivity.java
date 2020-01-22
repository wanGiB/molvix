package com.molvix.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.database.MolvixDB;

public class EmptyContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String episodeId = intent.getStringExtra(AppConstants.EPISODE_ID);
        if (episodeId != null) {
            DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
            if (downloadableEpisode != null) {
                MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
            }
            Episode episode = MolvixDB.getEpisode(episodeId);
            if (episode != null) {
                episode.setDownloadProgress(-1);
                episode.setProgressDisplayText("");
                MolvixDB.updateEpisode(episode);
            }
            MolvixNotification.with(this).cancel(Math.abs(episodeId.hashCode()));
        }
        finish();
    }

}
