package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.molvix.android.R;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.EpisodesAdapter;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

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
    private DataSubscription movieSubscription;
    private Handler mUIHandler = new Handler();
    private BottomSheetDialog bottomSheetDialog;

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
                addMovieChangeListener();
                List<Season> movieSeasons = movie.getSeasons();
                if (movieSeasons == null || movieSeasons.isEmpty()) {
                    spinMoviePullTask();
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
        RecyclerView bottomSheetRecyclerView = rootView.findViewById(R.id.bottom_sheet_recycler_view);
        bottomSheetTitleView.setText(WordUtils.capitalize(season.getSeasonName()));
        bottomSheetRecyclerViewAdapter = new EpisodesAdapter(getContext(), season.getEpisodes());
        LinearLayoutManager bottomSheetLinearLayoutManager = new LinearLayoutManager(getContext());
        bottomSheetRecyclerView.setLayoutManager(bottomSheetLinearLayoutManager);
        bottomSheetRecyclerView.setAdapter(bottomSheetRecyclerViewAdapter);
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
        moviePullTask = new MoviePullTask(movieId);
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
        backButton.setOnClickListener(v -> {
            removeMovieChangeListener();
            ((ViewGroup) getParent()).removeView(MovieDetailsView.this);
        });
    }

    private void addMovieChangeListener() {
        removeMovieChangeListener();
        movieSubscription = MolvixDB.getMovieBox().query().equal(Movie_.movieId, movieId).build().subscribe().observer(data -> {
            if (!data.isEmpty()) {
                Movie firstUpdatedMovie = data.get(0);
                if (firstUpdatedMovie != null && firstUpdatedMovie.getMovieId().equals(movieId) && firstUpdatedMovie.getMovieArtUrl() != null && firstUpdatedMovie.getSeasons() != null && !firstUpdatedMovie.getSeasons().isEmpty()) {
                    loadMovieDetails(firstUpdatedMovie);
                }
            }
        });
    }

    public void removeMovieChangeListener() {
        if (movieSubscription != null && !movieSubscription.isCanceled()) {
            movieSubscription.cancel();
            movieSubscription = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeMovieChangeListener();
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
