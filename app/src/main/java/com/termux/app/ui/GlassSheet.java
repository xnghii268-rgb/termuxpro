package com.termux.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.R;
import com.termux.app.theme.TermuxProThemeManager;

import java.util.List;

/**
 * Modern Liquid Glass BottomSheet for Termux Pro.
 * Replaces standard legacy AlertDialogs with unified, sentence-case Vietnamese glass sheets.
 * Scrim alpha 0.35 max, real blur behind, 28dp continuous-curvature squircle, round glass close button.
 */
public class GlassSheet {

    public interface OnItemSelectedListener {
        void onItemSelected(int index);
    }

    public interface OnInputListener {
        void onInput(String text);
    }

    public static BottomSheetDialog createBaseSheet(@NonNull Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                float density = context.getResources().getDisplayMetrics().density;
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadii(new float[]{
                    28 * density, 28 * density,
                    28 * density, 28 * density,
                    0, 0, 0, 0
                });

                String currentThemeId = TermuxProThemeManager.getCurrentThemeId(context);
                boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");

                if (isDark) {
                    bg.setColor(Color.parseColor("#E61A1D24"));
                    bg.setStroke((int)(1.2f * density), Color.parseColor("#33FFFFFF"));
                } else {
                    bg.setColor(Color.parseColor("#F5FBF9F5"));
                    bg.setStroke((int)(1.2f * density), Color.parseColor("#22000000"));
                }
                bottomSheet.setBackground(bg);
            }

