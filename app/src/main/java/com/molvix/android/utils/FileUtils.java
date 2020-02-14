package com.molvix.android.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import com.molvix.android.components.ApplicationLoader;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileUtils {

    private static final String MOLVIX_VIDEOS_FOLDER = "/Videos";

    public static String videoFolder() {
        return "Videos";
    }

    public static String getRootFolder() {
        return "Molvix";
    }

    public static File getVideosDir() {
        File filePath;
        String folder = "/" + getRootFolder() + MOLVIX_VIDEOS_FOLDER;
        File dir = new File(folder);
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalFilesDir = getAppExternalFilesDir();
            if (externalFilesDir != null) {
                dir = new File(externalFilesDir.getAbsoluteFile() + folder);
            }
        } else {
            ContextWrapper cw = new ContextWrapper(ApplicationLoader.getInstance());
            dir = cw.getDir(MOLVIX_VIDEOS_FOLDER, Context.MODE_PRIVATE);
        }
        filePath = dir;
        return filePath;
    }

    @Nullable
    private static File getAppExternalFilesDir() {
        File externalFilesDir = ApplicationLoader.getInstance().getExternalFilesDir(null);
        if (externalFilesDir != null && !externalFilesDir.exists()) {
            externalFilesDir.mkdir();
        }
        return externalFilesDir;
    }

    public static File getFilePath(String movieFolder, String seasonFolder) {
        File filePath;
        String folder = "/" + getRootFolder() + MOLVIX_VIDEOS_FOLDER + "/" + movieFolder + "/" + seasonFolder;
        File dir = new File(folder);
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalFilesDir = getAppExternalFilesDir();
            if (externalFilesDir != null) {
                dir = new File(externalFilesDir.getAbsoluteFile() + folder);
            }
        } else {
            ContextWrapper cw = new ContextWrapper(ApplicationLoader.getInstance());
            dir = cw.getDir(MOLVIX_VIDEOS_FOLDER, Context.MODE_PRIVATE);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        filePath = dir;
        return filePath;
    }

    public static File getFilePath(String fileName, String movieFolder, String seasonFolder) {
        File filePath;
        String folder = "/" + getRootFolder() + MOLVIX_VIDEOS_FOLDER + "/" + movieFolder + "/" + seasonFolder;
        File dir = new File(folder);
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalFilesDir = getAppExternalFilesDir();
            if (externalFilesDir != null) {
                dir = new File(externalFilesDir.getAbsoluteFile() + folder);
            }
        } else {
            ContextWrapper cw = new ContextWrapper(ApplicationLoader.getInstance());
            dir = cw.getDir(MOLVIX_VIDEOS_FOLDER, Context.MODE_PRIVATE);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        filePath = new File(dir, fileName);
        return filePath;
    }

    public static File getDataFilePath(String fileName) {
        File filePath;
        String folder = "/" + getRootFolder() + "/Data";
        File dir = new File(folder);
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalFilesDir = getAppExternalFilesDir();
            if (externalFilesDir != null) {
                dir = new File(externalFilesDir.getAbsoluteFile() + folder);
            }
        } else {
            ContextWrapper cw = new ContextWrapper(ApplicationLoader.getInstance());
            dir = cw.getDir("Data", Context.MODE_PRIVATE);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        filePath = new File(dir, fileName);
        return filePath;
    }

    public static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public static String getProgressDisplayLine(long currentBytes, long totalBytes) {
        return getBytesToMBString(currentBytes) + "/" + getBytesToMBString(totalBytes);
    }

    private static String getBytesToMBString(long bytes) {
        return String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00));
    }

    private static float getFileSizeInMB(long fileLength) {
        return (float) (fileLength / (1024.00 * 1024.00));
    }

    public static boolean isAtLeast10mB(File existingFile) {
        float existingFileLength = getFileSizeInMB(existingFile.length());
        return existingFileLength >= 10;
    }

}