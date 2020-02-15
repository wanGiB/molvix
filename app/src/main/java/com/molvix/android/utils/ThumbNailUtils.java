package com.molvix.android.utils;

import java.io.File;

public class ThumbNailUtils {

    public static String getThumbnailPath(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null && children.length > 0) {
                File nextFile = file;
                for (File childFile : children) {
                    if (childFile.isDirectory()) {
                        nextFile = childFile;
                        break;
                    }
                }
                if (nextFile.isDirectory()) {
                    return getThumbnailPath(nextFile);
                } else {
                    return nextFile.getPath();
                }
            } else {
                return null;
            }
        } else {
            return file.getPath();
        }

    }

}
