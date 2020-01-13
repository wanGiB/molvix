package com.molvix.android.models;


import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.molvix.android.converters.EpisodeQualityTypeConverter;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.enums.EpisodeQuality;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "unused", "NullableProblems"})
@Table(database = MolvixDB.class,
        primaryKeyConflict = ConflictAction.REPLACE,
        insertConflict = ConflictAction.REPLACE,
        updateConflict = ConflictAction.REPLACE)
public class Episode extends BaseModel implements Serializable {
    @PrimaryKey
    @Column
    @Expose
    public String episodeId;

    @Column
    @Expose
    public String movieId;

    @Column
    @Expose
    public String seasonId;

    @Nullable
    @Column
    @Expose
    public String episodeName;

    @Nullable
    @Column
    @Expose
    public String episodeLink;

    @Nullable
    @Column(typeConverter = EpisodeQualityTypeConverter.class)
    @Expose
    public EpisodeQuality episodeQuality;

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getEpisodeName() {
        return episodeName;
    }

    public void setEpisodeName(String episodeName) {
        this.episodeName = episodeName;
    }

    public String getEpisodeLink() {
        return episodeLink;
    }

    public void setEpisodeLink(String episodeLink) {
        this.episodeLink = episodeLink;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public EpisodeQuality getEpisodeQuality() {
        return episodeQuality;
    }

    public void setEpisodeQuality(EpisodeQuality episodeQuality) {
        this.episodeQuality = episodeQuality;
    }

}
