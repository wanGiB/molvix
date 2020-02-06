package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.molvix.android.R;
import com.molvix.android.ui.activities.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewUpdateAvailableView extends FrameLayout {

    @BindView(R.id.forced_update_version)
    TextView versionStringView;

    @BindView(R.id.update_app_btn)
    Button updateAppButton;

    public NewUpdateAvailableView(@NonNull Context context) {
        super(context);
        initUI(context);
    }

    public NewUpdateAvailableView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initUI(context);
    }

    public NewUpdateAvailableView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUI(context);
    }

    private void initUI(Context context) {
        removeAllViews();
        @SuppressLint("InflateParams") View newUpdateAvailableView = LayoutInflater.from(context).inflate(R.layout.app_updater_view, null);
        ButterKnife.bind(this, newUpdateAvailableView);
        addView(newUpdateAvailableView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @SuppressLint("SetTextI18n")
    public void displayNewUpdate(String versionString) {
        versionStringView.setText("Version ".concat(versionString));
        updateAppButton.setOnClickListener(v -> {
            if (getContext() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getContext();
                mainActivity.moveToPlayStore();
            }
        });
    }

}
