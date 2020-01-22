package com.molvix.android.models;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
public class Episode {

    @Id
    public long id;
    public String episodeId;
    public ToOne<Season> season;
    public String episodeName;
    public String episodeLink;
    public int episodeQuality;
    public String highQualityDownloadLink;
    public String standardQualityDownloadLink;
    public String lowQualityDownloadLink;
    public String episodeCaptchaSolverLink;
    public int downloadProgress = -1;
    public String progressDisplayText;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public Season getSeason() {
        return season.getTarget();
    }

    public void setSeason(ToOne<Season> season) {
        this.season = season;
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

    public int getEpisodeQuality() {
        return episodeQuality;
    }

    public void setEpisodeQuality(int episodeQuality) {
        this.episodeQuality = episodeQuality;
    }

    public String getHighQualityDownloadLink() {
        return highQualityDownloadLink;
    }

    public void setHighQualityDownloadLink(String highQualityDownloadLink) {
        this.highQualityDownloadLink = highQualityDownloadLink;
    }

    public String getStandardQualityDownloadLink() {
        return standardQualityDownloadLink;
    }

    public void setStandardQualityDownloadLink(String standardQualityDownloadLink) {
        this.standardQualityDownloadLink = standardQualityDownloadLink;
    }

    public String getLowQualityDownloadLink() {
        return lowQualityDownloadLink;
    }

    public void setLowQualityDownloadLink(String lowQualityDownloadLink) {
        this.lowQualityDownloadLink = lowQualityDownloadLink;
    }

    public String getEpisodeCaptchaSolverLink() {
        return episodeCaptchaSolverLink;
    }

    public void setEpisodeCaptchaSolverLink(String episodeCaptchaSolverLink) {
        this.episodeCaptchaSolverLink = episodeCaptchaSolverLink;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public String getProgressDisplayText() {
        return progressDisplayText;
    }

    public void setProgressDisplayText(String progressDisplayText) {
        this.progressDisplayText = progressDisplayText;
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
        return this.episodeId.equals(another.episodeId);
    }

}
