package com.molvix.android.ui.decorators;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.molvix.android.R;

public class MarginDecoration extends RecyclerView.ItemDecoration {
    private int margin;

    public MarginDecoration(Context context, int margin) {
        this.margin = margin;
        if (margin == 0) {
            this.margin = context.getResources().getDimensionPixelSize(R.dimen.item_margin);
        }
    }

    @Override
    public void getItemOffsets(
            @NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.set(margin, margin, margin, margin);
    }

}