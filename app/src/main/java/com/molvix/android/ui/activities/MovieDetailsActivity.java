package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.molvix.android.R;
import com.molvix.android.beans.MovieContentItem;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.managers.FileDownloadManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.models.DownloadableEpisode;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.EpisodesAdapter;
import com.molvix.android.ui.adapters.SeasonsWithEpisodesAdapter;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.delight.android.webview.AdvancedWebView;

public class MovieDetailsActivity extends BaseActivity {

    @BindView(R.id.hack_web_view)
    AdvancedWebView hackWebView;

    @BindView(R.id.seasons_and_episodes_recycler_view)
    RecyclerView seasonsAndEpisodesRecyclerView;

    @BindView(R.id.content_loading_layout)
    View loadingLayout;

    @BindView(R.id.content_loading_progress_msg)
    TextView loadingLayoutProgressMsgView;

    @SuppressWarnings("unused")
    @BindView(R.id.content_loading_progress)
    ProgressBar loadingLayoutProgressBar;

    @BindView(R.id.back_button)
    ImageView backButton;

    private MoviePullTask moviePullTask;

    private SeasonsWithEpisodesAdapter seasonsWithEpisodesAdapter;
    private String movieId;
    private List<MovieContentItem> movieContentItems = new ArrayList<>();
    private AtomicReference<String> currentEpisodeRef = new AtomicReference<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        initWebView();
        initBackButton();
        movieId = getIntent().getStringExtra(AppConstants.MOVIE_ID);
        cleanUpMovieContentItems();
        initMovieAdapter();
        addDownloadableEpisodesChangeListener();
        if (movieId != null) {
            MovieManager.setMovieRefreshable(movieId);
            loadingLayoutProgressMsgView.setText(getString(R.string.please_wait));
            Movie movie = MolvixDB.getMovie(movieId);
            initModelChangeListener();
            if (movie != null) {
                List<Season> movieSeasons = movie.getSeasons();
                if (movieSeasons == null || movieSeasons.isEmpty()) {
                    spinMoviePullTask();
                } else {
                    loadMovieDetails(movie);
                }
            }
        }
    }

    private void cleanUpMovieContentItems() {
        if (!movieContentItems.isEmpty()) {
            movieContentItems.clear();
        }
    }

    private void initMovieAdapter() {
        seasonsWithEpisodesAdapter = new SeasonsWithEpisodesAdapter(MovieDetailsActivity.this, movieContentItems);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MovieDetailsActivity.this, RecyclerView.VERTICAL, false);
        seasonsAndEpisodesRecyclerView.setLayoutManager(linearLayoutManager);
        seasonsAndEpisodesRecyclerView.setAdapter(seasonsWithEpisodesAdapter);
    }

    private void initBackButton() {
        backButton.setOnClickListener(v -> finish());
    }

    private void addDownloadableEpisodesChangeListener() {

    }

    private void loadUpdatedDownloadableEpisodes(List<DownloadableEpisode> changedData) {
        if (!changedData.isEmpty()) {
            if (EpisodesManager.isCaptchaSolvable()) {
                DownloadableEpisode downloadableEpisode = changedData.get(0);
                currentEpisodeRef.set(downloadableEpisode.getDownloadableEpisodeId());
                solveEpisodeCaptchaChallenge(downloadableEpisode.getEpisode());
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        hackWebView.getSettings().setJavaScriptEnabled(true);
        hackWebView.setCookiesEnabled(true);
        hackWebView.setMixedContentAllowed(true);
        hackWebView.setThirdPartyCookiesEnabled(true);
    }

    private void solveEpisodeCaptchaChallenge(Episode episode) {
        EpisodesManager.lockCaptchaSolver(episode.getEpisodeId());
        hackWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(ContentManager.class.getSimpleName(), "onPageFinished=" + url);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (!mimeTypeOfUrl.toLowerCase().contains("video")) {
                    injectMagicScript();
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(ContentManager.class.getSimpleName(), "onPageStarted=" + url);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (mimeTypeOfUrl.toLowerCase().contains("video")) {
                    UiUtils.showSafeToast("DownloadUrl Of Video=" + url);
                    hackWebView.stopLoading();
                    FileDownloadManager.startNewEpisodeDownload(episode);
                    if (episode.getEpisodeQuality() == AppConstants.STANDARD_QUALITY) {
                        episode.setStandardQualityDownloadLink(url);
                    } else if (episode.getEpisodeQuality() == AppConstants.HIGH_QUALITY) {
                        episode.setHighQualityDownloadLink(url);
                    } else {
                        episode.setLowQualityDownloadLink(url);
                    }
                    MolvixDB.updateEpisode(episode);
                    EpisodesManager.popEpisode(episode);
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

    private void loadMovieDetails(Movie movie) {
        runOnUiThread(() -> {
            addMovieHeaderView(movie, movieContentItems);
            List<Season> movieSeasons = movie.getSeasons();
            loadInMovieSeasons(movieContentItems, movieSeasons);
            UiUtils.toggleViewVisibility(loadingLayout, false);
            movie.setRecommendedToUser(true);
            MolvixDB.updateMovie(movie);
        });
    }

    private void loadInMovieSeasons(List<MovieContentItem> movieContentItems, List<Season> movieSeasons) {
        if (movieSeasons != null && !movieSeasons.isEmpty()) {
            for (Season season : movieSeasons) {
                MovieContentItem movieContentItem = new MovieContentItem();
                movieContentItem.setSeason(season);
                movieContentItem.setContentId(CryptoUtils.getSha256Digest(season.getSeasonName()));
                movieContentItem.setContentType(MovieContentItem.ContentType.GROUP_HEADER);
                if (!movieContentItems.contains(movieContentItem)) {
                    movieContentItems.add(movieContentItem);
                    seasonsWithEpisodesAdapter.notifyItemInserted(movieContentItems.size() - 1);
                } else {
                    int indexOfItem = movieContentItems.indexOf(movieContentItem);
                    movieContentItems.set(indexOfItem, movieContentItem);
                    seasonsWithEpisodesAdapter.notifyItemChanged(indexOfItem);
                }
            }
        }
    }

    private void addMovieHeaderView(Movie movie, List<MovieContentItem> movieContentItems) {
        MovieContentItem movieHeaderItem = new MovieContentItem();
        movieHeaderItem.setMovie(movie);
        movieHeaderItem.setContentId(CryptoUtils.getSha256Digest("MovieHeader"));
        movieHeaderItem.setContentType(MovieContentItem.ContentType.MOVIE_HEADER);
        if (!movieContentItems.contains(movieHeaderItem)) {
            movieContentItems.add(movieHeaderItem);
            seasonsWithEpisodesAdapter.notifyItemInserted(movieContentItems.size() - 1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hackWebView.onPause();
    }

    private void initModelChangeListener() {
        addMovieChangeListener();
    }

    private void addMovieChangeListener() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeMovieChangeListener();
        unLockCaptchaChallenge();
    }

    private void removeMovieChangeListener() {

    }

    private void spinMoviePullTask() {
        if (moviePullTask != null) {
            moviePullTask.cancel(true);
            moviePullTask = null;
        }
        moviePullTask = new MoviePullTask(movieId);
        moviePullTask.execute();
    }

    @Override
    public void onEventMainThread(Object event) {
        super.onEventMainThread(event);
        if (event instanceof LoadEpisodesForSeason) {
            runOnUiThread(() -> {
                LoadEpisodesForSeason seasonData = (LoadEpisodesForSeason) event;
                String seasonId = seasonData.getSeasonId();
                Season seasonToLoad = MolvixDB.getSeason(seasonId);
                if (seasonToLoad != null) {
                    loadEpisodesForSeason(seasonToLoad);
                }
            });
        }
    }

    private void loadEpisodesForSeason(Season season) {
        @SuppressLint("InflateParams") View bottomSheetRootView = getLayoutInflater().inflate(R.layout.bottom_sheet_content_view, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetRootView);
        fillInEpisodes(bottomSheetRootView, season);
        bottomSheetDialog.show();
    }

    private void fillInEpisodes(View rootView, Season season) {
        EpisodesAdapter bottomSheetRecyclerViewAdapter;
        TextView bottomSheetTitleView = rootView.findViewById(R.id.bottom_sheet_title_view);
        RecyclerView bottomSheetRecyclerView = rootView.findViewById(R.id.bottom_sheet_recycler_view);
        bottomSheetTitleView.setText(WordUtils.capitalize(season.getSeasonName()));
        bottomSheetRecyclerViewAdapter = new EpisodesAdapter(this, season.getEpisodes());
        LinearLayoutManager bottomSheetLinearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        bottomSheetRecyclerView.setLayoutManager(bottomSheetLinearLayoutManager);
        bottomSheetRecyclerView.setAdapter(bottomSheetRecyclerViewAdapter);
    }

    @Override
    public void finish() {
        unLockCaptchaChallenge();
        hackWebView.stopLoading();
        super.finish();
    }

    private void unLockCaptchaChallenge() {
        String currentEpisodeId = currentEpisodeRef.get();
        if (currentEpisodeId != null) {
            EpisodesManager.unLockCaptureSolver(currentEpisodeId);
        }
    }

    private void injectMagicScript() {
        String javascriptCodeInjection =
                "javascript:function clickCaptchaButton(){\n" +
                        "    document.getElementsByTagName('input')[0].click();\n" +
                        "}\n" +
                        "clickCaptchaButton();";
        hackWebView.loadUrl(javascriptCodeInjection);
    }

    static class MoviePullTask extends AsyncTask<Void, Void, Void> {

        private String movieId;

        MoviePullTask(String movieId) {
            this.movieId = movieId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Movie movie = MolvixDB.getMovie(movieId);
            if (movie != null) {
                ContentManager.extractMetaDataFromMovieLink(movie.getMovieLink(), movie.getMovieId());
            }
            return null;
        }
    }

}