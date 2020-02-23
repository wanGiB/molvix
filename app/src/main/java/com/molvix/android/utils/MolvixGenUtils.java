package com.molvix.android.utils;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MolvixGenUtils {
    public static int hashCode(@Nullable Object... objects) {
        return Arrays.hashCode(objects);
    }

    @NotNull
    public static CharSequence[] getCharSequencesFromList(List<String> availableGenres) {
        int length = availableGenres.size();
        CharSequence[] options = new CharSequence[length];
        for (int i = 0; i < length; i++) {
            options[i] = WordUtils.capitalize(availableGenres.get(i));
        }
        return options;
    }

    public static CharSequence[] charSequencesToLowerCase(CharSequence[] charSequences) {
        CharSequence[] newCharSequences = new CharSequence[charSequences.length];
        int length = newCharSequences.length;
        for (int i = 0; i < length; i++) {
            newCharSequences[i]=charSequences[i].toString().toLowerCase();
        }
        return newCharSequences;
    }
}
