package com.molvix.android.eventbuses;

public class AttachLoadedAd {
    private boolean attach;

    public AttachLoadedAd(boolean attach) {
        this.attach = attach;
    }

    public boolean canAttach() {
        return attach;
    }

}
