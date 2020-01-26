package com.molvix.android.managers;

import android.util.Log;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixLogger;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

import okhttp3.OkHttpClient;

public class FileDownloadManager {

    public static void downloadEpisode(Episode episode, boolean resume) {
        Log.d(ContentManager.class.getSimpleName(), "Download about to begin for " + episode.getSeason().getMovie().getMovieName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getEpisodeName());
        AppPrefs.addToInProgressDownloads(episode);
        String episodeId = episode.getEpisodeId();
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String movieName = WordUtils.capitalize(movie.getMovieName());
        String movieDescription = movie.getMovieDescription();
        String seasonName = WordUtils.capitalize(season.getSeasonName());
        String seasonId = season.getSeasonId();
        int downloadQuality = episode.getEpisodeQuality();
        String downloadUrl;
        if (downloadQuality == AppConstants.HIGH_QUALITY) {
            downloadUrl = episode.getHighQualityDownloadLink();
        } else if (downloadQuality == AppConstants.STANDARD_QUALITY) {
            downloadUrl = episode.getStandardQualityDownloadLink();
        } else {
            downloadUrl = episode.getLowQualityDownloadLink();
        }
        String fileExtension = StringUtils.substringAfterLast(downloadUrl, ".");
        String fileName = episode.getEpisodeName() + "." + fileExtension;
        String dirPath = FileUtils.getFilePath(fileName, movieName, seasonName).getPath();
        Request downloadRequest = new Request(downloadUrl, dirPath);
        downloadRequest.setPriority(Priority.HIGH);
        downloadRequest.setNetworkType(NetworkType.ALL);
        Fetch fetch = getFetch();
        if (resume) {
            fetch.resume(getDownloadKeyFromEpisode(episode));
        } else {
            fetch.enqueue(downloadRequest, result -> {
                //Request was successfully enqueued for download.
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is has being enqueued for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                AppPrefs.saveDownloadId(getDownloadKey(episode), result.getId());
            }, error -> {
                //An error occurred enqueuing the request.
                Log.d(ContentManager.class.getSimpleName(), "An error occurred while queueing up file for download.Error is " + error.getThrowable());
                deleteDirPath(dirPath);
            });
        }
        fetch.addListener(new FetchListener() {
            @Override
            public void onAdded(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is added for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                AppPrefs.saveDownloadId(getDownloadKey(episode), download.getId());
            }

            @Override
            public void onQueued(@NotNull Download download, boolean b) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is now onQueue for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                AppPrefs.saveDownloadId(getDownloadKey(episode), download.getId());
            }

            @Override
            public void onWaitingNetwork(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is waiting for network for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());

            }

            @Override
            public void onCompleted(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is completed for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                finalizeDownload(episode, movie, episodeId, movieName, movieDescription, seasonId, seasonName);
                fetch.removeListener(this);
                fetch.close();
            }

            @Override
            public void onError(@NotNull Download download, @NotNull com.tonyodev.fetch2.Error error, @Nullable Throwable throwable) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while downloading the episode " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                resetEpisodeDownloadProgress(episode);
                deleteDirPath(dirPath);
                cleanUpTempFiles(movieName, seasonName);
            }

            @Override
            public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is blocked " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            }

            @Override
            public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is started for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            }

            @Override
            public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
                if (downloadRequest.getId() == download.getId()) {
                    updateDownloadProgress(episode, movieName, movieDescription, seasonName, seasonId, download.getDownloaded(), download.getTotal());
                }
            }

            @Override
            public void onPaused(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is paused for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            }

            @Override
            public void onResumed(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is resumed for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());

            }

            @Override
            public void onCancelled(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is cancelled for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
                resetEpisodeDownloadProgress(episode);
                deleteDirPath(dirPath);
                cleanUpTempFiles(movieName, seasonName);
            }

            @Override
            public void onRemoved(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is removed for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            }

            @Override
            public void onDeleted(@NotNull Download download) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Download is deleted for " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + "/" + episode.getSeason().getMovie().getMovieName());
            }

        });

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteDirPath(String dirPath) {
        File dirPathFile = new File(dirPath);
        if (dirPathFile.exists()) {
            dirPathFile.delete();
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

    private static void finalizeDownload(Episode episode, Movie movie, String episodeId, String movieName, String movieDescription, String seasonId, String seasonName) {
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
    }

    private static void updateDownloadProgress(Episode episode, String movieName, String movieDescription, String seasonName, String seasonId, long downloaded, long totalBytes) {
        Log.d(ContentManager.class.getSimpleName(), "Downloading in Progress");
        long progressPercent = downloaded * 100 / totalBytes;
        String progressMessage = FileUtils.getProgressDisplayLine(downloaded, totalBytes);
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episode.getEpisodeId(), episode.getEpisodeName() + "/" + seasonName + "/" + movieName, (int) progressPercent, progressMessage);
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), (int) progressPercent);
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), progressMessage);
    }

    public static void cancelDownload(Episode episode) {
        int downloadKeyFromEpisode = getDownloadKeyFromEpisode(episode);
        Fetch fetch = getFetch();
        fetch.cancel(downloadKeyFromEpisode);
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        String movieName = WordUtils.capitalize(movie.getMovieName());
        String seasonName = WordUtils.capitalize(season.getSeasonName());
        MolvixDB.updateEpisode(episode);
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
        }
        AppPrefs.removeFromInProgressDownloads(episode);
        resetEpisodeDownloadProgress(episode);
        cleanUpTempFiles(movieName, seasonName);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void cleanUpTempFiles(String movieName, String seasonName) {
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

    private static int getDownloadKeyFromEpisode(Episode episode) {
        String downloadIdKey = getDownloadKey(episode);
        return AppPrefs.getDownloadId(downloadIdKey);
    }

    private static String getDownloadKey(Episode episode) {
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
        String fileExtension = StringUtils.substringAfterLast(downloadUrl, ".");
        String fileName = episode.getEpisodeName() + "." + fileExtension;
        String dirPath = FileUtils.getFilePath(movieName, seasonName).getPath();
        return dirPath + fileName;
    }

}
