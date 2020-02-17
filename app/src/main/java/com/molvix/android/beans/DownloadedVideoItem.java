package com.molvix.android.beans;

import java.io.File;

public class DownloadedVideoItem implements Comparable<DownloadedVideoItem> {
    private File downloadedFile;
    private String parentFolderName;
    private String title;

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public void setDownloadedFile(File downloadedFile) {
        this.downloadedFile = downloadedFile;
    }

    public String getParentFolderName() {
        return parentFolderName;
    }

    public void setParentFolderName(String parentFolderName) {
        this.parentFolderName = parentFolderName;
    }

    public void setTitle(String s) {
        this.title = s;
    }

    public String getTitle() {
        return title;
    }


    @Override
    public int hashCode() {
        int result;
        result = this.downloadedFile.hashCode();
        final String name = getClass().getName();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DownloadedVideoItem another = (DownloadedVideoItem) obj;
        return this.getDownloadedFile().equals(another.getDownloadedFile());
    }

    @Override
    public int compareTo(DownloadedVideoItem o) {
        return this.getDownloadedFile().getName().compareTo(o.getDownloadedFile().getName());
    }

}
