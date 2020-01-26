package com.molvix.android.managers;

import android.util.Log;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
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

    public static void downloadEpisode(Episode episode) {
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
        String dirPath = FileUtils.getFilePath(movieName, seasonName).getPath();
        int downloadId = PRDownloader
                .download(downloadUrl, dirPath, fileName)
                .build()
                .setOnProgressListener(progress -> updateDownloadProgress(episode, movieName, movieDescription, seasonName, seasonId, progress))
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        finalizeDownload(episode, movie, episodeId, movieName, movieDescription, seasonId, seasonName);
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(ContentManager.class.getSimpleName(), "An error occurred, Server Error=" + error.getServerErrorMessage() + ", ConnectionException=" + error.getConnectionException() + ", ResponseCode=" + error.getResponseCode() + ",HeaderFields=" + error.getHeaderFields());
                        resetEpisodeDownloadProgress(episode);
                        cleanUpTempFiles(movieName, seasonName);
                    }

                });
        AppPrefs.saveDownloadId(getDownloadKey(episode), downloadId);
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

    private static void updateDownloadProgress(Episode episode, String movieName, String movieDescription, String seasonName, String seasonId, Progress progress) {
        Log.d(ContentManager.class.getSimpleName(), "Downloading in Progress");
        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
        String progressMessage = FileUtils.getProgressDisplayLine(progress.currentBytes, progress.totalBytes);
        MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episode.getEpisodeId(), episode.getEpisodeName() + "/" + seasonName + "/" + movieName, (int) progressPercent, progressMessage);
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), (int) progressPercent);
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), progressMessage);
    }

    public static void cancelDownload(Episode episode) {
        if (PRDownloader.getStatus(getDownloadKeyFromEpisode(episode)) == Status.RUNNING) {
            PRDownloader.cancel(getDownloadKeyFromEpisode(episode));
        }
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
