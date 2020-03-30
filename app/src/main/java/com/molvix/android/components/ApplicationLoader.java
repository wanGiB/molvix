package com.molvix.android.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.ads.MobileAds;
import com.huxq17.download.Pump;
import com.huxq17.download.config.DownloadConfig;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadListener;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.database.ObjectBox;
import com.molvix.android.eventbuses.EpisodeDownloadErrorException;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.managers.VideoCleaner;
import com.molvix.android.managers.MolvixNotificationManager;
import com.molvix.android.managers.MovieTracker;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.AuthorizationHeaderConnection;
import com.molvix.android.utils.DownloaderUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.NetworkClient;

import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ApplicationLoader extends MultiDexApplication {

    @SuppressLint("StaticFieldLeak")
    private static Context _INSTANCE;

    public static DownloadListener globalDownloadListener = new DownloadListener() {

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

    };

    private static void tryShutdownPump() {
        Set<String> inProgressDownloads = AppPrefs.getInProgressDownloads();
        if (inProgressDownloads.isEmpty()) {
            if (globalDownloadListener.isEnable()) {
                globalDownloadListener.disable();
            }
            Pump.shutdown();
        }
    }

    private static void handleCompletedDownload(@NotNull DownloadInfo download) {
        String episodeId = download.getId();
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Download completed for " + EpisodesManager.getEpisodeFullName(episode));
            finalizeDownload(episode);
            VideoCleaner.cleanVideoEpisode(episode);
        }
    }

    private static void handleDownloadError(@NotNull DownloadInfo download) {
        String episodeId = download.getId();
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
            MolvixLogger.d(ContentManager.class.getSimpleName(), "An error occurred while downloading " + EpisodesManager.getEpisodeFullName(episode));
            EventBus.getDefault().post(new EpisodeDownloadErrorException(episode));
            resetEpisodeDownloadProgress(episode);
            new Handler().postDelayed(DownloaderUtils::checkAndResumePausedDownloads, 5000);
        }
    }

    private static void handleDownloadProgress(@NotNull DownloadInfo downloadInfo) {
        String episodeId = downloadInfo.getId();
        if (episodeId != null) {
            Episode episode = MolvixDB.getEpisode(episodeId);
            updateDownloadProgress(episode, downloadInfo);
        }
    }

    public static void resetEpisodeDownloadProgress(Episode episode) {
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
    }

    private static void updateDownloadProgress(Episode episode, @NotNull DownloadInfo downloadInfo) {
        try {
            Season season = episode.getSeason();
            Movie movie = season.getMovie();
            String movieName = WordUtils.capitalize(movie.getMovieName());
            String movieDescription = movie.getMovieDescription();
            String seasonId = season.getSeasonId();
            String seasonName = season.getSeasonName();
            int progressPercent = downloadInfo.getProgress();
            long completedSize = downloadInfo.getCompletedSize();
            long totalSize = downloadInfo.getContentLength();
            String progressMessage = FileUtils.getDataSize(completedSize) + "/" + FileUtils.getDataSize(totalSize);
            MolvixNotificationManager.showEpisodeDownloadProgressNotification(movieName, movieDescription, seasonId, episode.getEpisodeId(), episode.getEpisodeName() + "/" + seasonName + "/" + movieName, progressPercent, progressMessage);
            AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), progressPercent);
            AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), progressMessage);
            if (completedSize > totalSize) {
                Pump.pause(FileDownloadManager.getDownloadIdFromEpisode(episode));
                Pump.stop(FileDownloadManager.getDownloadIdFromEpisode(episode));
                //Fuck, this is mostly an invalid download
                Pump.deleteById(FileDownloadManager.getDownloadIdFromEpisode(episode));
            }
        } catch (Exception e) {
            backwardCompatibilityCleanUp(episode, e);
        }
    }

    private static void backwardCompatibilityCleanUp(Episode episode, Exception e) {
        AppPrefs.removeFromInProgressDownloads(episode);
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), "");
        EpisodesManager.popDownloadableEpisode(episode);
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        Pump.stop(episode.getEpisodeId());
        tryShutdownPump();
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            errorMessage = "unknown";
        }
        MolvixLogger.d(ContentManager.class.getSimpleName(), EpisodesManager.getEpisodeFullName(episode) + " backward scrapped due to " + errorMessage);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(getBaseContext());
        initContext();
        initDownloadManager();
        initDataBase();
        initAdMob();
        initPresets();
    }

    private void initDownloadManager() {
        DownloadConfig.newBuilder()
                .setMaxRunningTaskNum(AppConstants.MAXIMUM_RUNNABLE_TASK)
                .setMinUsableStorageSpace(getMinUsableStorageSpace())
                .setDownloadConnectionFactory(new AuthorizationHeaderConnection
                        .Factory(NetworkClient.getIgnoreCertificateOkHttpClient()))
                .build();
    }

    private long getMinUsableStorageSpace() {
        return 10 * 1024L * 1024;
    }

    private void initPresets() {
        ContentManager.fetchPresets();
    }

    private void initDataBase() {
        ObjectBox.init(this);
    }

    private void initAdMob() {
        MobileAds.initialize(this, getString(R.string.admob_app_id));
    }

    private void initContext() {
        if (_INSTANCE == null) {
            _INSTANCE = this;
        }
    }

    public static Context getInstance() {
        return _INSTANCE;
    }

}
