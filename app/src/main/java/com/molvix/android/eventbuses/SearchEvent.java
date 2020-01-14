package com.molvix.android.eventbuses;

public class SearchEvent {
    private String searchString;

    public SearchEvent(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchString() {
        return searchString;
    }
}
