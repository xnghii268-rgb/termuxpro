package com.termux.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.termux.R;

/**
 * Reusable Liquid Glass UI Components for Termux Pro.
 * Adheres strictly to the Warm Graphite + Warm Cream + Terminal Mint palette.
 * Replaces stock AlertDialog, ListView, Preference widgets, and Buttons.
 */
public class GlassComponents {

    // Art Direction Color Tokens
    public static final int COLOR_OBSIDIAN_BASE = 0xFF0E0F11;
    public static final int COLOR_OBSIDIAN_SURFACE = 0xFF14161B;
    public static final int COLOR_CREAM_PRIMARY = 0xFFF2EDE4;
    public static final int COLOR_CREAM_SECONDARY = 0xFF9E988F;
    public static final int COLOR_CREAM_MUTED = 0xFF6B665E;
    public static final int COLOR_MINT_PRIMARY = 0xFF3DDC97;
    public static final int COLOR_MINT_GLOW = 0x333DDC97;
    public static final int COLOR_MINT_SURFACE = 0x1A3DDC97;
    public static final int COLOR_CHAMPAGNE = 0xFFCDB89A;
    public static final int COLOR_GLASS_BORDER = 0x2EFFFFFF;
    public static final int COLOR_GLASS_FILL = 0xE61A1D24;

    /**
     * Creates a round glass close or back button (44dp visible size inside a 56dp touch target).
     */
    public static FrameLayout createRoundGlassButton(@NonNull Context context, int iconResId, @Nullable View.OnClickListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        FrameLayout container = new FrameLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int)(56 * density), (int)(56 * density));
        container.setLayoutParams(params);

        View innerCircle = new View(context);
        FrameLayout.LayoutParams innerParams = new FrameLayout.LayoutParams((int)(40 * density), (int)(40 * density), Gravity.CENTER);
        innerCircle.setLayoutParams(innerParams);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x26FFFFFF);
        bg.setStroke((int)(1 * density), COLOR_GLASS_BORDER);
        innerCircle.setBackground(bg);

        ImageView icon = new AppCompatImageView(context);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams((int)(18 * density), (int)(18 * density), Gravity.CENTER);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconResId);
        icon.setColorFilter(COLOR_CREAM_PRIMARY);

        container.addView(innerCircle);
        container.addView(icon);

        MotionKit.applyLiquidPressFeedback(container);
        if (listener != null) {
            container.setOnClickListener(listener);
        }
        return container;
    }

    /**
     * Creates a primary pill button (Mint #3DDC97 background with Obsidian text #0E0F11).
     */
    public static TextView createPrimaryMintButton(@NonNull Context context, @NonNull String text, @Nullable View.OnClickListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(14.5f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(COLOR_OBSIDIAN_BASE);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(12 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(24 * density);
        bg.setColor(COLOR_MINT_PRIMARY);
        btn.setBackground(bg);

        MotionKit.applyLiquidPressFeedback(btn);
        if (listener != null) {
            btn.setOnClickListener(listener);
        }
        return btn;
    }

    /**
     * Creates a secondary glass button (Translucent glass fill with Cream text #F2EDE4).
     */
    public static TextView createSecondaryGlassButton(@NonNull Context context, @NonNull String text, @Nullable View.OnClickListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTypeface(null, Typeface.NORMAL);
        btn.setTextColor(COLOR_CREAM_PRIMARY);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding((int)(18 * density), (int)(12 * density), (int)(18 * density), (int)(12 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20 * density);
        bg.setColor(0x22FFFFFF);
        bg.setStroke((int)(1 * density), COLOR_GLASS_BORDER);
        btn.setBackground(bg);

        MotionKit.applyLiquidPressFeedback(btn);
        if (listener != null) {
            btn.setOnClickListener(listener);
        }
        return btn;
    }

    /**
     * Shows a modal Glass Dialog with confirmation (e.g. destructive action without red fill).
     */
    public static void showGlassConfirmDialog(@NonNull Context context,
                                              @NonNull String title,
                                              @NonNull String message,
                                              @NonNull String positiveText,
                                              @Nullable Runnable onPositive,
                                              @NonNull String negativeText,
                                              @Nullable Runnable onNegative) {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        float density = context.getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(0x55000000); // 0.33 scrim

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            (int)(320 * density), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        card.setLayoutParams(cardParams);
        card.setPadding((int)(24 * density), (int)(22 * density), (int)(24 * density), (int)(20 * density));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(24 * density);
        cardBg.setColor(COLOR_GLASS_FILL);
        cardBg.setStroke((int)(1.2f * density), COLOR_GLASS_BORDER);
        card.setBackground(cardBg);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(COLOR_CREAM_PRIMARY);
        titleView.setPadding(0, 0, 0, (int)(8 * density));
        card.addView(titleView);

        // Message
        TextView msgView = new TextView(context);
        msgView.setText(message);
        msgView.setTextSize(14);
        msgView.setTextColor(COLOR_CREAM_SECONDARY);
        msgView.setLineSpacing(0, 1.25f);
        msgView.setPadding(0, 0, 0, (int)(20 * density));
        card.addView(msgView);

        // Buttons Row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        TextView negBtn = createSecondaryGlassButton(context, negativeText, v -> {
            if (onNegative != null) onNegative.run();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams negParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        negParams.rightMargin = (int)(10 * density);
        btnRow.addView(negBtn, negParams);

        TextView posBtn = createPrimaryMintButton(context, positiveText, v -> {
            if (onPositive != null) onPositive.run();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        btnRow.addView(posBtn, posParams);

        card.addView(btnRow);
        root.addView(card);
        root.setOnClickListener(v -> dialog.dismiss());
        card.setOnClickListener(v -> {}); // Prevent dismiss when tapping card

        dialog.setContentView(root);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Window win = dialog.getWindow();
            if (win != null) {
                win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                win.getAttributes().setBlurBehindRadius(28);
            }
        }
        dialog.show();
    }

    /**
     * Shows a Glass Toast pill at the bottom center of the screen above the tab bar.
     */
    public static void showGlassToast(@NonNull Context context, @NonNull View parentView, @NonNull String message) {
        if (parentView == null) return;
        float density = context.getResources().getDisplayMetrics().density;

        ViewGroup root = (parentView instanceof ViewGroup) ? (ViewGroup) parentView : (ViewGroup) parentView.getRootView();
        final FrameLayout toastContainer = new FrameLayout(context);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        containerParams.bottomMargin = (int)(86 * density);
        toastContainer.setLayoutParams(containerParams);

        TextView toastView = new TextView(context);
        toastView.setText(message);
        toastView.setTextSize(13.5f);
        toastView.setTextColor(COLOR_CREAM_PRIMARY);
        toastView.setTypeface(null, Typeface.NORMAL);
        toastView.setPadding((int)(18 * density), (int)(10 * density), (int)(18 * density), (int)(10 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(22 * density);
        bg.setColor(0xF0181B22);
        bg.setStroke((int)(1 * density), COLOR_GLASS_BORDER);
        toastView.setBackground(bg);

        toastContainer.addView(toastView);
        root.addView(toastContainer);

        // Spring entrance animation
        toastContainer.setAlpha(0f);
        toastContainer.setTranslationY(20 * density);
        toastContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .start();

        // Auto dismiss after 3 seconds with spring exit
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            toastContainer.animate()
                .alpha(0f)
                .translationY(15 * density)
                .setDuration(240)
                .withEndAction(() -> root.removeView(toastContainer))
                .start();
        }, 3000);
    }
}
