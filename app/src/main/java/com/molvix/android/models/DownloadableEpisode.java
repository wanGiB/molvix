package com.molvix.android.models;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
public class DownloadableEpisode {

    @Id
    public long id;
    public String downloadableEpisodeId;
    public ToOne<Episode> episode;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDownloadableEpisodeId() {
        return downloadableEpisodeId;
    }

    public void setDownloadableEpisodeId(String downloadableEpisodeId) {
        this.downloadableEpisodeId = downloadableEpisodeId;
    }

    public Episode getEpisode() {
        return episode.getTarget();
    }

    public void setEpisode(ToOne<Episode> episode) {
        this.episode = episode;
    }

    @NonNull
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
