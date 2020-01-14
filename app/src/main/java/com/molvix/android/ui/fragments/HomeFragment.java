package com.molvix.android.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_Table;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.ui.services.ContentGenerationService;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.ScaleInAnimator;

import static android.view.View.GONE;

@SuppressWarnings("ConstantConditions")
public class HomeFragment extends BaseFragment {

    @BindView(R.id.content_loading_layout)
    View contentLoadingView;

    @BindView(R.id.content_loading_progress)
    ProgressBar contentLoadingProgressBar;

    @BindView(R.id.content_loading_progress_msg)
    TextView contentLoadingProgressMessageView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.movies_recycler_view)
    RecyclerView moviesRecyclerView;

    @BindView(R.id.nothing_found_error_message)
    TextView nothingFoundMessageView;

    private List<Movie> movies = new ArrayList<>();
    private MoviesAdapter moviesAdapter;

    private Handler mUiHandler;

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
        mUiHandler = new Handler();
        contentLoadingProgressMessageView.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            spinMoviesDownloadJob();
        });
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
        mUiHandler.post(() -> {
            int indexOfMovie = movies.indexOf(movie);
            movies.remove(movie);
            if (indexOfMovie != -1) {
                movies.remove(movie);
                moviesAdapter.notifyItemRemoved(indexOfMovie);
            }
            checkAndInvalidateUI();
        });
    }

    private void updateMovieIndex(Movie movie) {
            mUiHandler.post(() -> {
            if (movies.contains(movie)) {
                int indexOfMovie = movies.indexOf(movie);
                movies.set(indexOfMovie, movie);
                moviesAdapter.notifyItemChanged(indexOfMovie);
            }
        });
    }

    private void addMovie(Movie movie) {
        mUiHandler.post(() -> {
            checkAndAddAd();
            if (!movies.contains(movie)) {
                movies.add(movie);
                moviesAdapter.notifyItemInserted(movies.size() - 1);
            }
            checkAndInvalidateUI();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void checkAndAddAd() {
        if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            int nextMovieCollectionSize = movies.size() + 1;
            if (nextMovieCollectionSize % 5 == 0) {
                Movie adMovie = new Movie();
                adMovie.setAd(true);
                movies.add(adMovie);
                moviesAdapter.notifyItemInserted(movies.size() - 1);
            }
        }
    }

    private void checkAndInvalidateUI() {
        UiUtils.toggleViewVisibility(contentLoadingView, nothingFoundMessageView.getVisibility() == GONE && movies.isEmpty());
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
        SQLite.select()
                .from(Movie.class)
                .async()
                .queryListResultCallback((transaction, tResult) -> {
                    if (!tResult.isEmpty()) {
                        Collections.shuffle(tResult, new SecureRandom());
                        for (Movie movie : tResult) {
                            addMovie(movie);
                        }
                    }
                })
                .execute();
        spinMoviesDownloadJob();
    }

    @SuppressLint("SetTextI18n")
    private void spinMoviesDownloadJob() {
        if (movies.isEmpty()) {
            UiUtils.toggleViewVisibility(contentLoadingView, true);
            UiUtils.toggleViewVisibility(contentLoadingProgressBar, true);
            UiUtils.toggleViewVisibility(contentLoadingProgressMessageView, true);
            contentLoadingProgressMessageView.setText("Loading...");
        }
        initMovieExtractionTask();
    }

    private void initMovieExtractionTask() {
        Intent contentServiceIntent = new Intent(getActivity(), ContentGenerationService.class);
        ContentGenerationService.enqueueWork(getActivity(), contentServiceIntent);
    }

    private void searchMovies(String searchString) {
        SQLite.select()
                .from(Movie.class)
                .where(Movie_Table.movieName.like("%" + searchString + "%"))
                .or(Movie_Table.movieDescription.like("%" + searchString + "%"))
                .async()
                .queryListResultCallback((transaction, queriedMovies) -> mUiHandler.post(() -> {
                    movies.clear();
                    moviesAdapter.notifyDataSetChanged();
                    if (!queriedMovies.isEmpty()) {
                        moviesAdapter.setSearchString(searchString);
                        for (Movie movie : queriedMovies) {
                            addMovie(movie);
                        }
                    }
                    UiUtils.toggleViewVisibility(nothingFoundMessageView, queriedMovies.isEmpty());
                }))
                .execute();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onEvent(Object event) {
        super.onEvent(event);
        if (event instanceof SearchEvent) {
            SearchEvent searchEvent = (SearchEvent) event;
            String searchString = searchEvent.getSearchString();
            if (StringUtils.isNotEmpty(searchString)) {
                mUiHandler.post(() -> searchMovies(searchEvent.getSearchString().toLowerCase()));
            } else {
                fetchAllAvailableMovies();
            }
        } else if (event instanceof Exception) {
            mUiHandler.post(() -> {
                //Most likely a network error
                if (movies.isEmpty()) {
                    UiUtils.toggleViewVisibility(contentLoadingProgressBar, false);
                    contentLoadingProgressMessageView.setText("Network error.Please review your data connection and tap here to try again.");
                }
            });
        } else if (event instanceof String) {
            mUiHandler.post(() -> {
                String s = (String) event;
                if (s.equals(AppConstants.EMPTY_SEARCH)) {
                    moviesAdapter.setSearchString("");
                    moviesAdapter.notifyDataSetChanged();
                    UiUtils.toggleViewVisibility(nothingFoundMessageView, false);
                }
            });
        }
    }
}