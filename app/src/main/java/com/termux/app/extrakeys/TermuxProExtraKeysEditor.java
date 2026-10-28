package com.termux.app.extrakeys;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.app.TermuxActivity;
import com.termux.app.theme.TermuxProThemeManager;
import com.termux.app.ui.GlassSheet;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Interactive Extra Keys Layout Editor for Termux Pro.
 * Allows adding, removing, and reordering keys on Row 1 and Row 2.
 * Supports optional Pro keys (Dán ảnh, Ảnh mới, Lệnh, Telex) and standard terminal keys.
 */
public class TermuxProExtraKeysEditor {

    private static final String[] DEFAULT_ROW_1 = {"ESC", "/", "-", "HOME", "UP", "END", "PGUP"};
    private static final String[] DEFAULT_ROW_2 = {"TAB", "CTRL", "ALT", "LEFT", "DOWN", "RIGHT", "PGDN"};

    private static final String[][] OPTIONAL_PRO_KEYS = {
        {"CLIP_IMAGE", "Dán ảnh"},
        {"RECENT_IMAGE", "Ảnh mới"},
        {"PALETTE", "Lệnh"},
        {"TELEX", "Telex"}
    };

    private static final String[] AVAILABLE_EXTRA_KEYS = {
        "|", "~", "\\", "QUOTE", "APOSTROPHE", "INS", "DEL", "BKSP", "ENTER",
        "FN", "SHIFT", "PASTE", "DRAWER", "KEYBOARD",
        "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"
    };

    public static void show(@NonNull Activity activity) {
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(activity, false);
        if (preferences == null) return;

        List<String> row1 = new ArrayList<>();
        List<String> row2 = new ArrayList<>();
        loadCurrentLayout(preferences, row1, row2);

        BottomSheetDialog dialog = GlassSheet.createBaseSheet(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        String currentThemeId = TermuxProThemeManager.getCurrentThemeId(activity);
        boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");
        int textColor = isDark ? Color.parseColor("#F2EDE4") : Color.parseColor("#1C1917");
        int subTextColor = isDark ? Color.parseColor("#9E988F") : Color.parseColor("#64748B");
        int accentColor = Color.parseColor("#3DDC97");

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int)(16 * density), (int)(12 * density), (int)(16 * density), (int)(24 * density));

        // Drag handle
        View handle = new View(activity);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(12 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // Title
        TextView titleView = new TextView(activity);
        titleView.setText("Tùy biến phím phụ");
        titleView.setTextSize(18);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(4 * density));
        root.addView(titleView);

        TextView subTitleView = new TextView(activity);
        subTitleView.setText("Chạm vào từng phím để di chuyển, đổi hàng hoặc xóa.");
        subTitleView.setTextSize(13);
        subTitleView.setTextColor(subTextColor);
        subTitleView.setPadding(0, 0, 0, (int)(16 * density));
        root.addView(subTitleView);

