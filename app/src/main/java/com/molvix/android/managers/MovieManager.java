package com.molvix.android.managers;

import com.molvix.android.preferences.AppPrefs;

public class MovieManager {

    public static boolean canFetchMovieDetails(String movieId) {
        return AppPrefs.canRefreshMovieDetails(movieId);
    }

    public static void clearAllRefreshedMovies() {
        AppPrefs.clearAllRefreshedMovies();
    }

    static void addToRefreshedMovies(String movieId) {
        AppPrefs.addToRefreshedMovies(movieId);
    }

    public static void setMovieRefreshable(String movieId) {
        AppPrefs.setMovieRefreshable(movieId);
    }

}
