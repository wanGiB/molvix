package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.delight.android.webview.AdvancedWebView;
import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class MainActivity extends BaseActivity {

    @BindView(R.id.search_box_outer_container)
    View searchBoxOuterContainer;

    @BindView(R.id.search_box)
    EditText searchBox;

    @BindView(R.id.close_search)
    ImageView closeSearchView;

    @BindView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavView;

    @BindView(R.id.hack_web_view)
    AdvancedWebView hackWebView;

    private NavController navController;

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        realm = Realm.getDefaultInstance();
        initSearchBox();
        initNavBarTints();
        initWebView();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavView, navController);
        initEventHandlers();
        checkForNewIntent();
        listenToIncomingDownloadableEpisodes();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        hackWebView.getSettings().setJavaScriptEnabled(true);
        hackWebView.setCookiesEnabled(true);
        hackWebView.setMixedContentAllowed(true);
        hackWebView.setThirdPartyCookiesEnabled(true);
    }

    @SuppressWarnings("ConstantConditions")
    private void listenToIncomingDownloadableEpisodes() {
        RealmResults<DownloadableEpisode> downloadableEpisodes = realm.where(DownloadableEpisode.class).findAllAsync();
        RealmChangeListener<RealmResults<DownloadableEpisode>> downloadableEpisodesChangeListener = results -> {
            if (!results.isEmpty()) {
                if (EpisodesManager.isCaptchaSolvable()) {
                    solveEpisodeCaptchaChallenge(results.get(0).getDownloadableEpisode());
                }
            }
        };
        downloadableEpisodes.removeAllChangeListeners();
        downloadableEpisodes.addChangeListener(downloadableEpisodesChangeListener);
    }

    private void injectMagicScript() {
        String javascriptCodeInjection =
                "javascript:function clickCaptchaButton(){\n" +
                        "    document.getElementsByTagName('input')[0].click();\n" +
                        "}\n" +
                        "clickCaptchaButton();";
        hackWebView.loadUrl(javascriptCodeInjection);
    }

    private void solveEpisodeCaptchaChallenge(Episode episode) {
        EpisodesManager.lockCaptchaSolver(episode.getEpisodeId());
        hackWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (!mimeTypeOfUrl.toLowerCase().contains("video")) {
                    injectMagicScript();
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (mimeTypeOfUrl.toLowerCase().contains("video")) {
                    hackWebView.stopLoading();
                    realm.executeTransaction(r -> {
                        Episode updatableEpisode = r.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episode.getEpisodeId()).findFirst();
                        if (updatableEpisode != null) {
                            if (updatableEpisode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                                updatableEpisode.setStandardQualityDownloadLink(url);
                            } else if (updatableEpisode.getEpisodeQuality() == AppConstants.HIGH_QUALITY) {
                                updatableEpisode.setHighQualityDownloadLink(url);
                            } else {
                                updatableEpisode.setLowQualityDownloadLink(url);
                            }
                            r.copyToRealmOrUpdate(updatableEpisode, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                            EpisodesManager.popEpisode(updatableEpisode);
                        }
                    });
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
        hackWebView.onResume();
        listenToIncomingDownloadableEpisodes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hackWebView.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        hackWebView.onPause();
    }

    private void checkForNewIntent() {
        String invocationType = getIntent().getStringExtra(AppConstants.INVOCATION_TYPE);
        if (invocationType != null && invocationType.equals(AppConstants.NAVIGATE_TO_SECOND_FRAGMENT)) {
            navController.navigate(R.id.navigation_notification);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String invocationType = intent.getStringExtra(AppConstants.INVOCATION_TYPE);
        if (invocationType != null && invocationType.equals(AppConstants.NAVIGATE_TO_SECOND_FRAGMENT)) {
            navController.navigate(R.id.navigation_notification);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initEventHandlers() {
        searchBox.setOnClickListener(v -> searchBox.setCursorVisible(true));
        searchBox.setOnTouchListener((v, event) -> {
            if (!searchBox.isCursorVisible()) {
                searchBox.setCursorVisible(true);
            }
            return false;
        });
        searchBoxOuterContainer.setOnClickListener(v -> searchBox.performClick());
        searchBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                searchBox.setCursorVisible(false);
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.navigation_home) {
                    navController.navigate(R.id.navigation_home);
                }
                String searchedString = s.toString();
                EventBus.getDefault().post(new SearchEvent(searchedString));
                UiUtils.toggleViewVisibility(closeSearchView, StringUtils.isNotEmpty(searchedString));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        closeSearchView.setOnClickListener(v -> searchBox.setText(""));
    }

    @Override
    public void onBackPressed() {
        String searchString = searchBox.getText().toString().trim();
        if (StringUtils.isNotEmpty(searchString)) {
            searchBox.setText("");
        } else {
            super.onBackPressed();
        }
    }

    private void initSearchBox() {
        VectorDrawableCompat searchIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_search_fair_white_24dp, null);
        searchBox.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null);
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
