package com.termux.app.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.view.Window;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TextStyle;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TermuxProThemeManager {

    private static final String LOG_TAG = "TermuxProThemeManager";
    private static final String PREFS_NAME = "termuxpro_preferences";
    private static final String KEY_CURRENT_THEME = "current_theme_id";
    public static final String DEFAULT_THEME_ID = "obsidian_pro";

    public static class ThemeItem {
        public final String id;
        public final String name;
        public final boolean isDark;
        public final String background;
        public final String foreground;
        public final String cursor;
        public final Map<String, String> colors = new LinkedHashMap<>();

        public ThemeItem(String id, String name, boolean isDark, String background, String foreground, String cursor) {
            this.id = id;
            this.name = name;
            this.isDark = isDark;
            this.background = background;
            this.foreground = foreground;
            this.cursor = cursor;
        }
    }

    private static final String[] THEME_FILES = {
        "obsidian_pro.json",
        "mocha_warm.json",
        "nordic_pine.json",
        "slate_steel.json",
        "paper_silk.json",
        "alabaster_frost.json"
    };

    private static final Map<String, ThemeItem> sThemes = new LinkedHashMap<>();
    private static boolean sLoaded = false;

    public static synchronized void init(Context context) {
        if (sLoaded) return;
        AssetManager assets = context.getAssets();
        for (String fileName : THEME_FILES) {
            try (InputStream is = assets.open("themes/" + fileName);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject obj = new JSONObject(sb.toString());

                String id = obj.getString("id");
                String name = obj.getString("name");
                boolean isDark = obj.optBoolean("isDark", true);
                String bg = obj.getString("background");
                String fg = obj.getString("foreground");
                String cursor = obj.getString("cursor");

                ThemeItem item = new ThemeItem(id, name, isDark, bg, fg, cursor);
                for (int i = 0; i < 16; i++) {
                    String colorKey = "color" + i;
                    if (obj.has(colorKey)) {
                        item.colors.put(colorKey, obj.getString(colorKey));
                    }
                }
                sThemes.put(id, item);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Failed to load theme " + fileName + ": " + e.getMessage());
            }
        }
        sLoaded = true;
    }

    public static List<ThemeItem> getAvailableThemes(Context context) {
        init(context);
        return new ArrayList<>(sThemes.values());
    }

    public static String getCurrentThemeId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENT_THEME, DEFAULT_THEME_ID);
    }

    public static void setCurrentThemeId(Context context, String themeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_THEME, themeId).apply();
    }

    public static boolean applyTheme(TermuxActivity activity, String themeId) {
        init(activity);
        ThemeItem theme = sThemes.get(themeId);
        if (theme == null) {
            theme = sThemes.get(DEFAULT_THEME_ID);
            if (theme == null) return false;
        }

        try {
            Properties props = new Properties();
            props.setProperty("background", theme.background);
            props.setProperty("foreground", theme.foreground);
            props.setProperty("cursor", theme.cursor);
            for (Map.Entry<String, String> entry : theme.colors.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }

            TerminalColors.COLOR_SCHEME.updateWith(props);

            // Update terminal emulator colors
            TerminalSession session = activity.getCurrentSession();
            if (session != null && session.getEmulator() != null) {
                session.getEmulator().mColors.reset();
            }

            // Update window and activity background
            int bgColor = android.graphics.Color.parseColor(theme.background);
            Window window = activity.getWindow();
            if (window != null && window.getDecorView() != null) {
                window.getDecorView().setBackgroundColor(bgColor);
            }
            if (activity.getTerminalView() != null) {
                activity.getTerminalView().setBackgroundColor(bgColor);
                activity.getTerminalView().invalidate();
            }

            setCurrentThemeId(activity, theme.id);
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error applying theme " + themeId + ": " + e.getMessage());
            return false;
        }
    }

    public static void showThemeDialog(final TermuxActivity activity) {
        init(activity);
        List<ThemeItem> themeList = getAvailableThemes(activity);
        String currentId = getCurrentThemeId(activity);

        String[] names = new String[themeList.size()];
        int selectedIndex = 0;
        for (int i = 0; i < themeList.size(); i++) {
            ThemeItem t = themeList.get(i);
            names[i] = t.name + (t.isDark ? " (Tối)" : " (Sáng)");
            if (t.id.equals(currentId)) selectedIndex = i;
        }

        new AlertDialog.Builder(activity)
            .setTitle("Chọn chủ đề giao diện")
            .setSingleChoiceItems(names, selectedIndex, (dialog, which) -> {
                ThemeItem selected = themeList.get(which);
                applyTheme(activity, selected.id);
                dialog.dismiss();
            })
            .setNegativeButton("Đóng", null)
            .show();
    }
}
