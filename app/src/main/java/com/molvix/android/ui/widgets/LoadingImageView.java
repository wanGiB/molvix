package com.molvix.android.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.molvix.android.R;

/**
 * @author Wan Clem
 */
public class LoadingImageView extends RoundedImageView {

    private boolean isLoading = false;
    private int width, height, x, y, rX, rY, rWidth, rHeight;
    private Paint loadingPaint;
    private Paint ripplePaint;
    private static final int OFFSET = 15;

    public LoadingImageView(Context context) {
        super(context);
        init(context);
    }

    public LoadingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadingPaint.setColor(ContextCompat.getColor(context, R.color.gray5));
        ripplePaint = new Paint();
        ripplePaint.setColor(ContextCompat.getColor(context, R.color.icons_unselected_color));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isLoading) {
            super.onDraw(canvas);
        } else {
            canvas.drawRect(x, y, x + width, y + height, loadingPaint);
            canvas.drawRect(rX, rY, rX + rWidth, rY + rHeight, ripplePaint);
            rX = (rX < width) ? rX + OFFSET : x - rWidth;
            post(rippleRunner);
        }
    }

    private Runnable rippleRunner = this::invalidate;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        x = 0;
        y = 0;
        rWidth = width - OFFSET;
        rHeight = height;
    }

    public void startLoading() {
        isLoading = true;
        rX = x - rWidth;
        rY = y;
        invalidate();
    }

    public void stopLoading() {
        isLoading = false;
        invalidate();
    }

    @SuppressWarnings("unused")
    public boolean isLoading() {
        return isLoading;
    }

}
