package com.molvix.android.managers;

import com.molvix.android.eventbuses.CheckForDownloadableEpisodes;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.database.MolvixDB;

import org.greenrobot.eventbus.EventBus;

public class EpisodesManager {

    public static void enqueDownloadableEpisode(Episode episode) {
        DownloadableEpisode existingDownloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (existingDownloadableEpisode != null) {
            return;
        }
        DownloadableEpisode newDownloadableEpisode = new DownloadableEpisode();
        newDownloadableEpisode.setDownloadableEpisodeId(episode.getEpisodeId());
        newDownloadableEpisode.episode.setTarget(episode);
        MolvixDB.createNewDownloadableEpisode(newDownloadableEpisode);
        EventBus.getDefault().post(new CheckForDownloadableEpisodes());
    }

    public static void popDownloadableEpisode(Episode episode) {
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
            unLockCaptchaSolver();
        }
        EventBus.getDefault().post(new CheckForDownloadableEpisodes());
    }

    public static void lockCaptchaSolver(String episodeId) {
        AppPrefs.lockCaptchaSolver(episodeId);
    }

    public static void unLockCaptchaSolver() {
        AppPrefs.unLockCaptchaSolver();
    }

    public static boolean isCaptchaSolvable() {
        return AppPrefs.isCaptchaSolvable();
    }

}
