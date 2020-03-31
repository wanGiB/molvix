package com.molvix.android.utils;

import android.annotation.SuppressLint;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.managers.ContentManager;

import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
            newCharSequences[i] = charSequences[i].toString().toLowerCase();
        }
        return newCharSequences;
    }

    public static String getDeviceId() {
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(ApplicationLoader.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
        return md5(android_id).toUpperCase();
    }

    private static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & b));
                while (h.length() < 2)
                    h.insert(0, "0");
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), e.getMessage());
        }
        return "";
    }

}
