package com.termux.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.R;
import com.termux.app.theme.TermuxProThemeManager;

import java.util.List;

/**
 * Liquid Glass Theme Picker with horizontal carousel of theme cards.
 * Displays live mini terminal preview for each palette, name, and light/dark tag.
 * Selected card uses a Mint #3DDC97 2dp outline and Mint check. No pink/wine fill.
 */
public class ThemePickerSheet {

    public static void show(@NonNull Activity activity) {
        TermuxProThemeManager.init(activity);
        List<TermuxProThemeManager.ThemeItem> themes = TermuxProThemeManager.getAvailableThemes(activity);
        String currentId = TermuxProThemeManager.getCurrentThemeId(activity);

        BottomSheetDialog sheet = GlassSheet.createBaseSheet(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(28 * density));

        // Glass grabber
        View grabber = new View(activity);
        GradientDrawable grabberBg = new GradientDrawable();
        grabberBg.setShape(GradientDrawable.RECTANGLE);
        grabberBg.setCornerRadius(2.5f * density);
        grabberBg.setColor(0x4DFFFFFF);
        LinearLayout.LayoutParams grabberParams = new LinearLayout.LayoutParams((int)(38 * density), (int)(5 * density));
        grabberParams.gravity = Gravity.CENTER_HORIZONTAL;
        grabberParams.bottomMargin = (int)(14 * density);
        grabber.setLayoutParams(grabberParams);
        grabber.setBackground(grabberBg);
        root.addView(grabber);

        // Header Row: Large Title + Round Glass Close Button
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, (int)(16 * density));

        TextView titleView = new TextView(activity);
        titleView.setText("Chủ đề giao diện");
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(GlassComponents.COLOR_CREAM_PRIMARY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);

        FrameLayout closeBtn = GlassComponents.createRoundGlassButton(activity, R.drawable.ic_close, v -> sheet.dismiss());
        headerRow.addView(closeBtn);
        root.addView(headerRow);

        // Horizontal Carousel Scroll View
        HorizontalScrollView hScroll = new HorizontalScrollView(activity);
        hScroll.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        hScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout cardsContainer = new LinearLayout(activity);
        cardsContainer.setOrientation(LinearLayout.HORIZONTAL);
        cardsContainer.setPadding(0, (int)(4 * density), 0, (int)(12 * density));

        for (int i = 0; i < themes.size(); i++) {
            final TermuxProThemeManager.ThemeItem theme = themes.get(i);
            final boolean isSelected = theme.id.equals(currentId);

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            int cardWidth = (int)(170 * density);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.rightMargin = (int)(14 * density);
            card.setLayoutParams(cardParams);
            card.setPadding((int)(12 * density), (int)(12 * density), (int)(12 * density), (int)(14 * density));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(20 * density);
            if (isSelected) {
                cardBg.setColor(0x1F3DDC97);
                cardBg.setStroke((int)(2 * density), GlassComponents.COLOR_MINT_PRIMARY);
            } else {
                cardBg.setColor(0xCC1A1D24);
                cardBg.setStroke((int)(1 * density), GlassComponents.COLOR_GLASS_BORDER);
            }
            card.setBackground(cardBg);

            // Mini Terminal Live Preview Box
            LinearLayout miniTerminal = new LinearLayout(activity);
            miniTerminal.setOrientation(LinearLayout.VERTICAL);
            int termHeight = (int)(95 * density);
            LinearLayout.LayoutParams termParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, termHeight);
            termParams.bottomMargin = (int)(10 * density);
            miniTerminal.setLayoutParams(termParams);
            miniTerminal.setPadding((int)(8 * density), (int)(8 * density), (int)(8 * density), (int)(8 * density));

            GradientDrawable termBg = new GradientDrawable();
            termBg.setShape(GradientDrawable.RECTANGLE);
            termBg.setCornerRadius(12 * density);
            try {
                termBg.setColor(Color.parseColor(theme.background));
            } catch (Exception e) {
                termBg.setColor(0xFF0E0F11);
            }
            termBg.setStroke((int)(0.5f * density), 0x22FFFFFF);
            miniTerminal.setBackground(termBg);

            int fgColor;
            try {
                fgColor = Color.parseColor(theme.foreground);
            } catch (Exception e) {
                fgColor = 0xFFF2EDE4;
            }

            TextView line1 = new TextView(activity);
            line1.setText("~ $ neofetch");
            line1.setTextSize(10.5f);
            line1.setTypeface(Typeface.MONOSPACE);
            line1.setTextColor(fgColor);
            miniTerminal.addView(line1);

            TextView line2 = new TextView(activity);
            line2.setText("OS: Termux Pro");
            line2.setTextSize(10.5f);
            line2.setTypeface(Typeface.MONOSPACE);
            line2.setTextColor(GlassComponents.COLOR_MINT_PRIMARY);
            miniTerminal.addView(line2);

            TextView line3 = new TextView(activity);
            line3.setText("~ $ _");
            line3.setTextSize(10.5f);
            line3.setTypeface(Typeface.MONOSPACE);
            line3.setTextColor(GlassComponents.COLOR_MINT_PRIMARY);
            miniTerminal.addView(line3);

            card.addView(miniTerminal);

            // Theme Name & Tag Row
            LinearLayout nameRow = new LinearLayout(activity);
            nameRow.setOrientation(LinearLayout.HORIZONTAL);
            nameRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameText = new TextView(activity);
            nameText.setText(theme.name);
            nameText.setTextSize(15);
            nameText.setTypeface(null, Typeface.BOLD);
            nameText.setTextColor(isSelected ? GlassComponents.COLOR_MINT_PRIMARY : GlassComponents.COLOR_CREAM_PRIMARY);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameText.setLayoutParams(nameParams);
            nameRow.addView(nameText);

            if (isSelected) {
                TextView checkIcon = new TextView(activity);
                checkIcon.setText("✓");
                checkIcon.setTextSize(16);
                checkIcon.setTextColor(GlassComponents.COLOR_MINT_PRIMARY);
                checkIcon.setTypeface(null, Typeface.BOLD);
                nameRow.addView(checkIcon);
            }
            card.addView(nameRow);

            // Light/Dark tag pill
            TextView tagPill = new TextView(activity);
            tagPill.setText(theme.isDark ? "Tối" : "Sáng");
            tagPill.setTextSize(11);
            tagPill.setTextColor(GlassComponents.COLOR_CREAM_SECONDARY);
            tagPill.setPadding(0, (int)(2 * density), 0, 0);
            card.addView(tagPill);

            MotionKit.applyLiquidPressFeedback(card);
            card.setOnClickListener(v -> {
                TermuxProThemeManager.applyTheme(activity, theme.id);
                GlassComponents.showGlassToast(activity, activity.getWindow().getDecorView(), "Đã kích hoạt: " + theme.name);
                sheet.dismiss();
            });

            cardsContainer.addView(card);
        }

        hScroll.addView(cardsContainer);
        root.addView(hScroll);

        sheet.setContentView(root);
        sheet.show();
    }
}
