package com.molvix.android.ui.widgets.lovelydialog;

import android.view.View;

public interface ViewConfigurator<T extends View> {
  void configureView(T v);
}
