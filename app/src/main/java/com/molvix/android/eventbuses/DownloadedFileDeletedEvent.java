package com.molvix.android.eventbuses;

import com.molvix.android.beans.DownloadedVideoItem;

public class DownloadedFileDeletedEvent {

    private DownloadedVideoItem downloadedVideoItem;

    public DownloadedFileDeletedEvent(DownloadedVideoItem downloadedVideoItem) {
        this.downloadedVideoItem = downloadedVideoItem;
    }

    public DownloadedVideoItem getDownloadedVideoItem() {
        return downloadedVideoItem;
    }

}
