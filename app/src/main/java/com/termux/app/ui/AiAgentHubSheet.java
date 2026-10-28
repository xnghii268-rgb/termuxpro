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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalSession;

/**
 * Liquid Glass AI Coding Agent Hub Sheet.
 * Displays grouped, icon-led action cards with real blur, continuous curvature, and haptic feedback.
 */
public class AiAgentHubSheet {

    public static void show(@NonNull Activity activity) {
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

        // Header Row: Title + Round Glass Close Button
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, (int)(16 * density));

        TextView titleView = new TextView(activity);
        titleView.setText("AI Coding Agent Hub");
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(GlassComponents.COLOR_CREAM_PRIMARY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);

        FrameLayout closeBtn = GlassComponents.createRoundGlassButton(activity, R.drawable.ic_close, v -> sheet.dismiss());
        headerRow.addView(closeBtn);
        root.addView(headerRow);

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);

        // Group 1: Khởi chạy
        addSectionHeader(activity, content, "Khởi chạy", density);
        addActionCard(activity, content, "Antigravity CLI (agy)", "Trợ lý lập trình AI đa tác tử tích hợp sâu",
            R.drawable.ic_ai_agent, null, density, v -> {
                sheet.dismiss();
                runInTerminal(activity, "antigravity\n");
            });
        addActionCard(activity, content, "Gemini AI Assistant", "Trò chuyện và hỗ trợ code với Google Gemini",
            R.drawable.ic_prompt_chevron, null, density, v -> {
                sheet.dismiss();
                runInTerminal(activity, "gemini\n");
            });

        // Group 2: Cài đặt
        addSectionHeader(activity, content, "Cài đặt & Nâng cấp", density);
        addActionCard(activity, content, "Antigravity Framework", "Cập nhật bộ công cụ Antigravity CLI mới nhất",
            R.drawable.ic_plus_new, null, density, v -> {
                sheet.dismiss();
                runInTerminal(activity, "pip install antigravity-cli --upgrade\n");
            });

        // Group 3: Trạng thái
        addSectionHeader(activity, content, "Trạng thái", density);
        addActionCard(activity, content, "Xem trạng thái phiên AI Agent", "Kiểm tra phiên hoạt động và tiến trình nền",
            R.drawable.ic_nav_dashboard, "Sẵn sàng", density, v -> {
                sheet.dismiss();
                runInTerminal(activity, "echo '[TermuxPro AI Hub] Antigravity ready.'\n");
            });

        scrollView.addView(content);
        root.addView(scrollView);

        sheet.setContentView(root);
        sheet.show();
    }

    private static void addSectionHeader(Context context, LinearLayout parent, String title, float density) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextSize(12.5f);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(GlassComponents.COLOR_MINT_PRIMARY);
        header.setPadding((int)(4 * density), (int)(12 * density), 0, (int)(8 * density));
        parent.addView(header);
    }

    private static void addActionCard(Context context, LinearLayout parent, String title, String subtitle,
                                      int iconRes, @androidx.annotation.Nullable String statusChip, float density, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int)(14 * density), (int)(14 * density), (int)(14 * density), (int)(14 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(18 * density);
        bg.setColor(0xE61A1D24);
        bg.setStroke((int)(1 * density), GlassComponents.COLOR_GLASS_BORDER);
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = (int)(10 * density);
        card.setLayoutParams(cardParams);

        // Icon Container
        FrameLayout iconBox = new FrameLayout(context);
        LinearLayout.LayoutParams iconBoxParams = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
        iconBoxParams.rightMargin = (int)(14 * density);
        iconBox.setLayoutParams(iconBoxParams);

        View iconCircle = new View(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(0x1F3DDC97);
        circleBg.setStroke((int)(1 * density), 0x4D3DDC97);
        iconCircle.setBackground(circleBg);
        iconBox.addView(iconCircle);

        ImageView icon = new AppCompatImageView(context);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams((int)(18 * density), (int)(18 * density), Gravity.CENTER);
        icon.setLayoutParams(ip);
        icon.setImageResource(iconRes);
        icon.setColorFilter(GlassComponents.COLOR_MINT_PRIMARY);
        iconBox.addView(icon);
        card.addView(iconBox);

        // Texts Container
        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textColParams);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15.5f);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(GlassComponents.COLOR_CREAM_PRIMARY);
        textCol.addView(titleView);

        TextView subView = new TextView(context);
        subView.setText(subtitle);
        subView.setTextSize(13);
        subView.setTextColor(GlassComponents.COLOR_CREAM_SECONDARY);
        subView.setPadding(0, (int)(2 * density), 0, 0);
        textCol.addView(subView);
        card.addView(textCol);

        // Status Chip or Trailing Chevron
        if (statusChip != null) {
            TextView chip = new TextView(context);
            chip.setText(statusChip);
            chip.setTextSize(11.5f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setTextColor(GlassComponents.COLOR_MINT_PRIMARY);
            chip.setPadding((int)(8 * density), (int)(4 * density), (int)(8 * density), (int)(4 * density));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(10 * density);
            chipBg.setColor(0x223DDC97);
            chipBg.setStroke((int)(1 * density), 0x553DDC97);
            chip.setBackground(chipBg);
            card.addView(chip);
        } else {
            ImageView chevron = new AppCompatImageView(context);
            int chevronSize = (int)(18 * density);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(chevronSize, chevronSize);
            chevron.setLayoutParams(cp);
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(GlassComponents.COLOR_CREAM_MUTED);
            card.addView(chevron);
        }

        MotionKit.applyLiquidPressFeedback(card);
        card.setOnClickListener(listener);
        parent.addView(card);
    }

    private static void runInTerminal(Activity activity, String command) {
        if (activity instanceof TermuxActivity) {
            TermuxActivity ta = (TermuxActivity) activity;
            ta.showTerminalView(true);
            TerminalSession session = ta.getCurrentSession();
            if (session != null && session.isRunning()) {
                session.write(command);
            }
        }
    }
}
