package com.molvix.android.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused"})
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

    public static String getSha512Digest(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
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

    public static byte[] fromHex(String digest) {
        List<Integer> integers = new ArrayList<>();
        for (int i = 0; i < digest.length(); i += 2) {
            String part = digest.substring(i, i + 2);
            int intVariant = Integer.parseInt(part, 16);
            integers.add(intVariant);
        }
        byte[] hBytes = new byte[32];
        for (int i = 0; i < integers.size(); i++) {
            hBytes[i] = (byte) integers.get(i).intValue();
        }
        return hBytes;
    }

    static String getHMAC512Digest(String message, String secret) {
        Mac sha512_HMAC;
        String result = null;
        try {
            byte[] byteKey = new byte[0];
            try {
                byteKey = secret.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            final String HMAC_SHA512 = "HmacSHA512";
            sha512_HMAC = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA512);
            sha512_HMAC.init(keySpec);
            byte[] mac_data = new byte[0];
            try {
                mac_data = sha512_HMAC.
                        doFinal(message.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            result = bytesToHex(mac_data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
