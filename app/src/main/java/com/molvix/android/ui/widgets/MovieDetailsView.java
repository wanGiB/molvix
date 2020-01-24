package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.molvix.android.R;
import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Episode_;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.EpisodesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.objectbox.reactive.DataSubscription;

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

    private String movieId;
    private Handler mUIHandler = new Handler();
    private BottomSheetDialog bottomSheetDialog;
    private DataSubscription episodesUpdateSubscription;

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
        this.movieId = movieId;
        initBackButton();
        if (movieId != null) {
            MovieManager.setMovieRefreshable(movieId);
            loadingLayoutProgressMsgView.setText(getContext().getString(R.string.please_wait));
            Movie movie = MolvixDB.getMovie(movieId);
            if (movie != null) {
                List<Season> movieSeasons = movie.getSeasons();
                if (movieSeasons == null || movieSeasons.isEmpty()) {
                    if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                        spinMoviePullTask();
                    } else {
                        UiUtils.showSafeToast("Please connect to the internet and try again.");
                    }
                } else {
                    loadMovieDetails(movie);
                }
            }
        }
    }

    public void loadEpisodesForSeason(Season season) {
        @SuppressLint("InflateParams") View bottomSheetRootView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_content_view, null);
        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(bottomSheetRootView);
        fillInEpisodes(bottomSheetRootView, season);
        bottomSheetDialog.show();
    }

    private void fillInEpisodes(View rootView, Season season) {
        EpisodesAdapter bottomSheetRecyclerViewAdapter;
        TextView bottomSheetTitleView = rootView.findViewById(R.id.bottom_sheet_title_view);
        Button randomEpisodeMutator = rootView.findViewById(R.id.random_episode_mutator);
        RecyclerView bottomSheetRecyclerView = rootView.findViewById(R.id.bottom_sheet_recycler_view);
        bottomSheetTitleView.setText(WordUtils.capitalize(season.getSeasonName()));
        List<Episode> seasonEpisodes = season.getEpisodes();
        bottomSheetRecyclerViewAdapter = new EpisodesAdapter(getContext(), seasonEpisodes);
        LinearLayoutManager bottomSheetLinearLayoutManager = new LinearLayoutManager(getContext());
        bottomSheetRecyclerView.setLayoutManager(bottomSheetLinearLayoutManager);
        bottomSheetRecyclerView.setAdapter(bottomSheetRecyclerViewAdapter);
        String[] episodeIds = new String[seasonEpisodes.size()];
        for (int i = 0; i < episodeIds.length; i++) {
            episodeIds[i] = seasonEpisodes.get(i).getEpisodeId();
        }
        episodesUpdateSubscription = MolvixDB.getEpisodeBox()
                .query().in(Episode_.episodeId, episodeIds)
                .build()
                .subscribe()
                .observer(data -> {
                    if (!data.isEmpty()) {
                        for (Episode updatedEpisode : data) {
                            if (seasonEpisodes.contains(updatedEpisode)) {
                                int indexOfEpisode = seasonEpisodes.indexOf(updatedEpisode);
                                seasonEpisodes.set(indexOfEpisode, updatedEpisode);
                                bottomSheetRecyclerViewAdapter.notifyItemChanged(indexOfEpisode);
                            }
                        }
                    }
                });
        randomEpisodeMutator.setOnClickListener(v -> {
            int randomEpisodeIndex = new SecureRandom().nextInt(seasonEpisodes.size());
            Episode randomEpisode = seasonEpisodes.get(randomEpisodeIndex);
            randomEpisode.setDownloadProgress(new Random().nextInt(100));
            MolvixDB.updateEpisode(randomEpisode);
        });
    }

    private void loadMovieDetails(Movie movie) {
        mUIHandler.post(() -> {
            loadInMovie(movie);
            UiUtils.toggleViewVisibility(loadingLayout, false);
            if (!movie.isRecommendedToUser() && movie.getSeasons() != null && !movie.getSeasons().isEmpty()) {
                movie.setRecommendedToUser(true);
                MolvixDB.updateMovie(movie);
            }
        });
    }

    private void spinMoviePullTask() {
        if (moviePullTask != null) {
            moviePullTask.cancel(true);
            moviePullTask = null;
        }
        moviePullTask = new MoviePullTask(movieId, (result, e) -> {
            if (result != null && e == null) {
                loadMovieDetails(result);
            }
        });
        moviePullTask.execute();
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
            seasonView.bindSeason(season);
            seasonsContainer.addView(seasonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void initBackButton() {
        backButton.setOnClickListener(v -> ((ViewGroup) getParent()).removeView(MovieDetailsView.this));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeEpisodeListener();
    }

    public void removeEpisodeListener() {
        if (episodesUpdateSubscription != null && !episodesUpdateSubscription.isCanceled()) {
            episodesUpdateSubscription.cancel();
            episodesUpdateSubscription = null;
        }
    }

    static class MoviePullTask extends AsyncTask<Void, Void, Void> {

        private String movieId;
        private DoneCallback<Movie> movieDoneLoadingCallBack;

        MoviePullTask(String movieId, DoneCallback<Movie> movieDoneLoadingCallBack) {
            this.movieId = movieId;
            this.movieDoneLoadingCallBack = movieDoneLoadingCallBack;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Movie movie = MolvixDB.getMovie(movieId);
            if (movie != null) {
                ContentManager.extractMetaDataFromMovieLink(movie.getMovieLink(), movie.getMovieId(), (result, e) -> movieDoneLoadingCallBack.done(result, e));
            }
            return null;
        }
    }

}
