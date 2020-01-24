package com.molvix.android.managers;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
import com.downloader.request.DownloadRequest;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;

public class FileDownloadManager {

    public static void startNewEpisodeDownload(Episode episode) {
        String episodeId = episode.getEpisodeId();
        AppPrefs.addToInProgressDownloads(episode);
        String episodeName = episode.getEpisodeName();
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
        String fileExtension = StringUtils.substringAfter(downloadUrl, ".");
        String fileName = episode.getEpisodeName() + "." + fileExtension;
        String dirPath = FileUtils.getFilePath(movieName, seasonName).getPath();
        int downloadId = Math.abs((dirPath + fileName).hashCode());
        DownloadRequest downloadRequest = null;
        if (Status.PAUSED == PRDownloader.getStatus(downloadId)) {
            PRDownloader.resume(downloadId);
        } else {
            downloadRequest = PRDownloader.download(downloadUrl, dirPath, fileName).build();
            downloadRequest = getDownloadRequest(episode, episodeId, episodeName, movieName, movieDescription, seasonName, seasonId, downloadRequest);
        }
        if (downloadRequest != null) {
            downloadRequest.start(new OnDownloadListener() {
                @Override
                public void onDownloadComplete() {
                    finalizeDownload(episode, movie, episodeId, movieName, movieDescription, seasonId, episodeName, seasonName);
                }

                @Override
                public void onError(Error error) {

                }

            });

        }

    }

    private static void finalizeDownload(Episode episode, Movie movie, String episodeId, String movieName, String movieDescription, String seasonId, String episodeName, String seasonName) {
        episode.setDownloadProgress(-1);
        MolvixDB.updateEpisode(episode);
        movie.setSeenByUser(true);
        movie.setRecommendedToUser(true);
        MolvixDB.updateMovie(movie);
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
            MovieTracker.recordEpisodeAsDownloaded(episode);
        }
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episodeId, episodeName + "/" + seasonName + "/" + movieName, 100, "");
    }

    private static DownloadRequest getDownloadRequest(Episode episode, String episodeId, String episodeName, String movieName, String movieDescription, String seasonName, String seasonId, DownloadRequest downloadRequest) {
        return downloadRequest.setOnStartOrResumeListener(() -> {
        }).setOnPauseListener(() -> {
        }).setOnCancelListener(() -> cancelDownload(episode, episodeId, movieName, seasonName)).setOnProgressListener(progress -> updateDownloadProgress(episode, episodeId, episodeName, movieName, movieDescription, seasonName, seasonId, progress));
    }

    private static void updateDownloadProgress(Episode episode, String episodeId, String episodeName, String movieName, String movieDescription, String seasonName, String seasonId, Progress progress) {
        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
        String progressMessage = FileUtils.getProgressDisplayLine(progress.currentBytes, progress.totalBytes);
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episodeId, episodeName + "/" + seasonName + "/" + movieName, (int) progressPercent, progressMessage);
        episode.setDownloadProgress((int) progressPercent);
        episode.setProgressDisplayText(progressMessage);
        MolvixDB.updateEpisode(episode);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void cancelDownload(Episode episode, String episodeId, String movieName, String seasonName) {
        episode.setDownloadProgress(-1);
        MolvixDB.updateEpisode(episode);
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
        if (downloadableEpisode != null) {
            MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
        }
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episodeId.hashCode()));
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

    public static void cancelDownload(int downloadId) {
        if (Status.RUNNING == PRDownloader.getStatus(downloadId)) {
            PRDownloader.cancel(downloadId);
        }
    }

}
