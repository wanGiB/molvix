package com.molvix.android.models;


import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.molvix.android.converters.EpisodeQualityTypeConverter;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.enums.EpisodeQuality;
import com.molvix.android.managers.EpisodesManager;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
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

    @Nullable
    @Column
    @Expose
    public String highQualityDownloadLink;

    @Nullable
    @Column
    @Expose
    public String standardQualityDownloadLink;

    @Nullable
    @Column
    @Expose
    public String lowQualityDownloadLink;

    @Nullable
    @Column
    @Expose
    public String episodeCaptchaSolverLink;

    @Column
    @Expose
    public int downloadProgress = -1;

    @Nullable
    @Column
    public String progressDisplayText;

    public void setProgressDisplayText(@Nullable String progressDisplayText) {
        this.progressDisplayText = progressDisplayText;
    }

    @Nullable
    public String getProgressDisplayText() {
        return progressDisplayText;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setEpisodeCaptchaSolverLink(@Nullable String episodeCaptchaSolverLink) {
        this.episodeCaptchaSolverLink = episodeCaptchaSolverLink;
    }

    @Nullable
    public String getEpisodeCaptchaSolverLink() {
        return episodeCaptchaSolverLink;
    }

    @Nullable
    public String getHighQualityDownloadLink() {
        return highQualityDownloadLink;
    }

    public void setHighQualityDownloadLink(@Nullable String highQualityDownloadLink) {
        this.highQualityDownloadLink = highQualityDownloadLink;
    }

    @Nullable
    public String getStandardQualityDownloadLink() {
        return standardQualityDownloadLink;
    }

    public void setStandardQualityDownloadLink(@Nullable String standardQualityDownloadLink) {
        this.standardQualityDownloadLink = standardQualityDownloadLink;
    }

    @Nullable
    public String getLowQualityDownloadLink() {
        return lowQualityDownloadLink;
    }

    public void setLowQualityDownloadLink(@Nullable String lowQualityDownloadLink) {
        this.lowQualityDownloadLink = lowQualityDownloadLink;
    }

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

    @Override
    public int hashCode() {
        int result;
        result = this.episodeId.hashCode();
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
        Episode another = (Episode) obj;
        return this.getEpisodeId().equals(another.getEpisodeId());
    }

    @Override
    public boolean update() {
        boolean result = super.update();
        EpisodesManager.fireEpisodeUpdate(episodeId,true);
        return result;
    }

}
