package com.molvix.android.beans;

import com.molvix.android.models.Movie;
import com.molvix.android.models.Season;

public class MovieContentItem {

    public enum ContentType {
        AD,
        MOVIE_HEADER,
        GROUP_HEADER
    }

    private ContentType contentType;
    private Movie movie;
    private Season season;

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Season getSeason() {
        return season;
    }

}
