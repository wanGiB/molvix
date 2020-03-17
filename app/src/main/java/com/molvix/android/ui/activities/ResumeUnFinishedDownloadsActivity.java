package com.molvix.android.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.ui.notifications.notification.MolvixNotification;
import com.molvix.android.utils.DownloadUtils;

public class ResumeUnFinishedDownloadsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkIntent();
    }

    private void checkIntent() {
        MolvixNotification.with(this).cancel(Math.abs(AppConstants.SHOW_UNFINISHED_DOWNLOADS.hashCode()));
        DownloadUtils.checkAndResumePausedDownloads();
    }

}
