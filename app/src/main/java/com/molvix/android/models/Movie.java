package com.molvix.android.models;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.molvix.android.converters.SeasonsTypeConverter;
import com.molvix.android.database.MolvixDB;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
@Table(database = MolvixDB.class,
        primaryKeyConflict = ConflictAction.REPLACE,
        insertConflict = ConflictAction.REPLACE,
        updateConflict = ConflictAction.REPLACE)
public class Movie extends BaseModel {

    @PrimaryKey
    @Column
    @Expose
    public String movieId;

    @Nullable
    @Column
    @Expose
    public String movieName;

    @Nullable
    @Column
    @Expose
    public String movieLink;

    @Nullable
    @Column
    @Expose
    public String movieDescription;

    @Nullable
    @Column
    @Expose
    public String movieArtUrl;

    @Nullable
    @Column(typeConverter = SeasonsTypeConverter.class)
    @Expose
    public List<Season> movieSeasons;

    @Column
    @Expose
    boolean ad;

    @Column
    @Expose
    boolean recommendedToUser;

    @Column
    @Expose
    boolean seenByUser;

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

    public void setAd(boolean ad) {
        this.ad = ad;
    }

    public boolean isAd() {
        return ad;
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

    public void setMovieSeasons(@Nullable List<Season> movieSeasons) {
        this.movieSeasons = movieSeasons;
    }

    @Nullable
    public List<Season> getMovieSeasons() {
        return movieSeasons;
    }

    public void setMovieLink(String movieLink) {
        this.movieLink = movieLink;
    }

    public String getMovieLink() {
        return movieLink;
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
