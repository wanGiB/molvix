package com.molvix.android.managers;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.CryptoUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmList;

public class ContentManager {

    public static void grabMovies() throws IOException {
        loadMoviesTitlesAndLinks();
    }

    private static void loadMoviesTitlesAndLinks() throws IOException {
        String TV_SERIES_URL = "https://o2tvseries.com/search/list_all_tv_series";
        Document document = Jsoup.connect(TV_SERIES_URL).get();
        Element moviesTitlesAndLinks = document.selectFirst("div.data_list");
        if (moviesTitlesAndLinks != null) {
            Elements dataListElements = moviesTitlesAndLinks.children();
            List<Pair<String, String>> movies = new ArrayList<>();
            for (Element element : dataListElements) {
                Pair<String, String> movieTitleAndLink = getMovieTitleAndLink(element);
                String movieTitle = movieTitleAndLink.first;
                String movieLink = movieTitleAndLink.second;
                if (StringUtils.isNotEmpty(movieTitle) && StringUtils.isNotEmpty(movieLink)) {
                    movies.add(movieTitleAndLink);
                }
            }
            if (!movies.isEmpty()) {
                performBulkInsertionOfMovies(movies);
            }
        }
    }

    public static void fetchNotifications() {
        try {
            String TV_SERIES_URL = "https://o2tvseries.com";
            Document document = Jsoup.connect(TV_SERIES_URL).get();
            Element update = document.selectFirst("div.data_list");
            if (update != null) {
                Log.d("NotifLogs", "Data List Found");
                Elements updates = update.children();
                for (Element updateItem : updates) {
                    String updateTitle = updateItem.text();
                    Log.d("NotifLogs", updateTitle);
                    String movieTitle = StringUtils.substringBefore(updateTitle, "- Season");
                }
            } else {
                Log.d("NotifLogs", "Data List Not Found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(e);
        }
    }

    private static void performBulkInsertionOfMovies(List<Pair<String, String>> movies) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransactionAsync(r -> {
                for (Pair<String, String> movieItem : movies) {
                    String movieTitle = movieItem.first;
                    String movieLink = movieItem.second;
                    String movieId = CryptoUtils.getSha256Digest(movieLink);
                    //Create Movie Objects only once
                    Movie newMovie = r.where(Movie.class).equalTo(AppConstants.MOVIE_ID, movieId).findFirst();
                    if (newMovie != null) {
                        return;
                    }
                    newMovie = r.createObject(Movie.class, movieId);
                    newMovie.setMovieName(movieTitle.toLowerCase());
                    newMovie.setMovieLink(movieLink);
                    r.copyToRealm(newMovie);
                }
            });
        }
    }

    private static Pair<String, String> getMovieTitleAndLink(Element element) {
        String movieLink = element.select("div>a").attr("href");
        String movieTitle = element.text();
        return new Pair<>(movieTitle, movieLink);
    }

