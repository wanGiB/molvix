package com.molvix.android.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.FileUtils;
import com.molvix.android.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ManagedSpaceActivity extends BaseActivity {

    @BindView(R.id.clear_button)
    Button clearDataButton;

    @BindView(R.id.delete_videos_too)
    CheckBox deleteVideosCheck;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.managed_space_activity_layout);
        ButterKnife.bind(this);
        clearDataButton.setOnClickListener(v -> {
            Map<String, ?> entries = AppPrefs.getAppPreferences().getAll();
            if (!entries.isEmpty()) {
                for (String key : entries.keySet()) {
                    AppPrefs.removeKey(key);
                }
            }
            if (deleteVideosCheck.isChecked()) {
                File appExternalFiles = getExternalFilesDir(null);
                if (appExternalFiles != null && appExternalFiles.exists()) {
                    File molvixFolder = new File(appExternalFiles, "Molvix");
                    if (molvixFolder.exists()) {
                        try {
                            boolean molvixDirDelete = FileUtils.deleteDirectory(molvixFolder);
                            if (molvixDirDelete) {
                                UiUtils.showSafeToast("Downloaded Videos and application specific settings deleted successfully");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                UiUtils.showSafeToast("Application specific settings cleared successfully");
            }
        });
    }
}
