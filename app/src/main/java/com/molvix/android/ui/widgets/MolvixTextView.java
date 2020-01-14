package com.molvix.android.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.molvix.android.R;
import com.molvix.android.utils.FontUtils;


public class MolvixTextView extends AppCompatTextView {

    private static final String DEFAULT_SCHEMA = "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    public MolvixTextView(Context context) {
        super(context);
    }

    public MolvixTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MolvixTextView);
            int textStyle;
            if (a.hasValue(R.styleable.MolvixTextView_textStyle)) {
                textStyle = a.getInt(R.styleable.MolvixTextView_textStyle, 0);
            } else {
                //use default schema
                textStyle = attrs.getAttributeIntValue(DEFAULT_SCHEMA, "textStyle", 0);
            }
            a.recycle();
            applyCustomFont(context, textStyle);
        }
    }

    public void applyCustomFont(Context context, int textStyle) {
        Typeface typeface = FontUtils.selectTypeface(context, textStyle);
        if (typeface != null) {
            setTypeface(typeface);
        }
    }

}
