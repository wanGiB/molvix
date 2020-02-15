package com.molvix.android.managers;

import android.util.Pair;

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
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;

public class FileDownloadManager {
    private static Fetch fetch;
    private static FetchListener fetchListener = new FetchListener() {

        @Override
        public void onAdded(@NotNull Download download) {

        }

        @Override
        public void onQueued(@NotNull Download download, boolean b) {
        }

        @Override
        public void onWaitingNetwork(@NotNull Download download) {

        }

        @Override
        public void onCompleted(@NotNull Download download) {
            String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
            if (episodeId != null) {
                Episode episode = MolvixDB.getEpisode(episodeId);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is completed for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                finalizeDownload(episode);
                AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
            }
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
            if (episodeId != null) {
                Episode episode = MolvixDB.getEpisode(episodeId);
                MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
                MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while downloading " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName() + " Error cause = " + error.getThrowable());
                EventBus.getDefault().post(new EpisodeDownloadErrorException(episode, error));
                Pair<String, String> downloadUrlAndDirPath = getDownloadUrlAndDirPathFrom(episode);
                deleteDirPath(downloadUrlAndDirPath.second);
                resetEpisodeDownloadProgress(episode);
                cleanUpTempFiles(episode);
                AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
            }
        }

        @Override
        public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {
        }

        @Override
        public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {

        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
            if (episodeId != null) {
                Episode episode = MolvixDB.getEpisode(episodeId);
                updateDownloadProgress(episode, download.getDownloaded(), download.getTotal());
                long savedFileLength = AppPrefs.getEstimatedFileLengthForEpisode(episodeId);
                long totalLength = download.getTotal();
                if (savedFileLength != totalLength) {
                    AppPrefs.saveEstimatedFileLengthForEpisode(episodeId, totalLength);
                }
            }
        }

        @Override
        public void onPaused(@NotNull Download download) {
            String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
            if (episodeId != null) {
                AppPrefs.setPaused(episodeId, true);
            }
        }

        @Override
        public void onResumed(@NotNull Download download) {
            String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
            if (episodeId != null) {
                AppPrefs.setPaused(episodeId, false);
            }
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
            if (episodeId != null) {
                Episode episode = MolvixDB.getEpisode(episodeId);
                MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
                Pair<String, String> downloadUrlAndDirPair = getDownloadUrlAndDirPathFrom(episode);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is cancelled for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                deleteDirPath(downloadUrlAndDirPair.second);
                cleanUpTempFiles(episode);
                AppPrefs.setPaused(episodeId, false);
                AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
                resetEpisodeDownloadProgress(episode);
                AppPrefs.removeFromInProgressDownloads(episode);
            }
        }

        @Override
        public void onRemoved(@NotNull Download download) {
        }

        @Override
        public void onDeleted(@NotNull Download download) {

        }

    };

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

    public static void downloadEpisode(Episode episode) {
        Pair<String, String> downloadUrlAndDirPathPair = getDownloadUrlAndDirPathFrom(episode);
        boolean isPaused = AppPrefs.isPaused(episode.getEpisodeId());
        if (fetch == null) {
            fetch = getFetch();
        }
        if (isPaused) {
            fetch.resume(getDownloadIdFromEpisode(episode));
        } else {
            Request downloadRequest = new Request(downloadUrlAndDirPathPair.first, downloadUrlAndDirPathPair.second);
            downloadRequest.setPriority(Priority.HIGH);
            downloadRequest.setNetworkType(NetworkType.ALL);
            fetch.enqueue(downloadRequest, result -> {
                //Request was successfully enqueued for download.
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download has being enqueued for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                AppPrefs.mapEpisodeIdToDownloadId(episode.getEpisodeId(), result.getId());
            }, error -> {
                //An error occurred enqueuing the request.
                MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while queueing up file for download.Error is " + error.getThrowable());
                deleteDirPath(downloadUrlAndDirPathPair.second);
            });
        }
        attachFetchListener(fetch);
    }

    private static void attachFetchListener(Fetch fetch) {
        fetch.addListener(fetchListener);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteDirPath(String dirPath) {
        File dirPathFile = new File(dirPath);
        if (dirPathFile.exists()) {
            if (!FileUtils.isAtLeast10mB(dirPathFile)) {
                dirPathFile.delete();
            }
        }
    }

    private static Fetch getFetch() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(ApplicationLoader.getInstance())
                .setDownloadConcurrentLimit(20)
                .setHttpDownloader(new OkHttpDownloader(okHttpClient))
                .build();
        return Fetch.Impl.getInstance(fetchConfiguration);
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
        AppPrefs.setPaused(episodeId, false);
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episodeId, movieName + "/" + seasonName + "/" + episode.getEpisodeName(), 100, "");
    }

    private static void updateDownloadProgress(Episode episode, long downloaded, long totalBytes) {
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String movieName = WordUtils.capitalize(movie.getMovieName());
        String movieDescription = movie.getMovieDescription();
        String seasonId = season.getSeasonId();
        String seasonName = season.getSeasonName();
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Downloading in Progress");
        long progressPercent = downloaded * 100 / totalBytes;
        String progressMessage = FileUtils.getProgressDisplayLine(downloaded, totalBytes);
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episode.getEpisodeId(), episode.getEpisodeName() + "/" + seasonName + "/" + movieName, (int) progressPercent, progressMessage);
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), (int) progressPercent);
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), progressMessage);
    }

    public static void cancelDownload(Episode episode) {
        int downloadKeyFromEpisode = getDownloadIdFromEpisode(episode);
        getFetch().cancel(downloadKeyFromEpisode);
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
        }
        AppPrefs.removeFromInProgressDownloads(episode);
        resetEpisodeDownloadProgress(episode);
        cleanUpTempFiles(episode);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void cleanUpTempFiles(Episode episode) {
        String seasonName = episode.getSeason().getSeasonName();
        String movieName = WordUtils.capitalize(episode.getSeason().getMovie().getMovieName());
        File seasonDir = FileUtils.getFilePath(movieName, seasonName);
        if (seasonDir.exists()) {
            File[] files = seasonDir.listFiles();
            if (files != null && files.length > 0) {
                for (File episodeFile : files) {
                    String entryName = episodeFile.getName();
                    String mimeTypeOfName = FileUtils.getMimeType(entryName);
                    if (mimeTypeOfName == null || !mimeTypeOfName.toLowerCase().contains("video")) {
                        episodeFile.delete();
                    }
                }
            }
        }
    }

    private static int getDownloadIdFromEpisode(Episode episode) {
        return AppPrefs.getDownloadIdFromEpisodeId(episode.getEpisodeId());
    }

    public static void pauseDownload(Episode episode) {
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        int downloadKeyFromEpisode = getDownloadIdFromEpisode(episode);
        getFetch().pause(downloadKeyFromEpisode);
        AppPrefs.setPaused(episode.getEpisodeId(), true);
    }

}