        // Container for Rows preview
        LinearLayout rowsContainer = new LinearLayout(activity);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rowsContainer);

        // Container for Optional Pro Keys
        LinearLayout optionalContainer = new LinearLayout(activity);
        optionalContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(optionalContainer);

        Runnable refreshUI = new Runnable() {
            @Override
            public void run() {
                renderRows(activity, rowsContainer, row1, row2, textColor, subTextColor, accentColor, isDark, density, this);
                renderOptionalKeys(activity, optionalContainer, row1, row2, textColor, subTextColor, accentColor, isDark, density, this);
            }
        };

        refreshUI.run();

        // Section "Thêm phím chuẩn khác"
        Button addKeyBtn = new Button(activity, null, android.R.attr.borderlessButtonStyle);
        addKeyBtn.setText("+ Thêm phím khác...");
        addKeyBtn.setTextColor(accentColor);
        addKeyBtn.setTextSize(14);
        addKeyBtn.setAllCaps(false);
        addKeyBtn.setOnClickListener(v -> {
            List<String> availableList = new ArrayList<>(Arrays.asList(AVAILABLE_EXTRA_KEYS));
            GlassSheet.showListSheet(activity, "Chọn phím muốn thêm", availableList, -1, index -> {
                String chosen = availableList.get(index);
                List<String> targetRowOptions = Arrays.asList("Thêm vào Hàng 1", "Thêm vào Hàng 2");
                GlassSheet.showListSheet(activity, "Thêm \"" + chosen + "\" vào đâu?", targetRowOptions, 1, rowChoice -> {
                    if (rowChoice == 0) {
                        row1.add(chosen);
                    } else {
                        row2.add(chosen);
                    }
                    refreshUI.run();
                });
            });
        });
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addParams.topMargin = (int)(8 * density);
        addParams.bottomMargin = (int)(16 * density);
        root.addView(addKeyBtn, addParams);

        // Action buttons row (Reset to default & Save)
        LinearLayout actionsRow = new LinearLayout(activity);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.END);

        Button resetBtn = new Button(activity, null, android.R.attr.borderlessButtonStyle);
        resetBtn.setText("Đặt lại mặc định");
        resetBtn.setTextColor(subTextColor);
        resetBtn.setTextSize(14);
        resetBtn.setAllCaps(false);
        resetBtn.setOnClickListener(v -> {
            row1.clear();
            row1.addAll(Arrays.asList(DEFAULT_ROW_1));
            row2.clear();
            row2.addAll(Arrays.asList(DEFAULT_ROW_2));
            refreshUI.run();
            Toast.makeText(activity, "Đã khôi phục 14 phím chuẩn 7 cột x 2 hàng", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        resetParams.rightMargin = (int)(8 * density);
        actionsRow.addView(resetBtn, resetParams);

        Button saveBtn = new Button(activity);
        saveBtn.setText("Lưu & Áp dụng");
        saveBtn.setTextSize(14);
        saveBtn.setAllCaps(false);
        saveBtn.setTextColor(Color.parseColor("#1A0A10"));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(10 * density);
        saveBg.setColor(accentColor);
        saveBtn.setBackground(saveBg);
        saveBtn.setOnClickListener(v -> {
            saveLayout(preferences, row1, row2);
            TermuxActivity termuxActivity = TermuxActivity.getCurrentActivity();
            if (termuxActivity != null) {
                termuxActivity.reloadExtraKeys();
            }
            Toast.makeText(activity, "Đã áp dụng bố cục phím phụ mới", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        actionsRow.addView(saveBtn, saveParams);

        root.addView(actionsRow);

        scrollView.addView(root);
        dialog.setContentView(scrollView);
        dialog.show();
    }

    private static void loadCurrentLayout(TermuxAppSharedPreferences preferences, List<String> row1, List<String> row2) {
        String saved = preferences.getExtraKeysLayout();
        if (TextUtils.isEmpty(saved)) {
            saved = TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS;
        }

        try {
            JSONArray arr = new JSONArray(saved);
            if (arr.length() > 0) {
                JSONArray r1 = arr.getJSONArray(0);
                for (int i = 0; i < r1.length(); i++) {
                    Object item = r1.get(i);
                    row1.add(extractKeyName(item));
                }
            }
            if (arr.length() > 1) {
                JSONArray r2 = arr.getJSONArray(1);
                for (int i = 0; i < r2.length(); i++) {
                    Object item = r2.get(i);
                    row2.add(extractKeyName(item));
                }
            }
        } catch (Exception e) {
            row1.clear();
            row1.addAll(Arrays.asList(DEFAULT_ROW_1));
            row2.clear();
            row2.addAll(Arrays.asList(DEFAULT_ROW_2));
        }

        if (row1.isEmpty() && row2.isEmpty()) {
            row1.addAll(Arrays.asList(DEFAULT_ROW_1));
            row2.addAll(Arrays.asList(DEFAULT_ROW_2));
        }
    }

    private static String extractKeyName(Object item) {
        if (item instanceof String) {
            return (String) item;
        } else if (item instanceof org.json.JSONObject) {
            org.json.JSONObject obj = (org.json.JSONObject) item;
            return obj.optString("key", obj.optString("display", ""));
        }
        return String.valueOf(item);
    }

    private static void renderRows(Context context, LinearLayout container,
                                   List<String> row1, List<String> row2,
                                   int textColor, int subTextColor, int accentColor,
                                   boolean isDark, float density, Runnable refreshCallback) {
        container.removeAllViews();

        // Row 1
        renderSingleRow(context, container, "Hàng 1 (" + row1.size() + " phím)", row1, row2, 1,
            textColor, subTextColor, accentColor, isDark, density, refreshCallback);

        // Row 2
        renderSingleRow(context, container, "Hàng 2 (" + row2.size() + " phím)", row2, row1, 2,
            textColor, subTextColor, accentColor, isDark, density, refreshCallback);
    }

    private static void renderSingleRow(Context context, LinearLayout container, String title,
                                        List<String> thisRow, List<String> otherRow, int rowNumber,
                                        int textColor, int subTextColor, int accentColor,
                                        boolean isDark, float density, Runnable refreshCallback) {
        TextView rowHeader = new TextView(context);
        rowHeader.setText(title);
        rowHeader.setTextSize(14);
        rowHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        rowHeader.setTextColor(textColor);
        rowHeader.setPadding(0, (int)(8 * density), 0, (int)(6 * density));
        container.addView(rowHeader);

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipContainer = new LinearLayout(context);
        chipContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipContainer.setPadding(0, (int)(4 * density), 0, (int)(12 * density));

        for (int i = 0; i < thisRow.size(); i++) {
            final int index = i;
            final String key = thisRow.get(i);

            Button chip = new Button(context);
            chip.setText(getDisplayLabel(key));
            chip.setTextSize(12);
            chip.setAllCaps(false);
            chip.setTextColor(textColor);
            chip.setPadding((int)(12 * density), 0, (int)(12 * density), 0);
            chip.setMinHeight((int)(36 * density));
            chip.setMinimumHeight((int)(36 * density));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(8 * density);
            chipBg.setColor(isDark ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000"));
            chipBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#22000000"));
            chip.setBackground(chipBg);

            chip.setOnClickListener(v -> {
                List<String> actions = new ArrayList<>();
                if (index > 0) actions.add("Di chuyển sang trái (←)");
                if (index < thisRow.size() - 1) actions.add("Di chuyển sang phải (→)");
                actions.add(rowNumber == 1 ? "Chuyển xuống Hàng 2 (↓)" : "Chuyển lên Hàng 1 (↑)");
                actions.add("Xóa phím này (✕)");

                GlassSheet.showListSheet(context, "Thao tác với phím [" + getDisplayLabel(key) + "]", actions, -1, which -> {
                    String action = actions.get(which);
                    if (action.startsWith("Di chuyển sang trái")) {
                        String temp = thisRow.remove(index);
                        thisRow.add(index - 1, temp);
                    } else if (action.startsWith("Di chuyển sang phải")) {
                        String temp = thisRow.remove(index);
                        thisRow.add(index + 1, temp);
                    } else if (action.startsWith("Chuyển")) {
                        String temp = thisRow.remove(index);
                        otherRow.add(temp);
                    } else if (action.startsWith("Xóa")) {
                        thisRow.remove(index);
                    }
                    refreshCallback.run();
                });
            });

            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int)(36 * density));
            chipParams.rightMargin = (int)(6 * density);
            chipContainer.addView(chip, chipParams);
        }

        scroll.addView(chipContainer);
        container.addView(scroll);
    }

    private static void renderOptionalKeys(Context context, LinearLayout container,
                                           List<String> row1, List<String> row2,
                                           int textColor, int subTextColor, int accentColor,
                                           boolean isDark, float density, Runnable refreshCallback) {
        container.removeAllViews();

        TextView header = new TextView(context);
        header.setText("Phím tiện ích Termux Pro (Tùy chọn)");
        header.setTextSize(14);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(textColor);
        header.setPadding(0, (int)(12 * density), 0, (int)(8 * density));
        container.addView(header);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (String[] opt : OPTIONAL_PRO_KEYS) {
            final String keyId = opt[0];
            final String label = opt[1];
            boolean active = row1.contains(keyId) || row2.contains(keyId);

            Button optBtn = new Button(context);
            optBtn.setText(label + (active ? " ✓" : " +"));
            optBtn.setTextSize(12);
            optBtn.setAllCaps(false);
            optBtn.setTextColor(active ? Color.parseColor("#1A0A10") : textColor);
            optBtn.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
            optBtn.setMinHeight((int)(34 * density));
            optBtn.setMinimumHeight((int)(34 * density));

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(8 * density);
            if (active) {
                btnBg.setColor(accentColor);
            } else {
                btnBg.setColor(isDark ? Color.parseColor("#1FFFFFFF") : Color.parseColor("#0F000000"));
                btnBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#22000000"));
            }
            optBtn.setBackground(btnBg);

            optBtn.setOnClickListener(v -> {
                if (active) {
                    row1.remove(keyId);
                    row2.remove(keyId);
                } else {
                    row2.add(keyId);
                }
                refreshCallback.run();
            });

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int)(34 * density));
            btnParams.rightMargin = (int)(6 * density);
            btnParams.bottomMargin = (int)(6 * density);
            row.addView(optBtn, btnParams);
        }

        HorizontalScrollView optScroll = new HorizontalScrollView(context);
        optScroll.setHorizontalScrollBarEnabled(false);
        optScroll.addView(row);
        container.addView(optScroll);
    }

    private static String getDisplayLabel(String key) {
        if ("UP".equalsIgnoreCase(key)) return "↑";
        if ("DOWN".equalsIgnoreCase(key)) return "↓";
        if ("LEFT".equalsIgnoreCase(key)) return "←";
        if ("RIGHT".equalsIgnoreCase(key)) return "→";
        if ("TAB".equalsIgnoreCase(key)) return "⇥";
        if ("CLIP_IMAGE".equalsIgnoreCase(key)) return "Dán ảnh";
        if ("RECENT_IMAGE".equalsIgnoreCase(key)) return "Ảnh mới";
        if ("PALETTE".equalsIgnoreCase(key)) return "Lệnh";
        if ("THEME".equalsIgnoreCase(key)) return "Giao diện";
        if ("TELEX".equalsIgnoreCase(key)) return "Telex";
        return key;
    }

    private static void saveLayout(TermuxAppSharedPreferences preferences, List<String> row1, List<String> row2) {
        JSONArray arr = new JSONArray();
        JSONArray r1 = new JSONArray();
        for (String k : row1) r1.put(k);
        JSONArray r2 = new JSONArray();
        for (String k : row2) r2.put(k);
        arr.put(r1);
        arr.put(r2);

        String json = arr.toString();
        // If it equals default layout, we can store null to use DEFAULT_IVALUE_EXTRA_KEYS
        if (isMatchesDefault(row1, row2)) {
            preferences.setExtraKeysLayout(null);
        } else {
            preferences.setExtraKeysLayout(json);
        }
    }

    private static boolean isMatchesDefault(List<String> r1, List<String> r2) {
        if (r1.size() != DEFAULT_ROW_1.length || r2.size() != DEFAULT_ROW_2.length) return false;
        for (int i = 0; i < DEFAULT_ROW_1.length; i++) {
            if (!DEFAULT_ROW_1[i].equals(r1.get(i))) return false;
        }
        for (int i = 0; i < DEFAULT_ROW_2.length; i++) {
            if (!DEFAULT_ROW_2[i].equals(r2.get(i))) return false;
        }
        return true;
    }
}
