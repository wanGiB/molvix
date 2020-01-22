package com.molvix.android.managers;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.Status;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.database.MolvixDB;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

public class FileDownloadManager {

    @SuppressWarnings({"ConstantConditions", "UnusedAssignment"})
    public static void startNewEpisodeDownload(Episode episode) {
        String episodeId = episode.getEpisodeId();
        String episodeName = episode.getEpisodeName();
        Season season = episode.getSeason();
        Movie movie = season.getMovie();
        if (season != null) {
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
            int downloadId = (dirPath + fileName).hashCode();
            if (Status.PAUSED == PRDownloader.getStatus(downloadId)) {
                PRDownloader.resume(downloadId);
            } else {
                downloadId = PRDownloader.download(downloadUrl, dirPath, fileName)
                        .build()
                        .setOnStartOrResumeListener(() -> {
                        }).setOnPauseListener(() -> {
                        }).setOnCancelListener(() -> {
                            episode.setDownloadProgress(-1);
                            MolvixDB.updateEpisode(episode);
                            DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
                            if (downloadableEpisode != null) {
                                MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
                            }
                            MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episodeId.hashCode()));
                        }).setOnProgressListener(progress -> {
                            long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                            String progressMessage = FileUtils.getProgressDisplayLine(progress.currentBytes, progress.totalBytes);
                            MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episodeId, episodeName + "/" + seasonName + "/" + movieName, (int) progressPercent, progressMessage);
                            episode.setDownloadProgress((int) progressPercent);
                            episode.setProgressDisplayText(progressMessage);
                            MolvixDB.updateEpisode(episode);
                        }).start(new OnDownloadListener() {
                            @Override
                            public void onDownloadComplete() {
                                episode.setDownloadProgress(-1);
                                MolvixDB.updateEpisode(episode);
                                if (movie != null) {
                                    movie.setSeenByUser(true);
                                    movie.setRecommendedToUser(true);
                                    MolvixDB.updateMovie(movie);
                                }
                                DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episodeId);
                                if (downloadableEpisode != null) {
                                    MolvixDB.deleteDownloadableEpisode(downloadableEpisode);
                                    MovieTracker.recordEpisodeAsDownloaded(episode);
                                }
                            }

                            @Override
                            public void onError(Error error) {

                            }

                        });
            }
        }

    }

    public static void cancelDownload(int downloadId) {
        if (Status.RUNNING == PRDownloader.getStatus(downloadId)) {
            PRDownloader.cancel(downloadId);
        }
    }


}
