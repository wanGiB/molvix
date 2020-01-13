package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("unused")
public class MovieView extends FrameLayout {

    @BindView(R.id.movie_name_view)
    LoadingTextView movieNameView;

    @BindView(R.id.movie_art_view)
    LoadingImageView movieArtView;

    @BindView(R.id.movie_description_view)
    LoadingTextView movieDescriptionView;

    @BindView(R.id.movie_seasons_count_view)
    LoadingTextView movieSeasonsCountView;

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
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.movie_view, null);
        ButterKnife.bind(this, view);
        removeAllViews();
        addView(view);
        requestLayout();
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchString() {
        return searchString;
    }

    @SuppressLint("SetTextI18n")
    public void setupMovie(Movie movie) {
        String movieName = movie.getMovieName();
        String movieDescription = movie.getMovieDescription();
        String movieArtUrl = movie.getMovieArtUrl();
        List<Season> movieSeasonsCount = movie.getMovieSeasons();

        loadTextViews();
        if (StringUtils.isNotEmpty(getSearchString())) {
            movieNameView.setText(UiUtils.highlightTextIfNecessary(getSearchString(), WordUtils.capitalize(movieName), ContextCompat.getColor(getContext(), R.color.colorAccent)));
            stopLoading(movieNameView);
        } else {
            movieNameView.setText(WordUtils.capitalize(movieName));
            stopLoading(movieNameView);
        }
        if (StringUtils.isNotEmpty(movieDescription)) {
            movieDescriptionView.setText(StringUtils.capitalize(movieDescription));
            stopLoading(movieDescriptionView);
        }
        if (movieSeasonsCount != null && !movieSeasonsCount.isEmpty()) {
            int seasonsCount = movieSeasonsCount.size();
            movieSeasonsCountView.setText("Seasons " + seasonsCount);
            stopLoading(movieSeasonsCountView);
        }
        if (StringUtils.isNotEmpty(movieArtUrl)) {
            UiUtils.loadImageIntoView(movieArtView, movieArtUrl);
        }
    }

    private void stopLoading(LoadingTextView loadingTextView) {
        loadingTextView.stopLoading();
    }

    private void loadTextViews() {
        movieNameView.startLoading();
        movieDescriptionView.startLoading();
        movieSeasonsCountView.startLoading();
        movieArtView.startLoading();
    }

}
