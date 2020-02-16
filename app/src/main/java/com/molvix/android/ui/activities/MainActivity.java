package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.CheckForDownloadableEpisodes;
import com.molvix.android.eventbuses.EpisodeDownloadErrorException;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.eventbuses.UpdateNotification;
import com.molvix.android.managers.AdsLoadManager;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.managers.ThemeManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Presets;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.adapters.MainActivityPagerAdapter;
import com.molvix.android.ui.fragments.DownloadedVideosFragment;
import com.molvix.android.ui.fragments.HomeFragment;
import com.molvix.android.ui.fragments.MoreContentsFragment;
import com.molvix.android.ui.fragments.NotificationsFragment;
import com.molvix.android.ui.widgets.MolvixSearchView;
import com.molvix.android.ui.widgets.MolvixVideoPlayerView;
import com.molvix.android.ui.widgets.MovieDetailsView;
import com.molvix.android.ui.widgets.NewUpdateAvailableView;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.UiUtils;
import com.morsebyte.shailesh.twostagerating.dialog.UriHelper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.delight.android.webview.AdvancedWebView;
import io.objectbox.reactive.DataSubscription;

public class MainActivity extends BaseActivity {

    @BindView(R.id.search_view)
    MolvixSearchView searchView;

    @BindView(R.id.fragment_pager)
    ViewPager fragmentsPager;

    @BindView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavView;

    @BindView(R.id.container)
    FrameLayout rootContainer;

