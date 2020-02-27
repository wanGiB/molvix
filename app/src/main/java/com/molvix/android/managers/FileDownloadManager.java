package com.molvix.android.managers;

import android.util.Pair;

import com.huxq17.download.Pump;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadListener;
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
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class FileDownloadManager {

    public static void downloadEpisode(Episode episode) {
        Pair<String, String> downloadUrlAndDirPathPair = getDownloadUrlAndDirPathFrom(episode);
        String downloadUrl = downloadUrlAndDirPathPair.first;
        String filePath = downloadUrlAndDirPathPair.second;
        if (downloadUrl != null && filePath != null) {
            String downloadId = generateDownloadIdFromEpisode(episode);
            Pump.newRequest(downloadUrl, filePath)
                    .setId(downloadId)
                    .setDownloadTaskExecutor(ApplicationLoader.getVideoDownloadDispatcher())
                    .forceReDownload(false)
                    .listener(new DownloadListener() {

                        @Override
                        public void onProgress(int progress) {
                            handleDownloadProgress(getDownloadInfo());
                        }

                        @Override
                        public void onFailed() {
                            handleDownloadError(getDownloadInfo());
                        }

                        @Override
                        public void onSuccess() {
                            handleCompletedDownload(getDownloadInfo());
                        }

                    })
                    .submit();
            AppPrefs.mapEpisodeIdToDownloadId(episode.getEpisodeId(), episode.getEpisodeId().hashCode());
        } else {
            if (downloadUrl == null) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download url is null");
            }
            if (filePath == null) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "FilePath is null");
            }
            AppPrefs.removeFromInProgressDownloads(episode);
            EpisodesManager.popDownloadableEpisode(episode);
            EventBus.getDefault().post(new EpisodeDownloadErrorException(episode));
        }
    }

    private static void tryShutdownPump() {
        Set<String> inProgressDownloads = AppPrefs.getInProgressDownloads();
        if (inProgressDownloads.isEmpty()) {
            Pump.shutdown();
        }
    }

    private static void handleCompletedDownload(@NotNull DownloadInfo download) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(Integer.parseInt(download.getId()));
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is completed for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            finalizeDownload(episode);
            AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
            LetterBoxManager.stripOutLetterBox(episode);
        }
    }

    private static void handleDownloadError(@NotNull DownloadInfo download) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(Integer.parseInt(download.getId()));
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
            MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while downloading " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName() + "");
            EventBus.getDefault().post(new EpisodeDownloadErrorException(episode));
            resetEpisodeDownloadProgress(episode);
            AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
        }
    }

    private static void handleDownloadProgress(@NotNull DownloadInfo downloadInfo) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(Integer.parseInt(downloadInfo.getId()));
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            long completedSize = downloadInfo.getCompletedSize();
            long totalSize = downloadInfo.getContentLength();
            updateDownloadProgress(episode, completedSize, totalSize);
        }
    }

    public static void cancelDownload(Episode episode) {
        String downloadKeyFromEpisode = getDownloadIdFromEpisode(episode);
        Pump.stop(downloadKeyFromEpisode);
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
        }
        AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + episode.getEpisodeId().hashCode());
        AppPrefs.removeFromInProgressDownloads(episode);
        resetEpisodeDownloadProgress(episode);
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Episode " + episode.getEpisodeName() + " cancelled");
    }

    private static Pair<String, String> getDownloadUrlAndDirPathFrom(Episode episode) {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Download about to begin for " + episode.getSeason().getMovie().getMovieName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getEpisodeName());
        AppPrefs.addToInProgressDownloads(episode);
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

    private static void resetEpisodeDownloadProgress(Episode episode) {
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), "");
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
    }

    private static void finalizeDownload(Episode episode) {
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String movieName = WordUtils.capitalize(movie.getMovieName());
        String seasonName = season.getSeasonName();
        String seasonId = season.getSeasonId();
        String movieDescription = movie.getMovieDescription();
        String episodeId = episode.getEpisodeId();
        resetEpisodeDownloadProgress(episode);
        movie.setSeenByUser(true);
        movie.setRecommendedToUser(true);
        MolvixDB.updateMovie(movie);
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
        }
        MovieTracker.recordEpisodeAsDownloaded(episode);
        AppPrefs.removeFromInProgressDownloads(episode);
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episodeId, movieName + "/" + seasonName + "/" + episode.getEpisodeName(), 100, "");
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Episode " + episode.getEpisodeName() + " completed");
    }

    private static void updateDownloadProgress(Episode episode, long downloaded, long totalBytes) {
        try {
            Season season = episode.getSeason();
            Movie movie = season.getMovie();
            String movieName = WordUtils.capitalize(movie.getMovieName());
            String movieDescription = movie.getMovieDescription();
            String seasonId = season.getSeasonId();
            String seasonName = season.getSeasonName();
            long progressPercent = downloaded * 100 / totalBytes;
            String progressMessage = FileUtils.getProgressDisplayLine(downloaded, totalBytes);
            MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episode.getEpisodeId(), episode.getEpisodeName() + "/" + seasonName + "/" + movieName, (int) progressPercent, progressMessage);
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Download in Progress for " + episode.getEpisodeName() + " Progress=" + progressMessage);
            AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), (int) progressPercent);
            AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), progressMessage);
        } catch (Exception e) {
            backwardCompatibilityCleanUp(episode);
        }
    }

    private static void backwardCompatibilityCleanUp(Episode episode) {
        AppPrefs.removeFromInProgressDownloads(episode);
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), "");
        EpisodesManager.popDownloadableEpisode(episode);
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        Pump.stop(episode.getEpisodeId());
        tryShutdownPump();
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Episode " + episode.getEpisodeName() + " backward scrapped");
    }

    private static String getDownloadIdFromEpisode(Episode episode) {
        return String.valueOf(AppPrefs.getDownloadIdFromEpisodeId(episode.getEpisodeId()));
    }

    private static String generateDownloadIdFromEpisode(Episode episode) {
        return String.valueOf(episode.getEpisodeId().hashCode());
    }

}