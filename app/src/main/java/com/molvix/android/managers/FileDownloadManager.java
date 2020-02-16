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
import com.molvix.android.utils.NetworkClient;
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

import java.util.List;

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
            handleCompletedDownload(download);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            handleDownloadError(download, error);
        }

        @Override
        public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {
        }

        @Override
        public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {

        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            handleDownloadProgress(download);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            handlePausedDownload(download);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            handleResumedDownload(download);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            handleCancelledDownload(download);
        }

        @Override
        public void onRemoved(@NotNull Download download) {

        }

        @Override
        public void onDeleted(@NotNull Download download) {

        }

    };

    private static void handleCompletedDownload(@NotNull Download download) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is completed for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            finalizeDownload(episode);
            AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
        }
    }

    private static void handleDownloadError(@NotNull Download download, @NotNull Error error) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
            MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while downloading " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName() + " Error cause = " + error.getThrowable());
            EventBus.getDefault().post(new EpisodeDownloadErrorException(episode));
            resetEpisodeDownloadProgress(episode);
            AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
        }
    }

    private static void handleDownloadProgress(@NotNull Download download) {
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

    private static void handlePausedDownload(@NotNull Download download) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
        if (episodeId != null) {
            AppPrefs.setPaused(episodeId, true);
        }
    }

    private static void handleResumedDownload(@NotNull Download download) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
        if (episodeId != null) {
            AppPrefs.setPaused(episodeId, false);
        }
    }

    private static void handleCancelledDownload(@NotNull Download download) {
        String episodeId = AppPrefs.getEpisodeIdFromDownloadId(download.getId());
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is cancelled for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            AppPrefs.setPaused(episodeId, false);
            AppPrefs.removeKey(AppConstants.DOWNLOAD_ID_KEY + download.getId());
            resetEpisodeDownloadProgress(episode);
            AppPrefs.removeFromInProgressDownloads(episode);
        }
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

    public static void downloadEpisode(Episode episode) {
        Pair<String, String> downloadUrlAndDirPathPair = getDownloadUrlAndDirPathFrom(episode);
        String downloadUrl = downloadUrlAndDirPathPair.first;
        String filePath = downloadUrlAndDirPathPair.second;
        if (downloadUrl != null && filePath != null) {
            boolean isPaused = AppPrefs.isPaused(episode.getEpisodeId());
            if (fetch == null) {
                fetch = getFetch();
            }
            if (isPaused) {
                fetch.resume(getDownloadIdFromEpisode(episode));
            } else {
                Request downloadRequest = new Request(downloadUrl, filePath);
                downloadRequest.setPriority(Priority.HIGH);
                downloadRequest.setNetworkType(NetworkType.ALL);
                fetch.enqueue(downloadRequest, result -> {
                    //Request was successfully enqueued for download.
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Download has being enqueued for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                    AppPrefs.mapEpisodeIdToDownloadId(episode.getEpisodeId(), result.getId());
                }, error -> {
                    //An error occurred enqueuing the request.
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while queueing up file for download.Error is " + error.getThrowable());
                });
            }
            attachFetchListener(fetch);
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

    private static void attachFetchListener(Fetch fetch) {
        fetch.addListener(fetchListener);
    }

    private static Fetch getFetch() {
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(ApplicationLoader.getInstance())
                .enableAutoStart(true)
                .enableRetryOnNetworkGain(true)
                .preAllocateFileOnCreation(false)
                .setGlobalNetworkType(NetworkType.ALL)
                .setHasActiveDownloadsCheckInterval(1000)
                .setHttpDownloader(new OkHttpDownloader(NetworkClient.getOkHttpClient(true)))
                .setDownloadConcurrentLimit(20)
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
