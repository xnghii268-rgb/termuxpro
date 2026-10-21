package com.termux.app.terminal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;

/**
 * Root view for {@link TermuxActivity}.
 * In Termux Pro, insets, status/cutout padding and soft keyboard animations
 * are handled cleanly via AndroidX {@link androidx.core.view.WindowInsetsCompat}
 * and {@link androidx.core.view.WindowInsetsAnimationCompat}.
 *
 * The legacy margin-adjustment logic that toggled bottom margins between 0 and
 * pxHidden every 40ms has been completely eliminated to prevent screen jittering.
 */
public class TermuxActivityRootView extends LinearLayout implements ViewTreeObserver.OnGlobalLayoutListener {

    public TermuxActivity mActivity;

    public TermuxActivityRootView(Context context) {
        super(context);
    }

    public TermuxActivityRootView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TermuxActivityRootView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setActivity(TermuxActivity activity) {
        mActivity = activity;
    }

    public void setIsRootViewLoggingEnabled(boolean value) {
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onGlobalLayout() {
        // No-op: insets and soft keyboard visibility are managed cleanly by
        // WindowInsetsCompat in TermuxActivity.
    }

    public static class WindowInsetsListener implements View.OnApplyWindowInsetsListener {
        @Override
        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
            return v.onApplyWindowInsets(insets);
        }
    }
}
