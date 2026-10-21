package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.extrakeys.TermuxProExtraKeysEditor;
import com.termux.app.ui.GlassSheet;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.Arrays;
import java.util.List;

@Keep
public class TerminalIOPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalIOPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_terminal_io_preferences, rootKey);

        configureExtraKeysPreferences(context);
    }

    private void configureExtraKeysPreferences(Context context) {
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context, false);
        if (preferences == null) return;

        Preference stylePref = findPreference("extra_keys_style_picker");
        if (stylePref != null) {
            updateStyleSummary(stylePref, preferences.getExtraKeysStyleMode());
            stylePref.setOnPreferenceClickListener(preference -> {
                List<String> options = Arrays.asList(
                    "Pro (viên kính bo tròn 10dp, khe hở 4dp)",
                    "Cổ điển (thanh đen phẳng, chuẩn Termux)"
                );
                String current = preferences.getExtraKeysStyleMode();
                int selected = "classic".equalsIgnoreCase(current) ? 1 : 0;

                GlassSheet.showListSheet(getActivity(), "Chọn kiểu phím phụ", options, selected, which -> {
                    String chosen = which == 1 ? "classic" : "pro";
                    preferences.setExtraKeysStyleMode(chosen);
                    updateStyleSummary(stylePref, chosen);

                    TermuxActivity termuxActivity = TermuxActivity.getCurrentActivity();
                    if (termuxActivity != null) {
                        termuxActivity.reloadExtraKeys();
                    }
                    Toast.makeText(context, "Đã chuyển sang kiểu: " + (which == 1 ? "Cổ điển" : "Pro"), Toast.LENGTH_SHORT).show();
                });
                return true;
            });
        }

        Preference editorPref = findPreference("extra_keys_layout_editor");
        if (editorPref != null) {
            editorPref.setOnPreferenceClickListener(preference -> {
                if (getActivity() != null) {
                    TermuxProExtraKeysEditor.show(getActivity());
                }
                return true;
            });
        }

        Preference resetPref = findPreference("extra_keys_reset_layout");
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                GlassSheet.showConfirmationSheet(
                    getActivity(),
                    "Đặt lại phím phụ",
                    "Khôi phục thanh phím phụ về 14 phím chuẩn 7 cột x 2 hàng của Termux gốc:\n\n" +
                    "Hàng 1: ESC | / | - | HOME | UP | END | PGUP\n" +
                    "Hàng 2: TAB | CTRL | ALT | LEFT | DOWN | RIGHT | PGDN",
                    "Đặt lại mặc định",
                    () -> {
                        preferences.setExtraKeysLayout(null);
                        TermuxActivity termuxActivity = TermuxActivity.getCurrentActivity();
                        if (termuxActivity != null) {
                            termuxActivity.reloadExtraKeys();
                        }
                        Toast.makeText(context, "Đã khôi phục 14 phím chuẩn của Termux", Toast.LENGTH_SHORT).show();
                    },
                    "Hủy",
                    null
                );
                return true;
            });
        }
    }

    private void updateStyleSummary(Preference pref, String styleMode) {
        if ("classic".equalsIgnoreCase(styleMode)) {
            pref.setSummary("Cổ điển (thanh đen phẳng, 0 viền, phong cách Termux gốc)");
        } else {
            pref.setSummary("Pro (viên kính bo tròn 10dp, khe hở 4dp, hòa quyện chủ đề)");
        }
    }

}

class TerminalIOPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;

    private static TerminalIOPreferencesDataStore mInstance;

    private TerminalIOPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalIOPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalIOPreferencesDataStore(context);
        }
        return mInstance;
    }



    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null) return;
        if (key == null) return;

        switch (key) {
            case "soft_keyboard_enabled":
                    mPreferences.setSoftKeyboardEnabled(value);
                break;
            case "soft_keyboard_enabled_only_if_no_hardware":
                mPreferences.setSoftKeyboardEnabledOnlyIfNoHardware(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null) return false;

        switch (key) {
            case "soft_keyboard_enabled":
                return mPreferences.isSoftKeyboardEnabled();
            case "soft_keyboard_enabled_only_if_no_hardware":
                return mPreferences.isSoftKeyboardEnabledOnlyIfNoHardware();
            default:
                return false;
        }
    }

}
