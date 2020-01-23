package com.molvix.android.ui.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ExceptionViewModel extends ViewModel {

    public MutableLiveData<Exception> exceptionData = new MutableLiveData<>();

    public ExceptionViewModel() {

    }

    public void updateException(Exception e) {
        exceptionData.setValue(e);
    }

    public MutableLiveData<Exception> getExceptionData() {
        return exceptionData;
    }

}
