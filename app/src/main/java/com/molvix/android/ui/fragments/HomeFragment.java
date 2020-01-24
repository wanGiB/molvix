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

import com.liucanwen.app.headerfooterrecyclerview.HeaderAndFooterRecyclerViewAdapter;
import com.liucanwen.app.headerfooterrecyclerview.RecyclerViewUtils;
import com.molvix.android.R;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.ConnectivityChangedEvent;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
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
    private ContentPullOverTask contentPullOverTask;
    private TextView headerTextView;
    private String searchString;
    private DataSubscription moviesSubscription;

    private void setSearchString(String searchString) {
        this.searchString = searchString;
        if (moviesAdapter != null) {
            moviesAdapter.setSearchString(searchString);
        }
    }

    private String getSearchString() {
        return searchString;
    }

    @Override
    public void onResume() {
        super.onResume();
        MovieManager.clearAllRefreshedMovies();
        fetchMovies();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeAllMoviesSubscription();
        removeAllMoviesSubscription();
    }

    private void removeAllMoviesSubscription() {
        if (moviesSubscription != null && !moviesSubscription.isCanceled()) {
            moviesSubscription.cancel();
            moviesSubscription = null;
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, root);
        mUiHandler = new Handler();
        return root;
    }

    @Override
    public void onEvent(Object event) {
        super.onEvent(event);
        mUiHandler.post(() -> {
            if (event instanceof SearchEvent) {
                SearchEvent searchEvent = (SearchEvent) event;
                String searchString = searchEvent.getSearchString();
                if (StringUtils.isNotEmpty(searchString)) {
                    searchMovies(searchString.toLowerCase());
                } else {
                    fetchMovies();
                }
            } else if (event instanceof Exception) {
                if (movies.isEmpty()) {
                    UiUtils.toggleViewVisibility(contentLoadingProgressBar, false);
                    contentLoadingProgressMessageView.setText(getString(R.string.network_error_msg));
                    swipeRefreshLayout.setRefreshing(false);
                }
            } else if (event instanceof ConnectivityChangedEvent) {
                if (movies.isEmpty()) {
                    UiUtils.toggleViewVisibility(contentLoadingProgressBar, true);
                    contentLoadingProgressMessageView.setText(getString(R.string.loading_msg));
                    spinMoviesDownloadJob();
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contentLoadingProgressMessageView.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            spinMoviesDownloadJob();
        });
    }

    private void searchMovies(String searchString) {
        setSearchString(searchString);
        removeAllMoviesSubscription();
        new Thread(() -> {
            List<Movie> results = MolvixDB.getMovieBox()
                    .query().contains(Movie_.movieName, searchString)
                    .or()
                    .contains(Movie_.movieDescription, searchString)
                    .build().find();
            clearCurrentData();
            loadMovies(results);
        }).start();
    }

    private void fetchMovies() {
        clearCurrentData();
        nullifySearch();
        DataObserver<List<Movie>> moviesObserver = data -> {
            Collections.shuffle(data);
            loadChangedData(data);
        };
        moviesSubscription = MolvixDB.getMovieBox().query().build().subscribe().observer(moviesObserver);
        spinMoviesDownloadJob();
    }

    private void loadChangedData(List<Movie> changedData) {
        if (!changedData.isEmpty()) {
            loadMovies(changedData);
        }
    }

    private void postCreateUI() {
        checkAndInvalidateUI();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void checkAndInvalidateUI() {
        UiUtils.toggleViewVisibility(contentLoadingView, movies.isEmpty());
        UiUtils.toggleViewVisibility(contentLoadingProgressMessageView, movies.isEmpty() && ConnectivityUtils.isDeviceConnectedToTheInternet());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupSwipeRefreshLayoutColorScheme();
        initMoviesAdapter();
        fetchMovies();
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
            fetchMovies();
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
    }

    private void clearCurrentData() {
        mUiHandler.post(() -> {
            movies.clear();
            if (moviesAdapter != null) {
                moviesAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadMovies(List<Movie> result) {
        mUiHandler.post(() -> {
            if (movies.isEmpty()) {
                movies.addAll(result);
                moviesAdapter.notifyDataSetChanged();
            } else {
                for (Movie movie : result) {
                    if (!movies.contains(movie)) {
                        movies.add(movie);
                        moviesAdapter.notifyItemInserted(movies.size() - 1);
                    } else {
                        int indexOfMovie = movies.indexOf(movie);
                        movies.set(indexOfMovie, movie);
                        moviesAdapter.notifyItemChanged(indexOfMovie);
                    }
                }
            }
            postCreateUI();
            if (getSearchString() == null) {
                displayTotalNumberOfMoviesLoadedInHeader();
            } else {
                displayFoundResults(result);
            }
            swipeRefreshLayout.setRefreshing(false);
        });
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

    @SuppressLint("SetTextI18n")
    private void initMovieExtractionTask() {
        if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            if (contentPullOverTask != null) {
                contentPullOverTask.cancel(true);
                contentPullOverTask = null;
            }
            contentPullOverTask = new ContentPullOverTask();
            contentPullOverTask.execute();
        } else {
            if (movies.isEmpty()) {
                UiUtils.toggleViewVisibility(contentLoadingProgressBar, false);
                contentLoadingProgressMessageView.setText("Network error.\nPlease connect to the internet to download movies.");
                swipeRefreshLayout.setRefreshing(false);
            }
        }
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


    private void nullifySearch() {
        if (moviesAdapter != null) {
            moviesAdapter.setSearchString(null);
            setSearchString(null);
            moviesAdapter.notifyDataSetChanged();
        }
    }

    static class ContentPullOverTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ContentManager.grabMovies();
            } catch (IOException e) {
                e.printStackTrace();
                EventBus.getDefault().post(e);
            }
            return null;
        }
    }

}