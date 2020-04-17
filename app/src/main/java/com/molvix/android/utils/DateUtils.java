
package com.molvix.android.utils;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.molvix.android.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods to help display dates in a nice, easily readable way.
 */
public class DateUtils extends android.text.format.DateUtils {

    private static boolean isYesterday(final long when) {
        return DateUtils.isToday(when + TimeUnit.DAYS.toMillis(1));
    }

    private static String getFormattedDateTime(long time, Locale locale) {
        final String localizedPattern = getLocalizedPattern(locale);
        return new SimpleDateFormat(localizedPattern, locale).format(new Date(time));
    }

    public static String getRelativeDate(@NonNull Context context,
                                         @NonNull Locale locale,
                                         long timestamp) {
        if (isToday(timestamp)) {
            return context.getString(R.string.DateUtils_today);
        } else if (isYesterday(timestamp)) {
            return context.getString(R.string.DateUtils_yesterday);
        } else {
            return getFormattedDateTime(timestamp, locale);
        }
    }

    private static String getLocalizedPattern(Locale locale) {
        return DateFormat.getBestDateTimePattern(locale, "EEE, MMM d, yyyy");
    }

}
