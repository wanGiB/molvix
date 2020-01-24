package com.molvix.android.models;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
public class Movie {

    @Id
    public long id;
    public String movieId;
    public String movieName;
    public String movieLink;
    public String movieDescription;
    public String movieArtUrl;
    @Backlink(to = "movie")
    public ToMany<Season> seasons;
    public boolean ad;
    public boolean recommendedToUser;
    public boolean seenByUser;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public ToMany<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(ToMany<Season> seasons) {
        this.seasons = seasons;
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

    @NonNull
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
