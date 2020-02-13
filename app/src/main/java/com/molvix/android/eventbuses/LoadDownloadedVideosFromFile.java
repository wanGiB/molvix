package com.molvix.android.eventbuses;

import java.io.File;

public class LoadDownloadedVideosFromFile {
    private File parentFolder;
    private String parentFolderName;

    public LoadDownloadedVideosFromFile(String parentFolderName, File parentFolder) {
        this.parentFolderName = parentFolderName;
        this.parentFolder = parentFolder;
    }

    public File getParentFolder() {
        return parentFolder;
    }

    public String getParentFolderName() {
        return parentFolderName;
    }
}
