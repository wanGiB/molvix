package com.molvix.android.ui.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.utils.UiUtils;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ManagedSpaceActivity extends BaseActivity {

    @BindView(R.id.clear_button)
    Button clearDataButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.managed_space_activity_layout);
        ButterKnife.bind(this);
        clearDataButton.setOnClickListener(v -> {
            Map<String,?>entries = AppPrefs.getAppPreferences().getAll();
            if (!entries.isEmpty()){
                for (String key:entries.keySet()){
                    AppPrefs.removeKey(key);
                }
            }
            UiUtils.showSafeToast("Application specific settings cleared successfully");
        });
    }
}
