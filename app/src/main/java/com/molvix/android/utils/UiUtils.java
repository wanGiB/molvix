package com.molvix.android.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;
import com.molvix.android.components.ApplicationLoader;
import com.molvix.android.contracts.SnackBarActionClickedListener;
import com.molvix.android.ui.widgets.LoadingImageView;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOfIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@SuppressWarnings({"unused", "SameParameterValue"})
public class UiUtils {

    private static Handler handler = new Handler(Looper.getMainLooper());

    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void runOnMain(final @NonNull Runnable runnable) {
        if (isMainThread()) runnable.run();
        else handler.post(runnable);
    }

    public static void loadImageIntoView(ImageView imageView, String photoUrl, int size) {
        if (imageView instanceof LoadingImageView) {
            LoadingImageView loadingImageView = (LoadingImageView) imageView;
            loadingImageView.startLoading();
        }
        RequestOptions imageLoadRequestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        Glide.with(ApplicationLoader.getInstance())
                .load(photoUrl)
                .override(size, size)
                .apply(imageLoadRequestOptions)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (imageView instanceof LoadingImageView) {
                            LoadingImageView loadingImageView = (LoadingImageView) imageView;
                            loadingImageView.stopLoading();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (imageView instanceof LoadingImageView) {
                            LoadingImageView loadingImageView = (LoadingImageView) imageView;
                            loadingImageView.stopLoading();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }

    public static void showSafeToast(final String toastMessage) {
        runOnMain(() -> Toast.makeText(ApplicationLoader.getInstance(), toastMessage, Toast.LENGTH_LONG).show());
    }

    public static void dismissKeyboard(View trigger) {
        InputMethodManager imm = (InputMethodManager) ApplicationLoader.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(trigger.getWindowToken(), 0);
        }
    }

    public static void forceShowKeyboard(View trigger) {
        InputMethodManager imm = (InputMethodManager) ApplicationLoader.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(trigger, InputMethodManager.SHOW_FORCED);
        }
    }

    private static synchronized void animateView(View view, Animation animation) {
        if (view != null) {
            view.startAnimation(animation);
        }
    }

    private static Animation getAnimation(Context context, int animationId) {
        return AnimationUtils.loadAnimation(context, animationId);
    }

    public static void blinkView(View mView) {
        try {
            Animation mFadeInFadeIn = getAnimation(ApplicationLoader.getInstance(), android.R.anim.fade_in);
            mFadeInFadeIn.setRepeatMode(Animation.REVERSE);
            animateView(mView, mFadeInFadeIn);
        } catch (IllegalStateException | NullPointerException ignored) {

        }
    }

    public static void snackMessage(String message, View anchorView, boolean shortDuration, String actionMessage, SnackBarActionClickedListener snackBarActionClickedListener) {
        if (anchorView != null) {
            Snackbar snackbar = Snackbar.make(anchorView, message, actionMessage != null ? Snackbar.LENGTH_INDEFINITE : (shortDuration ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG));
            if (actionMessage != null) {
                snackbar.setAction(actionMessage, view -> {
                    if (snackBarActionClickedListener != null) {
                        snackBarActionClickedListener.onSnackActionClicked();
                    }
                });
            }
            snackbar.show();
        }
    }

    /**
     * Lightens a color by a given factor.
     *
     * @param color  The color to lighten
     * @param factor The factor to lighten the color. 0 will make the color unchanged. 1 will make the
     *               color white.
     * @return lighter version of the specified color.
     */
    public static int lighter(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) / 255 + factor) * 255);
        int green = (int) ((Color.green(color) * (1 - factor) / 255 + factor) * 255);
        int blue = (int) ((Color.blue(color) * (1 - factor) / 255 + factor) * 255);
        return Color.argb(Color.alpha(color), red, green, blue);
    }

    /**
     * Returns darker version of specified <code>color</code>.
     */
    public static int darker(int color, float factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }

    public static void toggleViewFlipperChild(ViewFlipper viewFlipper, int child) {
        if (viewFlipper.getDisplayedChild() != child) {
            viewFlipper.setDisplayedChild(child);
        }
    }

    public static boolean whitish(int color) {
//        int red = 0xFF & (color >> 16);
//        int green = 0xFF & (color >> 8);
//        int blue = 0xFF & (color);
//        int luminance = (int) (0.2126 * red + 0.7152 * green + 0.0722 * blue);
        return color == Color.WHITE;
    }

    public static void toggleViewVisibility(View view, boolean show) {
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public static void toggleViewAlpha(View view, boolean show) {
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public static Spanned fromHtml(String html) {
        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    public static Spannable highlightTextIfNecessary(String search, String originalText, int color) {
        if (isNotEmpty(search)) {
            if (containsIgnoreCase(originalText, search.trim())) {
                int startPost = indexOfIgnoreCase(originalText, search.trim());
                int endPost = startPost + search.length();
                Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
                if (startPost != -1) {
                    spanText.setSpan(new ForegroundColorSpan(color), startPost, endPost, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    return spanText;
                } else {
                    return new SpannableString(originalText);
                }
            } else {
                return new SpannableString(originalText);
            }

        } else {
            return new SpannableString(originalText);
        }
    }

    public static Spannable highlightTextIfNecessary(String search, Spanned originalText, int color) {
        try {
            if (isNotEmpty(search)) {
                if (containsIgnoreCase(originalText, search.trim())) {
                    int startPost = indexOfIgnoreCase(originalText, search.trim());
                    int endPost = startPost + search.length();
                    Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
                    if (startPost != -1) {
                        spanText.setSpan(new ForegroundColorSpan(color), startPost, endPost, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        return spanText;
                    } else {
                        return new SpannableString(originalText);
                    }
                } else {
                    return new SpannableString(originalText);
                }

            } else {
                return new SpannableString(originalText);
            }
        } catch (IndexOutOfBoundsException e) {
            return new SpannableString(originalText);
        }
    }
}
