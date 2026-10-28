package com.termux.app.ui;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * Reusable Motion & Physics Kit for Liquid Glass Experience.
 * Provides damped spring interpolators, touch-down liquid tilt feedback,
 * tactile haptic ticks, and respect for system/user Reduce Motion settings.
 */
public final class MotionKit {

    private MotionKit() {}

    /**
     * Damped Spring Interpolator (natural overshoot and settle).
     */
    public static class DampedSpringInterpolator implements TimeInterpolator {
        private final float mFactor;

        public DampedSpringInterpolator() {
            this(0.45f);
        }

        public DampedSpringInterpolator(float factor) {
            this.mFactor = factor;
        }

        @Override
        public float getInterpolation(float input) {
            return (float) (Math.pow(2, -10 * input) * Math.sin((input - mFactor / 4) * (2 * Math.PI) / mFactor) + 1);
        }
    }

    public static boolean isReduceMotionEnabled(Context context) {
        if (context == null) return false;
        try {
            float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f
            );
            return scale == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    public static void performHapticTick(View view) {
        if (view == null) return;
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public static void applyLiquidPressFeedback(View view) {
        if (view == null) return;
        performHapticTick(view);

        if (isReduceMotionEnabled(view.getContext())) {
            view.setAlpha(0.7f);
            view.animate().alpha(1.0f).setDuration(120).start();
            return;
        }

        view.animate().cancel();
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(280)
            .setInterpolator(new DampedSpringInterpolator(0.35f))
            .start();
    }

    public static void attachTouchTilt(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    applyLiquidPressFeedback(v);
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    return false;
            }
            return false;
        });
    }

    public static ValueAnimator createSpringAnimator(float start, float end, long duration, ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.setDuration(duration);
        anim.setInterpolator(new DampedSpringInterpolator(0.40f));
        if (listener != null) {
            anim.addUpdateListener(listener);
        }
        return anim;
    }
}
