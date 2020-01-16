package com.molvix.android.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.beans.MovieContentItem;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.widgets.AdMobNativeAdView;
import com.molvix.android.ui.widgets.MolvixTextView;
import com.molvix.android.ui.widgets.SeasonView;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeasonsWithEpisodesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //Group Types
    private static final int MOVIE_HEADER_ITEM = 1;
    private static final int SEASON_ITEM = 2;
    private static final int AD_ITEM = 3;

    private Context context;
    private List<MovieContentItem> contentItems;

    public SeasonsWithEpisodesAdapter(FragmentActivity context, List<MovieContentItem> contentItems) {
        this.context = context;
        this.contentItems = contentItems;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MOVIE_HEADER_ITEM) {
            return new MovieHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.movie_details_header_view, parent, false));
        } else if (viewType == AD_ITEM) {
            return new AdHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_item_admob_ad, parent, false));
        } else {
            return new SeasonHeaderGroupViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item_season_header, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MovieHeaderViewHolder) {
            MovieHeaderViewHolder movieHeaderViewHolder = (MovieHeaderViewHolder) holder;
            MovieContentItem movieContentItem = contentItems.get(position);
            movieHeaderViewHolder.bindData(context, movieContentItem);
        } else if (holder instanceof SeasonHeaderGroupViewHolder) {
            SeasonHeaderGroupViewHolder seasonHeaderGroupViewHolder = (SeasonHeaderGroupViewHolder) holder;
            MovieContentItem movieContentItem = contentItems.get(position);
            seasonHeaderGroupViewHolder.bindSeasonData(movieContentItem.getSeason());
        } else if (holder instanceof AdHeaderViewHolder) {
            AdHeaderViewHolder adHeaderViewHolder = (AdHeaderViewHolder) holder;
            adHeaderViewHolder.refreshAd(context);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MovieContentItem movieContentItem = contentItems.get(position);
        if (movieContentItem.getContentType() == MovieContentItem.ContentType.MOVIE_HEADER) {
            return MOVIE_HEADER_ITEM;
        } else if (movieContentItem.getContentType() == MovieContentItem.ContentType.AD) {
            return AD_ITEM;
        } else {
            return SEASON_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return contentItems.size();
    }

    static class SeasonHeaderGroupViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.season_view)
        SeasonView seasonView;

        SeasonHeaderGroupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindSeasonData(Season season) {
            seasonView.bindSeason(season);
        }
    }

    static class MovieHeaderViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.movie_art_view)
        ImageView movieArtView;

        @BindView(R.id.movie_name_view)
        MolvixTextView movieNameView;

        @BindView(R.id.movie_description_view)
        MolvixTextView movieDescriptionView;

        @BindView(R.id.movie_seasons_count_view)
        MolvixTextView seasonsCountView;

        MovieHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @SuppressLint("SetTextI18n")
        void bindData(Context context, MovieContentItem movieContentItem) {
            Movie movie = movieContentItem.getMovie();
            movieNameView.setText(WordUtils.capitalize(movie.getMovieName()));
            String movieDescription = movie.getMovieDescription();
            if (StringUtils.isNotEmpty(movieDescription)) {
                movieDescriptionView.setText(movieDescription);
            }
            String movieArtUrl = movie.getMovieArtUrl();
            if (StringUtils.isNotEmpty(movieArtUrl)) {
                UiUtils.loadImageIntoView(movieArtView, movieArtUrl);
            } else {
                movieArtView.setImageDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.light_grey)));
            }
            List<Season> movieSeasonsCount = movie.getMovieSeasons();
            if (movieSeasonsCount != null && !movieSeasonsCount.isEmpty()) {
                int seasonsCount = movieSeasonsCount.size();
                String pluralizer = seasonsCount == 1 ? " Season" : " Seasons";
                seasonsCountView.setText(seasonsCount + pluralizer);
            } else {
                seasonsCountView.setText("");
            }
        }

    }

    static class AdHeaderViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.admob_ad_view)
        AdMobNativeAdView adMobNativeAdView;

        AdHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void refreshAd(Context context) {
            adMobNativeAdView.refreshAd(context);
        }

    }

}