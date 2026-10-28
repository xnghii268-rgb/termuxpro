package com.termux.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;

import androidx.annotation.Nullable;

/**
 * Liquid Glass Toggle Switch.
 * Features a dynamic liquid glass droplet thumb, graphite/mint track,
 * squash physics on travel, and haptic feedback.
 */
public class GlassToggle extends View implements Checkable {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(GlassToggle toggle, boolean isChecked);
    }

    private boolean mChecked = false;
    private float mProgress = 0f; // 0f = OFF (left), 1f = ON (right)
    private float mSquash = 1.0f;
    private ValueAnimator mAnimator;
    private OnCheckedChangeListener mListener;

    private final Paint mTrackBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTrackBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mTrackBounds = new RectF();
    private final RectF mThumbBounds = new RectF();

    private float mTouchDownX = 0f;
    private boolean mIsDragging = false;

    public GlassToggle(Context context) {
        this(context, null);
    }

    public GlassToggle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mTrackBorderPaint.setStyle(Paint.Style.STROKE);
        mTrackBorderPaint.setStrokeWidth(1.5f);

        mThumbPaint.setStyle(Paint.Style.FILL);
        mThumbPaint.setColor(0xFFF2EDE4); // Warm cream thumb (contrast >= 3:1)

        mThumbHighlightPaint.setStyle(Paint.Style.STROKE);
        mThumbHighlightPaint.setStrokeWidth(1.0f);
        mThumbHighlightPaint.setColor(0x80FFFFFF);

        setClickable(true);
        setFocusable(true);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (mChecked == checked && (mAnimator == null || !mAnimator.isRunning())) {
            mProgress = checked ? 1f : 0f;
            invalidate();
            return;
        }

        mChecked = checked;
        float target = checked ? 1f : 0f;

        if (!animate || MotionKit.isReduceMotionEnabled(getContext())) {
            if (mAnimator != null) mAnimator.cancel();
            mProgress = target;
            mSquash = 1.0f;
            invalidate();
            if (mListener != null) {
                mListener.onCheckedChanged(this, mChecked);
            }
            return;
        }

        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        final float start = mProgress;
        mAnimator = ValueAnimator.ofFloat(start, target);
        mAnimator.setDuration(240);
        mAnimator.setInterpolator(new MotionKit.DampedSpringInterpolator(0.42f));
        mAnimator.addUpdateListener(anim -> {
            mProgress = (float) anim.getAnimatedValue();
            float velocity = (1.0f - Math.abs(mProgress - 0.5f) * 2f);
            mSquash = 1.0f + (velocity * 0.22f);
            invalidate();
        });
        mAnimator.start();

        if (mListener != null) {
            mListener.onCheckedChanged(this, mChecked);
        }
    }

    @Override
    public void toggle() {
        setChecked(!mChecked, true);
        MotionKit.performHapticTick(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int desiredWidth = (int) (48 * density + 0.5f);
        int desiredHeight = (int) (28 * density + 0.5f);

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = 1.5f;
        mTrackBounds.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cornerRadius = h / 2f;

        // 1. Draw Track (Graphite when OFF, Mint when ON)
        // Interpolate colors smoothly
        int offColor = 0xE622252C;
        int onColor = 0x4D3DDC97;
        int offBorder = 0x33FFFFFF;
        int onBorder = 0xFF3DDC97;

        int trackFill = MotionKit.lerpColor(offColor, onColor, mProgress);
        int trackBorder = MotionKit.lerpColor(offBorder, onBorder, mProgress);

        mTrackBgPaint.setColor(trackFill);
        mTrackBorderPaint.setColor(trackBorder);

        canvas.drawRoundRect(mTrackBounds, cornerRadius, cornerRadius, mTrackBgPaint);
        canvas.drawRoundRect(mTrackBounds, cornerRadius, cornerRadius, mTrackBorderPaint);

        // 2. Draw Liquid Droplet Thumb
        float density = getResources().getDisplayMetrics().density;
        float padding = 3f * density;
        float thumbDiameter = h - (padding * 2f);

        float minThumbX = padding;
        float maxThumbX = w - padding - thumbDiameter;
        float thumbX = minThumbX + (maxThumbX - minThumbX) * mProgress;
        float thumbY = padding;

        float thumbW = thumbDiameter * mSquash;
        float thumbH = thumbDiameter / Math.max(0.8f, mSquash);
        float centerThumbX = thumbX + (thumbDiameter / 2f);
        float centerThumbY = thumbY + (thumbDiameter / 2f);

        mThumbBounds.set(
            centerThumbX - (thumbW / 2f),
            centerThumbY - (thumbH / 2f),
            centerThumbX + (thumbW / 2f),
            centerThumbY + (thumbH / 2f)
        );

        canvas.drawRoundRect(mThumbBounds, thumbH / 2f, thumbH / 2f, mThumbPaint);
        canvas.drawRoundRect(mThumbBounds, thumbH / 2f, thumbH / 2f, mThumbHighlightPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                mIsDragging = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - mTouchDownX);
                if (dx > 8) {
                    mIsDragging = true;
                    float w = getWidth();
                    float newProgress = Math.max(0f, Math.min(1f, event.getX() / w));
                    mProgress = newProgress;
                    mSquash = 1.15f;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (mIsDragging) {
                    boolean shouldBeChecked = mProgress >= 0.5f;
                    setChecked(shouldBeChecked, true);
                    MotionKit.performHapticTick(this);
                } else {
                    toggle();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                setChecked(mChecked, true);
                return true;
        }

        return super.onTouchEvent(event);
    }
}
