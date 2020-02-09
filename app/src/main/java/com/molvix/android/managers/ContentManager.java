package com.molvix.android.managers;

import android.util.Pair;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.contracts.DoneCallback;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Movie_;
import com.molvix.android.models.Notification;
import com.molvix.android.models.Presets;
import com.molvix.android.models.Season;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.ConnectivityUtils;
import com.molvix.android.utils.CryptoUtils;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.NetworkClient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ContentManager {

    public static void fetchPresets() {
        if (ConnectivityUtils.isDeviceConnectedToTheInternet()) {
            if (StringUtils.isNotEmpty(AppConstants.PRESETS_DOWNSTREAM_URL)) {
                HttpUrl.Builder presetsUrlBuilder = Objects.requireNonNull(HttpUrl.parse(AppConstants.PRESETS_DOWNSTREAM_URL)).newBuilder();
                Request.Builder presetsRequestBuilder = new Request.Builder();
                presetsRequestBuilder.url(presetsUrlBuilder.build());
                presetsRequestBuilder.get();
                NetworkClient.getOkHttpClient().newCall(presetsRequestBuilder.build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        ResponseBody responseBody = response.body();
                        int responseCode = response.code();
                        MolvixLogger.d(ContentManager.class.getSimpleName(), "ResponseCode After Fetching Presets=" + responseCode);
                        String responseMessage = response.message();
                        MolvixLogger.d(ContentManager.class.getSimpleName(), "ResponseMessage After Fetching Presets\n" + responseMessage);
                        if (responseBody != null) {
                            try {
                                String responseBodyString = responseBody.string();
                                MolvixLogger.d(ContentManager.class.getSimpleName(), "FetchedPresets\n" + responseBodyString);
                                ContentManager.offloadPresets(responseBodyString);
                            } catch (IOException e) {
                                e.printStackTrace();
                                String errorMessage = e.getMessage();
                                if (errorMessage != null) {
                                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Error after Presets Fetch: " + errorMessage);
                                }
                            }

                        }
                    }
                });
            }
        }
    }

    private static void offloadPresets(String responseBodyString) {
        MolvixLogger.d(ContentManager.class.getSimpleName(), "Offloading Fetched Presets");
        try {
            JSONObject presetsObject = new JSONObject(responseBodyString);
            if (presetsObject.length() > 0) {
                Presets presets = MolvixDB.getPresets();
                if (presets == null) {
                    presets = new Presets();
                }
                String existingPresets = presets.getPresetString();
                if (existingPresets == null) {
                    presets.setPresetString(responseBodyString);
                    MolvixDB.updatePreset(presets);
                    updateMoviesWithPresets(presetsObject);
                } else {
                    if (!existingPresets.equals(responseBodyString)) {
                        presets.setPresetString(responseBodyString);
                        MolvixDB.updatePreset(presets);
                        updateMoviesWithPresets(presetsObject);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
        }
    }

    private static void updateMoviesWithPresets(JSONObject presetsObject) {
        //Update associated Movies Art Urls
        JSONArray dataObject = presetsObject.optJSONArray(AppConstants.DATA);
        if (dataObject != null && dataObject.length() > 0) {
            List<Movie> updatableMovies = new ArrayList<>();
            for (int i = 0; i < dataObject.length(); i++) {
                JSONObject movieObject = dataObject.optJSONObject(i);
                if (movieObject != null) {
                    String movieName = movieObject.optString(AppConstants.MOVIE_NAME);
                    String movieArtUrl = movieObject.optString(AppConstants.MOVIE_ART_URL);
                    //Find a movie whose name is = movieName
                    if (StringUtils.isNotEmpty(movieArtUrl)) {
                        AppConstants.MOVIE_NAME_TO_ART_URL_MAP.put(movieName.toLowerCase().trim(), movieArtUrl);
                    }
                    Movie movie = MolvixDB.getMovieBox().query().equal(Movie_.movieName, movieName.toLowerCase().trim()).build().findFirst();
                    if (movie != null) {
                        String existingArtUrl = movie.getMovieArtUrl();
                        if (existingArtUrl != null && StringUtils.isNotEmpty(movieArtUrl) && !existingArtUrl.equals(movieArtUrl)) {
                            AppConstants.canShuffleExistingMovieCollection.set(false);
                            MolvixLogger.d(ContentManager.class.getSimpleName(), "Updating Art Url for " + movie.getMovieName());
                            movie.setMovieArtUrl(movieArtUrl);
                            if (!updatableMovies.contains(movie)) {
                                updatableMovies.add(movie);
                            }
                        }
                    }
                }
            }
            if (!updatableMovies.isEmpty()) {
                AppConstants.canShuffleExistingMovieCollection.set(false);
                MolvixDB.getMovieBox().put(updatableMovies);
            }
        }
    }

    public static void grabMovies() throws Exception {
        loadMoviesTitlesAndLinks();
    }

    private static void loadMoviesTitlesAndLinks() throws Exception {
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
                    String realMovieTitle = StringUtils.strip(movieTitle, "-").trim();
                    String realSeasonName = StringUtils.strip(seasonName, "-").trim();
                    Movie result = MolvixDB.getMovieBox()
                            .query()
                            .equal(Movie_.movieName, realMovieTitle.toLowerCase())
                            .equal(Movie_.seenByUser, true)
                            .build()
                            .findFirst();
                    if (result != null) {
                        String realEpisodeName = StringUtils.strip(episodeName, "-").trim();
                        String message = "<b>" + realMovieTitle + "</b>" + "/" + "<b>" + realSeasonName + "</b>" + "/" + "<b>" + realEpisodeName + "</b>" + " is out.";
                        String displayMessage = "<b>" + realSeasonName + "</b>" + "/" + "<b>" + realEpisodeName + "</b>" + " is out.";
                        String checkKey = CryptoUtils.getSha256Digest(realMovieTitle + "/" + realSeasonName + "/" + realEpisodeName);
                        boolean hasBeenNotified = AppPrefs.hasBeenNotified(checkKey);
                        if (!hasBeenNotified) {
                            Notification existingNotification = MolvixDB.getNotification(checkKey);
                            if (existingNotification != null) {
                                return;
                            }
                            Notification newMovieAvailableNotification = new Notification();
                            newMovieAvailableNotification.setNotificationObjectId(checkKey);
                            newMovieAvailableNotification.setMessage(message);
                            newMovieAvailableNotification.setTimeStamp(System.currentTimeMillis());
                            newMovieAvailableNotification.setDestination(AppConstants.DESTINATION_NEW_EPISODE_AVAILABLE);
                            newMovieAvailableNotification.setDestinationKey(result.getMovieId());
                            MolvixDB.createNewNotification(newMovieAvailableNotification);
                            MolvixNotificationManager.displayNewMovieNotification(result, displayMessage, newMovieAvailableNotification, checkKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
        }
    }

    public static void cleanUpDeletedContents() {
        List<Notification> notifications = MolvixDB.getNotificationBox().query().build().find();
        if (!notifications.isEmpty()) {
            for (Notification notification : notifications) {
                if (notification.getDestination() == AppConstants.DESTINATION_DOWNLOADED_EPISODE) {
                    Episode episode = MolvixDB.getEpisode(notification.getDestinationKey());
                    if (episode != null) {
                        if (isContentDeleted(episode)) {
                            MolvixDB.getNotificationBox().remove(notification);
                        }
                    }
                }
            }
        }
    }

    private static boolean isContentDeleted(Episode episode) {
        String fileName = episode.getEpisodeName() + ".mp4";
        File downloadedFile = FileUtils.getFilePath(fileName, WordUtils.capitalize(episode.getSeason().getMovie().getMovieName()), WordUtils.capitalize(episode.getSeason().getSeasonName()));
        return !downloadedFile.exists();
    }

    private static Pair<String, String> getMovieTitleAndLink(Element element) {
        String movieLink = element.select("div>a").attr("href");
        String movieTitle = element.text();
        return new Pair<>(movieTitle, movieLink);
    }

    public static void extractMovieMetaData(Movie movie) {
        if (!MovieManager.canFetchMovieDetails(movie.getMovieId())) {
            return;
        }
        try {
            Document movieDoc = Jsoup.connect(movie.getMovieLink()).get();
            Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
            String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
            updateMovieArtUrlAndDescription(movie, movieInfoElement, movieArtUrl);
            extractOtherMovieMetaDataParts(movie, movieDoc);
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
        }
    }

    public static void extractMovieMetaData(Movie movie, DoneCallback<Movie> movieExtractionDoneCallback) {
        try {
            Document movieDoc = Jsoup.connect(movie.getMovieLink()).get();
            Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
            String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
            updateMovieArtUrlAndDescription(movie, movieInfoElement, movieArtUrl);
            extractOtherMovieMetaDataParts(movie, movieDoc, movieExtractionDoneCallback);
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
            movieExtractionDoneCallback.done(null, e);
        }
    }

    private static String getMovieArtUrlFromPresets(String movieName) {
        return AppConstants.MOVIE_NAME_TO_ART_URL_MAP.get(movieName.toLowerCase().trim());
    }

    private static void updateMovieArtUrlAndDescription(Movie movie, Element movieInfoElement, String movieArtUrl) {
        String movieDescription = movieInfoElement.select("div.serial_desc").text();
        String presetArtUrl = getMovieArtUrlFromPresets(movie.getMovieName());
        String existingMovieArtUrl = movie.getMovieArtUrl();
        if (StringUtils.isNotEmpty(presetArtUrl)) {
            if (StringUtils.isEmpty(existingMovieArtUrl)) {
                movie.setMovieArtUrl(presetArtUrl);
            } else {
                if (!presetArtUrl.equals(existingMovieArtUrl)) {
                    movie.setMovieArtUrl(presetArtUrl);
                }
            }
        } else {
            if (StringUtils.isNotEmpty(movieArtUrl)) {
                if (StringUtils.isEmpty(existingMovieArtUrl)) {
                    movie.setMovieArtUrl(movieArtUrl);
                } else {
                    if (!existingMovieArtUrl.equals(movieArtUrl)) {
                        movie.setMovieArtUrl(movieArtUrl);
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(movieDescription)) {
            movie.setMovieDescription(movieDescription);
        }
    }

    private static void spitException(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), errorMessage);
        }
        EventBus.getDefault().post(e);
    }

    private static void extractOtherMovieMetaDataParts(Movie movie, Document movieDoc, DoneCallback<Movie> movieExtractionDoneCallBack) {
        int totalNumberOfSeasons = getTotalNumberOfSeasons(movieDoc);
        if (totalNumberOfSeasons != 0) {
            loadInMovieSeasons(movie, totalNumberOfSeasons);
            MolvixDB.updateMovie(movie);
            movieExtractionDoneCallBack.done(movie, null);
        }
    }

    private static void extractOtherMovieMetaDataParts(Movie movie, Document movieDoc) {
        int totalNumberOfSeasons = getTotalNumberOfSeasons(movieDoc);
        if (totalNumberOfSeasons != 0) {
            loadInMovieSeasons(movie, totalNumberOfSeasons);
            MolvixDB.updateMovie(movie);
            MovieManager.addToRefreshedMovies(movie.getMovieId());
        }
    }

    private static void loadInMovieSeasons(Movie movie, int totalNumberOfSeasons) {
        for (int i = 0; i < totalNumberOfSeasons; i++) {
            String seasonAtI = generateSeasonFromMovieLink(movie.getMovieLink(), i + 1);
            String seasonName = generateSeasonValue(i + 1);
            Season season = generateSeason(movie, seasonAtI, seasonName);
            if (!movie.seasons.contains(season)) {
                movie.seasons.add(season);
            }
        }
    }

    private static int getTotalNumberOfSeasons(Document movieDoc) {
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
        return totalNumberOfSeasons;
    }

    private static Season generateSeason(Movie movie, String seasonAtI, String seasonName) {
        String seasonId = CryptoUtils.getSha256Digest(seasonAtI);
        Season season = MolvixDB.getSeason(seasonId);
        if (season == null) {
            season = new Season();
            season.setSeasonId(seasonId);
            season.setSeasonName(seasonName);
            season.movie.setTarget(movie);
            season.setSeasonLink(seasonAtI);
            MolvixDB.createNewSeason(season);
        }
        return season;
    }

    private static Episode generateEpisode(Season season, String episodeLink, String episodeName) {
        String episodeId = CryptoUtils.getSha256Digest(episodeLink);
        Episode episode = MolvixDB.getEpisode(episodeId);
        if (episode == null) {
            episode = new Episode();
            episode.setEpisodeId(episodeId);
            episode.setEpisodeLink(episodeLink);
            episode.setEpisodeName(episodeName);
            episode.season.setTarget(season);
            MolvixDB.createNewEpisode(episode);
        }
        return episode;
    }

    public static void extractMovieSeasonMetaData(Season season) {
        if (!SeasonsManager.canFetchSeasonDetails(season.getSeasonId())) {
            return;
        }
        try {
            int totalNumberOfEpisodes = getTotalNumberOfEpisodes(season.getSeasonLink());
            if (totalNumberOfEpisodes != 0) {
                loadInMovieSeasonEpisodes(season, totalNumberOfEpisodes);
                MolvixDB.updateSeason(season);
                SeasonsManager.addToRefreshedSeasons(season.getSeasonId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
        }
    }

    public static void extractMovieSeasonMetaData(Season season, DoneCallback<Season> extractionDoneCallBack) {
        try {
            int totalNumberOfEpisodes = getTotalNumberOfEpisodes(season.getSeasonLink());
            if (totalNumberOfEpisodes != 0) {
                loadInMovieSeasonEpisodes(season, totalNumberOfEpisodes);
                MolvixDB.updateSeason(season);
                extractionDoneCallBack.done(season, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
            extractionDoneCallBack.done(null, e);
        }
    }

    private static void loadInMovieSeasonEpisodes(Season season, int totalNumberOfEpisodes) {
        for (int i = 0; i < totalNumberOfEpisodes; i++) {
            String episodeLink = generateEpisodeFromSeasonLink(season.getSeasonLink(), i + 1);
            if (i == totalNumberOfEpisodes - 1) {
                episodeLink = checkForSeasonFinale(episodeLink);
            }
            String episodeName = generateEpisodeValue(i + 1);
            if (StringUtils.containsIgnoreCase(episodeLink, getSeasonFinaleSuffix())) {
                episodeName = generateEpisodeValue(i + 1) + getSeasonFinaleSuffix();
            }
            Episode newEpisode = generateEpisode(season, episodeLink, episodeName);
            if (!season.episodes.contains(newEpisode)) {
                season.episodes.add(newEpisode);
            }
        }
    }

    private static int getTotalNumberOfEpisodes(String seasonLink) throws Exception {
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
                    episodeLink = generateSeasonFinaleFromEpisode(episodeLink);
                }
            } else {
                return episodeLink;
            }
        } catch (Exception e) {
            e.printStackTrace();
            spitException(e);
            return episodeLink;
        }
        return episodeLink;
    }

    private static String generateSeasonFinaleFromEpisode(String episodeLink) {
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
