package com.molvix.android.ui.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.molvix.android.R;
import com.molvix.android.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        super.onCreate(savedInstanceState);
        checkAndRegisterEventBus();
        tintToolbarAndTabLayout(ContextCompat.getColor(this, R.color.light_grey));
    }

    protected void tintToolbarAndTabLayout(int colorPrimary) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(UiUtils.darker(colorPrimary, 0.9f));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        checkAndUnRegisterEventBus();
    }

    @Override
    public void onStop() {
        super.onStop();
        checkAndUnRegisterEventBus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        checkAndUnRegisterEventBus();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndRegisterEventBus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRegisterEventBus();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        checkAndRegisterEventBus();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        checkAndRegisterEventBus();
    }

    private void checkAndRegisterEventBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void checkAndUnRegisterEventBus() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEventMainThread(Object event) {

    }

}
