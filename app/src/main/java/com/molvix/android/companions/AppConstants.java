package com.molvix.android.companions;

import android.annotation.SuppressLint;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AppConstants {

    @SuppressLint("ConstantLocale")
    public static final SimpleDateFormat DATE_FORMATTER_IN_12HRS = new SimpleDateFormat("h:mm a", Locale.getDefault());
    @SuppressLint("ConstantLocale")
    public static final SimpleDateFormat DATE_FORMATTER_IN_BIRTHDAY_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    @SuppressLint("ConstantLocale")
    public static final SimpleDateFormat DATE_FORMATTER_IN_YEARS = new SimpleDateFormat("yyyy", Locale.getDefault());

    public static final String TEST_DEVICE_ID = "53D46815EE1FBEED38704D3C418F4402";
    public static final String DOWNLOADABLE = "o2tvseries.com/download/";

    public static final String APP_PREFS_NAME = "molvixapp_prefs";
    public static final String CAPTCHA_SOLVING = "captcha_solving";
    public static final String EPISODES = "episodes";
    public static final int HIGH_QUALITY = 1;
    public static final int STANDARD_QUALITY = 2;
    public static final int LOW_QUALITY = 3;

    //ModelKeys
    public static final String MOVIE_ID = "movieId";
    public static final String MOVIE_LINK = "movieLink";
    public static final String MOVIE_NAME = "movieName";
    public static final String MOVIE_DESCRIPTION = "movieDescription";
    public static final String MOVIE_ART_URL = "movieArtUrl";
    public static final String MOVIE_SEASONS = "seasons";
    public static final String MOVIE_RECOMMENDED_TO_USER = "recommendedToUser";
    public static final String MOVIE_SEEN_BY_USER = "seenByUser";

    public static final String SEASON_ID = "seasonId";
    public static final String SEASON_NAME = "seasonName";
    public static final String SEASON_LINK = "seasonLink";

    public static final String EPISODE_ID = "episodeId";
    public static final String EPISODE_NAME = "episodeName";
    public static final String EPISODE_LINK = "episodeLink";
    public static final String EPISODE_QUALITY = "episodeQuality";
    public static final String HIGH_QUALITY_DOWNLOAD_LINK = "highQualityDownloadLink";
    public static final String STANDARD_QUALITY_DOWNLOAD_LINK = "standardQualityDownloadLink";
    public static final String LOW_QUALITY_DOWNLOAD_LINK = "lowQualityDownloadLink";
    public static final String EPISODE_CAPTCHA_SOLVER_LINK = "episodeCaptchaSolverLink";
    public static final String EPISODE_DOWNLOAD_PROGRESS = "EpisodeDownloadProgress";
    public static final String EPISODE_DOWNLOAD_PROGRESS_TEXT = "EpisodeDownloadProgressText";

    public static final String INVOCATION_TYPE = "invocation_type";
    public static final String NAVIGATE_TO_SECOND_FRAGMENT = "navigate_to_second_fragment";

    public static final String NOTIFICATION_OBJECT_ID = "notificationObjectId";
    public static final String NOTIFICATION_RESOLUTION_KEY = "resolutionKey";
    public static final String NOTIFICATION_DESTINATION = "destination";
    public static final int DESTINATION_DOWNLOADED_EPISODE = 0;
    public static final int DESTINATION_NEW_EPISODE_AVAILABLE = 1;
    public static final String DAILY_MOVIES_RECOMMENDABILITY = "daily_movies_recommendability";
    public static final String DOWNLOADED_MOVIES_UPDATE = "downloaded_movies_update_key";
    public static final String AD_CONSUMED = "ad_consumed";
    public static final String LAST_MOVIES_RECOMMENDATION_TIME = "last_movies_recommendation_time";
    public static final String REFRESHED_MOVIES = "refreshed_movies";
    public static final String REFRESHED_SEASONS = "refreshed_seasons";
    public static final String UPDATED = "Updated";
    public static final String LAST_MOVIES_SIZE="last_movies_size";

    public static final String MOVIE = "Movie_";
    public static final String NOTIFICATION = "Notification_";
    public static final String EPISODE = "Episode_";
    public static final String SEASON = "Season_";
    public static final String DOWNLOADABLE_EPISODE = "Downloadable_";
    public static final String DISPLAY_MOVIE = "display_movie";
    public static final String IN_PROGRESS_DOWNLOADS = "in_progress_downloads";
    public static final String DOWNLOAD = "OngoingDownload_";

    public static final String EPISODE_FROM_MOVIE = "Episode_From_Movie_";
    public static final String MOVIE_EPISODES_DOWNLOADING = "MovieEpisodesDownloading";
    public static final String DOWNLOAD_PAUSED = "DownloadPaused_";
    public static final String DOWNLOAD_ID_KEY = "DownloadIdKey_";
    public static AtomicReference<UnifiedNativeAd>unifiedNativeAdAtomicReference=new AtomicReference<>();
}
