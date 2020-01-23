package com.molvix.android.eventbuses;

public class SearchEvent {
    private String searchString;
    public SearchEvent(String searchedString) {
        this.searchString = searchedString;
    }

    public String getSearchString() {
        return searchString;
    }
}
