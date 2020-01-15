package com.molvix.android.jobs;

import android.util.Pair;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.LocalDbUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContentMiner {

    public static void mineData() throws IOException {
        loadMoviesTitlesAndLinks();
    }

    private static void loadMoviesTitlesAndLinks() throws IOException {
        String TV_SERIES_URL = "https://o2tvseries.com/search/list_all_tv_series";
        Document document = Jsoup.connect(TV_SERIES_URL).get();
        Element moviesTitlesAndLinks = document.selectFirst("div.data_list");
        if (moviesTitlesAndLinks != null) {
            Elements dataListElements = moviesTitlesAndLinks.children();
            List<Movie> movies = new ArrayList<>();
            for (Element element : dataListElements) {
                Pair<String, String> movieTitleAndLink = getMovieTitleAndLink(element);
                String movieTitle = movieTitleAndLink.first;
                String movieLink = movieTitleAndLink.second;
                if (StringUtils.isNotEmpty(movieTitle) && StringUtils.isNotEmpty(movieLink)) {
                    String movieId = CryptoUtils.getSha256Digest(movieLink);
                    Movie newMovie = new Movie();
                    newMovie.setMovieId(movieId);
                    newMovie.setMovieName(movieTitle.toLowerCase());
                    newMovie.setMovieLink(movieLink);
                    movies.add(newMovie);
                }
            }
            if (!movies.isEmpty()) {
                LocalDbUtils.performBulkInsertionOfMovies(movies);
            }
        }
    }

    private static Pair<String, String> getMovieTitleAndLink(Element element) {
        String movieLink = element.select("div>a").attr("href");
        String movieTitle = element.text();
        return new Pair<>(movieTitle, movieLink);
    }

    public static void extractMetaDataFromMovieLink(String movieLink, Movie movie) {
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
                List<Season> seasons = new ArrayList<>();
                if (movie.getMovieSeasons() != null) {
                    seasons = movie.getMovieSeasons();
                }
                for (int i = 0; i < totalNumberOfSeasons; i++) {
                    String seasonAtI = generateSeasonFromMovieLink(movieLink, i + 1);
                    String seasonName = generateSeasonValue(i + 1);
                    Season season = new Season();
                    season.setSeasonName(seasonName);
                    season.setMovieId(movie.getMovieId());
                    season.setSeasonLink(seasonAtI);
                    season.setSeasonId(CryptoUtils.getSha256Digest(seasonAtI));
                    if (!seasons.contains(season)) {
                        seasons.add(season);
                    }
                }
                movie.setMovieSeasons(seasons);
                movie.update();
            }
        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void extractMetaDataFromMovieSeasonLink(Season season) {
        try {
            int totalNumberOfEpisodes = getTotalNumberOfEpisodes(season.getSeasonLink());
            if (totalNumberOfEpisodes != 0) {
                List<Episode> episodesList = new ArrayList<>();
                for (int i = 0; i < totalNumberOfEpisodes; i++) {
                    String episodeLink = generateEpisodeFromSeasonLink(season.getSeasonLink(), i + 1);
                    if (i == totalNumberOfEpisodes - 1) {
                        episodeLink = checkForSeasonFinale(episodeLink);
                    }
                    String episodeName = generateEpisodeValue(i + 1);
                    if (StringUtils.containsIgnoreCase(episodeLink, getSeasonFinaleSuffix())) {
                        episodeName = generateEpisodeValue(i + 1) + getSeasonFinaleSuffix();
                    }
                    Episode newEpisode = generateNewEpisode(season, episodeLink, episodeName);
                    episodesList.add(newEpisode);
                }
                season.setEpisodes(episodesList);
                season.update();
            }
        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(e);
        }
    }

    private static Episode generateNewEpisode(Season season, String episodeLink, String episodeName) {
        String episodeId = CryptoUtils.getSha256Digest(episodeLink);
        Episode newEpisode = new Episode();
        newEpisode.setEpisodeId(episodeId);
        newEpisode.setEpisodeLink(episodeLink);
        newEpisode.setEpisodeName(episodeName);
        newEpisode.setMovieId(season.getMovieId());
        newEpisode.setSeasonId(season.getSeasonId());
        return newEpisode;
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
