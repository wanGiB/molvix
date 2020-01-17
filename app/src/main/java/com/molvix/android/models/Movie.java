package com.molvix.android.models;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Movie extends RealmObject {

    @PrimaryKey
    private String movieId;
    private String movieName;
    private String movieLink;
    private String movieDescription;
    private String movieArtUrl;
    private RealmList<Season> movieSeasons;
    private boolean ad;
    private boolean recommendedToUser;
    private boolean seenByUser;

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

    public RealmList<Season> getMovieSeasons() {
        return movieSeasons;
    }

    public void setMovieSeasons(RealmList<Season> movieSeasons) {
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
