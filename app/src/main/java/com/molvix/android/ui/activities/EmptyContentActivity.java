package com.molvix.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;

import io.realm.ImportFlag;
import io.realm.Realm;
import ir.zadak.zadaknotify.notification.ZadakNotification;

public class EmptyContentActivity extends AppCompatActivity {

    private Realm realm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        Intent intent = getIntent();
        String episodeId = intent.getStringExtra(AppConstants.EPISODE_ID);
        if (episodeId != null) {
            realm.executeTransaction(r -> {
                DownloadableEpisode downloadableEpisode = r.where(DownloadableEpisode.class).equalTo(AppConstants.EPISODE_ID, episodeId).findFirst();
                if (downloadableEpisode != null) {
                    downloadableEpisode.deleteFromRealm();
                }
                Episode episode = r.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episodeId).findFirst();
                if (episode != null) {
                    episode.setDownloadProgress(-1);
                    episode.setProgressDisplayText("");
                    r.copyToRealmOrUpdate(episode, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                }
            });
            ZadakNotification.with(this).cancel(Math.abs(episodeId.hashCode()));
        }
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        realm.close();
    }

}
