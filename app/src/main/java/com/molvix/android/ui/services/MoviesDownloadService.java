package com.molvix.android.ui.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.molvix.android.companions.AppConstants;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MoviesDownloadService extends JobIntentService {

    private static final int JOB_ID = 1;
    private static final String TAG = MoviesDownloadService.class.getSimpleName();

    private String TV_SERIES_URL = "https://o2tvseries.com/search/list_all_tv_series";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, MoviesDownloadService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String sampleLink = "https://o2tvseries.com/24-Legacy-8/Season-01/index.html";
        extractMetaDataFromMovieLink(sampleLink);
    }

    private void loadMoviesTitlesAndLinks() throws IOException {
        Document document = Jsoup.connect(TV_SERIES_URL).get();
        Element moviesTitlesAndLinks = document.selectFirst("div.data_list");
        if (moviesTitlesAndLinks != null) {
            Elements dataListElements = moviesTitlesAndLinks.children();
            for (Element element : dataListElements) {
                Pair<String, String> movieTitleAndLink = getMovieTitleAndLink(element);
                Log.d(TAG, movieTitleAndLink.toString());
            }
        }
    }

    private Pair<String, String> getMovieTitleAndLink(Element element) {
        String movieLink = element.select("div>a").attr("href");
        String movieTitle = element.text();
        return new Pair<>(movieTitle, movieLink);
    }

    private void extractMetaDataFromMovieLink(String movieLink) {
        try {
            Document movieDoc = Jsoup.connect(movieLink).get();
            Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
            String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
            String movieDescription = movieInfoElement.select("div.serial_desc").text();
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
            Log.d(TAG, "Number of Seasons=" + totalNumberOfSeasons);
            if (totalNumberOfSeasons != 0) {
                for (int i = 0; i < totalNumberOfSeasons; i++) {
                    String seasonAtI = generateSeasonFromMovieLink(movieLink, i + 1);
                    extractMetaDataFromMovieSeasonLink(seasonAtI);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error loading movie document due to " + e.getMessage());
        }
    }

    private void extractMetaDataFromMovieSeasonLink(String seasonLink) {
        try {
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
            Log.d(TAG, "Number of Episodes=" + totalNumberOfEpisodes);
            if (totalNumberOfEpisodes != 0) {
                for (int i = 0; i < totalNumberOfEpisodes; i++) {
                    String episodeAtI = generateEpisodeFromSeasonLink(seasonLink, i + 1);
                    if (i == totalNumberOfEpisodes - 1) {
                        episodeAtI = checkForSeasonFinale(episodeAtI);
                    }
                    Log.d(TAG, generateEpisodeValue(i + 1) + "=" + episodeAtI);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error loading season details for " + seasonLink);
        }
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
                    String text = link.text();
                    if (href.contains(AppConstants.DOWNLOADABLE)) {
                        Log.d(TAG,text+"="+link);
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
