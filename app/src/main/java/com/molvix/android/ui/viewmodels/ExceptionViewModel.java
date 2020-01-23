package com.molvix.android.ui.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.molvix.android.utils.UiUtils;

public class ExceptionViewModel extends ViewModel {

    public MutableLiveData<Exception> exceptionData = new MutableLiveData<>();

    public ExceptionViewModel() {

    }

    public void updateException(Exception e) {
        UiUtils.runOnMain(() -> exceptionData.setValue(e));
    }

    public MutableLiveData<Exception> getExceptionData() {
        return exceptionData;
    }

}
