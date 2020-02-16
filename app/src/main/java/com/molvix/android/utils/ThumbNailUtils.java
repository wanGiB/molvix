package com.molvix.android.utils;

import java.io.File;

public class ThumbNailUtils {

    public static String getThumbnailPath(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null && children.length > 0) {
                return getThumbnailPath(children[0]);
            } else {
                return null;
            }
        } else {
            return file.getPath();
        }
    }

}