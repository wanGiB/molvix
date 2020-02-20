package com.molvix.android.eventbuses;

public class DownloadCoinsUpdatedEvent {
    private int coins;
    public DownloadCoinsUpdatedEvent(int coins){
        this.coins=coins;
    }

    public int getCoins() {
        return coins;
    }
}
