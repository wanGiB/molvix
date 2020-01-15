package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.jobs.ContentMiner;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.LocalDbUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeasonView extends FrameLayout {

    @BindView(R.id.list_item_season_name)
    MolvixTextView seasonNameView;

    private Movie movie;
    private Season season;
    private SeasonEpisodesExtractionTask seasonEpisodesExtractionTask;

    public SeasonView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SeasonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeasonView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        @SuppressLint("InflateParams") View seasonView = LayoutInflater.from(context).inflate(R.layout.season_view, null);
        ButterKnife.bind(this, seasonView);
        removeAllViews();
        addView(seasonView);
        requestLayout();
    }

    public void bindSeason(Season season) {
        this.season = season;
        movie = LocalDbUtils.getMovie(season.getMovieId());
        seasonNameView.setText(season.getSeasonName());
        loadSeasonEpisodes();
    }

    private void loadSeasonEpisodes() {
        if (seasonEpisodesExtractionTask != null) {
            seasonEpisodesExtractionTask.cancel(true);
            seasonEpisodesExtractionTask = null;
        }
        seasonEpisodesExtractionTask = new SeasonEpisodesExtractionTask(season.getSeasonLink(), season.getSeasonName(), movie);
        seasonEpisodesExtractionTask.execute();
    }

    static class SeasonEpisodesExtractionTask extends AsyncTask<Void, Void, Void> {

        private String seasonName;
        private String seasonLink;
        private Movie movie;

        SeasonEpisodesExtractionTask(String seasonLink, String seasonName, Movie movie) {
            this.movie = movie;
            this.seasonLink = seasonLink;
            this.seasonName = seasonName;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentMiner.extractMetaDataFromMovieSeasonLink(seasonLink, seasonName, movie);
            return null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadSeasonEpisodes();
    }

}
