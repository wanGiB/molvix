package com.molvix.android.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.beans.MovieContentItem;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.ui.widgets.AdMobNativeAdView;
import com.molvix.android.ui.widgets.EpisodeView;
import com.molvix.android.ui.widgets.MolvixTextView;
import com.molvix.android.ui.widgets.SeasonView;
import com.molvix.android.utils.UiUtils;
import com.thoughtbot.expandablerecyclerview.MultiTypeExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeasonsWithEpisodesAdapter extends MultiTypeExpandableRecyclerViewAdapter<GroupViewHolder, ChildViewHolder> {

    //Group Types
    private static final int MOVIE_HEADER = 10;
    private static final int SEASON_HEADER = 11;
    private static final int AD_HEADER = 12;

    //Child Types
    private static final int EMPTY_CHILD_VIEW = 13;
    private static final int NON_EMPTY_CHILD_VIEW = 14;

    private Context context;

    public SeasonsWithEpisodesAdapter(Context context, List<MovieContentItem> contentItems) {
        super(contentItems);
        this.context = context;
    }

    @Override
    public GroupViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        if (viewType == MOVIE_HEADER) {
            return new MovieHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.movie_details_header_view, parent, false));
        } else if (viewType == AD_HEADER) {
            return new AdHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_item_admob_ad, parent, false));
        } else {
            return new SeasonHeaderGroupViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item_season_header, parent, false));
        }
    }

    @Override
    public ChildViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        if (viewType == EMPTY_CHILD_VIEW) {
            return new EmptyChildViewHolder(LayoutInflater.from(context).inflate(R.layout.empty_recycler_view_item, parent, false));
        } else {
            return new SeasonChildViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item_episode_view, parent, false));
        }
    }

    @Override
    public void onBindChildViewHolder(ChildViewHolder holder, int flatPosition, ExpandableGroup group, int childIndex) {
        if (holder instanceof SeasonChildViewHolder) {
            SeasonChildViewHolder seasonChildViewHolder = (SeasonChildViewHolder) holder;
            MovieContentItem movieContentItem = (MovieContentItem) group;
            seasonChildViewHolder.bindEpisodeData(movieContentItem.getEpisodes().get(childIndex));
        } else if (holder instanceof EmptyChildViewHolder) {
            EmptyChildViewHolder emptyChildViewHolder = (EmptyChildViewHolder) holder;
            emptyChildViewHolder.bindEmptyData();
        }
    }

    @Override
    public void onBindGroupViewHolder(GroupViewHolder holder, int flatPosition, ExpandableGroup group) {
        if (holder instanceof MovieHeaderViewHolder) {
            MovieHeaderViewHolder movieHeaderViewHolder = (MovieHeaderViewHolder) holder;
            MovieContentItem movieContentItem = (MovieContentItem) group;
            movieHeaderViewHolder.bindData(context, movieContentItem);
        } else if (holder instanceof SeasonHeaderGroupViewHolder) {
            SeasonHeaderGroupViewHolder seasonHeaderGroupViewHolder = (SeasonHeaderGroupViewHolder) holder;
            MovieContentItem movieContentItem = (MovieContentItem) group;
            seasonHeaderGroupViewHolder.bindSeasonData(movieContentItem.getSeason());
        } else if (holder instanceof AdHeaderViewHolder) {
            AdHeaderViewHolder adHeaderViewHolder = (AdHeaderViewHolder) holder;
            adHeaderViewHolder.refreshAd();
        }
    }

    @Override
    public int getGroupViewType(int position, ExpandableGroup group) {
        MovieContentItem movieContentItem = (MovieContentItem) group;
        if (movieContentItem.getContentType() == MovieContentItem.ContentType.MOVIE_HEADER) {
            return MOVIE_HEADER;
        } else if (movieContentItem.getContentType() == MovieContentItem.ContentType.AD) {
            return AD_HEADER;
        } else {
            return SEASON_HEADER;
        }
    }

    @Override
    public boolean isGroup(int viewType) {
        return viewType == MOVIE_HEADER || viewType == AD_HEADER || viewType == SEASON_HEADER;
    }

    @Override
    public boolean isChild(int viewType) {
        return viewType == EMPTY_CHILD_VIEW || viewType == NON_EMPTY_CHILD_VIEW;
    }

    @Override
    public int getChildViewType(int position, ExpandableGroup group, int childIndex) {
        MovieContentItem movieContentItem = (MovieContentItem) group;
        if (movieContentItem.getEpisodes().isEmpty()) {
            return EMPTY_CHILD_VIEW;
        } else {
            return NON_EMPTY_CHILD_VIEW;
        }
    }

    static class SeasonHeaderGroupViewHolder extends GroupViewHolder {

        @BindView(R.id.season_view)
        SeasonView seasonNameView;

        SeasonHeaderGroupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindSeasonData(Season season) {
            seasonNameView.bindSeason(season);
        }

    }

    static class SeasonChildViewHolder extends ChildViewHolder {

        @BindView(R.id.episode_view)
        EpisodeView episodeView;

        SeasonChildViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindEpisodeData(Episode episode) {
            episodeView.bindEpisode(episode);
        }

    }

    static class EmptyChildViewHolder extends ChildViewHolder {

        @BindView(R.id.empty_container)
        View emptyContainer;

        EmptyChildViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindEmptyData() {
            emptyContainer.setVisibility(View.GONE);
        }

    }

    static class MovieHeaderViewHolder extends GroupViewHolder {

        @BindView(R.id.movie_art_view)
        ImageView movieArtView;

        @BindView(R.id.movie_name_view)
        MolvixTextView movieNameView;

        @BindView(R.id.movie_description_view)
        MolvixTextView movieDescriptionView;

        @BindView(R.id.movie_seasons_count_view)
        MolvixTextView movieSeasonsCountView;

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
            List<Season> movieSeasons = movie.getMovieSeasons();
            if (movieSeasons != null && !movieSeasons.isEmpty()) {
                movieSeasonsCountView.setText("Seasons " + movieSeasons.size());
            }
        }

    }

    static class AdHeaderViewHolder extends GroupViewHolder {

        @BindView(R.id.admob_ad_view)
        AdMobNativeAdView adMobNativeAdView;

        AdHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void refreshAd() {
            adMobNativeAdView.refreshAd();
        }

    }

}