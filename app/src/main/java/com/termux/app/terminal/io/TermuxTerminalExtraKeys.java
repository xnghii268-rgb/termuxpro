package com.termux.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.settings.properties.TermuxSharedProperties;
import com.termux.shared.termux.terminal.io.TerminalExtraKeys;
import com.termux.view.TerminalView;

import org.json.JSONException;

public class TermuxTerminalExtraKeys extends TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final TermuxActivity mActivity;
    final TermuxTerminalViewClient mTermuxTerminalViewClient;
    final TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    private static final String LOG_TAG = "TermuxTerminalExtraKeys";

    public TermuxTerminalExtraKeys(TermuxActivity activity, @NonNull TerminalView terminalView,
                                   TermuxTerminalViewClient termuxTerminalViewClient,
                                   TermuxTerminalSessionActivityClient termuxTerminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mTermuxTerminalViewClient = termuxTerminalViewClient;
        mTermuxTerminalSessionActivityClient = termuxTerminalSessionActivityClient;

        setExtraKeys();
    }


    public void reloadExtraKeys() {
        setExtraKeys();
    }

    private void checkAndRunMigration() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) return;
        if (preferences.getExtraKeysMigrationVersion() < 1) {
            try {
                // 1. Check custom layout in SharedPreferences
                String saved = preferences.getExtraKeysLayout();
                if (saved != null && !saved.trim().isEmpty()) {
                    if (isOldProDefault(saved)) {
                        preferences.setExtraKeysLayout(null);
                    } else if (saved.contains("PALETTE") || saved.contains("LỆNH") || saved.contains("Lệnh")) {
                        preferences.setExtraKeysLayout(removePaletteFromLayoutJson(saved));
                    }
                }

                // 2. Check termux.properties
                java.io.File propFile = new java.io.File(com.termux.shared.termux.TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE_PATH);
                if (propFile.exists() && propFile.canRead()) {
                    String content = new String(java.nio.file.Files.readAllBytes(propFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    if (content.contains("PALETTE") || content.contains("LỆNH") || content.contains("Lệnh")) {
                        String[] lines = content.split("\n");
                        StringBuilder sb = new StringBuilder();
                        for (String line : lines) {
                            if (line.trim().startsWith("extra-keys") && (line.contains("PALETTE") || line.contains("LỆNH") || line.contains("Lệnh"))) {
                                if (isOldProDefault(line)) {
                                    sb.append("extra-keys = ").append(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS).append("\n");
                                } else {
                                    int eq = line.indexOf('=');
                                    String val = line.substring(eq + 1).trim();
                                    sb.append("extra-keys = ").append(removePaletteFromLayoutJson(val)).append("\n");
                                }
                            } else {
                                sb.append(line).append("\n");
                            }
                        }
                        java.nio.file.Files.write(propFile.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        if (mActivity.getProperties() != null) {
                            mActivity.getProperties().loadTermuxPropertiesFromDisk();
                        }
                    }
                }
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "checkAndRunMigration failed: ", e);
            } finally {
                preferences.setExtraKeysMigrationVersion(1);
            }
        }
    }

    public static boolean isOldProDefault(String layout) {
        if (layout == null) return false;
        return layout.contains("PALETTE") && (layout.contains("THEME") || layout.contains("Giao diện") || layout.contains("popup"));
    }

    public static String removePaletteFromLayoutJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return jsonStr;
        try {
            org.json.JSONArray rows = new org.json.JSONArray(jsonStr);
            org.json.JSONArray newRows = new org.json.JSONArray();
            for (int r = 0; r < rows.length(); r++) {
                org.json.JSONArray row = rows.getJSONArray(r);
                org.json.JSONArray newRow = new org.json.JSONArray();
                for (int c = 0; c < row.length(); c++) {
                    Object item = row.get(c);
                    if (item instanceof String) {
                        String s = (String) item;
                        if ("PALETTE".equalsIgnoreCase(s) || "LỆNH".equalsIgnoreCase(s)) {
                            continue;
                        }
                        newRow.put(s);
                    } else if (item instanceof org.json.JSONObject) {
                        org.json.JSONObject obj = (org.json.JSONObject) item;
                        String key = obj.optString("key", "");
                        String display = obj.optString("display", "");
                        if ("PALETTE".equalsIgnoreCase(key) || "LỆNH".equalsIgnoreCase(display)) {
                            continue;
                        }
                        newRow.put(obj);
                    } else {
                        newRow.put(item);
                    }
                }
                newRows.put(newRow);
            }
            return newRows.toString();
        } catch (Exception e) {
            return jsonStr.replace("\"PALETTE\"", "").replace("'PALETTE'", "");
        }
    }

    /**
     * Set the terminal extra keys and style.
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        try {
            checkAndRunMigration();

            TermuxAppSharedPreferences preferences = mActivity.getPreferences();
            String extrakeys = preferences != null ? preferences.getExtraKeysLayout() : null;
            if (extrakeys == null || extrakeys.trim().isEmpty()) {
                extrakeys = (String) mActivity.getProperties().getInternalPropertyValue(TermuxPropertyConstants.KEY_EXTRA_KEYS, true);
            }
            String extraKeysStyle = (String) mActivity.getProperties().getInternalPropertyValue(TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE, true);

            ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle);
            if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY.equals(extraKeyDisplayMap) && !TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE.equals(extraKeysStyle)) {
                Logger.logError(TermuxSharedProperties.LOG_TAG, "The style \"" + extraKeysStyle + "\" for the key \"" + TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE + "\" is invalid. Using default style instead.");
                extraKeysStyle = TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
            }

            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            Logger.showToast(mActivity, "Could not load and set the \"" + TermuxPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: " + e.toString(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not load and set the \"" + TermuxPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS, TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (JSONException e2) {
                Logger.showToast(mActivity, "Can't create default extra keys",true);
                Logger.logStackTraceWithMessage(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mTermuxTerminalViewClient != null)
                mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mTermuxTerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onPasteTextFromClipboard(null);
        } else if ("CLIP_IMAGE".equalsIgnoreCase(key) || "DÁN ẢNH".equalsIgnoreCase(key) || "PASTE_IMAGE".equalsIgnoreCase(key)) {
            com.termux.app.image.TermuxProImageHelper.pasteImageFromClipboard(mActivity);
        } else if ("RECENT_IMAGE".equalsIgnoreCase(key) || "ẢNH MỚI".equalsIgnoreCase(key)) {
            com.termux.app.image.TermuxProImageHelper.fetchLatestImage(mActivity);
        } else if ("PALETTE".equalsIgnoreCase(key) || "LỆNH".equalsIgnoreCase(key)) {
            com.termux.app.features.TermuxProPaletteManager.showCommandPalette(mActivity);
        } else if ("THEME".equalsIgnoreCase(key) || "GIAO DIỆN".equalsIgnoreCase(key)) {
            com.termux.app.theme.TermuxProThemeManager.showThemeDialog(mActivity);
        } else if ("TELEX".equalsIgnoreCase(key)) {
            TerminalView tv = mActivity.getTerminalView();
            if (tv != null && tv.getTelexEngine() != null) {
                boolean nextState = !tv.getTelexEngine().isEnabled();
                tv.getTelexEngine().setEnabled(nextState);
                android.widget.Toast.makeText(mActivity, "Gõ tiếng Việt (Telex): " + (nextState ? "BẬT" : "TẮT"), android.widget.Toast.LENGTH_SHORT).show();
            }
        } else if ("SCROLL".equals(key)) {
            TerminalView terminalView = mTermuxTerminalViewClient.getActivity().getTerminalView();
            if (terminalView != null && terminalView.mEmulator != null)
                terminalView.mEmulator.toggleAutoScrollDisabled();
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

    @Override
    public boolean onExtraKeyButtonLongClick(View view, com.termux.shared.termux.extrakeys.ExtraKeyButton buttonInfo, com.google.android.material.button.MaterialButton button) {
        if (buttonInfo != null && buttonInfo.getKey() != null) {
            String key = buttonInfo.getKey();
            if ("CLIP_IMAGE".equalsIgnoreCase(key) || "DÁN ẢNH".equalsIgnoreCase(key) || "PASTE_IMAGE".equalsIgnoreCase(key)) {
                com.termux.app.image.TermuxProImageHelper.openSystemPhotoPicker(mActivity);
                return true;
            }
        }
        return false;
    }

}
