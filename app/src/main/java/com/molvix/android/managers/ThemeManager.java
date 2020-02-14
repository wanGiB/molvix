package com.molvix.android.managers;

import com.molvix.android.preferences.AppPrefs;

public class ThemeManager {

    public enum ThemeSelection {
        DEFAULT,
        LIGHT,
        DARK
    }

    public static void setThemeSelection(ThemeSelection themeSelection) {
        AppPrefs.savePreferredTheme(themeSelection);
    }

    public static ThemeSelection getThemeSelection() {
        int savedTheme = AppPrefs.getSavedPreferredTheme();
        return ThemeSelection.values()[savedTheme];
    }

}
