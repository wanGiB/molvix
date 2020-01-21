package com.molvix.android.managers;

import android.util.Log;
import android.util.Pair;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.MolvixDB;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                MolvixDB.performBulkInsertionOfMovies(movies);
            }
        }
    }

    public static void fetchNotifications() {
        try {
            String TV_SERIES_URL = "https://o2tvseries.com";
            Document document = Jsoup.connect(TV_SERIES_URL).get();
            Element update = document.selectFirst("div.data_list");
            if (update != null) {
                Elements updates = update.children();
                for (Element updateItem : updates) {
                    String updateTitle = updateItem.text();
                    String movieTitle = StringUtils.stripStart(StringUtils.stripEnd(StringUtils.substringBefore(updateTitle, "- Season"), "-"), "-").trim();
                    String secondProcessed = updateTitle.replace(movieTitle, "");
                    String seasonName = StringUtils.stripEnd(StringUtils.stripStart(StringUtils.substringBefore(secondProcessed, "- Episode"), "-"), "-").trim();
                    String thirdProcessed = secondProcessed.replace(seasonName, "");
                    String episodeName = StringUtils.stripStart(StringUtils.stripEnd(StringUtils.substringBeforeLast(thirdProcessed, "-"), "-"), "-").trim();
                    if (StringUtils.isNotEmpty(movieTitle) && StringUtils.isNotEmpty(seasonName) && StringUtils.isNotEmpty(episodeName)) {
                        Log.d(ContentManager.class.getSimpleName(), "Notif Text=" + updateTitle);
                        Log.d(ContentManager.class.getSimpleName(), "MovieTitle=" + movieTitle + "; SeasonName=" + seasonName + "; EpisodeName=" + episodeName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(e);
        }
    }

    private static Pair<String, String> getMovieTitleAndLink(Element element) {
        String movieLink = element.select("div>a").attr("href");
        String movieTitle = element.text();
        return new Pair<>(movieTitle, movieLink);
    }

    public static void extractMetaDataFromMovieLink(String movieLink, String movieId) {
        if (!MovieManager.canRefreshMovieDetails(movieId)) {
            return;
        }
        Log.d(ContentManager.class.getSimpleName(), "Loading Details for " + movieLink);
        try {
            Document movieDoc = Jsoup.connect(movieLink).get();
            Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
            String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
            String movieDescription = movieInfoElement.select("div.serial_desc").text();
            Movie updatableMovie = MolvixDB.getMovie(movieId);
            if (updatableMovie != null) {
                if (StringUtils.isNotEmpty(movieArtUrl)) {
                    updatableMovie.setMovieArtUrl(movieArtUrl);
                }
                if (StringUtils.isNotEmpty(movieDescription)) {
                    updatableMovie.setMovieDescription(movieDescription);
                }
                extractOtherMovieDataParts(movieLink, movieDoc, updatableMovie);
            }
        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(e);
        }
    }

    private static void extractOtherMovieDataParts(String movieLink, Document movieDoc, Movie updatableMovie) {
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
                Season season = generateNewSeason(updatableMovie, seasonAtI, seasonName);
                List<Season> existingSeasons = updatableMovie.getMovieSeasons();
                if (!existingSeasons.contains(season)) {
                    existingSeasons.add(season);
                    updatableMovie.setMovieSeasons(existingSeasons);
                }
            }
            MolvixDB.updateMovie(updatableMovie);
            MovieManager.addToRefreshedMovies(updatableMovie.getMovieId());
        }
    }

    private static Season generateNewSeason(Movie movie, String seasonAtI, String seasonName) {
        String seasonId = CryptoUtils.getSha256Digest(seasonAtI);
        Season season = MolvixDB.getSeason(seasonId);
        if (season == null) {
            season = new Season();
            season.setSeasonId(seasonId);
            season.setSeasonName(seasonName);
            season.setMovieId(movie.getMovieId());
            season.setSeasonLink(seasonAtI);
            MolvixDB.createNewSeason(season);
        }
        return season;
    }

    private static Episode generateNewEpisode(Season season, String episodeLink, String episodeName) {
        String episodeId = CryptoUtils.getSha256Digest(episodeLink);
        Episode episode = MolvixDB.getEpisode(episodeId);
        if (episode == null) {
            episode = new Episode();
            episode.setEpisodeId(episodeId);
            episode.setEpisodeLink(episodeLink);
            episode.setEpisodeName(episodeName);
            episode.setDownloadProgress(-1);
            episode.setMovieId(season.getMovieId());
            episode.setSeasonId(season.getSeasonId());
            MolvixDB.createNewEpisode(episode);
        }
        return episode;
    }

    public static void extractMetaDataFromMovieSeasonLink(String seasonLink, String seasonId) {
        if (!SeasonsManager.canRefreshSeason(seasonId)) {
            return;
        }
        try {
            Log.d(ContentManager.class.getSimpleName(), "Preparing to load season details " + seasonLink);
            Season updatableSeason = MolvixDB.getSeason(seasonId);
            if (updatableSeason != null) {
                int totalNumberOfEpisodes = getTotalNumberOfEpisodes(seasonLink);
                if (totalNumberOfEpisodes != 0) {
                    for (int i = 0; i < totalNumberOfEpisodes; i++) {
                        String episodeLink = generateEpisodeFromSeasonLink(updatableSeason.getSeasonLink(), i + 1);
                        if (i == totalNumberOfEpisodes - 1) {
                            episodeLink = checkForSeasonFinale(episodeLink);
                        }
                        String episodeName = generateEpisodeValue(i + 1);
                        if (StringUtils.containsIgnoreCase(episodeLink, getSeasonFinaleSuffix())) {
                            episodeName = generateEpisodeValue(i + 1) + getSeasonFinaleSuffix();
                        }
                        Episode newEpisode = generateNewEpisode(updatableSeason, episodeLink, episodeName);
                        List<Episode> existingEpisodes = updatableSeason.getEpisodes();
                        if (!existingEpisodes.contains(newEpisode)) {
                            existingEpisodes.add(newEpisode);
                            updatableSeason.setEpisodes(existingEpisodes);
                        }
                    }
                    MolvixDB.updateSeason(updatableSeason);
                    Log.d(ContentManager.class.getSimpleName(), "Updating Season Details for Season " + updatableSeason.getSeasonName());
                    SeasonsManager.addToRefreshedSeasons(updatableSeason.getSeasonId());
                } else {
                    Log.d(ContentManager.class.getSimpleName(), "Updatable Season episodes count is zero");
                }
            } else {
                Log.d(ContentManager.class.getSimpleName(), "Updatable Season is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
