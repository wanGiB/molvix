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
import com.molvix.android.companions.AppConstants;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.models.Movie;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Case;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
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

    private Realm realm;
    private RealmResults<Movie> movies;
    private RealmResults<Movie> searchResults;
    private MoviesAdapter moviesAdapter;

    private Handler mUiHandler;
    private ContentPullOverTask contentPullOverTask;
    private TextView headerTextView;
    private String searchString;

    private void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    private String getSearchString() {
        return searchString;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (movies != null) {
            movies.removeAllChangeListeners();
        }
        realm.close();
    }

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
    }

    private void listenToChangesInLocalMoviesDatabase() {
        OrderedRealmCollectionChangeListener<RealmResults<Movie>> realmChangeListener = (results, changeSet) -> {
            if (changeSet == null) {
                return;
            }
            Collections.shuffle(results, new SecureRandom());
            moviesAdapter.setData(results);
            checkAndInvalidateUI();
            swipeRefreshLayout.setRefreshing(false);
            displayTotalNumberOfMoviesLoadedInHeader();
        };
        movies.addChangeListener(realmChangeListener);
    }

    private void checkAndInvalidateUI() {
        UiUtils.toggleViewVisibility(contentLoadingView, movies.isEmpty());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupSwipeRefreshLayoutColorScheme();
        initMoviesAdapter();
        fetchAllAvailableMovies();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupSwipeRefreshLayoutColorScheme() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.gplus_color_1),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_2),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_3),
                ContextCompat.getColor(getActivity(), R.color.gplus_color_4));
        swipeRefreshLayout.setOnRefreshListener(this::fetchAllAvailableMovies);
    }

    @SuppressLint("InflateParams")
    private void initMoviesAdapter() {
        View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.home_recycler_view_header, null);
        headerTextView = headerView.findViewById(R.id.header_text_view);
        moviesAdapter = new MoviesAdapter(getActivity());
        HeaderAndFooterRecyclerViewAdapter headerAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(moviesAdapter);
        moviesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        moviesRecyclerView.setItemAnimator(new ScaleInAnimator());
        moviesRecyclerView.setAdapter(headerAndFooterRecyclerViewAdapter);
        RecyclerViewUtils.setHeaderView(moviesRecyclerView, headerView);
    }

    private void fetchAllAvailableMovies() {
        nullifySearch();
        movies = realm.where(Movie.class).findAllAsync();
        spinMoviesDownloadJob();
        listenToChangesInLocalMoviesDatabase();
    }

    private void searchMovies(String searchString) {
        searchResults = realm.where(Movie.class)
                .contains(AppConstants.MOVIE_NAME, searchString.toLowerCase(), Case.INSENSITIVE)
                .or()
                .contains(AppConstants.MOVIE_DESCRIPTION, searchString.toLowerCase(), Case.INSENSITIVE)
                .findAllAsync();
        moviesAdapter.setSearchString(searchString);
        listenToSearchChangeResults();
    }

    private void listenToSearchChangeResults() {
        OrderedRealmCollectionChangeListener<RealmResults<Movie>> realmChangeListener = (results, changeSet) -> {
            if (changeSet == null) {
                return;
            }
            moviesAdapter.setSearchString(getSearchString());
            moviesAdapter.setData(results);
            checkAndInvalidateUI();
            swipeRefreshLayout.setRefreshing(false);
            displayFoundResults(results);
        };
        searchResults.addChangeListener(realmChangeListener);
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

    @SuppressLint("SetTextI18n")
    private void displayFoundResults(List<Movie> queriedMovies) {
        mUiHandler.post(() -> {
            if (getSearchString() != null) {
                int totalNumberOfMovies = queriedMovies.size();
                DecimalFormat moviesNoFormatter = new DecimalFormat("#,###");
                String resultMsg = totalNumberOfMovies == 1 ? "result" : "results";
                headerTextView.setText(moviesNoFormatter.format(totalNumberOfMovies) + " " + resultMsg + " found");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onEvent(Object event) {
        super.onEvent(event);
        if (event instanceof SearchEvent) {
            SearchEvent searchEvent = (SearchEvent) event;
            String searchString = searchEvent.getSearchString();
            if (StringUtils.isNotEmpty(searchString) && searchString != null) {
                setSearchString(searchString);
                mUiHandler.post(() -> searchMovies(searchEvent.getSearchString().toLowerCase()));
            } else {
                mUiHandler.post(() -> {
                    setSearchString(null);
                    moviesAdapter.setSearchString(null);
                    moviesAdapter.setData(movies);
                    checkAndInvalidateUI();
                    displayTotalNumberOfMoviesLoadedInHeader();
                    swipeRefreshLayout.setRefreshing(false);
                    if (searchResults != null) {
                        searchResults.removeAllChangeListeners();
                    }
                });
            }
        } else if (event instanceof Exception) {
            mUiHandler.post(() -> {
                //Most likely a network error
                if (movies.isEmpty()) {
                    UiUtils.toggleViewVisibility(contentLoadingProgressBar, false);
                    contentLoadingProgressMessageView.setText("Network error.Please review your data connection and tap here to try again.");
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void nullifySearch() {
        moviesAdapter.setSearchString(null);
        moviesAdapter.notifyDataSetChanged();
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