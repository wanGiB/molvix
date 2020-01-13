package com.molvix.android.ui.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_Table;
import com.molvix.android.models.Season;
import com.molvix.android.ui.adapters.MoviesAdapter;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.LocalDbUtils;
import com.molvix.android.utils.UiUtils;
import com.raizlabs.android.dbflow.runtime.DirectModelNotifier;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.ScaleInAnimator;

import static android.view.View.GONE;

public class HomeFragment extends BaseFragment {

    @BindView(R.id.content_loading_layout)
    View contentLoadingView;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.movies_recycler_view)
    RecyclerView moviesRecyclerView;

    @BindView(R.id.nothing_found_error_message)
    TextView nothingFoundMessageView;

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
        checkAndAddAd();
        if (!movies.contains(movie)) {
            movies.add(movie);
            moviesAdapter.notifyItemInserted(movies.size() - 1);
        }
        checkAndInvalidateUI();
        swipeRefreshLayout.setRefreshing(false);
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
                        for (Movie movie : tResult) {
                            addMovie(movie);
                        }
                    }
                })
                .execute();
        spinMoviesDownloadJob();
    }

    private void spinMoviesDownloadJob() {
        new MovieContentsGenerationTask().execute();
    }

    @SuppressWarnings("ConstantConditions")
    private void searchMovies(String searchString) {
        SQLite.select()
                .from(Movie.class)
                .where(Movie_Table.movieName.like("%" + searchString + "%"))
                .or(Movie_Table.movieDescription.like("%" + searchString + "%"))
                .async()
                .queryListResultCallback((transaction, queriedMovies) -> getActivity().runOnUiThread(() -> {
                    if (!queriedMovies.isEmpty()) {
                        moviesAdapter.setSearchString(searchString);
                        movies.clear();
                        moviesAdapter.notifyDataSetChanged();
                        for (Movie movie : queriedMovies) {
                            addMovie(movie);
                        }
                    }
                    UiUtils.toggleViewVisibility(nothingFoundMessageView, queriedMovies.isEmpty());
                }))
                .execute();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEvent(Object event) {
        super.onEvent(event);
        if (event instanceof SearchEvent) {
            SearchEvent searchEvent = (SearchEvent) event;
            String searchString = searchEvent.getSearchString();
            if (StringUtils.isNotEmpty(searchString)) {
                getActivity().runOnUiThread(() -> searchMovies(searchEvent.getSearchString().toLowerCase()));
            } else {
                fetchAllAvailableMovies();
            }
        }
    }

    //Generate the Movie Contents here
    static class MovieContentsGenerationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                loadMoviesTitlesAndLinks();
                SQLite.select().from(Movie.class)
                        .async()
                        .queryListResultCallback((transaction, tResult) -> {
                            if (!tResult.isEmpty()) {
                                for (Movie movie : tResult) {
                                    String movieLink = movie.getMovieLink();
                                    if (StringUtils.isNotEmpty(movieLink)) {
                                        extractMetaDataFromMovieLink(movieLink, movie);
                                    }
                                }
                            }
                        }).execute();
            } catch (IOException e) {
                e.printStackTrace();
                EventBus.getDefault().post(e);
            }
            return null;
        }

        private void loadMoviesTitlesAndLinks() throws IOException {
            String TV_SERIES_URL = "https://o2tvseries.com/search/list_all_tv_series";
            Document document = Jsoup.connect(TV_SERIES_URL).get();
            Element moviesTitlesAndLinks = document.selectFirst("div.data_list");
            if (moviesTitlesAndLinks != null) {
                Elements dataListElements = moviesTitlesAndLinks.children();
                for (Element element : dataListElements) {
                    Pair<String, String> movieTitleAndLink = getMovieTitleAndLink(element);
                    String movieTitle = movieTitleAndLink.first;
                    String movieLink = movieTitleAndLink.second;
                    if (StringUtils.isNotEmpty(movieTitle) && StringUtils.isNotEmpty(movieLink)) {
                        String movieId = CryptoUtils.getSha256Digest(movieLink);
                        //Persist movie details to database
                        Movie existingMovie = LocalDbUtils.getMovie(movieId);
                        if (existingMovie != null) {
                            return;
                        }
                        Movie newMovie = new Movie();
                        newMovie.setMovieId(movieId);
                        newMovie.setMovieName(movieTitle.toLowerCase());
                        newMovie.setMovieLink(movieLink);
                        newMovie.save();
                    }
                }
            }
        }

        private Pair<String, String> getMovieTitleAndLink(Element element) {
            String movieLink = element.select("div>a").attr("href");
            String movieTitle = element.text();
            return new Pair<>(movieTitle, movieLink);
        }

        private void extractMetaDataFromMovieLink(String movieLink, Movie movie) {
            try {
                Document movieDoc = Jsoup.connect(movieLink).get();
                Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
                String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
                String movieDescription = movieInfoElement.select("div.serial_desc").text();
                if (StringUtils.isNotEmpty(movieArtUrl)) {
                    movie.setMovieArtUrl(movieArtUrl);
                }
                if (StringUtils.isNotEmpty(movieDescription)) {
                    movie.setMovieDescription(movieDescription);
                }
                //Update immediately, I nor get strength to shout
                movie.update();
                //Do more here
                Element otherInfoDocument = movieDoc.selectFirst("div.other_info");
                Elements otherInfoElements = otherInfoDocument.getAllElements();
                int totalNumberOfSeasons = 0;
                if (otherInfoElements != null) {
                    for (Element infoElement : otherInfoElements) {
                        Elements rowElementChildren = infoElement.children();
                        for (Element rowChild : rowElementChildren) {
                            String field = rowChild.select(".field").html();
                            String value = rowChild.select(".value").html();
                            if (field.trim().toLowerCase().equals("seasons:") && StringUtils.isNotEmpty(value)) {
                                totalNumberOfSeasons = Integer.parseInt(value.trim());
                            }
                        }
                    }
                }
                if (totalNumberOfSeasons != 0) {
                    for (int i = 0; i < totalNumberOfSeasons; i++) {
                        String seasonAtI = generateSeasonFromMovieLink(movieLink, i + 1);
                        String seasonName = generateSeasonValue(i + 1);
                        extractMetaDataFromMovieSeasonLink(seasonAtI, seasonName, movie);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                EventBus.getDefault().post(e);
            }
        }

        private void extractMetaDataFromMovieSeasonLink(String seasonLink, String seasonName, Movie movie) {
            try {
                int totalNumberOfEpisodes = getTotalNumberOfEpisodes(seasonLink);
                if (totalNumberOfEpisodes != 0) {
                    List<Season> existingSeasons = movie.getMovieSeasons();
                    if (existingSeasons == null) {
                        existingSeasons = new ArrayList<>();
                    }
                    String seasonId = CryptoUtils.getSha256Digest(seasonLink);
                    Season currentSeason = getSeason(seasonName, movie, seasonId, seasonLink);
                    if (existingSeasons.contains(currentSeason)) {
                        int indexOfCurrentSeason = existingSeasons.indexOf(currentSeason);
                        currentSeason = existingSeasons.get(indexOfCurrentSeason);
                    }
                    List<Episode> episodesList = currentSeason.getEpisodes();
                    if (episodesList == null) {
                        episodesList = new ArrayList<>();
                    }
                    int existingEpisodeListSize = episodesList.size();
                    for (int i = 0; i < totalNumberOfEpisodes; i++) {
                        String episodeLink = generateEpisodeFromSeasonLink(seasonLink, i + 1);
                        if (i == totalNumberOfEpisodes - 1) {
                            episodeLink = checkForSeasonFinale(episodeLink);
                        }
                        String episodeName = generateEpisodeValue(i + 1);
                        if (StringUtils.containsIgnoreCase(episodeLink, getSeasonFinaleSuffix())) {
                            episodeName = generateEpisodeValue(i + 1) + getSeasonFinaleSuffix();
                        }
                        String episodeId = CryptoUtils.getSha256Digest(episodeLink);
                        Episode newEpisode = getEpisode(movie, currentSeason, episodeLink, episodeName, episodeId);
                        if (!episodesList.contains(newEpisode)) {
                            episodesList.add(newEpisode);
                            if (existingEpisodeListSize > 0) {
                                int newSize = episodesList.size();
                                int diff = newSize - existingEpisodeListSize;
                                //noinspection StatementWithEmptyBody
                                if (diff > 0) {
                                    //TODO:Blow a notification that a new episode has being added to this movie season
                                }
                            }
                        } else {
                            int indexOfEpisode = episodesList.indexOf(newEpisode);
                            newEpisode = episodesList.get(indexOfEpisode);
                            newEpisode.setEpisodeLink(episodeLink);
                            newEpisode.setEpisodeName(episodeName);
                            episodesList.set(indexOfEpisode, newEpisode);
                        }
                    }
                    currentSeason.setEpisodes(episodesList);
                    if (!existingSeasons.contains(currentSeason)) {
                        existingSeasons.add(currentSeason);
                    } else {
                        int indexOfCurrentSeason = existingSeasons.indexOf(currentSeason);
                        currentSeason = existingSeasons.get(indexOfCurrentSeason);
                        currentSeason.setEpisodes(episodesList);
                        existingSeasons.set(indexOfCurrentSeason, currentSeason);
                    }
                    movie.setMovieSeasons(existingSeasons);
                    movie.update();
                }
            } catch (IOException e) {
                e.printStackTrace();
                EventBus.getDefault().post(e);
            }
        }

        private Episode getEpisode(Movie movie, Season currentSeason, String episodeLink, String episodeName, String episodeId) {
            Episode newEpisode = new Episode();
            newEpisode.setEpisodeId(episodeId);
            newEpisode.setEpisodeLink(episodeLink);
            newEpisode.setEpisodeName(episodeName);
            newEpisode.setMovieId(movie.getMovieId());
            newEpisode.setSeasonId(currentSeason.getSeasonId());
            return newEpisode;
        }

        private Season getSeason(String seasonName, Movie movie, String seasonId, String seasonLink) {
            Season currentSeason = new Season();
            currentSeason.setSeasonId(seasonId);
            currentSeason.setMovieId(movie.getMovieId());
            currentSeason.setSeasonName(seasonName);
            currentSeason.setSeasonLink(seasonLink);
            return currentSeason;
        }

        private int getTotalNumberOfEpisodes(String seasonLink) throws IOException {
            Document movieSeasonDoc = Jsoup.connect(seasonLink).get();
            Element otherInfoDocument = movieSeasonDoc.selectFirst("div.other_info");
            Elements otherInfoElements = otherInfoDocument.getAllElements();
            int totalNumberOfEpisodes = 0;
            if (otherInfoElements != null) {
                for (Element infoElement : otherInfoElements) {
                    Elements rowElementChildren = infoElement.children();
                    for (Element rowChild : rowElementChildren) {
                        String field = rowChild.select(".field").html();
                        String value = rowChild.select(".value").html();
                        if (field.trim().toLowerCase().equals("episodes:") && StringUtils.isNotEmpty(value)) {
                            totalNumberOfEpisodes = Integer.parseInt(value.trim());
                        }
                    }
                }
            }
            return totalNumberOfEpisodes;
        }

        private String checkForSeasonFinale(String episodeLink) {
            try {
                Document episodeDocument = Jsoup.connect(episodeLink).get();
                //Bring out all href elements containing
                Elements links = episodeDocument.select("a[href]");
                if (links != null && !links.isEmpty()) {
                    List<String> downloadLinks = new ArrayList<>();
                    for (Element link : links) {
                        String href = link.attr("href");
                        if (href.contains(AppConstants.DOWNLOADABLE)) {
                            downloadLinks.add(href);
                        }
                    }
                    if (downloadLinks.isEmpty()) {
                        episodeLink = generateSeasonFinaleForEpisode(episodeLink);
                    }
                } else {
                    return episodeLink;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return episodeLink;
            }
            return episodeLink;
        }

        private String generateSeasonFinaleForEpisode(String episodeLink) {
            String episodeLinkRip = StringUtils.removeEnd(episodeLink, "/index.html");
            return episodeLinkRip + getSeasonFinaleSuffix() + "/index.html";
        }

        private String getSeasonFinaleSuffix() {
            return "-Season-Finale";
        }

        private String generateSeasonValue(int value) {
            if (value < 10) {
                return "Season-0" + value;
            }
            return "Season-" + value;
        }

        private String generateEpisodeValue(int value) {
            if (value < 10) {
                return "Episode-0" + value;
            }
            return "Episode-" + value;
        }

        private String generateSeasonFromMovieLink(String movieLink, int seasonValue) {
            return movieLink.replace("index.html", generateSeasonValue(seasonValue) + "/index.html");
        }

        private String generateEpisodeFromSeasonLink(String seasonLink, int episodeValue) {
            return seasonLink.replace("index.html", generateEpisodeValue(episodeValue) + "/index.html");
        }

    }

}