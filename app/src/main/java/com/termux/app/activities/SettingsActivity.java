package com.termux.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.termux.R;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.file.FileUtils;
import com.termux.shared.models.ReportInfo;
import com.termux.app.models.UserAction;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.android.PackageUtils;
import com.termux.shared.termux.settings.preferences.TermuxAPIAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxFloatAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.android.AndroidUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureTermuxAPIPreference(context);
                    configureTermuxFloatPreference(context);
                    configureTermuxTaskerPreference(context);
                    configureTermuxWidgetPreference(context);
                    configureThemePreference(context);
                    configureBackupPreference(context);
                    configureImageAgentPreference(context);
                    configureAboutPreference(context);
                    configureDonatePreference(context);
                }
            }.start();
        }

        private void configureThemePreference(@NonNull Context context) {
            Preference themePreference = findPreference("theme_picker");
            if (themePreference != null) {
                themePreference.setOnPreferenceClickListener(preference -> {
                    com.termux.app.theme.TermuxProThemeManager.init(context);
                    java.util.List<com.termux.app.theme.TermuxProThemeManager.ThemeItem> themes = com.termux.app.theme.TermuxProThemeManager.getAvailableThemes(context);
                    String currentId = com.termux.app.theme.TermuxProThemeManager.getCurrentThemeId(context);
                    java.util.List<String> names = new java.util.ArrayList<>();
                    int selected = 0;
                    for (int i = 0; i < themes.size(); i++) {
                        com.termux.app.theme.TermuxProThemeManager.ThemeItem t = themes.get(i);
                        names.add(t.name + (t.isDark ? " (Tối)" : " (Sáng)"));
                        if (t.id.equals(currentId)) selected = i;
                    }
                    com.termux.app.ui.GlassSheet.showListSheet(getActivity(), "Chọn chủ đề giao diện", names, selected, which -> {
                        com.termux.app.theme.TermuxProThemeManager.setCurrentThemeId(context, themes.get(which).id);
                        android.widget.Toast.makeText(context, "Đã chọn chủ đề: " + themes.get(which).name, android.widget.Toast.LENGTH_SHORT).show();
                    });
                    return true;
                });
            }
        }

        private void configureBackupPreference(@NonNull Context context) {
            Preference backupPreference = findPreference("backup_restore");
            if (backupPreference != null) {
                backupPreference.setOnPreferenceClickListener(preference -> {
                    java.util.List<String> options = java.util.Arrays.asList("Tạo bản sao lưu thư mục cá nhân (~)", "Khôi phục dữ liệu từ bản sao lưu");
                    com.termux.app.ui.GlassSheet.showListSheet(getActivity(), "Sao lưu & phục hồi", options, -1, which -> {
                        if (which == 0) {
                            android.widget.Toast.makeText(context, "Vui lòng thực hiện tính năng sao lưu từ trong màn hình chính Termux Pro", android.widget.Toast.LENGTH_LONG).show();
                        } else {
                            android.widget.Toast.makeText(context, "Vui lòng mở phiên chính để khôi phục dữ liệu an toàn", android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
                    return true;
                });
            }
        }

        private void configureImageAgentPreference(@NonNull Context context) {
            Preference imagePref = findPreference("image_agent_settings");
            if (imagePref != null) {
                imagePref.setOnPreferenceClickListener(preference -> {
                    boolean current = com.termux.app.image.TermuxProImageHelper.isDownscaleEnabled(context);
                    com.termux.app.ui.GlassSheet.showConfirmationSheet(
                        getActivity(),
                        getString(R.string.image_downscale_title),
                        "Tự động nén ảnh (giới hạn cạnh dài 1568px, tối ưu cho Agent CLI và AI LLM).\n\n" +
                        "Chính sách bảo mật: Toàn bộ ảnh được lưu trữ cục bộ trong thư mục ~/.clip trên điện thoại của bạn, tuyệt đối không gửi lên bất kỳ máy chủ nào.",
                        current ? "Đang bật (Nhấn để tắt)" : "Đang tắt (Nhấn để bật)",
                        () -> {
                            com.termux.app.image.TermuxProImageHelper.setDownscaleEnabled(context, !current);
                            android.widget.Toast.makeText(context, !current ? "Đã bật tự động nén ảnh" : "Đã tắt tự động nén ảnh", android.widget.Toast.LENGTH_SHORT).show();
                        },
                        "Đóng",
                        null
                    );
                    return true;
                });
            }
        }

        private void configureTermuxAPIPreference(@NonNull Context context) {
            Preference termuxAPIPreference = findPreference("termux_api");
            if (termuxAPIPreference != null) {
                TermuxAPIAppSharedPreferences preferences = TermuxAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureTermuxFloatPreference(@NonNull Context context) {
            Preference termuxFloatPreference = findPreference("termux_float");
            if (termuxFloatPreference != null) {
                TermuxFloatAppSharedPreferences preferences = TermuxFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureTermuxTaskerPreference(@NonNull Context context) {
            Preference termuxTaskerPreference = findPreference("termux_tasker");
            if (termuxTaskerPreference != null) {
                TermuxTaskerAppSharedPreferences preferences = TermuxTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureTermuxWidgetPreference(@NonNull Context context) {
            Preference termuxWidgetPreference = findPreference("termux_widget");
            if (termuxWidgetPreference != null) {
                TermuxWidgetAppSharedPreferences preferences = TermuxWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    String aboutMessage = "Termux Pro (com.termuxpro.app)\n" +
                        "Phiên bản: 0.119.0 (Kiến trúc aarch64, API 28)\n\n" +
                        "• Giấy phép mã nguồn mở: GNU General Public License v3.0 (GPLv3)\n\n" +
                        "• Tri ân Upstream: Dự án Termux Pro được phát triển và tối ưu hóa dựa trên nền tảng mã nguồn mở Termux (termux/termux-app và termux/termux-packages) do Fredrik Fornwall và cộng đồng phát triển Termux sáng lập.\n\n" +
                        "• Tuyên bố độc lập: Termux Pro là một phiên bản phân phối mở rộng độc lập, không phải là ứng dụng chính thức hay được tài trợ bởi nhóm phát triển Termux gốc.\n\n" +
                        "• Mã nguồn công khai: https://github.com/xnghii268-rgb/termuxpro";

                    com.termux.app.ui.GlassSheet.showConfirmationSheet(
                        getActivity(),
                        "Giới thiệu & Bản quyền",
                        aboutMessage,
                        "Báo cáo chẩn đoán",
                        () -> {
                            new Thread() {
                                @Override
                                public void run() {
                                    String title = "Báo cáo chẩn đoán hệ thống";

                                    StringBuilder aboutString = new StringBuilder();
                                    aboutString.append(TermuxUtils.getAppInfoMarkdownString(context, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES));
                                    aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                                    aboutString.append("\n\n").append(TermuxUtils.getImportantLinksMarkdownString(context));

                                    String userActionName = UserAction.ABOUT.getName();

                                    ReportInfo reportInfo = new ReportInfo(userActionName,
                                        TermuxConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, title);
                                    reportInfo.setReportString(aboutString.toString());
                                    reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                        Environment.getExternalStorageDirectory() + "/" +
                                            FileUtils.sanitizeFileName(TermuxConstants.TERMUX_APP_NAME + "-" + userActionName + ".log", true, true));

                                    ReportActivity.startReportActivity(context, reportInfo);
                                }
                            }.start();
                        },
                        "Đóng",
                        null
                    );

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since Termux isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = TermuxUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openUrl(context, TermuxConstants.TERMUX_DONATE_URL);
                    return true;
                });
            }
        }
    }

}
