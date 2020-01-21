package com.molvix.android.models;

import com.molvix.android.utils.MolvixDB;
import com.orm.dsl.Table;
import com.orm.dsl.Unique;

import java.util.ArrayList;
import java.util.List;

@Table
public class Season {

    @Unique
    private String seasonId;
    private String seasonName;
    private String seasonLink;
    private String movieId;
    private List<Episode> episodes;
    private long id;

    public Season() {

    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
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

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public List<Episode> getEpisodes() {
        List<Episode> episodes = new ArrayList<>();
        for (Episode e : episodes) {
            Episode updatedEpisode = MolvixDB.getEpisode(e.getEpisodeId());
            episodes.add(updatedEpisode);
        }
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
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
