package com.molvix.android.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

public class FontUtils {
    private static String FONT_REGULAR = "Roboto-Regular.ttf";
    private static String FONT_BOLD = "Roboto-Bold.ttf";
    private static String FONT_LIGHT = "Roboto-Light.ttf";
    private static String FONT_MEDIUM = "Roboto-Medium.ttf";
    private static String COLOPHON = "colophon.ttf";
    private static String FONT_AWESOME = "fontawesome-webfont.ttf";
    private static String AVENIR_MEDIUM = "AvenirLTStd-Medium.otf";

    private static Map<String, Typeface> sCachedFonts = new HashMap<>();

    private static Typeface getTypeface(Context context, String assetPath) {
        if (!sCachedFonts.containsKey(assetPath)) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), assetPath);
            sCachedFonts.put(assetPath, tf);
        }
        return sCachedFonts.get(assetPath);
    }

    public static Typeface selectTypeface(Context context, int textStyle) {
        String RobotoPrefix = "fonts/";
        String font;
        switch (textStyle) {
            case 1:
                font = FontUtils.FONT_BOLD;
                break;
            case 3:
            case 5:
                font = FontUtils.FONT_LIGHT;
                break;
            case 4:
                font = FontUtils.FONT_MEDIUM;
                break;
            case 6:
                font = FontUtils.COLOPHON;
                break;
            case 7:
                font = FontUtils.FONT_AWESOME;
                break;
            case 8:
                return Typeface.DEFAULT;
            case 9:
                return Typeface.DEFAULT_BOLD;
            case 10:
                font = FontUtils.AVENIR_MEDIUM;
                break;
            case 11:
                return Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);
            default:
                font = FontUtils.FONT_REGULAR;
                break;
        }
        return getTypeface(context, RobotoPrefix + font);
    }

}

