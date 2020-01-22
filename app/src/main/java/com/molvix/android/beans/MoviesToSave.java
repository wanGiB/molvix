package com.molvix.android.beans;

import android.util.Pair;

import java.util.List;

public class MoviesToSave {
    private List<Pair<String, String>> movies;

    public MoviesToSave(List<Pair<String, String>> movies) {
        this.movies = movies;
    }

    public List<Pair<String, String>> getMovies() {
        return movies;
    }

}
