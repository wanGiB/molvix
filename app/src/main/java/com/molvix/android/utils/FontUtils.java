package com.molvix.android.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

public class FontUtils {

    private static String FONT_REGULAR = "Montserrat-Regular.otf";
    private static String FONT_BOLD = "Montserrat-Bold.otf";
    private static String FONT_LIGHT = "Montserrat-Light.otf";
    private static String FONT_MEDIUM = "Montserrat-Medium.otf";
    private static String FONT_THIN = "Montserrat-Thin.otf";
    private static String FONT_ITALIC = "Montserrat-Italic.otf";
    private static String FONT_NONE = FONT_REGULAR;

    private static Map<String, Typeface> sCachedFonts = new HashMap<>();

    private enum TypefaceType {
        REGULAR,
        BOLD,
        ITALIC,
        LIGHT,
        MEDIUM,
        THIN,
        NONE
    }

    private static Typeface getTypeface(Context context, String assetPath) {
        if (!sCachedFonts.containsKey(assetPath)) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), assetPath);
            sCachedFonts.put(assetPath, tf);
        }
        return sCachedFonts.get(assetPath);
    }

    public static Typeface selectTypeface(Context context, int textStyle) {
        String fontsPath = "fonts/";
        String font;
        TypefaceType typefaceType = TypefaceType.values()[textStyle];
        switch (typefaceType) {
            case BOLD:
                font = FontUtils.FONT_BOLD;
                break;
            case REGULAR:
                font = FontUtils.FONT_REGULAR;
                break;
            case LIGHT:
                font = FontUtils.FONT_LIGHT;
                break;
            case MEDIUM:
                font = FontUtils.FONT_MEDIUM;
                break;
            case ITALIC:
                font = FontUtils.FONT_ITALIC;
                break;
            case THIN:
                font = FontUtils.FONT_THIN;
                break;
            default:
                font = FontUtils.FONT_NONE;
        }
        return getTypeface(context, fontsPath + font);
    }

}