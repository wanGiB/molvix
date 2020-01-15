package com.molvix.android.jobs;

import com.molvix.android.models.DownloadableEpisodes;
import com.molvix.android.models.Episode;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.CryptoUtils;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EpisodesResolutionManager {

    public static void enqueEpisodeForDownload(Episode episode) {
        DownloadableEpisodes downloadableEpisodes = SQLite.select()
                .from(DownloadableEpisodes.class)
                .querySingle();
        if (downloadableEpisodes != null) {
            List<Episode> episodeList = downloadableEpisodes.getDownloadableEpisodes();
            if (episodeList == null) {
                episodeList = new ArrayList<>();
            }
            if (!episodeList.contains(episode)) {
                episodeList.add(episode);
            }
            downloadableEpisodes.setDownloadableEpisodes(episodeList);
            downloadableEpisodes.update();
        } else {
            DownloadableEpisodes newDownloadableEpisodes = new DownloadableEpisodes();
            newDownloadableEpisodes.setEpisodesId(CryptoUtils.getSha256Digest(String.valueOf(System.currentTimeMillis() + new Random().nextInt(256))));
            List<Episode> newEpisodeList = new ArrayList<>();
            newEpisodeList.add(episode);
            newDownloadableEpisodes.setDownloadableEpisodes(newEpisodeList);
            newDownloadableEpisodes.save();
        }
    }

    public static void popEpisode(Episode episode) {
        unLockCaptureSolver(episode.getEpisodeId());
        DownloadableEpisodes downloadableEpisodes = SQLite.select()
                .from(DownloadableEpisodes.class)
                .querySingle();
        if (downloadableEpisodes != null) {
            List<Episode> episodeList = downloadableEpisodes.getDownloadableEpisodes();
            if (episodeList != null && !episodeList.isEmpty()) {
                episodeList.remove(episode);
                downloadableEpisodes.setDownloadableEpisodes(episodeList);
                downloadableEpisodes.update();
            }
        }
    }

    public static void lockCaptureSolver(String episodeId) {
        AppPrefs.lockCaptchaSolver(episodeId);
    }

    private static void unLockCaptureSolver(String episodeId) {
        AppPrefs.unLockCaptchaSolver(episodeId);
    }

    public static boolean isCaptchaSolvable() {
        return AppPrefs.isCaptchaSolvable();
    }

}
