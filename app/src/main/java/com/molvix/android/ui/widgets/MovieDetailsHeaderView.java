package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
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

public class MovieDetailsHeaderView extends FrameLayout {
    @BindView(R.id.movie_art_view)
    ImageView movieArtView;

    @BindView(R.id.movie_name_view)
    MolvixTextView movieNameView;

    @BindView(R.id.movie_description_view)
    MolvixTextView movieDescriptionView;

    @BindView(R.id.movie_seasons_count_view)
    MolvixTextView seasonsCountView;

    public MovieDetailsHeaderView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MovieDetailsHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MovieDetailsHeaderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        @SuppressLint("InflateParams") View headerView = LayoutInflater.from(context).inflate(R.layout.full_movie_detals, null);
        ButterKnife.bind(this, headerView);
        removeAllViews();
        addView(headerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        requestLayout();
    }

    public void bindMovieHeader(Movie movie) {
        movieNameView.setText(WordUtils.capitalize(movie.getMovieName()));
        String movieDescription = movie.getMovieDescription();
        if (StringUtils.isNotEmpty(movieDescription)) {
            movieDescriptionView.setText(movieDescription);
        }
        String movieArtUrl = movie.getMovieArtUrl();
        if (StringUtils.isNotEmpty(movieArtUrl)) {
            UiUtils.loadImageIntoView(movieArtView, movieArtUrl, 200);
        } else {
            movieArtView.setImageDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.light_grey)));
        }
        List<Season> movieSeasonsCount = movie.getSeasons();
        if (movieSeasonsCount != null && !movieSeasonsCount.isEmpty()) {
            int seasonsCount = movieSeasonsCount.size();
            String pluralizer = seasonsCount == 1 ? " Season" : " Seasons";
            seasonsCountView.setText(String.valueOf(seasonsCount).concat(pluralizer));
        } else {
            seasonsCountView.setText("");
        }
    }

}
