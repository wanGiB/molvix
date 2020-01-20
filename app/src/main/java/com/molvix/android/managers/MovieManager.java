package com.molvix.android.managers;

import com.molvix.android.preferences.AppPrefs;

public class MovieManager {

    public static boolean canRefreshMovieDetails(String movieId) {
        return AppPrefs.canRefreshMovieDetails(movieId);
    }

    public static void clearAllRefreshedMovies() {
        AppPrefs.clearAllRefreshedMovies();
    }

    public static void addToRefreshedMovies(String movieId) {
        AppPrefs.addToRefreshedMovies(movieId);
    }

}
