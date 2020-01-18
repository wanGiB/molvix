package com.molvix.android.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.Status;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.FileUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import io.realm.ImportFlag;
import io.realm.Realm;

public class FileDownloadManager {

    public static int startNewEpisodeDownload(Episode episode) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Movie movie = realm.where(Movie.class).equalTo(AppConstants.MOVIE_ID, episode.getMovieId()).findFirst();
            Season season = realm.where(Season.class).equalTo(AppConstants.SEASON_ID, episode.getSeasonId()).findFirst();
            if (movie != null && season != null) {
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
                            }).setOnCancelListener(() -> realm.executeTransaction(r -> {
                                Episode updatableEpisode = r.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episode.getEpisodeId()).findFirst();
                                if (updatableEpisode != null) {
                                    updatableEpisode.setDownloadProgress(-1);
                                    r.copyToRealmOrUpdate(updatableEpisode, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            })).setOnProgressListener(progress -> {
                                long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                                String progressMessage = FileUtils.getProgressDisplayLine(progress.currentBytes, progress.totalBytes);
                                realm.executeTransaction(r -> {
                                    Episode updatableEpisode = r.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episode.getEpisodeId()).findFirst();
                                    if (updatableEpisode != null) {
                                        updatableEpisode.setDownloadProgress((int) progressPercent);
                                        updatableEpisode.setProgressDisplayText(progressMessage);
                                        r.copyToRealmOrUpdate(updatableEpisode, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                    }
                                });
                            }).start(new OnDownloadListener() {
                                @Override
                                public void onDownloadComplete() {
                                    realm.executeTransaction(r -> {
                                        Episode updatableEpisode = r.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episode.getEpisodeId()).findFirst();
                                        if (updatableEpisode != null) {
                                            updatableEpisode.setDownloadProgress(-1);
                                            r.copyToRealmOrUpdate(updatableEpisode, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Error error) {

                                }

                            });
                }
                return downloadId;
            }
        }
        return -1;
    }

    public static void pausDownload(int downloadId) {
        if (Status.RUNNING == PRDownloader.getStatus(downloadId)) {
            PRDownloader.pause(downloadId);
        }
    }

    public static void cancelDownload(int downloadId) {
        if (Status.RUNNING == PRDownloader.getStatus(downloadId)) {
            PRDownloader.cancel(downloadId);
        }
    }
}
