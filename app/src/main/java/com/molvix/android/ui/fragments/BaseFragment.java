package com.molvix.android.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

abstract class BaseFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRegisterEventBus();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAndRegisterEventBus();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        checkAndRegisterEventBus();
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
    public void onResume() {
        super.onResume();
        checkAndRegisterEventBus();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndRegisterEventBus();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
    public void onEvent(Object event) {

    }

}
