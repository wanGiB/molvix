package com.molvix.android.ui.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.molvix.android.models.Season;

public class LoadEpisodeViewModel extends ViewModel {
    public MutableLiveData<Season>seasonMutableLiveData = new MutableLiveData<>();
    public LoadEpisodeViewModel(){

    }
    public void loadEpisodesFor(Season season){
        seasonMutableLiveData.setValue(season);
    }

    public MutableLiveData<Season> getSeasonMutableLiveData() {
        return seasonMutableLiveData;
    }

}
