package com.molvix.android.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;

import com.molvix.android.components.ApplicationLoader;

import java.io.File;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileUtils {

    private static final String MOLVIX_VIDEOS_FOLDER = "/videos";

    private static String getRootFolder() {
        return "Molvix";
    }

    public static File getFilePath(String fileName, String movieFolder, String seasonFolder) {
        File filePath;
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String folder = "/" + getRootFolder() + MOLVIX_VIDEOS_FOLDER + "/" + movieFolder + "/" + seasonFolder;
            dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + folder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } else {
            ContextWrapper cw = new ContextWrapper(ApplicationLoader.getInstance());
            dir = cw.getDir(MOLVIX_VIDEOS_FOLDER, Context.MODE_PRIVATE);
        }
        filePath = new File(dir, fileName);
        return filePath;
    }

}