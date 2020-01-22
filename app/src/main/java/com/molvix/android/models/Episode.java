package com.molvix.android.models;

import androidx.annotation.Nullable;

import com.orm.dsl.Table;
import com.orm.dsl.Unique;

@Table
public class Episode {

    @Unique
    private String episodeId;
    private String movieId;
    private String seasonId;
    private String episodeName;
    private String episodeLink;
    private int episodeQuality;
    private String highQualityDownloadLink;
    private String standardQualityDownloadLink;
    private String lowQualityDownloadLink;
    private String episodeCaptchaSolverLink;
    private int downloadProgress = -1;
    private String progressDisplayText;

    public Episode() {

    }

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

    public int getEpisodeQuality() {
        return episodeQuality;
    }

    public void setEpisodeQuality(int episodeQuality) {
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

}
