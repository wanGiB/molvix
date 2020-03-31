package com.molvix.android.companions;

import android.annotation.SuppressLint;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AppConstants {

    public static final int MAXIMUM_RUNNABLE_TASK = 20;
    public static final String MEDIA_PLAY_BACK_FOR_ = "media_play_back_for_";
    public static final String PREFERRED_APP_THEME = "preferred_molvix_app_theme";
    public static final String DOWNLOAD_COINS = "download_coins";
    public static final String GENRES = "genres";
    public static final String SHOW_UNFINISHED_DOWNLOADS = "show_unfinished_downloads";
    public static final String NO_COMPLEX_CAPTCHA = "_no_complex_captcha";
    public static final String SOLVE_COMPLEX_CAPTCHA = "_solve_complex_captcha";
    public static final String NO_FORM_ELEMENT_FOUND = "_no_form_element_found";
    public static HashMap<String, String> MOVIE_NAME_TO_ART_URL_MAP = new HashMap<>();
    @SuppressLint("ConstantLocale")
    public static final SimpleDateFormat DATE_FORMATTER_IN_12HRS = new SimpleDateFormat("h:mm a", Locale.getDefault());
    @SuppressLint("ConstantLocale")
    public static final SimpleDateFormat DATE_FORMATTER_IN_YEARS = new SimpleDateFormat("yyyy", Locale.getDefault());

    public static final String DOWNLOADABLE = "o2tvseries.com/download/";

    public static final String APP_PREFS_NAME = "molvixapp_prefs";
    public static final String CAPTCHA_SOLVING = "captcha_solving";
    public static final int HIGH_QUALITY = 1;
    public static final int STANDARD_QUALITY = 2;
    public static final int LOW_QUALITY = 3;

    public static final String MOVIE_ID = "movieId";

    public static final String EPISODE_ID = "episodeId";
    public static final String EPISODE_DOWNLOAD_PROGRESS = "EpisodeDownloadProgress";
    public static final String EPISODE_DOWNLOAD_PROGRESS_TEXT = "DownloadProgressText";

    public static final String INVOCATION_TYPE = "invocation_type";
    public static final String NAVIGATE_TO_SECOND_FRAGMENT = "navigate_to_second_fragment";

    public static final int DESTINATION_DOWNLOADED_EPISODE = 0;
    public static final int DESTINATION_NEW_EPISODE_AVAILABLE = 1;
    public static final String DAILY_MOVIES_RECOMMENDABILITY = "daily_movies_recommendability";
    public static final String DOWNLOADED_MOVIES_UPDATE = "downloaded_movies_update_key";
    public static final String LAST_MOVIES_RECOMMENDATION_TIME = "last_movies_recommendation_time";
    public static final String REFRESHED_MOVIES = "refreshed_movies";
    public static final String REFRESHED_SEASONS = "refreshed_seasons";
    public static final String LAST_MOVIES_SIZE = "last_movies_size";

    public static final String NOTIFICATION = "Notification_";
    public static final String DISPLAY_MOVIE = "display_movie";
    public static final String IN_PROGRESS_DOWNLOADS = "in_progress_downloads";

    public static final String LAST_AD_LOAD_TIME = "last_ad_load_time";
    public static final String SEASON_EPISODES_REFRESHED = "SeasonEpisodesRefreshed_";
    public static final String DATA = "data";
    public static final String MOVIE_NAME = "movie_name";
    public static final String MOVIE_ART_URL = "movie_art_url";
    public static final String FORCED_VERSION_CODE_UPDATE = "forced_version_code_update";
    public static final String FORCED_VERSION_NAME_UPDATE = "forced_version_name_update";
    public static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";
    public static final String PRESETS_DOWNSTREAM_URL = "https://raw.githubusercontent.com/molvixapp/lizandry/master/presets.json";
    public static Stack<Integer> downloadedVideoItemsPositionsStack = new Stack<>();
    public static AtomicBoolean canShuffleExistingMovieCollection = new AtomicBoolean(true);
    public static AtomicReference<UnifiedNativeAd> unifiedNativeAdAtomicReference = new AtomicReference<>();
    public static AtomicBoolean MAIN_ACTIVITY_IN_FOCUS = new AtomicBoolean(false);
}
