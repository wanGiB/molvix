package com.molvix.android.eventbuses;

import java.util.List;

public class FilterByGenresEvent {

    private List<String>selectedGenres;

    public FilterByGenresEvent(List<String> selectedGenres) {
        this.selectedGenres = selectedGenres;
    }

    public List<String> getSelectedGenres() {
        return selectedGenres;
    }

}
