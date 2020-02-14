package com.molvix.android.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class CryptoUtils {

    public static String getSha256Digest(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            try {
                md.update(message.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            byte[] digest = md.digest();
            StringBuffer stringBuffer = toHex(digest);
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static StringBuffer toHex(byte[] digest) {
        StringBuffer stringBuffer = new StringBuffer();
        for (byte aDigest : digest) {
            stringBuffer.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer;
    }

}
