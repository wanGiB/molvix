package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
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
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.MainActivityPagerAdapter;
import com.molvix.android.ui.fragments.HomeFragment;
import com.molvix.android.ui.fragments.MoreContentsFragment;
import com.molvix.android.ui.fragments.NotificationsFragment;
import com.molvix.android.ui.widgets.MolvixSearchView;
import com.molvix.android.ui.widgets.MovieDetailsView;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

    private DataSubscription downloadableEpisodesSubscription;
    private MovieDetailsView movieDetailsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initSearchBox();
        initNavBarTints();
        initPager();
        observeNewIntent(getIntent());
        observeDownloadableEpisodes();
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

    private void initPager() {
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
        AdvancedWebView hackWebView = new AdvancedWebView(this);
        hackWebView.getSettings().setJavaScriptEnabled(true);
        hackWebView.setCookiesEnabled(true);
        hackWebView.setMixedContentAllowed(true);
        hackWebView.setThirdPartyCookiesEnabled(true);
        if (rootContainer.getChildAt(0) instanceof AdvancedWebView) {
            rootContainer.removeViewAt(0);
        }
        rootContainer.addView(hackWebView, 0);
        solveEpisodeCaptchaChallenge(hackWebView, episode);
    }

    private void observeDownloadableEpisodes() {
        stopObservingDownloadableEpisodes();
        downloadableEpisodesSubscription = MolvixDB.getDownloadableEpisodeBox().query().build().subscribe().observer(this::updatedDownloadableEpisodes);
    }

    private void stopObservingDownloadableEpisodes() {
        if (downloadableEpisodesSubscription != null && !downloadableEpisodesSubscription.isCanceled()) {
            downloadableEpisodesSubscription.cancel();
            downloadableEpisodesSubscription = null;
        }
    }

    private void updatedDownloadableEpisodes(List<DownloadableEpisode> changedData) {
        if (!changedData.isEmpty()) {
            if (EpisodesManager.isCaptchaSolvable()) {
                hackPage(changedData.get(0).getEpisode());
            }
        }
    }

    private void injectMagicScript(AdvancedWebView hackWebView) {
        String javascriptCodeInjection =
                "javascript:function clickCaptchaButton(){\n" +
                        "    document.getElementsByTagName('input')[0].click();\n" +
                        "}\n" +
                        "clickCaptchaButton();";
        hackWebView.loadUrl(javascriptCodeInjection);
    }

    private void solveEpisodeCaptchaChallenge(AdvancedWebView hackWebView, Episode episode) {
        EpisodesManager.lockCaptchaSolver(episode.getEpisodeId());
        hackWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (!mimeTypeOfUrl.toLowerCase().contains("video")) {
                    injectMagicScript(hackWebView);
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (mimeTypeOfUrl.toLowerCase().contains("video")) {
                    hackWebView.stopLoading();
                    if (episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                        episode.setStandardQualityDownloadLink(url);
                    } else if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY) {
                        episode.setHighQualityDownloadLink(url);
                    } else {
                        episode.setLowQualityDownloadLink(url);
                    }
                    FileDownloadManager.startNewEpisodeDownload(episode);
                    MolvixDB.updateEpisode(episode);
                    hackWebView.onDestroy();
                    rootContainer.removeView(hackWebView);
                    EpisodesManager.popDownloadableEpisode(episode);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                UiUtils.showSafeToast("Sorry, a web view error has occurred ");
            }

        });
        hackWebView.loadUrl(episode.getEpisodeCaptchaSolverLink());
    }

    @Override
    public void onResume() {
        super.onResume();
        observeDownloadableEpisodes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopObservingDownloadableEpisodes();
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
        String searchString = searchView.getText();
        if (StringUtils.isNotEmpty(searchString)) {
            searchView.setText("");
        } else {
            if (movieDetailsView != null) {
                if (movieDetailsView.isBottomSheetDialogShowing()) {
                    movieDetailsView.closeBottomSheetDialog();
                } else {
                    movieDetailsView.removeMovieChangeListener();
                    rootContainer.removeView(movieDetailsView);
                    movieDetailsView = null;
                }
            } else {
                if (fragmentsPager.getCurrentItem() != 0) {
                    fragmentsPager.setCurrentItem(0);
                } else {
                    super.onBackPressed();
                }
            }
        }
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
