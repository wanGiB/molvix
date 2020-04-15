package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.molvix.android.R;
import com.molvix.android.contracts.DoneCallback;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FullScreenDialog extends RelativeLayout {

    @BindView(R.id.progress_root_view)
    View progressRootView;

    @BindView(R.id.title_view)
    TextView titleView;

    @BindView(R.id.message_view)
    TextView messageView;

    private DoneCallback<Boolean> dismissedCallback;

    public FullScreenDialog(Context context) {
        super(context);
        init(context);
    }

    public FullScreenDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FullScreenDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        removeAllViews();
        @SuppressLint("InflateParams") View rootView = LayoutInflater.from(context).inflate(R.layout.full_screen_dialog, null);
        ButterKnife.bind(this, rootView);
        addView(rootView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public FullScreenDialog show(ViewGroup parentView, String title, String message) {
        progressRootView.setOnClickListener(view -> {
            //Just do nothing here, we don't want any form of external click interception
        });
        parentView.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (StringUtils.isNotEmpty(title)) {
            titleView.setText(title);
        }
        if (StringUtils.isNotEmpty(message)) {
            messageView.setText(message);
        }
        return this;
    }

    public void dismiss(ViewGroup parentView, DoneCallback<Boolean> dismissedCallback) {
        parentView.removeView(this);
        if (dismissedCallback != null) {
            dismissedCallback.done(true, null);
        }
        if (this.dismissedCallback != null) {
            this.dismissedCallback.done(true, null);
        }
    }

    public void setOnDismissedCallback(DoneCallback<Boolean> dismissedCallback) {
        this.dismissedCallback = dismissedCallback;
    }

}
