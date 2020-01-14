package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.beans.MovieContentItem;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.eventbuses.DownloadEpisodeEvent;
import com.molvix.android.jobs.ContentMiner;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.SeasonsWithEpisodesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.LocalDbUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.delight.android.webview.AdvancedWebView;

public class MovieDetailsActivity extends BaseActivity {

    @BindView(R.id.frontLayout)
    View frontView;

    @BindView(R.id.hack_web_view)
    AdvancedWebView hackWebView;

    @BindView(R.id.seasons_and_episodes_recycler_view)
    RecyclerView seasonsAndEpisodesRecyclerView;

    @BindView(R.id.content_loading_layout)
    View loadingLayout;

    @BindView(R.id.content_loading_progress_msg)
    TextView loadingLayoutProgressMsgView;

    @BindView(R.id.content_loading_progress)
    ProgressBar loadingLayoutProgressBar;

    private Movie movie;
    private MoviePullTask moviePullTask;
    private DirectModelNotifier.ModelChangedListener<Movie> movieModelChangedListener;

    private static AtomicBoolean downloadOptionsPageVisited = new AtomicBoolean(false);
    private static final String TAG = MovieDetailsActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        String movieId = getIntent().getStringExtra(AppConstants.MOVIE_ID);
        if (movieId != null) {
            loadingLayoutProgressMsgView.setText(getString(R.string.please_wait));
            movie = LocalDbUtils.getMovie(movieId);
            initModelChangeListener();
            List<Season> movieSeasons = movie.getMovieSeasons();
            if (movieSeasons == null || movieSeasons.isEmpty()) {
                spinMovePullTask();
            } else {
                loadMovieDetails(movie);
            }
        }
    }

    private void loadMovieDetails(Movie movie) {
        List<MovieContentItem> movieContentItems = new ArrayList<>();
        addMovieHeaderView(movie, movieContentItems);
        List<Season> movieSeasons = movie.getMovieSeasons();
        loadInMovieSeasons(movieContentItems, movieSeasons);
        checkAndLoadInAd(movieContentItems);
        SeasonsWithEpisodesAdapter seasonsWithEpisodesAdapter = new SeasonsWithEpisodesAdapter(this, movieContentItems);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        seasonsAndEpisodesRecyclerView.setLayoutManager(linearLayoutManager);
        seasonsAndEpisodesRecyclerView.setAdapter(seasonsWithEpisodesAdapter);
        UiUtils.toggleViewVisibility(loadingLayout, false);
    }

    private void checkAndLoadInAd(List<MovieContentItem> movieContentItems) {
        if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            MovieContentItem adItem = new MovieContentItem("", new ArrayList<>());
            adItem.setContentType(MovieContentItem.ContentType.AD);
            movieContentItems.add(adItem);
        }
    }

    private void loadInMovieSeasons(List<MovieContentItem> movieContentItems, List<Season> movieSeasons) {
        if (movieSeasons != null && !movieSeasons.isEmpty()) {
            for (Season season : movieSeasons) {
                MovieContentItem movieContentItem = new MovieContentItem(season.getSeasonName(), season.getEpisodes());
                movieContentItem.setContentType(MovieContentItem.ContentType.GROUP_HEADER);
                movieContentItems.add(movieContentItem);
            }
        }
    }

    private void addMovieHeaderView(Movie movie, List<MovieContentItem> movieContentItems) {
        MovieContentItem movieHeaderItem = new MovieContentItem("", new ArrayList<>());
        movieHeaderItem.setMovie(movie);
        movieHeaderItem.setContentType(MovieContentItem.ContentType.MOVIE_HEADER);
        movieContentItems.add(movieHeaderItem);
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
                if (action == BaseModel.Action.UPDATE || action == BaseModel.Action.CHANGE) {
                    if (movie != null) {
                        if (movie.getMovieId().equals(model.getMovieId())) {
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
        hackWebView.onDestroy();
    }

    private void spinMovePullTask() {
        if (moviePullTask != null) {
            moviePullTask.cancel(true);
            moviePullTask = null;
        }
        moviePullTask = new MoviePullTask(movie.getMovieLink(), movie);
        moviePullTask.execute();
    }

    @Override
    public void onEventMainThread(Object event) {
        super.onEventMainThread(event);
        if (event instanceof DownloadEpisodeEvent) {
            DownloadEpisodeEvent downloadEpisodeEvent = (DownloadEpisodeEvent) event;
            runOnUiThread(() -> {
                UiUtils.toggleViewVisibility(frontView, false);
                hackMovieSeasonEpisode(downloadEpisodeEvent.getEpisode());
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void hackMovieSeasonEpisode(Episode episode) {
        hackWebView.getSettings().setJavaScriptEnabled(true);
        hackWebView.setCookiesEnabled(true);
        hackWebView.setMixedContentAllowed(true);
        hackWebView.setThirdPartyCookiesEnabled(true);
        hackWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

        });
        hackWebView.loadUrl(episode.getEpisodeLink());
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
            ContentMiner.extractMetaDataFromMovieLink(movieLink, movie);
            return null;
        }

    }

    static class EpisodeDownloadOptionsExtractionTask extends AsyncTask<Void, Void, Void> {

        private String episodeLink;

        EpisodeDownloadOptionsExtractionTask(String episodeLink) {
            this.episodeLink = episodeLink;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            fetchDownloadOptionsForEpisode(episodeLink);
            return null;
        }

        private void fetchDownloadOptionsForEpisode(String episodeLink) {
            try {
                Document episodeDocument = Jsoup.connect(episodeLink).get();
                //Bring out all href elements containing
                Elements links = episodeDocument.select("a[href]");
                if (links != null && !links.isEmpty()) {
                    List<String> downloadLinks = new ArrayList<>();
                    for (Element link : links) {
                        String episodeFileName = link.text();
                        String episodeDownloadLink = link.attr("href");
                        if (episodeDownloadLink.contains(AppConstants.DOWNLOADABLE)) {
                            Log.d(TAG, episodeFileName + ", " + episodeDownloadLink);
                            downloadLinks.add(episodeDownloadLink);
                        }
                    }
                    if (!downloadLinks.isEmpty()) {
                        downloadOptionsPageVisited.set(true);
                        //Solve Captcha and download episode based on selected
                        //Quality

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
