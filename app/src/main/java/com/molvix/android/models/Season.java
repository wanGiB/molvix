package com.molvix.android.models;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Season extends RealmObject {

    @PrimaryKey
    private String seasonId;
    private String seasonName;
    private String seasonLink;
    private String movieId;
    private RealmList<Episode> episodes;

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

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public RealmList<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(RealmList<Episode> episodes) {
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

}
