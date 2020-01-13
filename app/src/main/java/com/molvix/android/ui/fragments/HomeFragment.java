package com.molvix.android.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.molvix.android.R;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.models.Movie;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.ui.services.MoviesDownloadService;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.ScaleInAnimator;

public class HomeFragment extends BaseFragment {

    @BindView(R.id.content_loading_layout)
    View contentLoadingView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.movies_recycler_view)
    RecyclerView moviesRecyclerView;

    private List<Movie> movies = new ArrayList<>();
    private MoviesAdapter moviesAdapter;

    private DirectModelNotifier.ModelChangedListener<Movie> movieModelChangedListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listenToChangesInLocalMoviesDatabase();
        fetchAllAvailableMovies();
    }

    private void listenToChangesInLocalMoviesDatabase() {
        movieModelChangedListener = new DirectModelNotifier.ModelChangedListener<Movie>() {
            @Override
            public void onModelChanged(@NonNull Movie model, @NonNull BaseModel.Action action) {
                if (action == BaseModel.Action.SAVE || action == BaseModel.Action.INSERT) {
                    addMovie(model);
                } else if (action == BaseModel.Action.CHANGE || action == BaseModel.Action.UPDATE) {
                    updateMovieIndex(model);
                } else if (action == BaseModel.Action.DELETE) {
                    removeMovie(model);
                }
            }

            @Override
            public void onTableChanged(@Nullable Class<?> tableChanged, @NonNull BaseModel.Action action) {

            }
        };
        DirectModelNotifier.get().registerForModelChanges(Movie.class, movieModelChangedListener);
    }

    private void removeMovie(Movie movie) {
        int indexOfMovie = movies.indexOf(movie);
        movies.remove(movie);
        if (indexOfMovie != -1) {
            movies.remove(movie);
            moviesAdapter.notifyItemRemoved(indexOfMovie);
        }
        checkAndInvalidateUI();
    }

    private void updateMovieIndex(Movie movie) {
        if (movies.contains(movie)) {
            int indexOfMovie = movies.indexOf(movie);
            movies.set(indexOfMovie, movie);
            moviesAdapter.notifyItemChanged(indexOfMovie);
        }
    }

    private void addMovie(Movie movie) {
        if (!movies.contains(movie)) {
            movies.add(movie);
            moviesAdapter.notifyItemInserted(movies.size() - 1);
        }
        checkAndInvalidateUI();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void checkAndInvalidateUI() {
        UiUtils.toggleViewVisibility(contentLoadingView, movies.isEmpty());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupSwipeRefreshLayoutColorScheme();
        initMoviesAdapter();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupSwipeRefreshLayoutColorScheme() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.gplus_color_1),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_2),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_3),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_4));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            movies.clear();
            moviesAdapter.notifyDataSetChanged();
            fetchAllAvailableMovies();
        });
    }

    private void initMoviesAdapter() {
        moviesAdapter = new MoviesAdapter(getActivity(), movies);
        moviesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        moviesRecyclerView.setItemAnimator(new ScaleInAnimator());
        moviesRecyclerView.setAdapter(moviesAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DirectModelNotifier.get().unregisterForModelChanges(Movie.class, movieModelChangedListener);
    }

    private void fetchAllAvailableMovies() {
        SQLite.select().from(Movie.class)
                .async()
                .queryListResultCallback((transaction, tResult) -> {
                    if (!tResult.isEmpty()) {
                        for (Movie movie : tResult) {
                            addMovie(movie);
                        }
                    }
                }).execute();
        spinMoviesDownloadJob();
    }

    private void spinMoviesDownloadJob() {
        Intent moviesDownloadIntent = new Intent(getActivity(), MoviesDownloadService.class);
        MoviesDownloadService.enqueueWork(getActivity(), moviesDownloadIntent);
    }

    private void searchMovies(String searchString) {
        moviesAdapter.setSearchString(searchString);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEvent(Object event) {
        super.onEvent(event);
        if (event instanceof SearchEvent) {
            SearchEvent searchEvent = (SearchEvent) event;
            getActivity().runOnUiThread(() -> searchMovies(searchEvent.getSearchString()));
        }
    }

}