package com.molvix.android.ui.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConnectivityChangedModel extends ViewModel {
    public MutableLiveData<Boolean> connectivityData = new MutableLiveData<>();

    public ConnectivityChangedModel() {

    }

    public void updateConnectivity(boolean connected) {
        connectivityData.setValue(connected);
    }

    public MutableLiveData<Boolean> getConnectivityData() {
        return connectivityData;
    }

}
