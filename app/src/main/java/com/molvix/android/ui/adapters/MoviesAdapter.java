package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Movie;
import com.molvix.android.ui.widgets.AdMobNativeAdView;
import com.molvix.android.ui.widgets.MovieView;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("FieldCanBeLocal")
public class MoviesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Movie> movies;
    private LayoutInflater layoutInflater;
    private String searchString;

    private static final int ITEM_TYPE_MOVIE = 0;
    private static final int ITEM_TYPE_AD = 1;

    public MoviesAdapter(Context context, List<Movie> movies) {
        this.movies = movies;
        layoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return viewType == ITEM_TYPE_MOVIE ? new MoviesItemViewHolder(layoutInflater.inflate(R.layout.recycler_view_item_movie, parent, false)) : new AdsItemViewHolder(layoutInflater.inflate(R.layout.recycler_view_item_admob_ad, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MoviesItemViewHolder) {
            MoviesItemViewHolder moviesItemViewHolder = (MoviesItemViewHolder) holder;
            moviesItemViewHolder.bindData(movies.get(position), getSearchString());
        } else {
            AdsItemViewHolder adsItemViewHolder = (AdsItemViewHolder) holder;
            adsItemViewHolder.loadAd();
        }
    }

    @Override
    public int getItemViewType(int position) {
        Movie movie = movies.get(position);
        return movie.isAd() ? ITEM_TYPE_AD : ITEM_TYPE_MOVIE;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    private String getSearchString() {
        return searchString;
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    static class MoviesItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.movie_view)
        MovieView movieView;

        MoviesItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindData(Movie movie, String searchString) {
            movieView.setSearchString(searchString);
            movieView.setupMovie(movie);
        }

    }

    static class AdsItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.admob_ad_view)
        AdMobNativeAdView adMobNativeAdView;

        AdsItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void loadAd() {
            UnifiedNativeAd unifiedNativeAd = AppConstants.unifiedNativeAdAtomicReference.get();
            if (unifiedNativeAd != null && ConnectivityUtils.isDeviceConnectedToTheInternet()) {
                UiUtils.toggleViewVisibility(adMobNativeAdView, true);
                adMobNativeAdView.loadInAd(unifiedNativeAd);
            } else {
                UiUtils.toggleViewVisibility(adMobNativeAdView, false);
            }
        }

    }

}
