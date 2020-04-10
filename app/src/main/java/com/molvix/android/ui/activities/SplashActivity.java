package com.molvix.android.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.molvix.android.R;
import com.molvix.android.managers.ContentManager;
import com.molvix.android.utils.MolvixLogger;
import com.molvix.android.utils.MolvixRuntimePermissions;
import com.molvix.android.utils.PermissionsUtils;
import com.molvix.android.utils.UiUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SplashActivity extends BaseActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    @BindView(R.id.footerAd)
    LinearLayout footerView;

    private MolvixRuntimePermissions molvixRuntimePermissions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        initAndroidPermissions();
        checkAndNavigate();
    }

    private void tryAskForPermissions() {
        if (Build.VERSION.SDK_INT >= 23 && PermissionsUtils.checkSelfForStoragePermission(this)) {
            molvixRuntimePermissions.requestStoragePermissions();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && PermissionsUtils.checkSelfForNetworkStatePermission(this)) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Checking network state permissions");
            molvixRuntimePermissions.requestNetworkStatePermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && PermissionsUtils.checkSelfForWifiStatePermission(this)) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Checking wifi state permissions");
            molvixRuntimePermissions.requestWifiPermission();
            return;
        }
        navigateToMainActivity();
    }

    private void initAndroidPermissions() {
        molvixRuntimePermissions = new MolvixRuntimePermissions(this, footerView);
    }

    private void checkAndNavigate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tryAskForPermissions();
        } else {
            navigateToMainActivity();
        }
    }

    private void navigateToMainActivity() {
        Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(mainIntent);
        SplashActivity.this.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionsUtils.REQUEST_NETWORK_STATE
                || requestCode == PermissionsUtils.REQUEST_STORAGE
                || requestCode == PermissionsUtils.REQUEST_WIFI_STATE) {
            MolvixLogger.d(ContentManager.class.getSimpleName(), "Checking for more permissions");
            tryAskForPermissions();
        } else {
            UiUtils.snackMessage("To access all features of Molvix, please grant the requested permissions.",
                    footerView, true, "OK", this::tryAskForPermissions);

        }
    }

}
