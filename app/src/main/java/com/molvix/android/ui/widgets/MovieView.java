package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.LoadMovieEvent;
import com.molvix.android.managers.AdsLoadManager;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Season;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.objectbox.reactive.DataSubscription;

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

    @BindView(R.id.admob_ad_view)
    AdMobNativeAdView adMobNativeAdView;

    private Movie movie;
    private String searchString;
    private DataSubscription movieSubscription;

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
        addMovieSubscription();
        initEventHandlers(movie);
        checkAndLoadAd();
        refreshMovieDetails(movie);
    }

    private void addMovieSubscription() {
        removeMovieSubscription();
        movieSubscription = MolvixDB.getMovieBox().query().equal(Movie_.movieId, movie.getMovieId()).build().subscribe().observer(data -> {
            if (!data.isEmpty()) {
                Movie updatedMovie = data.get(0);
                if (updatedMovie.equals(movie)) {
                    setupMovieCoreData(updatedMovie);
                }
            }
        });
    }

    private void removeMovieSubscription() {
        if (movieSubscription != null && !movieSubscription.isCanceled()) {
            movieSubscription.cancel();
            movieSubscription = null;
        }
    }

    private void checkAndLoadAd() {
        if (!AdsLoadManager.nativeAds.isEmpty() && !AdsLoadManager.adConsumed()) {
            UiUtils.toggleViewVisibility(adMobNativeAdView, true);
            adMobNativeAdView.loadInAd(AdsLoadManager.nativeAds.get(0));
            AdsLoadManager.setAdConsumed(true);
        } else {
            UiUtils.toggleViewVisibility(adMobNativeAdView, false);
        }
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

    @SuppressLint("SetTextI18n")
    private void setupMovieCoreData(Movie movie) {
        String movieName = movie.getMovieName();
        String movieDescription = movie.getMovieDescription();
        String movieArtUrl = movie.getMovieArtUrl();
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
        if (StringUtils.isNotEmpty(movieArtUrl)) {
            UiUtils.loadImageIntoView(movieArtView, movieArtUrl);
        } else {
            movieArtView.setImageDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.light_grey)));
        }
    }

    private void openMovieDetails(Movie movie) {
        movie.setRecommendedToUser(true);
        MolvixDB.updateMovie(movie);
        EventBus.getDefault().post(new LoadMovieEvent(movie.getMovieId()));
    }

    private void refreshMovieDetails(Movie movie) {
        if (MovieManager.canRefreshMovieDetails(movie.getMovieId()) && ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            MovieMetadataExtractionTask movieMetadataExtractionTask = new MovieMetadataExtractionTask(movie.getMovieLink(), movie.getMovieId());
            movieMetadataExtractionTask.execute();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addMovieSubscription();
        refreshMovieDetails(movie);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeMovieSubscription();
        Log.d(ContentManager.class.getSimpleName(), movie.getMovieName() + " Detached");
        AdsLoadManager.destroyAds();
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
