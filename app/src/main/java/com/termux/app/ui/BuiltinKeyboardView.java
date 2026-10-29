package com.termux.app.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Built-in In-App Keyboard "Bàn phím Termux Pro" (Section F).
 * Glass aesthetic matching the Termux Pro theme.
 * Emits pure-raw keystrokes directly into the single-writer pipeline.
 */
public class BuiltinKeyboardView extends LinearLayout {

    public interface KeyboardActionListener {
        void onText(String text);
        void onBackspace();
        void onEnter();
        void onCursorMove(int direction); // -1 left, +1 right
        void onGlobeClick();
        void onViToggleClick(boolean viActive);
    }

    private KeyboardActionListener mListener;
    private boolean mShiftActive = false;
    private boolean mCapsLock = false;
    private boolean mSymbolMode = false;
    private boolean mViActive = true;

    private final Handler mRepeatHandler = new Handler(Looper.getMainLooper());
    private Runnable mRepeatRunnable;

    private static final String[][] ROWS_QWERTY = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"z", "x", "c", "v", "b", "n", "m"}
    };

    private static final String[][] ROWS_SYMBOLS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"-", "/", ":", ";", "(", ")", "$", "&", "@", "\""},
        {".", ",", "?", "!", "'", "_", "~", "|", "`", "#"}
    };

    public BuiltinKeyboardView(Context context) {
        super(context);
        init();
    }

    public BuiltinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setKeyboardActionListener(KeyboardActionListener listener) {
        this.mListener = listener;
    }

    public void setViActive(boolean active) {
        this.mViActive = active;
        rebuildLayout();
    }

    public boolean isViActive() {
        return mViActive;
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setBackgroundColor(0xF0121418); // Frosted dark obsidian
        setPadding(dp(4), dp(6), dp(4), dp(8));
        rebuildLayout();
    }

    private int dp(float dpVal) {
        return (int) (dpVal * getResources().getDisplayMetrics().density + 0.5f);
    }

    public void rebuildLayout() {
        removeAllViews();

        String[][] currentRows = mSymbolMode ? ROWS_SYMBOLS : ROWS_QWERTY;

        // Row 1
        addView(createKeyRow(currentRows[0], 0));

        // Row 2
        addView(createKeyRow(currentRows[1], dp(12)));

        // Row 3
        if (mSymbolMode) {
            addView(createSymbolRow3(currentRows[2]));
        } else {
            addView(createQwertyRow3(currentRows[2]));
        }

        // Row 4: ?123 / ABC, Globe, Space, VI/EN, Enter
        addView(createBottomRow());
    }

    private LinearLayout createKeyRow(String[] keys, int sideMargin) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        row.setPadding(sideMargin, dp(2), sideMargin, dp(2));
        row.setGravity(Gravity.CENTER);

        for (String k : keys) {
            String label = (mShiftActive || mCapsLock) ? k.toUpperCase() : k;
            View keyView = createKeyButton(label, 1.0f, () -> {
                if (mListener != null) {
                    mListener.onText(label);
                }
                if (mShiftActive && !mCapsLock) {
                    mShiftActive = false;
                    rebuildLayout();
                }
            });
            row.addView(keyView);
        }
        return row;
    }

    private LinearLayout createQwertyRow3(String[] keys) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        row.setPadding(0, dp(2), 0, dp(2));
        row.setGravity(Gravity.CENTER);

        // Shift key
        String shiftLabel = mCapsLock ? "⇪" : (mShiftActive ? "▲" : "⇧");
        View shiftKey = createFunctionalButton(shiftLabel, 1.5f, mShiftActive || mCapsLock, () -> {
            if (mShiftActive && !mCapsLock) {
                mCapsLock = true;
            } else if (mCapsLock) {
                mCapsLock = false;
                mShiftActive = false;
            } else {
                mShiftActive = true;
            }
            rebuildLayout();
        });
        row.addView(shiftKey);

        for (String k : keys) {
            String label = (mShiftActive || mCapsLock) ? k.toUpperCase() : k;
            View keyView = createKeyButton(label, 1.0f, () -> {
                if (mListener != null) {
                    mListener.onText(label);
                }
                if (mShiftActive && !mCapsLock) {
                    mShiftActive = false;
                    rebuildLayout();
                }
            });
            row.addView(keyView);
        }

        // Backspace key
        View backspaceKey = createBackspaceButton(1.5f);
        row.addView(backspaceKey);

        return row;
    }

    private LinearLayout createSymbolRow3(String[] keys) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        row.setPadding(0, dp(2), 0, dp(2));
        row.setGravity(Gravity.CENTER);

        for (String k : keys) {
            View keyView = createKeyButton(k, 1.0f, () -> {
                if (mListener != null) mListener.onText(k);
            });
            row.addView(keyView);
        }

        View backspaceKey = createBackspaceButton(1.5f);
        row.addView(backspaceKey);

        return row;
    }

    private LinearLayout createBottomRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        row.setPadding(0, dp(2), 0, dp(2));
        row.setGravity(Gravity.CENTER);

        // 1. ?123 toggle
        String symLabel = mSymbolMode ? "ABC" : "?123";
        View symBtn = createFunctionalButton(symLabel, 1.3f, false, () -> {
            mSymbolMode = !mSymbolMode;
            rebuildLayout();
        });
        row.addView(symBtn);

        // 2. Globe button (switch to system IME)
        View globeBtn = createFunctionalButton("🌐", 1.0f, false, () -> {
            if (mListener != null) mListener.onGlobeClick();
        });
        row.addView(globeBtn);

        // 3. Spacebar with swipe cursor move
        View spaceBtn = createSpacebar(4.0f);
        row.addView(spaceBtn);

        // 4. VI/EN toggle button
        View viBtn = createFunctionalButton(mViActive ? "VI" : "EN", 1.2f, mViActive, () -> {
            mViActive = !mViActive;
            if (mListener != null) mListener.onViToggleClick(mViActive);
            rebuildLayout();
        });
        row.addView(viBtn);

        // 5. Enter button
        View enterBtn = createAccentButton("↵", 1.5f, () -> {
            if (mListener != null) mListener.onEnter();
        });
        row.addView(enterBtn);

        return row;
    }

    private View createKeyButton(String text, float weight, Runnable onClick) {
        FrameLayout frame = new FrameLayout(getContext());
        LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), 0, dp(2), 0);
        frame.setLayoutParams(lp);

        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(0xFFF2EDE4);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tv.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1FFFFFFF);
        bg.setCornerRadius(dp(7));
        bg.setStroke(dp(1), 0x14FFFFFF);
        tv.setBackground(bg);

        frame.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        frame.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            animatePress(v);
            if (onClick != null) onClick.run();
        });

        return frame;
    }

    private View createFunctionalButton(String text, float weight, boolean active, Runnable onClick) {
        FrameLayout frame = new FrameLayout(getContext());
        LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), 0, dp(2), 0);
        frame.setLayoutParams(lp);

        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(active ? 0xFF3DDC97 : 0xFFB3ADA3);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? 0x2E3DDC97 : 0x14FFFFFF);
        bg.setCornerRadius(dp(7));
        bg.setStroke(dp(1), active ? 0x663DDC97 : 0x14FFFFFF);
        tv.setBackground(bg);

        frame.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        frame.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            animatePress(v);
            if (onClick != null) onClick.run();
        });

        return frame;
    }

    private View createAccentButton(String text, float weight, Runnable onClick) {
        FrameLayout frame = new FrameLayout(getContext());
        LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), 0, dp(2), 0);
        frame.setLayoutParams(lp);

        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(0xFF0E0F11);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tv.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF3DDC97);
        bg.setCornerRadius(dp(7));
        tv.setBackground(bg);

        frame.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        frame.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            animatePress(v);
            if (onClick != null) onClick.run();
        });

        return frame;
    }

    @SuppressLint("ClickableViewAccessibility")
    private View createBackspaceButton(float weight) {
        FrameLayout frame = new FrameLayout(getContext());
        LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), 0, dp(2), 0);
        frame.setLayoutParams(lp);

        TextView tv = new TextView(getContext());
        tv.setText("⌫");
        tv.setTextColor(0xFFF2EDE4);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1FFFFFFF);
        bg.setCornerRadius(dp(7));
        bg.setStroke(dp(1), 0x14FFFFFF);
        tv.setBackground(bg);

        frame.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        frame.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    animatePress(v);
                    if (mListener != null) mListener.onBackspace();
                    startBackspaceRepeat();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopBackspaceRepeat();
                    return true;
            }
            return false;
        });

        return frame;
    }

    private void startBackspaceRepeat() {
        stopBackspaceRepeat();
        mRepeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onBackspace();
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
                mRepeatHandler.postDelayed(this, 60);
            }
        };
        mRepeatHandler.postDelayed(mRepeatRunnable, 350);
    }

    private void stopBackspaceRepeat() {
        if (mRepeatRunnable != null) {
            mRepeatHandler.removeCallbacks(mRepeatRunnable);
            mRepeatRunnable = null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private View createSpacebar(float weight) {
        FrameLayout frame = new FrameLayout(getContext());
        LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), 0, dp(2), 0);
        frame.setLayoutParams(lp);

        TextView tv = new TextView(getContext());
        tv.setText("Khoảng cách");
        tv.setTextColor(0xFF8C867D);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(dp(7));
        bg.setStroke(dp(1), 0x14FFFFFF);
        tv.setBackground(bg);

        frame.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final float[] startX = {0};
        final boolean[] isSwipe = {false};

        frame.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = event.getX();
                    isSwipe[0] = false;
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    animatePress(v);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float diffX = event.getX() - startX[0];
                    if (Math.abs(diffX) > dp(20)) {
                        isSwipe[0] = true;
                        int dir = diffX > 0 ? 1 : -1;
                        if (mListener != null) mListener.onCursorMove(dir);
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        startX[0] = event.getX();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isSwipe[0]) {
                        if (mListener != null) mListener.onText(" ");
                    }
                    return true;
            }
            return false;
        });

        return frame;
    }

    private void animatePress(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(50).withEndAction(() -> {
            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
        }).start();
    }
}
