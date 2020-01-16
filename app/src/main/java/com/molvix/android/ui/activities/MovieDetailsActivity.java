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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.molvix.android.R;
import com.molvix.android.beans.MovieContentItem;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.enums.EpisodeQuality;
import com.molvix.android.eventbuses.CheckForPendingDownloadableEpisodes;
import com.molvix.android.eventbuses.EpisodeResolutionEvent;
import com.molvix.android.eventbuses.LoadEpisodesForSeason;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.EpisodesManager;
import com.molvix.android.models.DownloadableEpisodes;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.EpisodesAdapter;
import com.molvix.android.ui.adapters.SeasonsWithEpisodesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.LocalDbUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;

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
    private DirectModelNotifier.ModelChangedListener<Movie> movieModelChangedListener;
    private DirectModelNotifier.ModelChangedListener<DownloadableEpisodes> downloadableEpisodesModelChangedListener;
    private static final String TAG = MovieDetailsActivity.class.getSimpleName();
    private SeasonsWithEpisodesAdapter seasonsWithEpisodesAdapter;
    private String movieId;
    private List<MovieContentItem> movieContentItems = new ArrayList<>();

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
        listenToIncomingDownloadableEpisodes();
        if (movieId != null) {
            loadingLayoutProgressMsgView.setText(getString(R.string.please_wait));
            Movie movie = LocalDbUtils.getMovie(movieId);
            initModelChangeListener();
            List<Season> movieSeasons = movie.getMovieSeasons();
            if (movieSeasons == null || movieSeasons.isEmpty()) {
                spinMoviePullTask();
            } else {
                loadMovieDetails(movie);
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

    private void listenToIncomingDownloadableEpisodes() {
        downloadableEpisodesModelChangedListener = new DirectModelNotifier.ModelChangedListener<DownloadableEpisodes>() {
            @Override
            public void onModelChanged(@NonNull DownloadableEpisodes model, @NonNull BaseModel.Action action) {
                List<Episode> episodes = model.getDownloadableEpisodes();
                if (episodes != null && !episodes.isEmpty()) {
                    if (EpisodesManager.isCaptchaSolvable()) {
                        solveEpisodeCaptchaChallenge(episodes.get(episodes.size() - 1));
                    }
                }
            }

            @Override
            public void onTableChanged(@Nullable Class<?> tableChanged, @NonNull BaseModel.Action action) {

            }
        };
        DirectModelNotifier.get().registerForModelChanges(DownloadableEpisodes.class, downloadableEpisodesModelChangedListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        hackWebView.getSettings().setJavaScriptEnabled(true);
        hackWebView.setCookiesEnabled(true);
        hackWebView.setMixedContentAllowed(true);
        hackWebView.setThirdPartyCookiesEnabled(true);
    }

    private void solveEpisodeCaptchaChallenge(Episode episode) {
        EpisodesManager.lockCaptureSolver(episode.getEpisodeId());
        hackWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectMagicScript();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String mimeTypeOfUrl = FileUtils.getMimeType(url);
                if (mimeTypeOfUrl.toLowerCase().contains("video")) {
                    Log.d(TAG, "Download Url of Video=" + url);
                    UiUtils.showSafeToast("DownloadUrl Of Video=" + url);
                    if (episode.getEpisodeQuality() == EpisodeQuality.STANDARD_QUALITY) {
                        episode.setStandardQualityDownloadLink(url);
                    } else if (episode.getEpisodeQuality() == EpisodeQuality.HIGH_QUALITY) {
                        episode.setHighQualityDownloadLink(url);
                    } else {
                        episode.setLowQualityDownloadLink(url);
                    }
                    episode.update();
                    EpisodesManager.popEpisode(episode);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }

        });
        hackWebView.loadUrl(episode.getEpisodeCaptchaSolverLink());
    }

    private void loadMovieDetails(Movie movie) {
        runOnUiThread(() -> {
            addMovieHeaderView(movie, movieContentItems);
            List<Season> movieSeasons = movie.getMovieSeasons();
            loadInMovieSeasons(movieContentItems, movieSeasons);
//            checkAndLoadInAd(movieContentItems);
            UiUtils.toggleViewVisibility(loadingLayout, false);
        });
    }

    private void checkAndLoadInAd(List<MovieContentItem> movieContentItems) {
        if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            MovieContentItem adItem = new MovieContentItem();
            adItem.setContentType(MovieContentItem.ContentType.AD);
            adItem.setContentId(CryptoUtils.getSha256Digest("AdFooter"));
            if (!movieContentItems.contains(adItem)) {
                movieContentItems.add(adItem);
            } else {
                int indexOfItem = movieContentItems.indexOf(adItem);
                movieContentItems.set(indexOfItem, adItem);
                seasonsWithEpisodesAdapter.notifyItemChanged(indexOfItem);
            }
        }
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
        } else {
            movieContentItems.set(0, movieHeaderItem);
            seasonsWithEpisodesAdapter.notifyItemChanged(0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hackWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        hackWebView.onPause();
    }

    private void initModelChangeListener() {
        movieModelChangedListener = new DirectModelNotifier.ModelChangedListener<Movie>() {
            @Override
            public void onModelChanged(@NonNull Movie model, @NonNull BaseModel.Action action) {
                if (action == BaseModel.Action.UPDATE) {
                    if (movieId != null) {
                        if (movieId.equals(model.getMovieId())) {
                            loadMovieDetails(model);
                        }
                    }
                }
            }

            @Override
            public void onTableChanged(@Nullable Class<?> tableChanged, @NonNull BaseModel.Action action) {

            }
        };
        DirectModelNotifier.get().registerForModelChanges(Movie.class, movieModelChangedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (movieModelChangedListener != null) {
            DirectModelNotifier.get().unregisterForModelChanges(Movie.class, movieModelChangedListener);
        }
        if (downloadableEpisodesModelChangedListener != null) {
            DirectModelNotifier.get().unregisterForModelChanges(DownloadableEpisodes.class, downloadableEpisodesModelChangedListener);
        }
        hackWebView.onDestroy();
    }

    private void spinMoviePullTask() {
        if (moviePullTask != null) {
            moviePullTask.cancel(true);
            moviePullTask = null;
        }
        Movie movie = LocalDbUtils.getMovie(movieId);
        moviePullTask = new MoviePullTask(movie.getMovieLink(), movie);
        moviePullTask.execute();
    }

    @Override
    public void onEventMainThread(Object event) {
        super.onEventMainThread(event);
        if (event instanceof EpisodeResolutionEvent) {
            EpisodeResolutionEvent episodeResolutionEvent = (EpisodeResolutionEvent) event;
            runOnUiThread(() -> hackWebView.loadUrl(episodeResolutionEvent.getEpisode().getEpisodeLink()));
        } else if (event instanceof CheckForPendingDownloadableEpisodes) {
            runOnUiThread(() -> {
                DownloadableEpisodes downloadableEpisodes = SQLite.select()
                        .from(DownloadableEpisodes.class)
                        .querySingle();
                if (downloadableEpisodes != null) {
                    List<Episode> episodeList = downloadableEpisodes.getDownloadableEpisodes();
                    if (episodeList != null && !episodeList.isEmpty()) {
                        solveEpisodeCaptchaChallenge(episodeList.get(episodeList.size() - 1));
                    }
                }
            });
        } else if (event instanceof LoadEpisodesForSeason) {
            runOnUiThread(() -> {
                LoadEpisodesForSeason value = (LoadEpisodesForSeason) event;
                loadEpisodesForSeason(value.getSeason());
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

    private void injectMagicScript() {
        String javascriptCodeInjection =
                "javascript:function clickCaptchaButton(){\n" +
                        "    document.getElementsByTagName('input')[0].click();\n" +
                        "}\n" +
                        "clickCaptchaButton();";
        hackWebView.loadUrl(javascriptCodeInjection);
    }

    static class MoviePullTask extends AsyncTask<Void, Void, Void> {
        private String movieLink;
        private Movie movie;

        MoviePullTask(String movieLink, Movie movie) {
            this.movieLink = movieLink;
            this.movie = movie;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.extractMetaDataFromMovieLink(movieLink, movie);
            return null;
        }

    }

}