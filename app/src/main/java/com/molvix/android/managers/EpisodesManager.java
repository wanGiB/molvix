package com.molvix.android.managers;

import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.preferences.AppPrefs;

import io.realm.ImportFlag;
import io.realm.Realm;

public class EpisodesManager {

    public static void enqueEpisodeForDownload(Episode episode) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                DownloadableEpisode newDownloadableEpisode = r.createObject(DownloadableEpisode.class, episode.getEpisodeId());
                newDownloadableEpisode.setDownloadableEpisode(episode);
                r.copyToRealmOrUpdate(newDownloadableEpisode, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
            });
        }
    }

    public static void popEpisode(Episode episode) {
        Realm realm = Realm.getDefaultInstance();
        DownloadableEpisode downloadableEpisode = realm.where(DownloadableEpisode.class).equalTo("episodeId", episode.getEpisodeId()).findFirst();
        if (downloadableEpisode != null) {
            realm.executeTransaction(r -> {
                downloadableEpisode.deleteFromRealm();
                unLockCaptureSolver(episode.getEpisodeId());
            });
        }
    }

    public static void lockCaptchaSolver(String episodeId) {
        AppPrefs.lockCaptchaSolver(episodeId);
    }

    private static void unLockCaptureSolver(String episodeId) {
        AppPrefs.unLockCaptchaSolver(episodeId);
    }

    public static boolean isCaptchaSolvable() {
        return AppPrefs.isCaptchaSolvable();
    }

}
