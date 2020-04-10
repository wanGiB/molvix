package com.molvix.android.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ThumbNailUtils {

    public static String getThumbnailPath(File file) {
        List<File> tempFiles = new ArrayList<>();
        try {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null && children.length > 0) {
                    addFiles(tempFiles, children);
                }
                if (!tempFiles.isEmpty()) {
                    String firstValidFile = takeValidThumb(tempFiles);
                    tempFiles.clear();
                    return firstValidFile;
                }
                return null;
            } else {
                return file.getPath();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String takeValidThumb(List<File> tempFiles) {
        String validThumb = null;
        for (File file : tempFiles) {
            if (!file.isHidden() && !file.isDirectory()) {
                String mimeTypeOfFile = FileUtils.getMimeType(file.getName());
                if (mimeTypeOfFile != null) {
                    if (mimeTypeOfFile.contains("video")) {
                        validThumb = file.getPath();
                        break;
                    }
                }
            }
        }
        return validThumb;
    }

    private static void addFiles(List<File> tempFiles, File[] files) {
        for (File file : files) {
            if (!tempFiles.contains(file)) {
                tempFiles.add(file);
            }
            if (file.isDirectory()) {
                File[] childFiles = file.listFiles();
                if (childFiles != null && childFiles.length > 0) {
                    addFiles(tempFiles, childFiles);
                }
            }
        }
    }

}