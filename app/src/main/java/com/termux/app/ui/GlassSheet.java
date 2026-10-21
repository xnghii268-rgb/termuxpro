package com.termux.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.app.theme.TermuxProThemeManager;

import java.util.List;

/**
 * Modern Material 3 Glass BottomSheet for Termux Pro.
 * Replaces standard legacy AlertDialogs with unified, sentence-case Vietnamese glass sheets.
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
                    20 * density, 20 * density,
                    20 * density, 20 * density,
                    0, 0, 0, 0
                });

                String currentThemeId = TermuxProThemeManager.getCurrentThemeId(context);
                boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");

                if (isDark) {
                    bg.setColor(Color.parseColor("#F5161B22"));
                    bg.setStroke((int)(1 * density), Color.parseColor("#33FFFFFF"));
                } else {
                    bg.setColor(Color.parseColor("#F5FBF9F5"));
                    bg.setStroke((int)(1 * density), Color.parseColor("#22000000"));
                }
                bottomSheet.setBackground(bg);
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
        int textColor = isDark ? Color.parseColor("#F3F4F6") : Color.parseColor("#1F2937");
        int subTextColor = isDark ? Color.parseColor("#9CA3AF") : Color.parseColor("#6B7280");
        int accentColor = Color.parseColor("#FF3C70");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(24 * density));

        // Drag handle
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(16 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(17);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(16 * density));
        root.addView(titleView);

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
            row.setPadding((int)(12 * density), (int)(14 * density), (int)(12 * density), (int)(14 * density));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(10 * density);
            if (isSelected) {
                rowBg.setColor(Color.parseColor("#26FF3C70"));
                rowBg.setStroke((int)(1 * density), Color.parseColor("#66FF3C70"));
            } else {
                rowBg.setColor(Color.TRANSPARENT);
            }
            row.setBackground(rowBg);

            TextView itemText = new TextView(context);
            itemText.setText(items.get(i));
            itemText.setTextSize(14);
            itemText.setTextColor(isSelected ? accentColor : textColor);
            if (isSelected) itemText.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            itemText.setLayoutParams(textParams);
            row.addView(itemText);

            if (isSelected) {
                TextView check = new TextView(context);
                check.setText("✓");
                check.setTextSize(16);
                check.setTextColor(accentColor);
                check.setTypeface(null, android.graphics.Typeface.BOLD);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                if (listener != null) listener.onItemSelected(index);
                dialog.dismiss();
            });

            itemsContainer.addView(row);
        }

        scrollView.addView(itemsContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.bottomMargin = (int)(12 * density);
        root.addView(scrollView, scrollParams);

        // Close button
        Button closeBtn = new Button(context, null, android.R.attr.borderlessButtonStyle);
        closeBtn.setText("Đóng");
        closeBtn.setTextColor(subTextColor);
        closeBtn.setTextSize(14);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(44 * density));
        root.addView(closeBtn, btnParams);

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
        int textColor = isDark ? Color.parseColor("#F3F4F6") : Color.parseColor("#1F2937");
        int subTextColor = isDark ? Color.parseColor("#9CA3AF") : Color.parseColor("#6B7280");
        int accentColor = Color.parseColor("#FF3C70");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(20 * density));

        // Handle
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(16 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(17);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(8 * density));
        root.addView(titleView);

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
            Button negBtn = new Button(context, null, android.R.attr.borderlessButtonStyle);
            negBtn.setText(negativeText);
            negBtn.setTextColor(subTextColor);
            negBtn.setTextSize(14);
            negBtn.setOnClickListener(v -> {
                if (onNegative != null) onNegative.run();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams negParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
            negParams.rightMargin = (int)(8 * density);
            btnRow.addView(negBtn, negParams);
        }

        Button posBtn = new Button(context);
        posBtn.setText(positiveText);
        posBtn.setTextSize(14);
        posBtn.setTextColor(Color.parseColor("#1A0A10")); // WCAG 5.60:1 on #FF3C70
        GradientDrawable posBg = new GradientDrawable();
        posBg.setCornerRadius(10 * density);
        posBg.setColor(accentColor);
        posBtn.setBackground(posBg);
        posBtn.setOnClickListener(v -> {
            if (onPositive != null) onPositive.run();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        posParams.leftMargin = (int)(4 * density);
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
        int textColor = isDark ? Color.parseColor("#F3F4F6") : Color.parseColor("#1F2937");
        int subTextColor = isDark ? Color.parseColor("#9CA3AF") : Color.parseColor("#6B7280");
        int accentColor = Color.parseColor("#FF3C70");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(20 * density));

        // Handle
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(16 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(17);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(12 * density));
        root.addView(titleView);

        // EditText
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
        inputBg.setCornerRadius(10 * density);
        inputBg.setColor(isDark ? Color.parseColor("#26000000") : Color.parseColor("#14000000"));
        inputBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#22000000"));
        editText.setBackground(inputBg);
        editText.setPadding((int)(12 * density), (int)(10 * density), (int)(12 * density), (int)(10 * density));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.bottomMargin = (int)(20 * density);
        root.addView(editText, inputParams);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        Button negBtn = new Button(context, null, android.R.attr.borderlessButtonStyle);
        negBtn.setText("Hủy");
        negBtn.setTextColor(subTextColor);
        negBtn.setTextSize(14);
        negBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams negParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        negParams.rightMargin = (int)(8 * density);
        btnRow.addView(negBtn, negParams);

        Button posBtn = new Button(context);
        posBtn.setText(positiveText);
        posBtn.setTextSize(14);
        posBtn.setTextColor(Color.parseColor("#1A0A10"));
        GradientDrawable posBg = new GradientDrawable();
        posBg.setCornerRadius(10 * density);
        posBg.setColor(accentColor);
        posBtn.setBackground(posBg);
        posBtn.setOnClickListener(v -> {
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
