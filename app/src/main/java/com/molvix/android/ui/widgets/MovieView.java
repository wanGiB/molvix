package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.managers.ThemeManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.activities.MainActivity;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MovieView extends FrameLayout {

    @BindView(R.id.parent_card_view)
    View parentCardView;

    @BindView(R.id.movie_name_view)
    TextView movieNameView;

    @BindView(R.id.new_movie_indicator)
    TextView newMovieIndicatorView;

    @BindView(R.id.movie_art_view)
    ImageView movieArtView;

    @BindView(R.id.movie_description_view)
    TextView movieDescriptionView;

    @BindView(R.id.movie_seasons_count_view)
    TextView movieSeasonsCountView;

    @BindView(R.id.movie_genre_view)
    TextView movieGenreView;

    private Movie movie;

    private String searchString;

    public MovieView(Context context) {
        super(context);
        init(context);
    }

    public MovieView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MovieView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        removeAllViews();
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.movie_view, null);
        ButterKnife.bind(this, view);
        addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchString() {
        return searchString;
    }

    @SuppressLint("SetTextI18n")
    public void setupMovie(Movie movie) {
        this.movie = movie;
        setupMovieCoreData(movie);
        initEventHandlers(movie);
        refreshMovieDetails(movie);
    }

    public Movie getMovie() {
        return movie;
    }

    private void initEventHandlers(Movie movie) {
        parentCardView.setOnClickListener(v -> {
            UiUtils.blinkView(parentCardView);
            openMovieDetails(movie);
        });
        OnClickListener onClickListener = v -> parentCardView.performClick();
        setOnClickListener(onClickListener);
        movieArtView.setOnClickListener(onClickListener);
        movieNameView.setOnClickListener(onClickListener);
        movieDescriptionView.setOnClickListener(onClickListener);
        movieSeasonsCountView.setOnClickListener(onClickListener);
    }

    private String capitalizeGenre(String genreString) {
        StringBuilder genresBuilder = new StringBuilder();
        String[] parts = genreString.split(",");
        for (String p : parts) {
            genresBuilder.append(WordUtils.capitalize(p)).append(",");
        }
        return StringUtils.removeEnd(genresBuilder.toString(),",");
    }

    @SuppressLint("SetTextI18n")
    private void setupMovieCoreData(Movie movie) {
        UiUtils.toggleViewVisibility(newMovieIndicatorView, movie.isNewMovie());
        String movieName = movie.getMovieName();
        String movieDescription = movie.getMovieDescription();
        String movieArtUrl = movie.getMovieArtUrl();
        String movieGenre = movie.getMovieGenre();
        if (StringUtils.isNotEmpty(movieGenre)) {
            UiUtils.toggleViewVisibility(movieGenreView, true);
            if (StringUtils.isNotEmpty(getSearchString())) {
                movieGenreView.setText(UiUtils.highlightTextIfNecessary(getSearchString(), capitalizeGenre(movieGenre), ContextCompat.getColor(getContext(), R.color.colorAccentDark)));
            } else {
                movieGenreView.setText(capitalizeGenre(movieGenre));
            }
        } else {
            UiUtils.toggleViewVisibility(movieGenreView, false);
        }
        List<Season> movieSeasonsCount = movie.getSeasons();
        if (StringUtils.isNotEmpty(getSearchString())) {
            movieNameView.setText(UiUtils.highlightTextIfNecessary(getSearchString(), WordUtils.capitalize(movieName), ContextCompat.getColor(getContext(), R.color.colorAccentDark)));
        } else {
            movieNameView.setText(WordUtils.capitalize(movieName));
        }
        if (StringUtils.isNotEmpty(movieDescription)) {
            if (StringUtils.isNotEmpty(getSearchString())) {
                movieDescriptionView.setText(UiUtils.highlightTextIfNecessary(getSearchString(), StringUtils.capitalize(movieDescription), ContextCompat.getColor(getContext(), R.color.colorAccentDark)));
            } else {
                movieDescriptionView.setText(StringUtils.capitalize(movieDescription));
            }
        } else {
            movieDescriptionView.setText("");
        }
        if (movieSeasonsCount != null && !movieSeasonsCount.isEmpty()) {
            int seasonsCount = movieSeasonsCount.size();
            String pluralizer = seasonsCount == 1 ? " Season" : " Seasons";
            movieSeasonsCountView.setText(seasonsCount + pluralizer);
        } else {
            movieSeasonsCountView.setText("");
        }
        ThemeManager.ThemeSelection themeSelection = ThemeManager.getThemeSelection();
        if (StringUtils.isNotEmpty(movieArtUrl)) {
            UiUtils.loadImageIntoView(movieArtView, movieArtUrl);
        } else {
            movieArtView.setImageDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), themeSelection == ThemeManager.ThemeSelection.DARK ? R.color.dracula_surface_color : R.color.icons_unselected_color)));
        }
    }

    private void openMovieDetails(Movie movie) {
        if (getContext() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getContext();
            mainActivity.loadMovieDetails(movie.getMovieId());
        }
    }

    private void refreshMovieDetails(Movie movie) {
        if (MovieManager.canFetchMovieDetails(movie.getMovieId()) && ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            MovieMetadataExtractionTask movieMetadataExtractionTask = new MovieMetadataExtractionTask();
            movieMetadataExtractionTask.execute(movie);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshMovieDetails(movie);
    }

    static class MovieMetadataExtractionTask extends AsyncTask<Movie, Void, Void> {

        @Override
        protected Void doInBackground(Movie... movies) {
            ContentManager.extractMovieMetaData(movies[0]);
            return null;
        }

    }

}
