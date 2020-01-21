package com.molvix.android.observers;

import com.molvix.android.contracts.OnContentChangedListener;
import com.molvix.android.models.Episode;
import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;

import java.util.List;

public class MolvixContentChangeObserver {

    public static void addMoviesChangedListener(OnContentChangedListener<List<Movie>> moviesChanged) {
        removeMoviesChangeListener();
    }

    public static void addMovieChangedListener(String movieId, OnContentChangedListener<Movie> movieOnContentChangedListener) {

    }

    public static void removeMovieChangeListener(String movieId) {

    }

    public static void removeMoviesChangeListener() {

    }

    public static void removeNotificationsChangeListener() {

    }

    public static void addChangeListenerOn(Episode episode, OnContentChangedListener<Episode> episodeOnContentChangedListener) {

    }

    public static void removeChangeListenerOnEpisode(Episode episode) {

    }

    public static void addChangeListenerOnSeason(Season season, OnContentChangedListener<Season> seasonOnContentChangedListener) {

    }

    public static void removeChangeListenerOnSeason(Season season) {

    }

}
