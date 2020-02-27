package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.database.MolvixDB;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ManagedSpaceActivity extends BaseActivity {

    @BindView(R.id.clear_button)
    Button clearDataButton;

    @BindView(R.id.delete_videos_too)
    CheckBox deleteVideosCheck;

    @BindView(R.id.back_button)
    ImageView backButton;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.managed_space_activity_layout);
        ButterKnife.bind(this);
        backButton.setOnClickListener(v -> {
            UiUtils.blinkView(v);
            finish();
        });
        clearDataButton.setOnClickListener(v -> {
            AppPrefs.getAppPreferences().edit().clear().commit();
            AppPrefs.setDownloadCoinsToZero();
            cleanUpDatabase();
            if (deleteVideosCheck.isChecked()) {
                File appExternalFiles = getExternalFilesDir(null);
                if (appExternalFiles != null && appExternalFiles.exists()) {
                    File molvixFolder = new File(appExternalFiles, FileUtils.getRootFolder());
                    if (molvixFolder.exists()) {
                        boolean molvixDirDelete = FileUtils.deleteDirectory(molvixFolder);
                        if (molvixDirDelete) {
                            UiUtils.showSafeToast("Downloaded Videos and application specific settings deleted successfully");
                        }else{
                            UiUtils.showSafeToast("Sorry, we couldn't delete downloaded videos. Please try again.");
                        }
                    }
                }
            } else {
                UiUtils.showSafeToast("Application specific settings cleared successfully");
            }
        });
    }

    private void cleanUpDatabase() {
        MolvixDB.getMovieBox().removeAll();
        MolvixDB.getNotificationBox().removeAll();
        MolvixDB.getPresetsBox().removeAll();
        MolvixDB.getSeasonBox().removeAll();
        MolvixDB.getEpisodeBox().removeAll();
        MolvixDB.getDownloadableEpisodeBox().removeAll();
    }
}
