package com.molvix.android.utils;

import com.molvix.android.beans.DownloadedVideoItem;
import com.molvix.android.eventbuses.DownloadedFileDeletedEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;

public class ThumbNailUtils {

    public static String getThumbnailPath(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null && children.length > 0) {
                return getThumbnailPath(children[0]);
            } else {
                deleteEmptyDirectory(file);
                return null;
            }
        } else {
            return file.getPath();
        }
    }

    private static void deleteEmptyDirectory(File file) {
        try {
            File parentFile = file.getParentFile();
            String parentFileName = null;
            if (parentFile != null && parentFile.exists()) {
                parentFileName = parentFile.getName();
            }
            boolean deleted = FileUtils.deleteDirectory(file);
            if (deleted) {
                DownloadedVideoItem downloadedVideoItem = new DownloadedVideoItem();
                downloadedVideoItem.setDownloadedFile(file);
                downloadedVideoItem.setParentFolderName(parentFileName);
                EventBus.getDefault().post(new DownloadedFileDeletedEvent(downloadedVideoItem));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}