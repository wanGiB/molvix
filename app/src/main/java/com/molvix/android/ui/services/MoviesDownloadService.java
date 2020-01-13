package com.molvix.android.ui.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MoviesDownloadService extends JobIntentService {

    private static final int JOB_ID = 1;
    private static final String TAG = MoviesDownloadService.class.getSimpleName();

    private String TV_SERIES_URL = "https://o2tvseries.com/search/list_all_tv_series";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, MoviesDownloadService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String sampleTitle = "https://o2tvseries.com/Medical-Police/index.html";
        String sampleTwoTitle = "https://o2tvseries.com/The-Owl-House/index.html";
        String sampleThreeTitle = "https://o2tvseries.com/Deadwater-Fell/index.html";
        try {
            Document movieDoc = Jsoup.connect(sampleThreeTitle).get();
            Element movieInfoElement = movieDoc.select("div.tv_series_info").first();
            String movieArtUrl = movieInfoElement.select("div.img>img").attr("src");
            String movieDescription = movieInfoElement.select("div.serial_desc").text();
            Log.d(TAG, "MovieArtUrl: " + movieArtUrl);
            Log.d(TAG, "MovieDescription: " + movieDescription);
            Element movieSeasons = movieDoc.select("div.data_list").first();
            if (movieSeasons != null) {
                for (Element seasonElement : movieSeasons.children()) {
                    Pair<String, String> seasonDetails = getSeasonTitleAndLink(seasonElement);
                    Log.d(TAG, "SeasonDetails: " + seasonDetails.first + "," + seasonDetails.second);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to load Document because of " + e.getMessage());
        }
    }

    private void loadMoviesTitlesAndLinks() throws IOException {
        Document document = Jsoup.connect(TV_SERIES_URL).get();
        Element moviesTitlesAndLinks = document.getElementsByClass("data_list").first();
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

    private Pair<String, String> getSeasonTitleAndLink(Element element) {
        String seasonLink = element.select("div>a").attr("href");
        String seasonTitle = element.text();
        return new Pair<>(seasonTitle, seasonLink);
    }

    private void extractMetaDataFromLink(String movieLink) {
        try {
            Document movieInfoDoc = Jsoup.connect(movieLink).get();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error loading movie document due to " + e.getMessage());
        }
    }

}
