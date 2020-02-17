package com.molvix.android.eventbuses;

import java.io.File;

public class LoadDownloadedVideosFromDir {
    private File dir;

    public LoadDownloadedVideosFromDir(File dir) {
        this.dir = dir;
    }

    public File getDir() {
        return dir;
    }

}
