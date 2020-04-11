package com.molvix.android.ui.widgets.lovelydialog;

import android.content.Context;

import com.molvix.android.R;

public class LovelyProgressDialog extends AbsLovelyDialog<LovelyProgressDialog> {

    public LovelyProgressDialog(Context context) {
        super(context);
    }

    public LovelyProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    {
        setCancelable(false);
    }

    @Override
    protected int getLayout() {
        return R.layout.dialog_progress;
    }
}
