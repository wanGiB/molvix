package com.molvix.android.models;


import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.molvix.android.converters.EpisodesTypeConverter;
import com.molvix.android.database.MolvixDB;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused", "NullableProblems"})
@Table(database = MolvixDB.class,
        primaryKeyConflict = ConflictAction.REPLACE,
        insertConflict = ConflictAction.REPLACE,
        updateConflict = ConflictAction.REPLACE)
public class Season extends BaseModel {

    @PrimaryKey
    @Column
    @Expose
    public String seasonId;

    @Nullable
    @Column
    @Expose
    public String seasonLink;

    @Column
    @Expose
    public String movieId;

    @Column(typeConverter = EpisodesTypeConverter.class)
    @Expose
    public List<Episode> episodes;

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    @Nullable
    public String getSeasonLink() {
        return seasonLink;
    }

    public void setSeasonLink(@Nullable String seasonLink) {
        this.seasonLink = seasonLink;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }

}
