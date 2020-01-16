package com.molvix.android.ui.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
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

import com.liucanwen.app.headerfooterrecyclerview.EndlessRecyclerOnScrollListener;
import com.liucanwen.app.headerfooterrecyclerview.HeaderAndFooterRecyclerViewAdapter;
import com.liucanwen.app.headerfooterrecyclerview.RecyclerViewUtils;
import com.molvix.android.R;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_Table;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.ScaleInAnimator;

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

    private List<Movie> movies = new ArrayList<>();
    private MoviesAdapter moviesAdapter;

    private Handler mUiHandler;
    private DirectModelNotifier.ModelChangedListener<Movie> movieModelChangedListener;
    private ContentPullOverTask contentPullOverTask;

    private TextView headerTextView;
    private String activeSearchString;

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
    }

    private void listenToChangesInLocalMoviesDatabase() {
        movieModelChangedListener = new DirectModelNotifier.ModelChangedListener<Movie>() {
            @Override
            public void onModelChanged(@NonNull Movie model, @NonNull BaseModel.Action action) {
                if (action == BaseModel.Action.SAVE) {
                    addMovie(model);
                } else if (action == BaseModel.Action.UPDATE) {
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
                moviesAdapter.notifyDataSetChanged();
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
        UiUtils.toggleViewVisibility(contentLoadingView, movies.isEmpty());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupSwipeRefreshLayoutColorScheme();
        initMoviesAdapter();
        fetchAllAvailableMovies(0);
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
            fetchAllAvailableMovies(0);
        });
    }

    @SuppressLint("InflateParams")
    private void initMoviesAdapter() {
        View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.home_recycler_view_header, null);
        headerTextView = headerView.findViewById(R.id.header_text_view);
        moviesAdapter = new MoviesAdapter(getActivity(), movies);
        HeaderAndFooterRecyclerViewAdapter headerAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(moviesAdapter);
        moviesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        moviesRecyclerView.setItemAnimator(new ScaleInAnimator());
        moviesRecyclerView.setAdapter(headerAndFooterRecyclerViewAdapter);
        RecyclerViewUtils.setHeaderView(moviesRecyclerView, headerView);
        moviesRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadNextPage(View view) {
                if (!movies.isEmpty()) {
                    if (StringUtils.isNotEmpty(getActiveSearchString())) {
                        searchMovies(getActiveSearchString(), movies.size());
                    } else {
                        fetchAllAvailableMovies(movies.size());
                    }
                }
            }
        });
    }

    private String getActiveSearchString() {
        return activeSearchString;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DirectModelNotifier.get().unregisterForModelChanges(Movie.class, movieModelChangedListener);
    }

    private void fetchAllAvailableMovies(int skip) {
        nullifySearch();
        SQLite.select()
                .from(Movie.class)
                .offset(skip)
                .limit(2000)
                .async()
                .queryListResultCallback((transaction, tResult) -> {
                    if (!tResult.isEmpty()) {
                        if (skip == 0) {
                            movies.clear();
                            moviesAdapter.notifyDataSetChanged();
                        }
                        Collections.shuffle(tResult, new SecureRandom());
                        for (Movie movie : tResult) {
                            addMovie(movie);
                        }
                        displayTotalNumberOfMoviesLoadedInHeader();
                    }
                })
                .execute();
        spinMoviesDownloadJob();
    }

    @SuppressLint("SetTextI18n")
    private void displayTotalNumberOfMoviesLoadedInHeader() {
        mUiHandler.post(() -> {
            if (!movies.isEmpty()) {
                int totalNumberOfMovies = movies.size();
                DecimalFormat moviesNoFormatter = new DecimalFormat("#,###");
                headerTextView.setText("Over " + moviesNoFormatter.format(totalNumberOfMovies) + " movies available");
            }
        });
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
        if (contentPullOverTask != null) {
            contentPullOverTask.cancel(true);
            contentPullOverTask = null;
        }
        contentPullOverTask = new ContentPullOverTask();
        contentPullOverTask.execute();
    }

    private void searchMovies(String searchString, int skip) {
        SQLite.select()
                .from(Movie.class)
                .where(Movie_Table.movieName.like("%" + searchString.toLowerCase() + "%"))
                .or(Movie_Table.movieDescription.like("%" + searchString.toLowerCase() + "%"))
                .offset(skip)
                .limit(2000)
                .async()
                .queryListResultCallback((transaction, queriedMovies) -> mUiHandler.post(() -> {
                    if (skip == 0) {
                        movies.clear();
                    }
                    moviesAdapter.setSearchString(searchString);
                    moviesAdapter.notifyDataSetChanged();
                    if (!queriedMovies.isEmpty()) {
                        for (Movie movie : queriedMovies) {
                            addMovie(movie);
                        }
                    }
                    displayFoundResults(movies);
                }))
                .execute();
    }

    @SuppressLint("SetTextI18n")
    private void displayFoundResults(List<Movie> queriedMovies) {
        mUiHandler.post(() -> {
            int totalNumberOfMovies = queriedMovies.size();
            DecimalFormat moviesNoFormatter = new DecimalFormat("#,###");
            String resultMsg = totalNumberOfMovies == 1 ? "result" : "results";
            headerTextView.setText(moviesNoFormatter.format(totalNumberOfMovies) + " " + resultMsg + " found");
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onEvent(Object event) {
        super.onEvent(event);
        if (event instanceof SearchEvent) {
            SearchEvent searchEvent = (SearchEvent) event;
            String searchString = searchEvent.getSearchString();
            setActiveSearchString(searchString);
            if (StringUtils.isNotEmpty(searchString)) {
                mUiHandler.post(() -> searchMovies(searchEvent.getSearchString().toLowerCase(), 0));
            } else {
                fetchAllAvailableMovies(0);
            }
        } else if (event instanceof Exception) {
            mUiHandler.post(() -> {
                //Most likely a network error
                if (movies.isEmpty()) {
                    UiUtils.toggleViewVisibility(contentLoadingProgressBar, false);
                    contentLoadingProgressMessageView.setText("Network error.Please review your data connection and tap here to try again.");
                }
            });
        }
    }

    private void setActiveSearchString(String searchString) {
        this.activeSearchString = searchString;
    }

    private void nullifySearch() {
        moviesAdapter.setSearchString(null);
        moviesAdapter.notifyDataSetChanged();
    }

    static class ContentPullOverTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ContentManager.mineData();
            } catch (IOException e) {
                e.printStackTrace();
                EventBus.getDefault().post(e);
            }
            return null;
        }
    }

}