package com.molvix.android.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;
import com.molvix.android.models.Movie;
import com.molvix.android.ui.widgets.MovieView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("FieldCanBeLocal")
public class MoviesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Movie> movies = Collections.emptyList();
    private LayoutInflater layoutInflater;

    private String searchString;

    public MoviesAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);
    }

    public void setData(List<Movie> movies) {
        this.movies = movies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MoviesItemViewHolder(layoutInflater.inflate(R.layout.recycler_item_movie, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MoviesItemViewHolder moviesItemViewHolder = (MoviesItemViewHolder) holder;
        moviesItemViewHolder.bindData(movies.get(position), getSearchString());
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

}
