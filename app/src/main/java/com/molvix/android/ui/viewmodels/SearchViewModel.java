package com.molvix.android.ui.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchViewModel extends ViewModel {
    private MutableLiveData<String> searchData = new MutableLiveData<>();

    public SearchViewModel() {

    }

    public void updateSearch(String newData) {
        searchData.setValue(newData);
    }

    public MutableLiveData<String> getSearchData() {
        return searchData;
    }

}
