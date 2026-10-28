package com.termux.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.R;

/**
 * Liquid Drop Floating Glass Tab Bar.
 * Features a dynamic liquid glass droplet lens that springs, stretches,
 * and snaps between the 4 core tabs: Trang chủ, Terminal, Tiện ích, Cài đặt.
 */
public class LiquidTabBar extends View {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private static class TabItem {
        final String title;
        final int iconResId;
        Drawable drawable;
        float centerX;

        TabItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    private final TabItem[] mTabs = new TabItem[]{
        new TabItem("Trang chủ", R.drawable.ic_tab_home),
        new TabItem("Terminal", R.drawable.ic_tab_terminal),
        new TabItem("Tiện ích", R.drawable.ic_tab_utilities),
        new TabItem("Cài đặt", R.drawable.ic_tab_settings)
    };

    private int mSelectedTab = 0;
    private float mDropX = -1f;
    private float mDropStretch = 1.0f;
    private ValueAnimator mDropSpringAnimator;
    private OnTabSelectedListener mListener;

    private final Paint mPillBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPillBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDropBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mPillBounds = new RectF();
    private final RectF mDropBounds = new RectF();

    public LiquidTabBar(Context context) {
        this(context, null);
    }

    public LiquidTabBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPillBgPaint.setColor(0xE614161B); // Level 3 Floating Glass
        mPillBorderPaint.setColor(0x33FFFFFF);
        mPillBorderPaint.setStyle(Paint.Style.STROKE);
        mPillBorderPaint.setStrokeWidth(1.2f);

        mDropPaint.setColor(0x2E3DDC97); // Mint Liquid Droplet Tint
        mDropBorderPaint.setColor(0x803DDC97);
        mDropBorderPaint.setStyle(Paint.Style.STROKE);
        mDropBorderPaint.setStrokeWidth(1.5f);

        mTextPaint.setTextAlign(Paint.Align.CENTER);

        for (TabItem item : mTabs) {
            item.drawable = ContextCompat.getDrawable(getContext(), item.iconResId);
            if (item.drawable != null) {
                item.drawable.mutate();
            }
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.mListener = listener;
    }

    public int getSelectedTab() {
        return mSelectedTab;
    }

    public void setSelectedTab(int index, boolean animate) {
        if (index < 0 || index >= mTabs.length) return;
        if (mSelectedTab == index && mDropX >= 0) return;

        int oldIndex = mSelectedTab;
        mSelectedTab = index;

        float targetX = getTabCenterX(index);
        if (!animate || MotionKit.isReduceMotionEnabled(getContext()) || mDropX < 0) {
            mDropX = targetX;
            mDropStretch = 1.0f;
            invalidate();
            return;
        }

        if (mDropSpringAnimator != null && mDropSpringAnimator.isRunning()) {
            mDropSpringAnimator.cancel();
        }

        final float startX = mDropX;
        final float distance = targetX - startX;

        mDropSpringAnimator = ValueAnimator.ofFloat(0f, 1f);
        mDropSpringAnimator.setDuration(340);
        mDropSpringAnimator.setInterpolator(new MotionKit.DampedSpringInterpolator(0.38f));
        mDropSpringAnimator.addUpdateListener(anim -> {
            float frac = (float) anim.getAnimatedValue();
            mDropX = startX + distance * frac;
            // Squash and stretch in travel direction
            float velocity = (1.0f - Math.abs(frac - 0.5f) * 2f);
            mDropStretch = 1.0f + (velocity * 0.28f);
            invalidate();
        });
        mDropSpringAnimator.start();
    }

    private float getTabCenterX(int index) {
        int w = getWidth();
        if (w <= 0) return 0f;
        float tabWidth = (float) w / mTabs.length;
        return (index * tabWidth) + (tabWidth / 2f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPillBounds.set(1.5f, 1.5f, w - 1.5f, h - 1.5f);
        mDropX = getTabCenterX(mSelectedTab);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float cornerRadius = h / 2f;

        // 1. Floating Pill Glass Frame
        canvas.drawRoundRect(mPillBounds, cornerRadius, cornerRadius, mPillBgPaint);
        canvas.drawRoundRect(mPillBounds, cornerRadius, cornerRadius, mPillBorderPaint);

        // 2. Liquid Glass Droplet Lens (Active Tab Indicator)
        if (mDropX < 0) mDropX = getTabCenterX(mSelectedTab);

        float tabWidth = (float) w / mTabs.length;
        TabItem activeTab = mTabs[mSelectedTab];
        float textSize = 12f * density;
        mTextPaint.setTextSize(textSize);
        float activeLabelWidth = mTextPaint.measureText(activeTab.title);
        float maxAllowedLabelWidth = tabWidth - (8f * density);
        if (activeLabelWidth > maxAllowedLabelWidth) {
            textSize = 11f * density;
            mTextPaint.setTextSize(textSize);
            activeLabelWidth = mTextPaint.measureText(activeTab.title);
        }

        float baseDropWidth = Math.max(72f * density, activeLabelWidth + 24f * density);
        float dropWidth = baseDropWidth * mDropStretch;
        float dropHeight = Math.min(48f * density, h - 4f * density);
        mDropBounds.set(
            mDropX - (dropWidth / 2f),
            (h - dropHeight) / 2f,
            mDropX + (dropWidth / 2f),
            (h + dropHeight) / 2f
        );

        canvas.drawRoundRect(mDropBounds, dropHeight / 2f, dropHeight / 2f, mDropPaint);
        canvas.drawRoundRect(mDropBounds, dropHeight / 2f, dropHeight / 2f, mDropBorderPaint);

        // 3. Render Tab Icons and Vietnamese Labels
        int iconSize = (int) (18 * density + 0.5f);

        for (int i = 0; i < mTabs.length; i++) {
            TabItem tab = mTabs[i];
            float cx = (i * tabWidth) + (tabWidth / 2f);
            boolean isSelected = (i == mSelectedTab);

            int tintColor = isSelected ? 0xFF3DDC97 : 0xFF9E988F; // Mint or Warm cream secondary

            if (tab.drawable != null) {
                tab.drawable.setTint(tintColor);
                int left = (int) (cx - (iconSize / 2f));
                int top = (int) (h * 0.16f);
                tab.drawable.setBounds(left, top, left + iconSize, top + iconSize);
                tab.drawable.draw(canvas);
            }

            mTextPaint.setColor(tintColor);
            mTextPaint.setFakeBoldText(isSelected);
            mTextPaint.setTextSize(isSelected ? textSize : 11.5f * density);
            float textY = (h * 0.82f);
            canvas.drawText(tab.title, cx, textY, mTextPaint);
        }
    }

    private float mTouchDownX = 0f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int w = getWidth();
        if (w <= 0) return super.onTouchEvent(event);

        float tabWidth = (float) w / mTabs.length;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                MotionKit.applyLiquidPressFeedback(this);
                return true;

            case MotionEvent.ACTION_MOVE:
                float currentX = Math.max(tabWidth / 2f, Math.min(w - tabWidth / 2f, event.getX()));
                mDropX = currentX;
                mDropStretch = 1.15f;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                int targetIndex = (int) (event.getX() / tabWidth);
                targetIndex = Math.max(0, Math.min(mTabs.length - 1, targetIndex));
                MotionKit.performHapticTick(this);
                setSelectedTab(targetIndex, true);
                if (mListener != null) {
                    mListener.onTabSelected(targetIndex);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                setSelectedTab(mSelectedTab, true);
                return true;
        }

        return super.onTouchEvent(event);
    }
}
