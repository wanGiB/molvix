package com.molvix.android.models;

import com.molvix.android.utils.MolvixDB;
import com.orm.dsl.Table;
import com.orm.dsl.Unique;

import java.util.ArrayList;
import java.util.List;

@Table
public class Movie {

    @Unique
    private String movieId;
    private String movieName;
    private String movieLink;
    private String movieDescription;
    private String movieArtUrl;
    private List<Season> movieSeasons;
    private boolean ad;
    private boolean recommendedToUser;
    private boolean seenByUser;

    public Movie() {

    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getMovieLink() {
        return movieLink;
    }

    public void setMovieLink(String movieLink) {
        this.movieLink = movieLink;
    }

    public String getMovieDescription() {
        return movieDescription;
    }

    public void setMovieDescription(String movieDescription) {
        this.movieDescription = movieDescription;
    }

    public String getMovieArtUrl() {
        return movieArtUrl;
    }

    public void setMovieArtUrl(String movieArtUrl) {
        this.movieArtUrl = movieArtUrl;
    }

    public List<Season> getMovieSeasons() {
        List<Season> updatedSeasons = new ArrayList<>();
        for (Season s : movieSeasons) {
            Season newSeason = MolvixDB.getSeason(s.getSeasonId());
            updatedSeasons.add(newSeason);
        }
        return updatedSeasons;
    }

    public void setMovieSeasons(List<Season> movieSeasons) {
        this.movieSeasons = movieSeasons;
    }

    public boolean isAd() {
        return ad;
    }

    public void setAd(boolean ad) {
        this.ad = ad;
    }

    public boolean isRecommendedToUser() {
        return recommendedToUser;
    }

    public void setRecommendedToUser(boolean recommendedToUser) {
        this.recommendedToUser = recommendedToUser;
    }

    public boolean isSeenByUser() {
        return seenByUser;
    }

    public void setSeenByUser(boolean seenByUser) {
        this.seenByUser = seenByUser;
    }

    @Override
    public int hashCode() {
        int result;
        result = this.movieId.hashCode();
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
        Movie another = (Movie) obj;
        return this.getMovieId().equals(another.getMovieId());
    }

}
