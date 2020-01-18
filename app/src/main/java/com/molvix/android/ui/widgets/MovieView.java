package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.activities.MovieDetailsActivity;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;

@SuppressWarnings("unused")
public class MovieView extends FrameLayout {

    @BindView(R.id.parent_card_view)
    View parentCardView;

    @BindView(R.id.movie_name_view)
    TextView movieNameView;

    @BindView(R.id.movie_art_view)
    ImageView movieArtView;

    @BindView(R.id.movie_description_view)
    TextView movieDescriptionView;

    @BindView(R.id.movie_seasons_count_view)
    TextView movieSeasonsCountView;

    private Movie movie;

    private String searchString;
    private MovieMetadataExtractionTask movieMetadataExtractionTask;

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
        this.movie = movie;
        setupMovieCoreData(movie);
        initEventHandlers(movie);
        refreshMovieDetails(movie);
    }

    private void initEventHandlers(Movie movie) {
        OnClickListener onClickListener = v -> {
            UiUtils.blinkView(parentCardView);
            openMovieDetails(movie);
        };
        setOnClickListener(onClickListener);
        movieArtView.setOnClickListener(onClickListener);
        movieNameView.setOnClickListener(onClickListener);
        movieDescriptionView.setOnClickListener(onClickListener);
        movieSeasonsCountView.setOnClickListener(onClickListener);
        parentCardView.setOnClickListener(onClickListener);
    }

    @SuppressLint("SetTextI18n")
    private void setupMovieCoreData(Movie movie) {
        String movieName = movie.getMovieName();
        String movieDescription = movie.getMovieDescription();
        String movieArtUrl = movie.getMovieArtUrl();
        List<Season> movieSeasonsCount = movie.getMovieSeasons();
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
        if (StringUtils.isNotEmpty(movieArtUrl)) {
            UiUtils.loadImageIntoView(movieArtView, movieArtUrl);
        } else {
            movieArtView.setImageDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.light_grey)));
        }
    }

    private void openMovieDetails(Movie movie) {
        Intent movieDetailsIntent = new Intent(getContext(), MovieDetailsActivity.class);
        movieDetailsIntent.putExtra(AppConstants.MOVIE_ID, movie.getMovieId());
        getContext().startActivity(movieDetailsIntent);
    }

    private void refreshMovieDetails(Movie movie) {
        if (movieMetadataExtractionTask != null) {
            movieMetadataExtractionTask.cancel(true);
        }
        movieMetadataExtractionTask = new MovieMetadataExtractionTask(movie.getMovieLink(), movie.getMovieId());
        movieMetadataExtractionTask.execute();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshMovieDetails(movie);
    }

    static class MovieMetadataExtractionTask extends AsyncTask<Void, Void, Void> {

        private String movieLink;
        private String movieId;

        MovieMetadataExtractionTask(String movieLink, String movieId) {
            this.movieLink = movieLink;
            this.movieId = movieId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentManager.extractMetaDataFromMovieLink(movieLink, movieId);
            return null;
        }

    }

}
