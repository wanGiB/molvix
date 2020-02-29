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
import com.molvix.android.database.MolvixDB;
import com.molvix.android.eventbuses.ConnectivityChangedEvent;
import com.molvix.android.eventbuses.DisplayNewMoviesEvent;
import com.molvix.android.eventbuses.FetchMoviesEvent;
import com.molvix.android.eventbuses.FilterByGenresEvent;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.managers.MovieManager;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.MolvixGenUtils;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private DataSubscription moviesSubscription;

    public enum LoadMode {
        MODE_DEFAULT,
        MODE_SEARCH,
        MODE_GENRES,
        MODE_LATEST_MOVIES
    }

    public static AtomicReference<LoadMode> activeLoadMode = new AtomicReference<>(LoadMode.MODE_DEFAULT);

    private void setSearchString(String searchString) {
        if (moviesAdapter != null) {
            moviesAdapter.setSearchString(searchString);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MovieManager.clearAllRefreshedMovies();
        if (movies.isEmpty()) {
            fetchMovies();
        }
    }

    private void removeAllMoviesSubscription() {
        if (moviesSubscription != null && !moviesSubscription.isCanceled()) {
            moviesSubscription.cancel();
            moviesSubscription = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        removeAllMoviesSubscription();
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
            } else if (event instanceof DisplayNewMoviesEvent) {
                displayNewestMovies();
            } else if (event instanceof FilterByGenresEvent) {
                FilterByGenresEvent filterByGenresEvent = (FilterByGenresEvent) event;
                displayMoviesByGenre(filterByGenresEvent.getSelectedGenres());
            } else if (event instanceof FetchMoviesEvent) {
                fetchMovies();
            }
        });
    }

    private void displayMoviesByGenre(List<String> selectedGenres) {
        nullifyActiveSearch();
        removeAllMoviesSubscription();
        new Thread(() -> {
            List<Movie> results = MolvixDB.getMovieBox()
                    .query()
                    .filter(entity -> {
                        if (entity.getMovieGenre() == null) {
                            return false;
                        }
                        return StringUtils.containsAny(entity.getMovieGenre().toLowerCase(), MolvixGenUtils.charSequencesToLowerCase(MolvixGenUtils.getCharSequencesFromList(selectedGenres)));
                    })
                    .build()
                    .find();
            clearCurrentData();
            loadMovies(results, LoadMode.MODE_GENRES);
        }).start();
    }

    private void displayNewestMovies() {
        nullifyActiveSearch();
        removeAllMoviesSubscription();
        new Thread(() -> {
            List<Movie> results = MolvixDB.getMovieBox()
                    .query()
                    .equal(Movie_.newMovie, true)
                    .build()
                    .find();
            clearCurrentData();
            loadMovies(results, LoadMode.MODE_LATEST_MOVIES);
        }).start();
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
                    .or()
                    .contains(Movie_.movieGenre, searchString)
                    .build()
                    .find();
            clearCurrentData();
            loadMovies(results, LoadMode.MODE_SEARCH);
        }).start();
    }

    private void fetchMovies() {
        try {
            clearCurrentData();
            nullifyActiveSearch();
            DataObserver<List<Movie>> moviesObserver = data -> {
                if (AppConstants.canShuffleExistingMovieCollection.get()) {
                    Collections.shuffle(data);
                } else {
                    AppConstants.canShuffleExistingMovieCollection.set(true);
                }
                loadChangedData(data);
            };
            moviesSubscription = MolvixDB.getMovieBox().query().build().subscribe().observer(moviesObserver);
            spinMoviesDownloadJob();
        } catch (Exception ignored) {

        }
    }

    private void loadChangedData(List<Movie> changedData) {
        if (!changedData.isEmpty()) {
            loadMovies(changedData, LoadMode.MODE_DEFAULT);
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
        LinearLayoutManager homeLinearLayoutManager = new LinearLayoutManager(getActivity());
        moviesRecyclerView.setLayoutManager(homeLinearLayoutManager);
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

    private void checkAndAddAd() {
        int nextPosition = movies.size();
        int nextAdPosition = nextPosition + 1;
        if (nextAdPosition % 5 == 0) {
            Movie adView = new Movie();
            adView.setAd(true);
            movies.add(adView);
        }
    }

    private void loadMovies(List<Movie> result, LoadMode loadMode) {
        mUiHandler.post(() -> {
            activeLoadMode.set(loadMode);
            for (Movie movie : result) {
                if (!movies.contains(movie)) {
                    checkAndAddAd();
                    movies.add(movie);
                    moviesAdapter.notifyItemInserted(movies.size() - 1);
                } else {
                    int indexOfMovie = movies.indexOf(movie);
                    movies.set(indexOfMovie, movie);
                    moviesAdapter.notifyItemChanged(indexOfMovie);
                }
            }
            postCreateUI();
            if (loadMode == LoadMode.MODE_DEFAULT) {
                displayTotalNumberOfMoviesLoadedInHeader();
            } else {
                displayTotalNumberOfFoundResultsInHeader(result);
            }
            swipeRefreshLayout.setRefreshing(false);
//            createDebugPresets();
        });
    }

//    private void createDebugPresets() {
//        if (!movies.isEmpty() && BuildConfig.DEBUG) {
//            //Let's grab the movies and create Presets
//            createPresetsFromMovies(movies);
//        }
//    }

//    private void createPresetsFromMovies(List<Movie> movies) {
//        try {
//            PackageManager packageManager = ApplicationLoader.getInstance().getPackageManager();
//            if (packageManager != null) {
//                PackageInfo packageInfo = packageManager.getPackageInfo(ApplicationLoader.getInstance().getPackageName(), 0);
//                if (packageInfo != null) {
//                    long versionCode;
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                        versionCode = packageInfo.getLongVersionCode();
//                    } else {
//                        versionCode = packageInfo.versionCode;
//                    }
//                    String versionName = packageInfo.versionName;
//                    JSONObject presetsObject = new JSONObject();
//                    JSONArray data = new JSONArray();
//                    presetsObject.put(AppConstants.FORCED_VERSION_CODE_UPDATE, versionCode);
//                    presetsObject.put(AppConstants.FORCED_VERSION_NAME_UPDATE, versionName);
//                    for (Movie movie : movies) {
//                        if (!movie.isAd()) {
//                            JSONObject movieObject = new JSONObject();
//                            movieObject.put(AppConstants.MOVIE_NAME, movie.getMovieName().toLowerCase());
//                            String movieArtUrl = movie.getMovieArtUrl();
//                            if (StringUtils.isEmpty(movieArtUrl)) {
//                                movieObject.put(AppConstants.MOVIE_ART_URL, "");
//                            } else {
//                                if (!movieArtUrl.contains("o2tvseries")) {
//                                    movieObject.put(AppConstants.MOVIE_ART_URL, movieArtUrl);
//                                }
//                            }
//                            data.put(movieObject);
//                        }
//                    }
//                    presetsObject.put(AppConstants.DATA, data);
//                    String presetsString = presetsObject.toString();
//                    File dataFile = FileUtils.getDataFilePath("presets.json");
//                    writeDataToFile(dataFile, presetsString);
//                }
//            }
//        } catch (Exception ignored) {
//
//        }
//    }

//    private void writeDataToFile(File dataFile, String presetsString) {
//        FileWriter fileWriter = null;
//        BufferedWriter bufferedWriter = null;
//        try {
//            fileWriter = new FileWriter(dataFile.getPath());
//            bufferedWriter = new BufferedWriter(fileWriter);
//            bufferedWriter.write(presetsString);
//            MolvixLogger.d(ContentManager.class.getSimpleName(), "Presets Written to File Successfully!!!");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            String errorMessage = e.getMessage();
//            if (errorMessage != null) {
//                MolvixLogger.d(ContentManager.class.getSimpleName(), "Error saving presets due to " + errorMessage);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            String errorMessage = e.getMessage();
//            if (errorMessage != null) {
//                MolvixLogger.d(ContentManager.class.getSimpleName(), "Error saving presets due to " + errorMessage);
//            }
//        } finally {
//            if (bufferedWriter != null) {
//                try {
//                    bufferedWriter.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (fileWriter != null) {
//                try {
//                    fileWriter.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    @SuppressLint("SetTextI18n")
    private void displayTotalNumberOfMoviesLoadedInHeader() {
        mUiHandler.post(() -> {
            if (!movies.isEmpty()) {
                int totalNumberOfMovies = movies.size();
                DecimalFormat moviesNoFormatter = new DecimalFormat("#,###");
                headerTextView.setText("Over " + moviesNoFormatter.format(totalNumberOfMovies) + " series available");
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
    private void displayTotalNumberOfFoundResultsInHeader(List<Movie> queriedMovies) {
        mUiHandler.post(() -> {
            int totalNumberOfMovies = queriedMovies.size();
            DecimalFormat moviesNoFormatter = new DecimalFormat("#,###");
            String resultMsg = totalNumberOfMovies == 1 ? "result" : "results";
            headerTextView.setText(moviesNoFormatter.format(totalNumberOfMovies) + " " + resultMsg + " found");
            UiUtils.toggleViewVisibility(contentLoadingView, queriedMovies.isEmpty());
        });
    }

    private void nullifyActiveSearch() {
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
            } catch (Exception e) {
                e.printStackTrace();
                EventBus.getDefault().post(e);
            }
            return null;
        }
    }

}