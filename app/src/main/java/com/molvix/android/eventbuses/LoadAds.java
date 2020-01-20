package com.molvix.android.eventbuses;

public class LoadAds {
    private boolean load;

    public LoadAds(boolean load) {
        this.load = load;
    }

    public boolean canLoad() {
        return load;
    }

}