    private List<Fragment> fragments;
    private DataSubscription presetsSubscription;
    private AtomicBoolean activeVideoPlayBackPaused = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initSearchBox();
        initNavBarTints();
        unLockAppCaptchaSolver();
        setupViewPager();
        observeNewIntent(getIntent());
        fetchDownloadableEpisodes();
        checkAndResumePausedDownloads();
        cleanUpUnLinkedDownloadKeys();
        resetAdsLoader();
        AdsLoadManager.spin();
    }

    private void resetAdsLoader() {
        AppPrefs.persistLastAdLoadTime(System.currentTimeMillis());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanUp();
    }

    private void cleanUp() {
        AdsLoadManager.destroy();
        unSubscribeFromPresetsChanges();
    }

    @Override
    public void onPause() {
        super.onPause();
        checkAndPauseAnyActivePlayBack();
    }

    private void checkAndPauseAnyActivePlayBack() {
        if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof MolvixVideoPlayerView) {
            MolvixVideoPlayerView molvixVideoPlayerView = (MolvixVideoPlayerView) rootContainer.getChildAt(rootContainer.getChildCount() - 1);
            if (molvixVideoPlayerView.isVideoPlaying()) {
                molvixVideoPlayerView.pauseVideo();
                activeVideoPlayBackPaused.set(true);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndResumeAnyInActivePlayBack();
        fetchDownloadableEpisodes();
    }

    private void checkAndResumeAnyInActivePlayBack() {
        if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof MolvixVideoPlayerView) {
            MolvixVideoPlayerView molvixVideoPlayerView = (MolvixVideoPlayerView) rootContainer.getChildAt(rootContainer.getChildCount() - 1);
            if (activeVideoPlayBackPaused.get()) {
                molvixVideoPlayerView.tryResumeVideo();
                activeVideoPlayBackPaused.set(false);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        cleanUp();
    }

    private void unSubscribeFromPresetsChanges() {
        if (presetsSubscription != null && !presetsSubscription.isCanceled()) {
            presetsSubscription.cancel();
            presetsSubscription = null;
        }
    }

    private void cleanUpUnLinkedDownloadKeys() {
        Map<String, ?> allPrefs = AppPrefs.getAppPreferences().getAll();
        List<String> removables = new ArrayList<>();
        if (!allPrefs.isEmpty()) {
            //Let's get keys with episode progress
            Set<String> keySet = allPrefs.keySet();
            for (String key : keySet) {
                if (key.contains(AppConstants.EPISODE_DOWNLOAD_PROGRESS)) {
                    Object value = allPrefs.get(key);
                    if (value != null) {
                        String valueString = String.valueOf(value);
                        if (StringUtils.isNotEmpty(valueString)) {
                            try {
                                if (Integer.parseInt(valueString) == 0) {
                                    DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(extractEpisodeIdFromKey(key).trim());
                                    if (downloadableEpisode == null) {
                                        AppPrefs.updateEpisodeDownloadProgress(extractEpisodeIdFromKey(key).trim(), -1);
                                    }
                                }
                            } catch (NumberFormatException ignore) {

                            }
                        } else {
                            removables.add(key);
                        }
                    }
                }
            }
        }
        if (!removables.isEmpty()) {
            for (String key : removables) {
                AppPrefs.removeKey(key);
            }
        }
    }

    @NotNull
    private String extractEpisodeIdFromKey(String key) {
        return key.replace(AppConstants.EPISODE_DOWNLOAD_PROGRESS, "");
    }

    private void unLockAppCaptchaSolver() {
        EpisodesManager.unLockCaptchaSolver();
    }

    private void checkAndResumePausedDownloads() {
        Set<String> pausedDownloads = AppPrefs.getInProgressDownloads();
        if (!pausedDownloads.isEmpty()) {
            for (String episodeId : pausedDownloads) {
                Episode episode = MolvixDB.getEpisode(episodeId);
                if (episode != null && AppPrefs.getEpisodeDownloadProgress(episodeId) == -1) {
                    boolean paused = AppPrefs.isPaused(episodeId);
                    if (!paused) {
                        FileDownloadManager.downloadEpisode(episode);
                    }
                }
            }
        }
    }

    @Override
    public void onEventMainThread(Object event) {
        super.onEventMainThread(event);
        runOnUiThread(() -> {
            if (event instanceof SearchEvent) {
                if (fragmentsPager.getCurrentItem() != 0) {
                    fragmentsPager.setCurrentItem(0);
                }
            } else if (event instanceof LoadEpisodesForSeason) {
                LoadEpisodesForSeason loadEpisodesForSeason = (LoadEpisodesForSeason) event;
                Season seasonToLoad = loadEpisodesForSeason.getSeason();
                MovieDetailsView movieDetailsView = (MovieDetailsView) rootContainer.getChildAt(rootContainer.getChildCount() - 1);
                if (seasonToLoad != null && movieDetailsView != null) {
                    movieDetailsView.loadEpisodesForSeason(seasonToLoad, loadEpisodesForSeason.canShowLoadingProgress());
                }
            } else if (event instanceof CheckForDownloadableEpisodes) {
                fetchDownloadableEpisodes();
            } else if (event instanceof UpdateNotification) {
                UpdateNotification updateNotification = (UpdateNotification) event;
                new Handler().postDelayed(() -> MolvixDB.updateNotification(updateNotification.getNotification()), 5000);
            } else if (event instanceof EpisodeDownloadErrorException) {
                EpisodeDownloadErrorException episodeDownloadErrorException = (EpisodeDownloadErrorException) event;
                Episode episode = episodeDownloadErrorException.getEpisode();
                UiUtils.snackMessage("Sorry, an error occurred while downloading " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + " of " + WordUtils.capitalize(episode.getSeason().getMovie().getMovieName()) + ".Please try again", rootContainer, true, null, null);
            }
        });
    }

    private void setupViewPager() {
        fragments = new ArrayList<>();
        fragments.add(new HomeFragment());
        fragments.add(new NotificationsFragment());
        fragments.add(new DownloadedVideosFragment());
        fragments.add(new MoreContentsFragment());
        MainActivityPagerAdapter fragmentsPagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager(), fragments);
        fragmentsPager.setAdapter(fragmentsPagerAdapter);
        fragmentsPager.setOffscreenPageLimit(fragments.size());
        fragmentsPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    bottomNavView.setSelectedItemId(R.id.navigation_home);
                } else if (position == 1) {
                    bottomNavView.setSelectedItemId(R.id.navigation_notification);
                } else if (position == 2) {
                    bottomNavView.setSelectedItemId(R.id.navigation_downloaded_videos);
                } else {
                    bottomNavView.setSelectedItemId(R.id.navigation_more);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        bottomNavView.setOnNavigationItemSelectedListener(menuItem -> {
            if (menuItem.getItemId() == R.id.navigation_home) {
                fragmentsPager.setCurrentItem(0);
            } else if (menuItem.getItemId() == R.id.navigation_notification) {
                fragmentsPager.setCurrentItem(1);
            } else if (menuItem.getItemId() == R.id.navigation_downloaded_videos) {
                fragmentsPager.setCurrentItem(2);
            } else {
                fragmentsPager.setCurrentItem(3);
            }
            return true;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void hackPage(Episode episode) {
        runOnUiThread(() -> {
            AdvancedWebView hackWebView = new AdvancedWebView(MainActivity.this);
            hackWebView.getSettings().setJavaScriptEnabled(true);
            hackWebView.setCookiesEnabled(true);
            hackWebView.setMixedContentAllowed(true);
            hackWebView.setThirdPartyCookiesEnabled(true);
            if (rootContainer.getChildAt(0) instanceof AdvancedWebView) {
                rootContainer.removeViewAt(0);
            }
            rootContainer.addView(hackWebView, 0);
            solveEpisodeCaptchaChallenge(hackWebView, episode);
        });
    }

    private void fetchDownloadableEpisodes() {
        new Thread(() -> {
            List<DownloadableEpisode> downloadableEpisodes = MolvixDB.getDownloadableEpisodeBox().query().build().find();
            List<DownloadableEpisode> processed = new ArrayList<>();
            if (!downloadableEpisodes.isEmpty()) {
                for (DownloadableEpisode existingData : downloadableEpisodes) {
                    Set<String> downloadsInProgress = AppPrefs.getInProgressDownloads();
                    if (downloadsInProgress.contains(existingData.getDownloadableEpisodeId())) {
                        MolvixDB.getDownloadableEpisodeBox().remove(existingData);
                    } else {
                        processed.add(existingData);
                    }
                }
                processDownloadableEpisodes(processed);
            }
        }).start();
    }

    private void processDownloadableEpisodes(List<DownloadableEpisode> changedData) {
        if (!changedData.isEmpty()) {
            DownloadableEpisode first = changedData.get(0);
            if (EpisodesManager.isCaptchaSolvable()) {
                hackPage(first.getEpisode());
            }
        }
    }

    private void injectMagicScript(AdvancedWebView hackWebView) {
        String javascriptCodeInjection =
                "javascript:function clickCaptchaButton(){\n" +
                        "    document.getElementsByTagName('input')[0].click();\n" +
                        "}\n" +
                        "clickCaptchaButton();";
        if (Build.VERSION.SDK_INT >= 19) {
            hackWebView.evaluateJavascript(javascriptCodeInjection, value -> MolvixLogger.d(ContentManager.class.getSimpleName(), "Result after evaluating JavaScript=" + value));
        } else {
            hackWebView.loadUrl(javascriptCodeInjection);
        }
    }

    private void solveEpisodeCaptchaChallenge(AdvancedWebView hackWebView, Episode episode) {
        EpisodesManager.lockCaptchaSolver(episode.getEpisodeId());
        hackWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "OnPageFinished and url=" + url);
                if (url.toLowerCase().contains("areyouhuman")) {
                    injectMagicScript(hackWebView);
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "OnPageStarted and url=" + url);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (mimeTypeOfUrl == null) {
                    return;
                }
                if (mimeTypeOfUrl.toLowerCase().contains("video")) {
                    hackWebView.stopLoading();
                    if (episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                        episode.setStandardQualityDownloadLink(url);
                    } else if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY) {
                        episode.setHighQualityDownloadLink(url);
                    } else {
                        episode.setLowQualityDownloadLink(url);
                    }
                    MolvixDB.updateEpisode(episode);
                    FileDownloadManager.downloadEpisode(episode);
                    hackWebView.onDestroy();
                    rootContainer.removeView(hackWebView);
                    EpisodesManager.popDownloadableEpisode(episode);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                unLockAppCaptchaSolver();
                UiUtils.showSafeToast("An error occurred while trying to download " + episode.getEpisodeName() + "/" + episode.getSeason().getSeasonName() + " of " + WordUtils.capitalize(episode.getSeason().getMovie().getMovieName() + ".Please try again"));
                AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
                AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), "");
                EventBus.getDefault().post(new CheckForDownloadableEpisodes());
            }

        });
        hackWebView.loadUrl(episode.getEpisodeCaptchaSolverLink());
    }

    private void observeNewIntent(Intent intent) {
        String invocationType = intent.getStringExtra(AppConstants.INVOCATION_TYPE);
        if (invocationType != null) {
            if (invocationType.equals(AppConstants.NAVIGATE_TO_SECOND_FRAGMENT)) {
                fragmentsPager.setCurrentItem(1);
            } else if (invocationType.equals(AppConstants.DISPLAY_MOVIE)) {
                String movieId = intent.getStringExtra(AppConstants.MOVIE_ID);
                loadMovieDetails(movieId);
            }
        }
    }

    public void loadMovieDetails(String movieId) {
        UiUtils.dismissKeyboard(searchView);
        checkAndRemovePreviousMovieDetailsView();
        addNewMovieDetailsViewAndLoad(movieId);
    }

    private void addNewMovieDetailsViewAndLoad(String movieId) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        MovieDetailsView movieDetailsView = new MovieDetailsView(this);
        rootContainer.addView(movieDetailsView, layoutParams);
        movieDetailsView.loadMovieDetails(movieId);
    }

    private void checkAndRemovePreviousMovieDetailsView() {
        if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof MovieDetailsView) {
            rootContainer.removeViewAt(rootContainer.getChildCount() - 1);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        observeNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof MolvixVideoPlayerView) {
            MolvixVideoPlayerView molvixVideoPlayerView = (MolvixVideoPlayerView) rootContainer.getChildAt(rootContainer.getChildCount() - 1);
            molvixVideoPlayerView.trySaveCurrentPlayerPosition();
            molvixVideoPlayerView.cleanUpVideoView();
            rootContainer.removeViewAt(rootContainer.getChildCount() - 1);
            return;
        }
        if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof MovieDetailsView) {
            MovieDetailsView movieDetailsView = (MovieDetailsView) rootContainer.getChildAt(rootContainer.getChildCount() - 1);
            if (movieDetailsView.isBottomSheetDialogShowing()) {
                movieDetailsView.closeBottomSheetDialog();
            } else {
                movieDetailsView.removeEpisodeListener();
                rootContainer.removeView(movieDetailsView);
            }
            return;
        }
        String searchString = searchView.getText();
        if (StringUtils.isNotEmpty(searchString)) {
            searchView.setText("");
            return;
        }
        DownloadedVideosFragment downloadedVideosFragment = (DownloadedVideosFragment) fragments.get(2);
        if (fragmentsPager.getCurrentItem() == 2 && downloadedVideosFragment.needsToNavigateBack()) {
            downloadedVideosFragment.navigateBack();
            return;
        }
        if (fragmentsPager.getCurrentItem() != 0) {
            fragmentsPager.setCurrentItem(0);
            return;
        }
        super.onBackPressed();
    }

    private void initSearchBox() {
        searchView.setup();
    }

    private void initNavBarTints() {

        ColorStateList lightModeIconsColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.grey500), Color.BLACK});

        ColorStateList lightModeTextColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.grey500), Color.BLACK});

        ColorStateList darkModeIconsColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.light_gray_inactive_icon), Color.WHITE});

        ColorStateList darkModeTextColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.light_gray_inactive_icon), Color.WHITE});

        ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();

        bottomNavView.setItemIconTintList(themeSelection == ThemeManager.ThemeSelection.DARK ? darkModeIconsColorStates : lightModeIconsColorStates);
        bottomNavView.setItemTextColor(themeSelection == ThemeManager.ThemeSelection.DARK ? darkModeTextColorStates : lightModeTextColorStates);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        subscribeToPresetsChanges();
        ContentManager.fetchPresets();
    }

    private void subscribeToPresetsChanges() {
        presetsSubscription = MolvixDB.getPresetsBox()
                .query()
                .build()
                .subscribe()
                .observer(data -> {
                    if (!data.isEmpty()) {
                        Presets firstData = data.get(0);
                        if (firstData != null) {
                            String presetString = firstData.getPresetString();
                            if (presetString != null) {
                                try {
                                    JSONObject presetJSONObject = new JSONObject(presetString);
                                    long forcedVersionCodeUpdate = presetJSONObject.optLong(AppConstants.FORCED_VERSION_CODE_UPDATE);
                                    String forcedVersionNameUpdate = presetJSONObject.optString(AppConstants.FORCED_VERSION_NAME_UPDATE);
                                    checkForAppUpdate(forcedVersionCodeUpdate, forcedVersionNameUpdate);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
    }

    private void checkForAppUpdate(long forcedVersionCodeUpdate, String forcedVersionNameUpdate) {
        try {
            PackageManager packageManager = ApplicationLoader.getInstance().getPackageManager();
            if (packageManager != null) {
                PackageInfo packageInfo = packageManager.getPackageInfo(ApplicationLoader.getInstance().getPackageName(), 0);
                if (packageInfo != null) {
                    if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof NewUpdateAvailableView) {
                        rootContainer.removeViewAt(rootContainer.getChildCount() - 1);
                    }
                    long versionCode;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        versionCode = packageInfo.getLongVersionCode();
                    } else {
                        versionCode = packageInfo.versionCode;
                    }
                    if (forcedVersionCodeUpdate > versionCode) {
                        tintStatusBar(ContextCompat.getColor(this, R.color.colorPrimaryDarkTheme));
                        NewUpdateAvailableView newUpdateAvailableView = new NewUpdateAvailableView(this);
                        rootContainer.addView(newUpdateAvailableView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                        newUpdateAvailableView.displayNewUpdate(forcedVersionNameUpdate);
                    }
                }
            }
        } catch (Exception ignored) {

        }

    }

    public static Intent createIntentForGooglePlay(Context context) {
        String packageName = context.getPackageName();
        Intent intent = new Intent(Intent.ACTION_VIEW, UriHelper.getGooglePlay(packageName));
        if (UriHelper.isPackageExists(context, AppConstants.GOOGLE_PLAY_PACKAGE_NAME)) {
            intent.setPackage(AppConstants.GOOGLE_PLAY_PACKAGE_NAME);
        }
        return intent;
    }

    public void moveToPlayStore() {
        startActivity(createIntentForGooglePlay(this));
        finish();
    }

    public void playVideo(List<DownloadedVideoItem> downloadedVideoItems, DownloadedVideoItem downloadedVideoItem) {
        if (rootContainer.getChildAt(rootContainer.getChildCount() - 1) instanceof MolvixVideoPlayerView) {
            rootContainer.removeViewAt(rootContainer.getChildCount() - 1);
        }
        MolvixVideoPlayerView molvixVideoPlayerView = new MolvixVideoPlayerView(this);
        rootContainer.addView(molvixVideoPlayerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        molvixVideoPlayerView.loadVideo(downloadedVideoItems, downloadedVideoItems.indexOf(downloadedVideoItem));
    }

}