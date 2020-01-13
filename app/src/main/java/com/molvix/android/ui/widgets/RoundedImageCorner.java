package com.molvix.android.ui.widgets;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        RoundedImageCorner.TOP_LEFT, RoundedImageCorner.TOP_RIGHT,
        RoundedImageCorner.BOTTOM_LEFT, RoundedImageCorner.BOTTOM_RIGHT
})
public @interface RoundedImageCorner {
    int TOP_LEFT = 0;
    int TOP_RIGHT = 1;
    int BOTTOM_RIGHT = 2;
    int BOTTOM_LEFT = 3;
}