    public static void extractMetaDataFromMovieLink(String movieLink, String movieId) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Document movieDoc = Jsoup.connect(movieLink).get();
            Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
            String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
            String movieDescription = movieInfoElement.select("div.serial_desc").text();
            realm.executeTransaction(r -> {
                Movie updatableMovie = r.where(Movie.class).equalTo(AppConstants.MOVIE_ID, movieId).findFirst();
                if (updatableMovie != null) {
                    if (StringUtils.isNotEmpty(movieArtUrl)) {
                        updatableMovie.setMovieArtUrl(movieArtUrl);
                    }
                    if (StringUtils.isNotEmpty(movieDescription)) {
                        updatableMovie.setMovieDescription(movieDescription);
                    }
                    r.copyToRealmOrUpdate(updatableMovie, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    extractOtherMovieDataParts(movieLink, movieDoc, r, updatableMovie);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(e);
        }
    }

    private static void extractOtherMovieDataParts(String movieLink, Document movieDoc, Realm r, Movie updatableMovie) {
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
                Season season = generateNewSeason(r, updatableMovie, seasonAtI, seasonName);
                if (!updatableMovie.getMovieSeasons().contains(season)) {
                    updatableMovie.getMovieSeasons().add(season);
                }
            }
            r.copyToRealmOrUpdate(updatableMovie, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
        }
    }

    private static Season generateNewSeason(Realm realm, Movie movie, String seasonAtI, String seasonName) {
        String seasonId = CryptoUtils.getSha256Digest(seasonAtI);
        Season season = realm.where(Season.class).equalTo(AppConstants.SEASON_ID, seasonId).findFirst();
        if (season == null) {
            season = realm.createObject(Season.class, seasonId);
            season.setSeasonName(seasonName);
            season.setMovieId(movie.getMovieId());
            season.setSeasonLink(seasonAtI);
            realm.copyToRealm(season);
        }
        return season;
    }

    private static Episode generateNewEpisode(Realm realm, Season season, String episodeLink, String episodeName) {
        String episodeId = CryptoUtils.getSha256Digest(episodeLink);
        Episode episode = realm.where(Episode.class).equalTo(AppConstants.EPISODE_ID, episodeId).findFirst();
        if (episode == null) {
            episode = realm.createObject(Episode.class, episodeId);
            episode.setEpisodeLink(episodeLink);
            episode.setEpisodeName(episodeName);
            episode.setDownloadProgress(-1);
            episode.setMovieId(season.getMovieId());
            episode.setSeasonId(season.getSeasonId());
            realm.copyToRealm(episode);
        }
        return episode;
    }

    public static void extractMetaDataFromMovieSeasonLink(String seasonLink, String seasonId) {
        Log.d(ContentManager.class.getSimpleName(), "Preparing to load season details " + seasonLink);
        try (Realm realm = Realm.getDefaultInstance()) {
            Season updatableSeason = realm.where(Season.class).equalTo(AppConstants.SEASON_ID, seasonId).findFirst();
            if (updatableSeason != null) {
                int totalNumberOfEpisodes = getTotalNumberOfEpisodes(seasonLink);
                if (totalNumberOfEpisodes != 0) {
                    realm.executeTransaction(r -> {
                        for (int i = 0; i < totalNumberOfEpisodes; i++) {
                            String episodeLink = generateEpisodeFromSeasonLink(updatableSeason.getSeasonLink(), i + 1);
                            if (i == totalNumberOfEpisodes - 1) {
                                episodeLink = checkForSeasonFinale(episodeLink);
                            }
                            String episodeName = generateEpisodeValue(i + 1);
                            if (StringUtils.containsIgnoreCase(episodeLink, getSeasonFinaleSuffix())) {
                                episodeName = generateEpisodeValue(i + 1) + getSeasonFinaleSuffix();
                            }
                            Episode newEpisode = generateNewEpisode(r, updatableSeason, episodeLink, episodeName);
                            if (!updatableSeason.getEpisodes().contains(newEpisode)) {
                                updatableSeason.getEpisodes().add(newEpisode);
                                Log.d(ContentManager.class.getSimpleName(), "Adding New Episode " + newEpisode.getEpisodeName() + " to season episodes");
                            }
                        }
                        Log.d(ContentManager.class.getSimpleName(), "Updating Season Details for Season " + updatableSeason.getSeasonName());
                        r.copyToRealmOrUpdate(updatableSeason, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(ContentManager.class.getSimpleName(), "Error Updating Season Details");
            EventBus.getDefault().post(e);
        }
    }

    private static int getTotalNumberOfEpisodes(String seasonLink) throws IOException {
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

    private static String checkForSeasonFinale(String episodeLink) {
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

    private static String generateSeasonFinaleForEpisode(String episodeLink) {
        String episodeLinkRip = StringUtils.removeEnd(episodeLink, "/index.html");
        return episodeLinkRip + getSeasonFinaleSuffix() + "/index.html";
    }

    private static String getSeasonFinaleSuffix() {
        return "-Season-Finale";
    }

    private static String generateSeasonValue(int value) {
        if (value < 10) {
            return "Season-0" + value;
        }
        return "Season-" + value;
    }

    private static String generateEpisodeValue(int value) {
        if (value < 10) {
            return "Episode-0" + value;
        }
        return "Episode-" + value;
    }

    private static String generateSeasonFromMovieLink(String movieLink, int seasonValue) {
        return movieLink.replace("index.html", generateSeasonValue(seasonValue) + "/index.html");
    }

    private static String generateEpisodeFromSeasonLink(String seasonLink, int episodeValue) {
        return seasonLink.replace("index.html", generateEpisodeValue(episodeValue) + "/index.html");
    }

}
