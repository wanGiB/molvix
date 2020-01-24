package com.molvix.android.models;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
public class Season {

    @Id
    public long id;
    public String seasonId;
    public String seasonName;
    public String seasonLink;
    public ToOne<Movie> movie;
    @Backlink(to = "season")
    public ToMany<Episode> episodes;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }

    public String getSeasonLink() {
        return seasonLink;
    }

    public void setSeasonLink(String seasonLink) {
        this.seasonLink = seasonLink;
    }

    public Movie getMovie() {
        return movie.getTarget();
    }

    public void setMovie(ToOne<Movie> movie) {
        this.movie = movie;
    }

    public ToMany<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(ToMany<Episode> episodes) {
        this.episodes = episodes;
    }

    @Override
    public int hashCode() {
        int result;
        result = this.seasonId.hashCode();
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
        Season another = (Season) obj;
        return this.getSeasonId().equals(another.getSeasonId());
    }

    @NonNull
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
