package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.managers.SeasonsManager;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.ui.adapters.EpisodesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MovieDetailsView extends FrameLayout {

    @BindView(R.id.content_loading_layout)
    View loadingLayout;

    @BindView(R.id.content_loading_progress_msg)
    TextView loadingLayoutProgressMsgView;

    @SuppressWarnings("unused")
    @BindView(R.id.content_loading_progress)
    ProgressBar loadingLayoutProgressBar;

    @BindView(R.id.back_button)
    ImageView backButton;

    @BindView(R.id.seasons_container)
    LinearLayout seasonsContainer;

    private MoviePullTask moviePullTask;

    private Handler mUIHandler = new Handler();
    private BottomSheetDialog bottomSheetDialog;
    private EpisodesAdapter episodesAdapter;

    public MovieDetailsView(@NonNull Context context) {
        super(context);
        initUI(context);
    }

    public MovieDetailsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initUI(context);
    }

    public MovieDetailsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUI(context);
    }

    private void initUI(Context context) {
        @SuppressLint("InflateParams") View detailsView = LayoutInflater.from(context).inflate(R.layout.movie_details_view, null);
        ButterKnife.bind(this, detailsView);
        removeAllViews();
        addView(detailsView);
        requestLayout();
    }

    public void loadMovieDetails(String movieId) {
        initBackButton();
        if (movieId != null) {
            MovieManager.setMovieRefreshable(movieId);
            loadingLayoutProgressMsgView.setText(getContext().getString(R.string.please_wait));
            Movie movie = MolvixDB.getMovie(movieId);
            if (movie != null) {
                List<Season> movieSeasons = movie.getSeasons();
                if (movieSeasons == null || movieSeasons.isEmpty()) {
                    if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                        pullMovieDetailsFromTheInternet(movie);
                    } else {
                        UiUtils.showSafeToast("Please connect to the internet and try again.");
                        closeView();
                    }
                } else {
                    loadMovieDetails(movie);
                }
            }
        }
    }

    private void closeView() {
        if (getContext() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getContext();
            mainActivity.onBackPressed();
        }
    }

    private void updateTheRelevantNotificationKeyHolderForMovie(Movie movie) {
        Notification associatedNotification = MolvixDB.getNotification(movie.getMovieId());
        if (associatedNotification != null) {
            boolean seenStatus = associatedNotification.isSeen();
            if (!seenStatus) {
                associatedNotification.setSeen(true);
                MolvixDB.updateNotification(associatedNotification);
            }
        }
    }

    public void loadEpisodesForSeason(Season season, boolean canShowLoadingProgress) {
        @SuppressLint("InflateParams") View bottomSheetRootView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_content_view, null);
        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(bottomSheetRootView);
        fillInEpisodes(bottomSheetRootView, canShowLoadingProgress, season);
        bottomSheetDialog.show();
    }

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private void fillInEpisodes(View rootView, boolean canShowLoadingProgress, Season season) {
        TextView bottomSheetTitleView = rootView.findViewById(R.id.bottom_sheet_title_view);
        ProgressBar episodesRefreshingProgressBar = rootView.findViewById(R.id.episodes_loading_bar);
        UiUtils.toggleViewVisibility(episodesRefreshingProgressBar, canShowLoadingProgress);
        AdView adView = rootView.findViewById(R.id.adView);
        RecyclerView bottomSheetRecyclerView = rootView.findViewById(R.id.bottom_sheet_recycler_view);
        bottomSheetTitleView.setText(WordUtils.capitalize(season.getSeasonName()));
        List<Episode> seasonEpisodes = season.getEpisodes();
        List<Episode> episodes = new ArrayList<>(seasonEpisodes);
        episodesAdapter = new EpisodesAdapter(getContext(), episodes);
        LinearLayoutManager bottomSheetLinearLayoutManager = new LinearLayoutManager(getContext());
        bottomSheetRecyclerView.setLayoutManager(bottomSheetLinearLayoutManager);
        bottomSheetRecyclerView.setAdapter(episodesAdapter);
        onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.contains(AppConstants.EPISODE_DOWNLOAD_PROGRESS)) {
                String episodeId = key.replace(AppConstants.EPISODE_DOWNLOAD_PROGRESS, "").trim();
                Episode dummyEpisode = new Episode();
                dummyEpisode.setEpisodeId(episodeId);
                if (episodes.contains(dummyEpisode)) {
                    int indexOfEpisode = episodes.indexOf(dummyEpisode);
                    episodesAdapter.notifyItemChanged(indexOfEpisode, episodes.get(indexOfEpisode));
                }
            } else if (key.contains(AppConstants.SEASON_EPISODES_REFRESHED)) {
                String seasonId = key.replace(AppConstants.SEASON_EPISODES_REFRESHED, "").trim();
                if (seasonId.equals(season.getSeasonId())) {
                    if (AppPrefs.isSeasonEpisodesRefreshed(seasonId)) {
                        Season updatedSeason = MolvixDB.getSeason(seasonId);
                        List<Episode> updatedEpisodes = updatedSeason.getEpisodes();
                        for (Episode episode : updatedEpisodes) {
                            if (!episodes.contains(episode)) {
                                episodes.add(episode);
                            }
                        }
                        episodesAdapter.notifyDataSetChanged();
                        SeasonsManager.refreshSeasonEpisodes(updatedSeason, false);
                        episodesRefreshingProgressBar.setVisibility(GONE);
                    }
                }
            }
        };
        AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        loadBannerAd(adView);
    }

    private void loadBannerAd(AdView adView) {
        AdRequest.Builder adBuilder = new AdRequest.Builder();
        AdRequest adRequest = adBuilder.build();
        adView.setAdListener(new AdListener() {

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);
                MolvixLogger.d(ContentManager.class.getSimpleName(), "Banner ad failed to load with error code " + errorCode);
            }

        });
        adView.loadAd(adRequest);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (episodesAdapter != null) {
            episodesAdapter.notifyDataSetChanged();
        }
        if (onSharedPreferenceChangeListener != null) {
            AppPrefs.getAppPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeEpisodeListener();
    }

    public void removeEpisodeListener() {
        if (onSharedPreferenceChangeListener != null) {
            AppPrefs.getAppPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            onSharedPreferenceChangeListener = null;
        }
    }

    private void loadMovieDetails(Movie movie) {
        mUIHandler.post(() -> {
            loadInMovie(movie);
            UiUtils.toggleViewVisibility(loadingLayout, false);
            if (!movie.isRecommendedToUser() && movie.getSeasons() != null && !movie.getSeasons().isEmpty()) {
                movie.setRecommendedToUser(true);
                MolvixDB.updateMovie(movie);
            }
            updateTheRelevantNotificationKeyHolderForMovie(movie);
        });
    }

    private void pullMovieDetailsFromTheInternet(Movie movie) {
        cancelPreviousMovieFetchTask();
        moviePullTask = new MoviePullTask(movie, (result, e) -> {
            if (result != null && e == null) {
                loadMovieDetails(result);
            }
        });
        moviePullTask.execute();
    }

    private void cancelPreviousMovieFetchTask() {
        if (moviePullTask != null) {
            moviePullTask.cancel(true);
            moviePullTask = null;
        }
    }

    public boolean isBottomSheetDialogShowing() {
        return bottomSheetDialog != null && bottomSheetDialog.isShowing();
    }

    public void closeBottomSheetDialog() {
        bottomSheetDialog.dismiss();
        bottomSheetDialog.cancel();
        bottomSheetDialog = null;
    }

    private void loadInMovie(Movie movie) {
        seasonsContainer.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.light_grey));
        seasonsContainer.removeAllViews();
        MovieDetailsHeaderView movieDetailsHeaderView = new MovieDetailsHeaderView(getContext());
        movieDetailsHeaderView.bindMovieHeader(movie);
        seasonsContainer.addView(movieDetailsHeaderView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        List<Season> movieSeasons = movie.getSeasons();
        for (Season season : movieSeasons) {
            SeasonView seasonView = new SeasonView(getContext());
            SeasonsManager.setSeasonRefreshable(season.getSeasonId());
            seasonView.bindSeason(season);
            seasonsContainer.addView(seasonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (AppConstants.unifiedNativeAdAtomicReference.get() != null) {
            AdMobNativeAdView adMobNativeAdView = new AdMobNativeAdView(getContext());
            seasonsContainer.addView(adMobNativeAdView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            adMobNativeAdView.loadInAd(AppConstants.unifiedNativeAdAtomicReference.get());
        }
    }

    private void initBackButton() {
        backButton.setOnClickListener(v -> ((ViewGroup) getParent()).removeView(MovieDetailsView.this));
    }

    static class MoviePullTask extends AsyncTask<Void, Void, Void> {

        private Movie movie;
        private DoneCallback<Movie> movieDoneLoadingCallBack;

        MoviePullTask(Movie movie, DoneCallback<Movie> movieDoneLoadingCallBack) {
            this.movie = movie;
            this.movieDoneLoadingCallBack = movieDoneLoadingCallBack;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.extractMovieMetaData(movie, (result, e) -> movieDoneLoadingCallBack.done(result, e));
            return null;
        }
    }

}
