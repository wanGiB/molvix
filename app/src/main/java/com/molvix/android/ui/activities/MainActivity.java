package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.huxq17.download.Pump;
import com.molvix.android.R;
import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.CheckForDownloadableEpisodes;
import com.molvix.android.eventbuses.ConnectivityChangedEvent;
import com.molvix.android.eventbuses.DisplayNewMoviesEvent;
import com.molvix.android.eventbuses.EpisodeDownloadErrorException;
import com.molvix.android.eventbuses.FetchMoviesEvent;
import com.molvix.android.eventbuses.FilterByGenresEvent;
import com.molvix.android.eventbuses.FilterSeriesAlphabetically;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.eventbuses.UpdateNotification;
import com.molvix.android.managers.AdsLoadManager;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.managers.GenreManager;
import com.molvix.android.managers.ThemeManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Presets;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.receivers.ConnectivityChangeReceiver;
import com.molvix.android.tasks.ConnectivityCheckWorker;
import com.molvix.android.ui.adapters.MainActivityPagerAdapter;
import com.molvix.android.ui.fragments.DownloadedVideosFragment;
import com.molvix.android.ui.fragments.HomeFragment;
import com.molvix.android.ui.fragments.MoreContentsFragment;
import com.molvix.android.ui.fragments.NotificationsFragment;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.ui.widgets.FullScreenDialog;
import com.molvix.android.ui.widgets.MolvixSearchView;
import com.molvix.android.ui.widgets.MolvixVideoPlayerView;
import com.molvix.android.ui.widgets.MovieDetailsView;
import com.molvix.android.ui.widgets.NewUpdateAvailableView;
import com.molvix.android.ui.widgets.lovelydialog.LovelyStandardDialog;
import com.molvix.android.ui.widgets.lovelydialog.LovelyTextInputDialog;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixGenUtils;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.UiUtils;
import com.morsebyte.shailesh.twostagerating.dialog.UriHelper;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.delight.android.webview.AdvancedWebView;
import io.objectbox.reactive.DataSubscription;

import static com.molvix.android.preferences.AppPrefs.getAppPreferences;

public class MainActivity extends BaseActivity implements RewardedVideoAdListener {

    @BindView(R.id.search_view)
    MolvixSearchView searchView;

    @BindView(R.id.fragment_pager)
    ViewPager fragmentsPager;

    @BindView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavView;

    @BindView(R.id.content_filterer)
    View contentFilterer;

    @BindView(R.id.container)
    FrameLayout rootContainer;

    private AdvancedWebView hackWebView;

    private List<Fragment> fragments;
    private DataSubscription presetsSubscription;
    private RewardedVideoAd mRewardedVideoAd;

