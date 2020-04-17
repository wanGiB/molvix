package com.molvix.android.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

@SuppressWarnings("WeakerAccess")
public class PermissionsUtils {

    public static final int REQUEST_STORAGE = 0;
    public static final int REQUEST_LOCATION = 1;
    public static final int REQUEST_PHONE_STATE = 2;
    public static final int REQUEST_AUDIO_RECORD = 3;
    public static final int REQUEST_CALL_PHONE = 4;
    public static final int REQUEST_CAMERA = 5;
    public static final int REQUEST_CONTACT = 6;
    public static final int REQUEST_CONTACT_OR_LOCATION = 7;
    public static final int REQUEST_WIFI_STATE = 8;
    public static final int REQUEST_NETWORK_STATE = 9;


    public static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION};
    public static String[] PERMISSION_CALL = {Manifest.permission.CALL_PHONE};
    public static String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    public static String[] PERMISSIONS_RECORD_AUDIO = {Manifest.permission.RECORD_AUDIO};
    public static String[] PERMISSION_CAMERA = {Manifest.permission.CAMERA};
    public static String[] PERMISSION_CONTACT = {Manifest.permission.READ_CONTACTS};
    public static String[] PERMISSION_PHONE_STATE = {Manifest.permission.READ_PHONE_STATE};
    public static String[] PERMISSION_WIFI = {Manifest.permission.ACCESS_WIFI_STATE};
    public static String[] PERMISSION_NETWORK_STATE = {Manifest.permission.ACCESS_NETWORK_STATE};

    public static boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean shouldShowRequestForLocationPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    public static boolean shouldShowRequestForAudioPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.RECORD_AUDIO));
    }


    public static boolean shouldShowRequestForStoragePermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE));
    }

    public static boolean shouldShowRequestForCallPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.CALL_PHONE));
    }

    public static boolean shouldShowRequestForCameraPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.CAMERA));
    }

    public static boolean shouldShowRequestForContactPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_CONTACTS));
    }

    public static boolean shouldShowRequestForNetworkStatePermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_NETWORK_STATE));
    }

    public static boolean shouldShowRequestForWifiStatePermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_WIFI_STATE));
    }

    public static boolean shouldShowRequestForPhoneStatePermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_PHONE_STATE));
    }

    public static boolean checkSelfForStoragePermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfPermissionForLocation(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfPermissionForAudioRecording(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForCallPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForPhoneStatePermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForNetworkStatePermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForWifiStatePermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForCameraPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForContactPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static boolean isAudioRecordingPermissionGranted(Context context) {
        String permission = "android.permission.RECORD_AUDIO";
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isCameraPermissionGranted(Context context) {
        int res = context.checkCallingOrSelfPermission(Manifest.permission.CAMERA);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isCallPermissionGranted(Context context) {
        int res = context.checkCallingOrSelfPermission(Manifest.permission.CALL_PHONE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }
}
