package com.molvix.android.managers;

import android.util.Pair;

import com.huxq17.download.Pump;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.EpisodeDownloadErrorException;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixLogger;

import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;

public class FileDownloadManager {

    public static void downloadEpisode(Episode episode) {
        Pair<String, String> downloadUrlAndDirPathPair = getDownloadUrlAndDirPathFrom(episode);
        String downloadUrl = downloadUrlAndDirPathPair.first;
        String filePath = downloadUrlAndDirPathPair.second;
        if (downloadUrl != null && filePath != null) {
            String downloadId = getDownloadIdFromEpisode(episode);
            Pump.newRequest(downloadUrl, filePath)
                    .setId(downloadId)
                    .forceReDownload(false)
                    .submit();
            tryEnableDownloadListener();
        } else {
            cleanUpDownloadWithInvalidParameters(episode, downloadUrl, filePath);
        }
    }

    private static void cleanUpDownloadWithInvalidParameters(Episode episode, String downloadUrl, String filePath) {
        if (downloadUrl == null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Download url is null for " + EpisodesManager.getEpisodeFullName(episode));
        }
        if (filePath == null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "FilePath is null for " + EpisodesManager.getEpisodeFullName(episode));
        }
        AppPrefs.removeFromInProgressDownloads(episode);
        EpisodesManager.popDownloadableEpisode(episode);
        EventBus.getDefault().post(new EpisodeDownloadErrorException(episode));
    }

    private static void tryEnableDownloadListener() {
        if (!ApplicationLoader.globalDownloadListener.isEnable()) {
            ApplicationLoader.globalDownloadListener.enable();
        }
    }

    public static void cancelDownload(Episode episode) {
        String downloadKeyFromEpisode = getDownloadIdFromEpisode(episode);
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
        }
        AppPrefs.removeFromInProgressDownloads(episode);
        ApplicationLoader.resetEpisodeDownloadProgress(episode);
        Pump.stop(downloadKeyFromEpisode);
        MolvixLogger.d(ContentManager.class.getSimpleName(), EpisodesManager.getEpisodeFullName(episode) + " was cancelled");
    }

    private static Pair<String, String> getDownloadUrlAndDirPathFrom(Episode episode) {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Preparing download of " + EpisodesManager.getEpisodeFullName(episode));
        AppPrefs.addToInProgressDownloads(episode);
        MolvixDB.deepLinkEpisodeProperly(episode);
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String movieName = WordUtils.capitalize(movie.getMovieName());
        String seasonName = WordUtils.capitalize(season.getSeasonName());
        int downloadQuality = episode.getEpisodeQuality();
        String downloadUrl;
        if (downloadQuality == AppConstants.HIGH_QUALITY) {
            downloadUrl = episode.getHighQualityDownloadLink();
        } else if (downloadQuality == AppConstants.STANDARD_QUALITY) {
            downloadUrl = episode.getStandardQualityDownloadLink();
        } else {
            downloadUrl = episode.getLowQualityDownloadLink();
        }
        String fileName = episode.getEpisodeName() + ".mp4";
        String dirPath = FileUtils.getFilePath(fileName, movieName, seasonName).getPath();
        return new Pair<>(downloadUrl, dirPath);
    }

    private static String getDownloadIdFromEpisode(Episode episode) {
        return episode.getEpisodeId();
    }

}