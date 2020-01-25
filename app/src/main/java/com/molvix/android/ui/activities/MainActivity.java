package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.adapters.MainActivityPagerAdapter;
import com.molvix.android.ui.fragments.HomeFragment;
import com.molvix.android.ui.fragments.MoreContentsFragment;
import com.molvix.android.ui.fragments.NotificationsFragment;
import com.molvix.android.ui.widgets.MolvixSearchView;
import com.molvix.android.ui.widgets.MovieDetailsView;
import com.molvix.android.utils.FileUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.delight.android.webview.AdvancedWebView;

public class MainActivity extends BaseActivity {

    @BindView(R.id.search_view)
    MolvixSearchView searchView;

    @BindView(R.id.fragment_pager)
    ViewPager fragmentsPager;

    @BindView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavView;

    @BindView(R.id.container)
    FrameLayout rootContainer;

    private MovieDetailsView movieDetailsView;

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
    }

    private void unLockAppCaptchaSolver() {
        EpisodesManager.unLockCaptureSolver();
    }

    private void checkAndResumePausedDownloads() {
        Set<String> pausedDownloads = AppPrefs.getInProgressDownloads();
        if (!pausedDownloads.isEmpty()) {
            for (String episodeId : pausedDownloads) {
                Episode episode = MolvixDB.getEpisode(episodeId);
                if (episode != null && AppPrefs.getEpisodeDownloadProgress(episodeId) == -1) {
                    FileDownloadManager.downloadEpisode(episode);
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
                if (seasonToLoad != null && movieDetailsView != null) {
                    movieDetailsView.loadEpisodesForSeason(seasonToLoad);
                }
            }
        });
    }

    private void setupViewPager() {
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new HomeFragment());
        fragments.add(new NotificationsFragment());
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
            } else {
                fragmentsPager.setCurrentItem(2);
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
            hackWebView.evaluateJavascript(javascriptCodeInjection, value -> Log.d(ContentManager.class.getSimpleName(), "Result after evaluating JavaScript=" + value));
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
                if (url.toLowerCase().contains("areyouhuman")) {
                    injectMagicScript(hackWebView);
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
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
            }

        });
        hackWebView.loadUrl(episode.getEpisodeCaptchaSolverLink());
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchDownloadableEpisodes();
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
        movieDetailsView = new MovieDetailsView(this);
        checkAndRemovePreviousMovieDetailsView();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addNewMovieDetailsViewAndLoad(movieId, layoutParams);
    }

    private void addNewMovieDetailsViewAndLoad(String movieId, FrameLayout.LayoutParams layoutParams) {
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
        if (movieDetailsView != null) {
            if (movieDetailsView.isBottomSheetDialogShowing()) {
                movieDetailsView.closeBottomSheetDialog();
            } else {
                movieDetailsView.removeEpisodeListener();
                rootContainer.removeView(movieDetailsView);
                movieDetailsView = null;
            }
            return;
        }
        String searchString = searchView.getText();
        if (StringUtils.isNotEmpty(searchString)) {
            searchView.setText("");
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
        ColorStateList iconsColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.grey500), Color.BLACK});

        ColorStateList textColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.grey500), Color.BLACK});
        bottomNavView.setItemIconTintList(iconsColorStates);
        bottomNavView.setItemTextColor(textColorStates);
    }

}
