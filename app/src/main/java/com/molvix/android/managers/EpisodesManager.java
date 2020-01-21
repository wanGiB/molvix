package com.molvix.android.managers;

import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.MolvixDB;

public class EpisodesManager {

    public static void enqueEpisodeForDownload(Episode episode) {
        DownloadableEpisode existingDownloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (existingDownloadableEpisode != null) {
            return;
        }
        DownloadableEpisode newDownloadableEpisode = new DownloadableEpisode();
        newDownloadableEpisode.setEpisodeId(episode.getEpisodeId());
        newDownloadableEpisode.setDownloadableEpisode(episode);
        MolvixDB.createNewDownloadableEpisode(newDownloadableEpisode);
    }

    public static void popEpisode(Episode episode) {
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
            unLockCaptureSolver(episode.getEpisodeId());
        }
    }

    public static void lockCaptchaSolver(String episodeId) {
        AppPrefs.lockCaptchaSolver(episodeId);
    }

    public static void unLockCaptureSolver(String episodeId) {
        AppPrefs.unLockCaptchaSolver(episodeId);
    }

    public static boolean isCaptchaSolvable() {
        return AppPrefs.isCaptchaSolvable();
    }

}