            // Window level blur and scrim alpha
            Window win = dialog.getWindow();
            if (win != null) {
                win.setDimAmount(0.35f); // 35% max scrim so living backdrop refracts
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    win.getAttributes().setBlurBehindRadius(28);
                }
            }
        });
        return dialog;
    }

    public static void showListSheet(@NonNull Context context,
                                     @NonNull String title,
                                     @NonNull List<String> items,
                                     int selectedIndex,
                                     @Nullable OnItemSelectedListener listener) {
        BottomSheetDialog dialog = createBaseSheet(context);
        float density = context.getResources().getDisplayMetrics().density;

        String currentThemeId = TermuxProThemeManager.getCurrentThemeId(context);
        boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");
        int textColor = isDark ? Color.parseColor("#F2EDE4") : Color.parseColor("#1C1917");
        int subTextColor = isDark ? Color.parseColor("#9E988F") : Color.parseColor("#64748B");
        int accentColor = Color.parseColor("#3DDC97"); // Mint

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(24 * density));

        // Drag handle (Glass grabber)
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2.5f * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(38 * density), (int)(5 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(14 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Header Row: Large Title + Round Glass Close Button
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, (int)(14 * density));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(textColor);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);

        FrameLayout closeBtn = GlassComponents.createRoundGlassButton(context, R.drawable.ic_close, v -> dialog.dismiss());
        headerRow.addView(closeBtn);
        root.addView(headerRow);

        // Scrollable Items list
        ScrollView scrollView = new ScrollView(context);
        LinearLayout itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            boolean isSelected = (i == selectedIndex);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int)(14 * density), (int)(14 * density), (int)(14 * density), (int)(14 * density));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(14 * density);
            if (isSelected) {
                rowBg.setColor(Color.parseColor("#1A3DDC97"));
                rowBg.setStroke((int)(1.5f * density), Color.parseColor("#3DDC97"));
            } else {
                rowBg.setColor(isDark ? Color.parseColor("#12FFFFFF") : Color.parseColor("#0A000000"));
                rowBg.setStroke((int)(0.5f * density), isDark ? Color.parseColor("#1FFFFFFF") : Color.parseColor("#0F000000"));
            }
            row.setBackground(rowBg);

            TextView itemText = new TextView(context);
            itemText.setText(items.get(i));
            itemText.setTextSize(14.5f);
            itemText.setTextColor(isSelected ? accentColor : textColor);
            if (isSelected) itemText.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            itemText.setLayoutParams(textParams);
            row.addView(itemText);

            if (isSelected) {
                TextView check = new TextView(context);
                check.setText("✓");
                check.setTextSize(16);
                check.setTextColor(accentColor);
                check.setTypeface(null, Typeface.BOLD);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                if (listener != null) listener.onItemSelected(index);
                dialog.dismiss();
            });

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = (int)(8 * density);
            row.setLayoutParams(rowParams);
            itemsContainer.addView(row);
        }

        scrollView.addView(itemsContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scrollView, scrollParams);

        dialog.setContentView(root);
        dialog.show();
    }

    public static void showConfirmationSheet(@NonNull Context context,
                                            @NonNull String title,
                                            @NonNull CharSequence message,
                                            @NonNull String positiveText,
                                            @Nullable Runnable onPositive,
                                            @Nullable String negativeText,
                                            @Nullable Runnable onNegative) {
        BottomSheetDialog dialog = createBaseSheet(context);
        float density = context.getResources().getDisplayMetrics().density;

        String currentThemeId = TermuxProThemeManager.getCurrentThemeId(context);
        boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");
        int textColor = isDark ? Color.parseColor("#F2EDE4") : Color.parseColor("#1C1917");
        int subTextColor = isDark ? Color.parseColor("#9E988F") : Color.parseColor("#64748B");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(24 * density));

        // Glass grabber
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2.5f * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(38 * density), (int)(5 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(14 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Header Row: Title + Round Glass Close Button
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, (int)(8 * density));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(textColor);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);

        FrameLayout closeBtn = GlassComponents.createRoundGlassButton(context, R.drawable.ic_close, v -> dialog.dismiss());
        headerRow.addView(closeBtn);
        root.addView(headerRow);

        // Message
        TextView msgView = new TextView(context);
        msgView.setText(message);
        msgView.setTextSize(14);
        msgView.setTextColor(subTextColor);
        msgView.setLineSpacing(0, 1.25f);
        msgView.setPadding(0, 0, 0, (int)(20 * density));
        root.addView(msgView);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        if (!TextUtils.isEmpty(negativeText)) {
            TextView negBtn = GlassComponents.createSecondaryGlassButton(context, negativeText, v -> {
                if (onNegative != null) onNegative.run();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams negParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
            negParams.rightMargin = (int)(10 * density);
            btnRow.addView(negBtn, negParams);
        }

        TextView posBtn = GlassComponents.createPrimaryMintButton(context, positiveText, v -> {
            if (onPositive != null) onPositive.run();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        btnRow.addView(posBtn, posParams);

        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }

    public static void showInputSheet(@NonNull Context context,
                                      @NonNull String title,
                                      @NonNull String hint,
                                      @Nullable String initialText,
                                      @NonNull String positiveText,
                                      @Nullable OnInputListener onPositive) {
        BottomSheetDialog dialog = createBaseSheet(context);
        float density = context.getResources().getDisplayMetrics().density;

        String currentThemeId = TermuxProThemeManager.getCurrentThemeId(context);
        boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");
        int textColor = isDark ? Color.parseColor("#F2EDE4") : Color.parseColor("#1C1917");
        int subTextColor = isDark ? Color.parseColor("#9E988F") : Color.parseColor("#64748B");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(24 * density));

        // Glass grabber
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2.5f * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(38 * density), (int)(5 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(14 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Header Row: Title + Round Close Button
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, (int)(12 * density));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(textColor);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);

        FrameLayout closeBtn = GlassComponents.createRoundGlassButton(context, R.drawable.ic_close, v -> dialog.dismiss());
        headerRow.addView(closeBtn);
        root.addView(headerRow);

        // EditText in glass pill
        EditText editText = new EditText(context);
        editText.setHint(hint);
        if (initialText != null) {
            editText.setText(initialText);
            editText.setSelection(initialText.length());
        }
        editText.setTextColor(textColor);
        editText.setHintTextColor(subTextColor);
        editText.setTextSize(15);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(14 * density);
        inputBg.setColor(isDark ? Color.parseColor("#22000000") : Color.parseColor("#14000000"));
        inputBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#22000000"));
        editText.setBackground(inputBg);
        editText.setPadding((int)(14 * density), (int)(12 * density), (int)(14 * density), (int)(12 * density));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.bottomMargin = (int)(20 * density);
        root.addView(editText, inputParams);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        TextView negBtn = GlassComponents.createSecondaryGlassButton(context, "Hủy", v -> dialog.dismiss());
        LinearLayout.LayoutParams negParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        negParams.rightMargin = (int)(10 * density);
        btnRow.addView(negBtn, negParams);

        TextView posBtn = GlassComponents.createPrimaryMintButton(context, positiveText, v -> {
            if (onPositive != null) onPositive.onInput(editText.getText().toString());
            dialog.dismiss();
        });
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        btnRow.addView(posBtn, posParams);

        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }
}
