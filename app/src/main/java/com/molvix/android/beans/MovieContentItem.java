package com.molvix.android.beans;

import com.molvix.android.models.Episode;
import com.molvix.android.models.Season;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.List;

public class MovieContentItem extends ExpandableGroup {
    private List<Episode> episodes;
    private String title;

    public MovieContentItem(String title, List items) {
        super(title, items);
        this.title = title;
        this.episodes = items;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    enum ContentType {
        AD,
        ANON_HEADER,
        GROUP_HEADER
    }

    private ContentType contentType;
    private Season season;

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Season getSeason() {
        return season;
    }

}
