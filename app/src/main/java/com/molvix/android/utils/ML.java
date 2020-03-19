package com.molvix.android.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.managers.ContentManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ML {

    private static final String TESSBASE_PATH = FileUtils.getDataFilePath("ml").toString();
    private static final String DEFAULT_LANGUAGE = "eng";
    private static final String TESSDATA_PATH = TESSBASE_PATH + "/tessdata/";
    private static final String[] EXPECTED_CUBE_DATA_FILES_ENG = {
            "eng.cube.bigrams",
            "eng.cube.fold",
            "eng.cube.lm",
            "eng.cube.nn",
            "eng.cube.params",
            "eng.cube.size",
            "eng.cube.word-freq",
            "eng.tesseract_cube.nn"
    };

    private static void checkDataFileExists() {
        // Check that the data file(s) exist.
        for (String languageCode : DEFAULT_LANGUAGE.split("\\+")) {
            if (!languageCode.startsWith("~")) {
                String fileName = languageCode + ".traineddata";
                String expectedFilePath = TESSDATA_PATH + fileName;
                File expectedFile = new File(expectedFilePath);
                if (!expectedFile.exists()) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), fileName + " doesn't exist");
                    copyFileFromAssets(fileName, expectedFile.getPath());
                } else {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), fileName + " already exists");
                }
            }
        }
    }

    private static void checkCubeData() {
        // Make sure the cube data files exist.
        for (String expectedFilename : EXPECTED_CUBE_DATA_FILES_ENG) {
            String expectedFilePath = TESSDATA_PATH + expectedFilename;
            File expectedFile = new File(expectedFilePath);
            if (!expectedFile.exists()) {
                MolvixLogger.d(ContentManager.class.getSimpleName(), expectedFilename + " doesn't exist");
                copyFileFromAssets(expectedFilename, expectedFile.getPath());
            } else {
                MolvixLogger.d(ContentManager.class.getSimpleName(), expectedFilename + " already exists");
            }
        }
    }

    private static void copyFileFromAssets(String filename, String sdcardPath) {
        AssetManager assetManager = ApplicationLoader.getInstance().getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Copying " + filename + " to " + sdcardPath);
            in = assetManager.open("ml/" + filename);
            out = new FileOutputStream(sdcardPath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            MolvixLogger.d(ContentManager.class.getSimpleName(), filename + " successfully copied to " + sdcardPath);
        } catch (Exception e) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Exception while closing input stream");
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    MolvixLogger.d(ContentManager.class.getSimpleName(), "Exception while closing output stream");
                }
            }
        }
    }

    public static void checkAndMoveMLFilesToDevice() {
        checkDataFileExists();
        checkCubeData();
    }

    public static String predictTextFromBitmap(Bitmap bitmap) {
        final TessBaseAPI baseApi = new TessBaseAPI();
        boolean success = baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE, TessBaseAPI.OEM_TESSERACT_ONLY);
        if (success) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "TessBaseAPI Init successfully");
        } else {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "TessBaseAPI Init failed");
        }
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        baseApi.setImage(bitmap);
        String result = baseApi.getHOCRText(0);
        baseApi.end();
        bitmap.recycle();
        return result;
    }

}
