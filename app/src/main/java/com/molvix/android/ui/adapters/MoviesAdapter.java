package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.models.Movie;
import com.molvix.android.ui.widgets.AdMobNativeAdView;
import com.molvix.android.ui.widgets.MovieView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("FieldCanBeLocal")
public class MoviesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Movie> movies;
    private LayoutInflater layoutInflater;

    private final int ITEM_TYPE_MOVIE = 0;
    private final int ITEM_TYPE_AD = 1;
    private String searchString;

    public MoviesAdapter(Context context, List<Movie> movies) {
        layoutInflater = LayoutInflater.from(context);
        this.movies = movies;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return viewType == ITEM_TYPE_MOVIE ? new MoviesItemViewHolder(layoutInflater.inflate(R.layout.recycler_item_movie, parent, false)) : new AdMobItemViewHolder(layoutInflater.inflate(R.layout.recycler_item_admob_ad, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MoviesItemViewHolder) {
            MoviesItemViewHolder moviesItemViewHolder = (MoviesItemViewHolder) holder;
            moviesItemViewHolder.bindData(movies.get(position), getSearchString());
        } else {
            AdMobItemViewHolder adMobItemViewHolder = (AdMobItemViewHolder) holder;
            adMobItemViewHolder.refreshAd();
        }
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    private String getSearchString() {
        return searchString;
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    @Override
    public int getItemViewType(int position) {
        boolean isAd = movies.get(position).isAd();
        return !isAd ? ITEM_TYPE_MOVIE : ITEM_TYPE_AD;
    }

    static class MoviesItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.movie_view)
        MovieView movieView;

        MoviesItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindData(Movie movie, String searchString) {
            if (searchString != null) {
                movieView.setSearchString(searchString);
            }
            movieView.setupMovie(movie);
        }
    }

    static class AdMobItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.admob_ad_view)
        AdMobNativeAdView adMobNativeAdView;

        AdMobItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void refreshAd() {
            adMobNativeAdView.refreshAd();
        }

    }

}
