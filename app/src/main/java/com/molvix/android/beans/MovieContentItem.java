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
    private String contentId;

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentId() {
        return contentId;
    }

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


    @Override
    public int hashCode() {
        int result;
        result = this.contentId.hashCode();
        final String name = getClass().getName();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        MovieContentItem another = (MovieContentItem) obj;
        return this.getContentId().equals(another.getContentId());
    }

}