    public static AtomicBoolean canShowLoadedRewardedVideoAd = new AtomicBoolean(false);
    private AtomicReference<String> lastCaptchaErrorMessage = new AtomicReference<>("");
    private AtomicBoolean solvableCaptchaDialogShowing = new AtomicBoolean(false);
    private AtomicBoolean activeVideoPlayBackPaused = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        AppConstants.MAIN_ACTIVITY_IN_FOCUS.set(true);
        initContentFilterClickListener();
        initSearchBox();
        tintNavBar();
        unLockAppCaptchaSolver();
        setupViewPager();
        observeNewIntent(getIntent());
        checkAndDisplayUnFinishedDownloads(false);
        cleanUpUnLinkedDownloadKeys();
        setupRewardedVideoAd();
        resetLastAdLoadTime();
        AdsLoadManager.spin();
        ConnectivityChangeReceiver.fetchNotifications();
    }

    @Override
    protected void onRestart() {
        AppConstants.MAIN_ACTIVITY_IN_FOCUS.set(true);
        super.onRestart();
    }

    private void initContentFilterClickListener() {
        contentFilterer.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            displayFilters();
        });
    }

    private void displayFilters() {
        PopupMenu filterMenu = new PopupMenu(this, contentFilterer);
        filterMenu.inflate(R.menu.filter_menu);
        filterMenu.setOnMenuItemClickListener(item -> {
            filterMenu.dismiss();
            fragmentsPager.setCurrentItem(0);
            if (item.getItemId() == R.id.display_new_movies) {
                EventBus.getDefault().post(new DisplayNewMoviesEvent());
            } else if (item.getItemId() == R.id.filter_by_genre) {
                fetchAvailableGenres();
            } else if (item.getItemId() == R.id.filter_alphabetically) {
                filterSeriesAlphabetically();
            }
            return true;
        });
        filterMenu.show();
    }

    private void filterSeriesAlphabetically() {
        EventBus.getDefault().post(new FilterSeriesAlphabetically());
    }

    private void fetchAvailableGenres() {
        List<String> availableGenres = GenreManager.fetchAvailableGenres();
        if (!availableGenres.isEmpty()) {
            Collections.sort(availableGenres);
            CharSequence[] options = MolvixGenUtils.getCharSequencesFromList(availableGenres);
            AlertDialog.Builder genresDialogBuilder = new AlertDialog.Builder(this);
            genresDialogBuilder.setTitle("Select Genres");
            List<String> selectedGenres = new ArrayList<>();
            genresDialogBuilder.setMultiChoiceItems(options, null, (dialog, which, isChecked) -> {
                CharSequence selection = options[which];
                String selectionToLowerCase = selection.toString().toLowerCase();
                if (isChecked) {
                    if (!selectedGenres.contains(selectionToLowerCase)) {
                        selectedGenres.add(selectionToLowerCase);
                    }
                } else {
                    selectedGenres.remove(selectionToLowerCase);
                }
            });
            genresDialogBuilder.setPositiveButton("FILTER", (dialog, which) -> {
                dialog.dismiss();
                if (!selectedGenres.isEmpty()) {
                    EventBus.getDefault().post(new FilterByGenresEvent(selectedGenres));
                } else {
                    UiUtils.showSafeToast("Nothing selected");
                }
            });
            genresDialogBuilder.setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss());
            genresDialogBuilder.create().show();
        } else {
            UiUtils.showSafeToast("Sorry, we couldn't load available genres.Please try again later.");
        }
    }

    private void setupRewardedVideoAd() {
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        loadRewardedVideoAdNow();
    }

    private void loadRewardedVideoAdNow() {
        AdRequest.Builder adBuilder = new AdRequest.Builder();
        mRewardedVideoAd.loadAd(getString(R.string.rewarded_video_release_ad_unit_id),
                adBuilder.build());
    }

    private void resetLastAdLoadTime() {
        AppPrefs.persistLastAdLoadTime(System.currentTimeMillis());
    }

    @Override
    protected void onDestroy() {
        AppConstants.MAIN_ACTIVITY_IN_FOCUS.set(false);
        mRewardedVideoAd.destroy(this);
        cleanUp();
        super.onDestroy();
    }

    private void cleanUp() {
        AdsLoadManager.destroy();
        unSubscribeFromPresetsChanges();
    }

    @Override
    public void onPause() {
        AppConstants.MAIN_ACTIVITY_IN_FOCUS.set(false);
        mRewardedVideoAd.pause(this);
        checkAndPauseAnyActivePlayBack();
        super.onPause();
    }

    @Override
    public void onResume() {
        AppConstants.MAIN_ACTIVITY_IN_FOCUS.set(true);
        mRewardedVideoAd.resume(this);
        checkAndResumeAnyActivePlayBack();
        super.onResume();
    }

    private void checkAndPauseAnyActivePlayBack() {
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof MolvixVideoPlayerView) {
            MolvixVideoPlayerView molvixVideoPlayerView = (MolvixVideoPlayerView) rootContainer.getChildAt(rootContainerPenultimateFront());
            if (molvixVideoPlayerView.isVideoPlaying()) {
                molvixVideoPlayerView.pauseVideo();
                activeVideoPlayBackPaused.set(true);
            }
        }
    }

    private int rootContainerPenultimateFront() {
        return rootContainer.getChildCount() - 1;
    }

    private void checkAndResumeAnyActivePlayBack() {
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof MolvixVideoPlayerView) {
            MolvixVideoPlayerView molvixVideoPlayerView = (MolvixVideoPlayerView) rootContainer.getChildAt(rootContainerPenultimateFront());
            if (activeVideoPlayBackPaused.get()) {
                molvixVideoPlayerView.tryResumeVideo();
                activeVideoPlayBackPaused.set(false);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AppConstants.MAIN_ACTIVITY_IN_FOCUS.set(false);
        cleanUp();
    }

    private void unSubscribeFromPresetsChanges() {
        if (presetsSubscription != null && !presetsSubscription.isCanceled()) {
            presetsSubscription.cancel();
            presetsSubscription = null;
        }
    }

    private void cleanUpUnLinkedDownloadKeys() {
        Map<String, ?> allPrefs = getAppPreferences().getAll();
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
                MovieDetailsView movieDetailsView = (MovieDetailsView) rootContainer.getChildAt(rootContainerPenultimateFront());
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
                UiUtils.snackMessage("Sorry, an error occurred while downloading " + EpisodesManager.getEpisodeFullName(episode) + ".Please try again", rootContainer, true, null, null);
            } else if (event instanceof ConnectivityChangedEvent) {
                if (ConnectivityUtils.isConnected()) {
                    checkAndDisplayUnFinishedDownloads(false);
                }
            }
        });
    }

    private void checkAndDisplayUnFinishedDownloads(boolean showDialogImmediately) {
        Set<String> pausedDownloads = AppPrefs.getInProgressDownloads();
        if (!pausedDownloads.isEmpty()) {
            if (ConnectivityUtils.isConnected()) {
                int sizeOfUnFinishedDownloads = pausedDownloads.size();
                String quantifier = sizeOfUnFinishedDownloads == 1 ? "download" : "downloads";
                String message = "You have " + sizeOfUnFinishedDownloads + " unfinished " + quantifier;
                AlertDialog.Builder pausedEpisodesOptionsDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                pausedEpisodesOptionsDialogBuilder.setTitle("Incomplete Downloads");
                List<Episode> episodes = new ArrayList<>();
                List<CharSequence> episodeNames = new ArrayList<>();
                for (String episodeId : pausedDownloads) {
                    Episode episode = MolvixDB.getEpisode(episodeId);
                    if (episode != null) {
                        episodes.add(episode);
                        episodeNames.add(EpisodesManager.getEpisodeAbbrev(episode));
                    }
                }
                List<Episode> selections = new ArrayList<>();
                pausedEpisodesOptionsDialogBuilder.setMultiChoiceItems(episodeNames.toArray(new CharSequence[0]), null, (dialogInterface, which, selected) -> {
                    Episode selection = episodes.get(which);
                    if (selected) {
                        if (!selections.contains(selection)) {
                            selections.add(selection);
                        }
                    } else {
                        selections.remove(selection);
                    }
                });
                pausedEpisodesOptionsDialogBuilder.setPositiveButton("RESUME", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    dialogInterface.cancel();
                    resume(selections);
                });
                pausedEpisodesOptionsDialogBuilder.setNegativeButton("REMOVE", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    dialogInterface.cancel();
                    remove(selections);
                });
                pausedEpisodesOptionsDialogBuilder.setNeutralButton("CLOSE", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    dialogInterface.cancel();
                });
                if (showDialogImmediately) {
                    pausedEpisodesOptionsDialogBuilder.create().show();
                }
                if (!ApplicationLoader.globalDownloadListener.isEnable()) {
                    UiUtils.snackMessage(message, bottomNavView, false, "VIEW", () -> pausedEpisodesOptionsDialogBuilder.create().show());
                }
            }
        } else {
            if (ConnectivityUtils.isConnected()) {
                fetchDownloadableEpisodes();
            }
        }
    }

    private void remove(List<Episode> selections) {
        Set<String> ids = AppPrefs.getInProgressDownloads();
        if (!selections.isEmpty()) {
            for (Episode episode : selections) {
                cancelDownload(episode);
                ids.remove(episode.getEpisodeId());
            }
            AppPrefs.updateInProgressDownloads(ids);
            UiUtils.showSafeToast("Removed successfully!");
            checkAndDisplayUnFinishedDownloads(true);
        } else {
            UiUtils.showSafeToast("No selection made");
        }
    }

    private void resume(List<Episode> selections) {
        if (!selections.isEmpty()) {
            for (Episode episode : selections) {
                FileDownloadManager.downloadEpisode(episode);
            }
        } else {
            UiUtils.showSafeToast("No selection made");
        }
    }

    public static void cancelDownload(Episode episode) {
        String downloadKeyFromEpisode = episode.getEpisodeId();
        MolvixNotification.with(ApplicationLoader.getInstance()).cancel(Math.abs(episode.getEpisodeId().hashCode()));
        DownloadableEpisode downloadableEpisode = MolvixDB.getDownloadableEpisode(episode.getEpisodeId());
        if (downloadableEpisode != null) {
            EpisodesManager.popDownloadableEpisode(downloadableEpisode.getEpisode());
        } else {
            EpisodesManager.unLockCaptchaSolver();
            EventBus.getDefault().post(new CheckForDownloadableEpisodes());
        }
        ApplicationLoader.resetEpisodeDownloadProgress(episode);
        Pump.stop(downloadKeyFromEpisode);
        MolvixLogger.d(ContentManager.class.getSimpleName(), EpisodesManager.getEpisodeFullName(episode) + " was cancelled");
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
    private void tryByPassEpisodeCaptcha(Episode episode) {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "About to bypass captcha for " + EpisodesManager.getEpisodeFullName(episode));
        runOnUiThread(() -> {
            if (rootContainer.getChildAt(getHackWebViewIndex()) instanceof AdvancedWebView) {
                //We already have an existing hack web view for this app session
                //Simply re-use it
                hackWebView = (AdvancedWebView) rootContainer.getChildAt(getHackWebViewIndex());
            } else {
                //Otherwise create a new hack web view and add it as the first child
                //in the View Group
                hackWebView = new AdvancedWebView(MainActivity.this);
                hackWebView.getSettings().setJavaScriptEnabled(true);
                hackWebView.getSettings().setDomStorageEnabled(true);
                hackWebView.setCookiesEnabled(true);
                hackWebView.setMixedContentAllowed(true);
                hackWebView.setThirdPartyCookiesEnabled(true);
                rootContainer.addView(hackWebView, getHackWebViewIndex());
            }
            solveEpisodeCaptchaChallenge(hackWebView, episode);
        });
    }

    private int getHackWebViewIndex() {
        return 0;
    }

    private void fetchDownloadableEpisodes() {
        new Thread(() -> {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Checking for downloadable episodes");
            List<DownloadableEpisode> downloadableEpisodes = MolvixDB.getDownloadableEpisodeBox().query().build().find();
            List<DownloadableEpisode> processed = new ArrayList<>();
            if (!downloadableEpisodes.isEmpty()) {
                for (DownloadableEpisode existingData : downloadableEpisodes) {
                    Set<String> downloadsInProgress = AppPrefs.getInProgressDownloads();
                    if (downloadsInProgress.contains(existingData.getDownloadableEpisodeId())) {
                        //Remove downloads that are already in progress, we don't want them
                        //We only want to process downloads that are yet to start
                        MolvixDB.getDownloadableEpisodeBox().remove(existingData);
                    } else {
                        processed.add(existingData);
                    }
                }
                processDownloadableEpisodes(processed);
            } else {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "No downloadable episodes found");
            }
        }).start();
    }

    private void processDownloadableEpisodes(List<DownloadableEpisode> changedData) {
        if (!changedData.isEmpty()) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Some processed downloads waiting to be downloaded have being received...");
            DownloadableEpisode first = changedData.get(0);
            if (EpisodesManager.isCaptchaSolvable()) {
                if (ConnectivityUtils.isConnected()) {
                    tryByPassEpisodeCaptcha(first.getEpisode());
                } else {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "No network to bypass captcha of " + EpisodesManager.getEpisodeFullName(first.getEpisode()));
                }
            } else {
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Unfortunately, the captcha solver is currently locked against " + EpisodesManager.getEpisodeFullName(first.getEpisode()) + ".We'll wait for now.");
            }
        }
    }

    private void checkForCaptchaAvailability(AdvancedWebView hackWebView) {
        String checkForCaptchaAvailability = "javascript:(function checkForCaptchaAvailability() {\n" +
                "    var formElement = document.getElementsByTagName(\"form\")[0];\n" +
                "    if (formElement != null) {\n" +
                "        var allImages = document.getElementsByTagName(\"img\");\n" +
                "        if (allImages != undefined) {\n" +
                "            var pageImgsLength = allImages.length;\n" +
                "            var i;\n" +
                "            var captchaFound = false;\n" +
                "            for (i = 0; i < pageImgsLength; i++) {\n" +
                "                var pageImg = allImages[i];\n" +
                "                var imageSrc = pageImg.src;\n" +
                "                var targetKeyword = \"captcha\";\n" +
                "                if (imageSrc.toLowerCase().indexOf(targetKeyword) != -1) {\n" +
                "                    captchaFound = true;\n" +
                "                }\n" +
                "            }\n" +
                "            if (captchaFound) {\n" +
                "                console.log(\"_solve_complex_captcha\");\n" +
                "            } else {\n" +
                "                console.log(\"_no_complex_captcha\");\n" +
                "            }\n" +
                "        }\n" +
                "    } else {\n" +
                "        console.log(\"_no_form_element_found\");\n" +
                "    }\n" +
                "})();";
        evaluateJavaScript(hackWebView, checkForCaptchaAvailability);
    }

    private void evaluateJavaScript(AdvancedWebView hackWebView, String javascript) {
        hackWebView.evaluateJavascript(javascript, null);
    }

    private void solveEpisodeCaptchaChallenge(AdvancedWebView hackWebView, Episode episode) {
        EpisodesManager.lockCaptchaSolver(episode.getEpisodeId());
        hackWebView.setWebViewClient(getWebViewClient(hackWebView, episode));
        hackWebView.setWebChromeClient(getWebViewChromeClient(hackWebView, episode));
        hackWebView.loadUrl(episode.getEpisodeCaptchaSolverLink());
    }

    @NotNull
    private WebChromeClient getWebViewChromeClient(AdvancedWebView hackWebView, Episode episode) {
        return new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String consoleMessageString = consoleMessage.message();
                if (consoleMessageString.contains("molvixData")) {
                    try {
                        JSONObject jsonObject = new JSONObject(consoleMessageString);
                        String imageData = jsonObject.optString("imageData");
                        String cleanImage = imageData.replace("data:image/png;base64,", "").replace("data:image/jpeg;base64,", "");
                        if (StringUtils.isNotEmpty(cleanImage)) {
                            loadCaptchaImage(hackWebView, cleanImage);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (consoleMessageString.contains("molvixCaptcha")) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), consoleMessageString);
                    try {
                        JSONObject jsonObject = new JSONObject(consoleMessageString);
                        String bodyString = jsonObject.optString("molvixCaptcha");
                        if (StringUtils.isNotEmpty(bodyString)) {
                            if (bodyString.toLowerCase().contains("Captcha Does Not Match".toLowerCase())
                                    && !bodyString.toLowerCase().contains("Please update your browser first and then try again, it will start working".toLowerCase())) {
                                //Display captcha error
                                MolvixLogger.d(ContentManager.class.getSimpleName(), "Last Captcha was incorrect");
                                lastCaptchaErrorMessage.set("Last Captcha was incorrect.");
                                if (hackWebView.canGoBack()) {
                                    MolvixLogger.d(ContentManager.class.getSimpleName(), "WebView can go back");
                                    hackWebView.goBack();
                                } else {
                                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Sorry, WebView cannot go back");
                                }
                            } else {
                                checkForCaptchaAvailability(hackWebView);
                            }
                        } else {
                            checkForCaptchaAvailability(hackWebView);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        MolvixLogger.d(ContentManager.class.getSimpleName(), e.getMessage());
                    }
                } else if (consoleMessageString.contains("molvixMeta")) {
                    try {
                        JSONObject jsonObject = new JSONObject(consoleMessageString);
                        String bodyString = jsonObject.optString("molvixMeta");
                        if (bodyString.toLowerCase().contains("Webpage not available".toLowerCase())) {
                            unLockAppCaptchaSolver();
                            if (hackWebView.getUrl() != null && !hackWebView.getUrl().contains("google")) {
                                if (!ApplicationLoader.globalDownloadListener.isEnable()) {
                                    if (ConnectivityUtils.isConnected()) {
                                        displayEpisodeDownloadErrorMessage("An error occurred while trying to download <b>" + EpisodesManager.getEpisodeFullName(episode) + "</b>.Please review your data connection", episode);
                                    }
                                } else {
                                    MolvixLogger.d(ContentManager.class.getSimpleName(), "A download error happened but, not to worry it would self fix");
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (consoleMessageString.contains(AppConstants.SOLVE_COMPLEX_CAPTCHA)) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), consoleMessageString);
                    String captchaBase64String = "javascript:(function getBase64StringOfCaptcha() {\n" +
                            "    var pageImgs = document.getElementsByTagName(\"img\");\n" +
                            "    if (pageImgs != undefined) {\n" +
                            "        var pageImgsLength = pageImgs.length;\n" +
                            "        var i;\n" +
                            "        for (i = 0; i < pageImgsLength; i++) {\n" +
                            "            var pageImg = pageImgs[i];\n" +
                            "            var imageSrc = pageImg.src;\n" +
                            "            var targetKeyword = \"captcha\";\n" +
                            "            if (imageSrc.toLowerCase().indexOf(targetKeyword) != -1) {\n" +
                            "                var canvas = document.createElement(\"canvas\");\n" +
                            "                var ctx = canvas.getContext(\"2d\");\n" +
                            "                ctx.drawImage(pageImg, 0, 0);\n" +
                            "                var dataURL = canvas.toDataURL(\"image/png\");\n" +
                            "                var result = {};\n" +
                            "                result[\"imageData\"] = dataURL;\n" +
                            "                result[\"molvixData\"] = \"molvixData\";\n" +
                            "                console.log(JSON.stringify(result));\n" +
                            "                break;\n" +
                            "            }\n" +
                            "        }\n" +
                            "    }\n" +
                            "})();\n";
                    evaluateJavaScript(hackWebView, captchaBase64String);
                } else if (consoleMessageString.contains(AppConstants.NO_COMPLEX_CAPTCHA)) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), consoleMessageString);
                    String continueDownload =
                            "javascript:(function clickCaptchaButton() {\n" +
                                    "    document.getElementsByTagName('input')[0].click();\n" +
                                    "})();";
                    evaluateJavaScript(hackWebView, continueDownload);
                } else if (consoleMessageString.contains(AppConstants.NO_FORM_ELEMENT_FOUND)) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "No Form Element was found");
                    if (hackWebView.canGoBack()) {
                        hackWebView.goBack();
                    } else {
                        hackWebView.reload();
                    }
                }
                return super.onConsoleMessage(consoleMessage);
            }
        };
    }

    @NotNull
    private WebViewClient getWebViewClient(AdvancedWebView hackWebView, Episode episode) {
        return new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "OnPageFinished and url=" + url);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (mimeTypeOfUrl != null) {
                    if (mimeTypeOfUrl.toLowerCase().contains("video")) {
                        lastCaptchaErrorMessage.set("");
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
                        //Just re-route to google to refresh the page
                        hackWebView.loadUrl("https://www.google.come");
                        EpisodesManager.popDownloadableEpisode(episode);
                        return;
                    }
                }
                if (url.toLowerCase().contains("areyouhuman")) {
                    //Check that the last captcha was not wrong
                    String captchaAttackFeasibilityTest = "javascript:(function captchaMatchTest(){\n" +
                            "        var documentBody = document.getElementsByTagName(\"body\")[0].innerHTML;\n" +
                            "        var captchaData={};\n" +
                            "        captchaData[\"molvixCaptcha\"]=documentBody;\n" +
                            "        console.log(JSON.stringify(captchaData));\n" +
                            "    })();";
                    evaluateJavaScript(hackWebView, captchaAttackFeasibilityTest);
                }
                String errorCheck = "\n" +
                        "    javascript:(function docBodyString(){\n" +
                        "                var docBody = document.getElementsByTagName('body')[0];\n" +
                        "                if(docBody!=undefined){\n" +
                        "                    var responseJSON = {};\n" +
                        "                    responseJSON[\"molvixMeta\"]=docBody.innerHTML;\n" +
                        "                    console.log(JSON.stringify(responseJSON));\n" +
                        "                }\n" +
                        "            })();";
                evaluateJavaScript(hackWebView, errorCheck);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "OnPageStarted and url=" + url);
            }

        };

    }

    private void loadCaptchaImage(AdvancedWebView advancedWebView, String cleanImage) {
        if (StringUtils.isNotEmpty(cleanImage)) {
            byte[] decodedString = Base64.decode(cleanImage, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (decodedByte != null) {
                if (!solvableCaptchaDialogShowing.get()) {
                    loadUserSolvableCaptcha(advancedWebView, decodedByte);
                }
            }
        }
    }

    private void displayEpisodeDownloadErrorMessage(String errorMessage, Episode episode) {
        LovelyStandardDialog dialog = new LovelyStandardDialog(this)
                .setTopColorRes(R.color.red800)
                .setTitle("Download Error!")
                .setCancelable(false)
                .setMessage(UiUtils.fromHtml(errorMessage))
                .setPositiveButton("Try Again", view -> {
                    if (ConnectivityUtils.isConnected()) {
                        EventBus.getDefault().post(new CheckForDownloadableEpisodes());
                    } else {
                        UiUtils.snackMessage("Network error!", bottomNavView, true, null, null);
                    }
                }).setNegativeButton(android.R.string.cancel, view -> {
                });
        dialog.show();
        AppPrefs.updateEpisodeDownloadProgress(episode.getEpisodeId(), -1);
        AppPrefs.updateEpisodeDownloadProgressMsg(episode.getEpisodeId(), "");
    }

    @SuppressLint("SetTextI18n")
    private void loadUserSolvableCaptcha(AdvancedWebView advancedWebView, Bitmap bitmap) {
        LovelyTextInputDialog dialog = new LovelyTextInputDialog(this)
                .setTopColorRes(R.color.colorAccentDark)
                .setTitle(R.string.enter_text_shown_title)
                .setCancelable(false)
                .setMessage((StringUtils.isNotEmpty(lastCaptchaErrorMessage.get()) ? "Your last entry was wrong\n" : "") + "Please enter the text shown above to start your pending downloads")
                .setIcon(bitmap)
                .configureEditText(v -> {
                    v.setImeOptions(6);
                    v.setSingleLine(true);
                })
                .setInputFilter(R.string.text_input_error_message, text -> text.matches("\\w+"))
                .setConfirmButton(android.R.string.ok, text -> {
                    if (StringUtils.isNotEmpty(text)) {
                        String injectionString = "javascript: function injectCaptcha(captcha){\n" +
                                "    var form = document.getElementsByTagName(\"form\")[0];\n" +
                                "    var captchaInput = form.elements[0];\n" +
                                "    var captchaButton = form.elements[1];\n" +
                                "    captchaInput.value=captcha;\n" +
                                "    captchaButton.click();\n" +
                                "}\n" +
                                "injectCaptcha(\"" + text + "\");";
                        evaluateJavaScript(advancedWebView, injectionString);
                        solvableCaptchaDialogShowing.set(false);
                    } else {
                        UiUtils.showSafeToast("Please enter the captcha above");
                    }
                }).setNegativeButton(android.R.string.cancel, view -> {
                    solvableCaptchaDialogShowing.set(false);
                    EpisodesManager.unLockCaptchaSolver();
                });
        dialog.show();
        solvableCaptchaDialogShowing.set(true);
    }

    private void observeNewIntent(Intent intent) {
        String invocationType = intent.getStringExtra(AppConstants.INVOCATION_TYPE);
        if (invocationType != null) {
            switch (invocationType) {
                case AppConstants.NAVIGATE_TO_SECOND_FRAGMENT:
                    fragmentsPager.setCurrentItem(1);
                    break;
                case AppConstants.DISPLAY_MOVIE:
                    String movieId = intent.getStringExtra(AppConstants.MOVIE_ID);
                    loadMovieDetails(movieId);
                    break;
                case AppConstants.SHOW_UNFINISHED_DOWNLOADS:
                    checkAndDisplayUnFinishedDownloads(false);
                    break;
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
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof MovieDetailsView) {
            rootContainer.removeViewAt(rootContainerPenultimateFront());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        observeNewIntent(intent);
    }

    public boolean isProgressDialogShowing() {
        return rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof FullScreenDialog;
    }

    public void dismissProgressDialog() {
        FullScreenDialog fullScreenDialog = (FullScreenDialog) rootContainer.getChildAt(rootContainerPenultimateFront());
        fullScreenDialog.dismiss(rootContainer, null);
    }

    @Override
    public void onBackPressed() {
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof FullScreenDialog) {
            FullScreenDialog fullScreenDialog = (FullScreenDialog) rootContainer.getChildAt(rootContainerPenultimateFront());
            fullScreenDialog.dismiss(rootContainer, (result, e) -> canShowLoadedRewardedVideoAd.set(false));
            return;
        }
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof MolvixVideoPlayerView) {
            MolvixVideoPlayerView molvixVideoPlayerView = (MolvixVideoPlayerView) rootContainer.getChildAt(rootContainerPenultimateFront());
            molvixVideoPlayerView.trySaveCurrentPlayerPosition();
            molvixVideoPlayerView.cleanUpVideoView();
            rootContainer.removeViewAt(rootContainerPenultimateFront());
            return;
        }
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof MovieDetailsView) {
            MovieDetailsView movieDetailsView = (MovieDetailsView) rootContainer.getChildAt(rootContainerPenultimateFront());
            if (movieDetailsView.isBottomSheetDialogShowing()) {
                movieDetailsView.closeBottomSheetDialog();
            } else {
                movieDetailsView.removeEpisodeListener();
                rootContainer.removeView(movieDetailsView);
            }
            rootContainer.invalidate();
            rootContainer.requestLayout();
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
        if (HomeFragment.activeLoadMode.get() != HomeFragment.LoadMode.MODE_DEFAULT) {
            EventBus.getDefault().post(new FetchMoviesEvent());
            return;
        }
        super.onBackPressed();
    }

    private void initSearchBox() {
        searchView.setup();
    }

    private void tintNavBar() {
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
        ContentManager.fetchMovieGenres();
        enqueConnectivityCheckWorker();
    }

    private void enqueConnectivityCheckWorker() {
        PeriodicWorkRequest.Builder connectivityPeriodicWorkCheckBuilder = new PeriodicWorkRequest.Builder(ConnectivityCheckWorker.class, TimeUnit.MINUTES.toMillis(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS), TimeUnit.MINUTES);
        connectivityPeriodicWorkCheckBuilder.setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build());
        PeriodicWorkRequest connectivityPeriodicWorkRequest = connectivityPeriodicWorkCheckBuilder.build();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(connectivityPeriodicWorkRequest);
        workManager.getWorkInfoByIdLiveData(connectivityPeriodicWorkRequest.getId()).observe(this, workInfo -> {
            if (workInfo != null) {
                WorkInfo.State state = workInfo.getState();
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Molvix WorkManager State\n" + state.toString());
            }
        });
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
                    if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof NewUpdateAvailableView) {
                        rootContainer.removeViewAt(rootContainerPenultimateFront());
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

    public void playVideo(List<DownloadedVideoItem> downloadedVideoItems, DownloadedVideoItem startItem) {
        if (rootContainer.getChildAt(rootContainerPenultimateFront()) instanceof MolvixVideoPlayerView) {
            rootContainer.removeViewAt(rootContainerPenultimateFront());
        }
        MolvixVideoPlayerView molvixVideoPlayerView = new MolvixVideoPlayerView(this);
        rootContainer.addView(molvixVideoPlayerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Collections.sort(downloadedVideoItems);
        molvixVideoPlayerView.playVideos(downloadedVideoItems, downloadedVideoItems.indexOf(startItem));
    }

    public void loadRewardedVideoAsRequested() {
        canShowLoadedRewardedVideoAd.set(true);
        if (mRewardedVideoAd.isLoaded()) {
            mRewardedVideoAd.show();
        } else {
            showProgressDialog(this, "Loading ad", "Please wait...");
            loadRewardedVideoAdNow();
        }
    }

    public FullScreenDialog showProgressDialog(Context context, String title, String message) {
        return new FullScreenDialog(context).show(rootContainer, title, message);
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        closeGamificationProgressDialog();
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video ad loaded");
        if (canShowLoadedRewardedVideoAd.get()) {
            mRewardedVideoAd.show();
        }
    }

    @Override
    public void onRewardedVideoAdOpened() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video ad opened");
    }

    @Override
    public void onRewardedVideoStarted() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video Load started");
    }

    @Override
    public void onRewardedVideoAdClosed() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video Ad closed");
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video reward with " + rewardItem.getType() + ",amount=" + rewardItem.getAmount());
        AppPrefs.incrementDownloadCoins(12);
        UiUtils.showSafeToast("You have received 12 download coins!!!");
        canShowLoadedRewardedVideoAd.set(false);
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video has left the application");
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
        closeGamificationProgressDialog();
        if (canShowLoadedRewardedVideoAd.get()) {
            UiUtils.showSafeToast("Failed to load video ad.Please review your data connection and try again.");
        }
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video Failed to load due to error code " + i);
    }

    private void closeGamificationProgressDialog() {
        try {
            if (isProgressDialogShowing()) {
                dismissProgressDialog();
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void onRewardedVideoCompleted() {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Rewarded Video Completed");
    }

}