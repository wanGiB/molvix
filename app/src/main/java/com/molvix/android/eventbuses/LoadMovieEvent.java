package com.molvix.android.eventbuses;

public class LoadMovieEvent {
    private String movieId;
    public LoadMovieEvent(String movieId){
        this.movieId = movieId;
    }

    public String getMovieId() {
        return movieId;
    }
}
