package com.termux.app.features;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Environment;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TermuxProBackupManager {

    private static final String LOG_TAG = "TermuxProBackup";

    // Strict exclusion regex patterns: never back up secret keys, tokens, keystores, or .env
    private static final Pattern[] EXCLUDE_PATTERNS = {
        Pattern.compile("^\\.secrets(/.*)?$"),
        Pattern.compile(".*\\.env.*"),
        Pattern.compile(".*\\.keystore$"),
        Pattern.compile(".*\\.jks$"),
        Pattern.compile(".*id_rsa.*"),
        Pattern.compile(".*id_ed25519.*"),
        Pattern.compile(".*id_ecdsa.*"),
        Pattern.compile(".*token.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.pem$"),
        Pattern.compile(".*\\.key$")
    };

    public static File getBackupDirectory() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupDir = new File(new File(downloads, "termuxpro"), "backup");
        if (!backupDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            backupDir.mkdirs();
        }
        return backupDir;
    }

    public static boolean isExcluded(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        for (Pattern p : EXCLUDE_PATTERNS) {
            if (p.matcher(normalized).matches()) {
                return true;
            }
            // Check filename part
            String[] parts = normalized.split("/");
            for (String part : parts) {
                if (p.matcher(part).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void createBackup(final TermuxActivity activity) {
        final File homeDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        final File backupDir = getBackupDirectory();

        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        final File targetArchive = new File(backupDir, "termuxpro-backup-" + timeStamp + ".tar.gz");

        final ProgressDialog progress = ProgressDialog.show(activity, "Đang sao lưu",
            "Đang tạo bản sao lưu thư mục cá nhân (đã loại trừ các tệp nhạy cảm)...", true, false);

        new Thread(() -> {
            boolean success = false;
            String errorMsg = null;
            try {
                // Execute tar in shell or tar command if available, or write tar stream
                // Usable tar command with exclusions:
                String tarCmd = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/tar";
                if (new File(tarCmd).exists()) {
                    ProcessBuilder pb = new ProcessBuilder(
                        tarCmd,
                        "-czf", targetArchive.getAbsolutePath(),
                        "--exclude=.secrets",
                        "--exclude=*.env*",
                        "--exclude=*.keystore",
                        "--exclude=*.jks",
                        "--exclude=id_rsa*",
                        "--exclude=id_ed25519*",
                        "--exclude=*token*",
                        "--exclude=*.pem",
                        "--exclude=*.key",
                        "-C", homeDir.getAbsolutePath(),
                        "."
                    );
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    success = (exitCode == 0);
                } else {
                    // Fallback to java zip
                    success = backupDirectoryManually(homeDir, targetArchive);
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                Logger.logError(LOG_TAG, "Backup failed: " + errorMsg);
            }

            final boolean finalSuccess = success;
            final String finalMsg = errorMsg;
            activity.runOnUiThread(() -> {
                progress.dismiss();
                if (finalSuccess) {
                    new AlertDialog.Builder(activity)
                        .setTitle("Sao lưu thành công")
                        .setMessage(String.format(activity.getString(R.string.backup_created_success), targetArchive.getAbsolutePath()) +
                            "\n\n" + activity.getString(R.string.backup_security_note))
                        .setPositiveButton("Đồng ý", null)
                        .show();
                } else {
                    Toast.makeText(activity, "Sao lưu thất bại: " + (finalMsg != null ? finalMsg : "Lỗi không xác định"), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    public static void showRestoreDialog(final TermuxActivity activity) {
        File backupDir = getBackupDirectory();
        File[] archives = backupDir.listFiles((dir, name) -> name.endsWith(".tar.gz") || name.endsWith(".tgz"));
        if (archives == null || archives.length == 0) {
            Toast.makeText(activity, "Không tìm thấy tệp sao lưu nào trong ~/storage/downloads/termuxpro/backup/", Toast.LENGTH_LONG).show();
            return;
        }

        Arrays.sort(archives, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        String[] items = new String[archives.length];
        for (int i = 0; i < archives.length; i++) {
            items[i] = archives[i].getName() + " (" + (archives[i].length() / 1024) + " KB)";
        }

        new AlertDialog.Builder(activity)
            .setTitle("Chọn bản sao lưu để khôi phục")
            .setItems(items, (dialog, which) -> {
                File selected = archives[which];
                confirmAndRestore(activity, selected);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private static void confirmAndRestore(final TermuxActivity activity, final File archive) {
        new AlertDialog.Builder(activity)
            .setTitle(R.string.backup_confirm_restore_title)
            .setMessage(R.string.backup_confirm_restore_msg + "\n\nTệp: " + archive.getName())
            .setPositiveButton("Khôi phục", (dialog, which) -> {
                performRestore(activity, archive);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private static void performRestore(final TermuxActivity activity, final File archive) {
        final File homeDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        final ProgressDialog progress = ProgressDialog.show(activity, "Đang khôi phục", "Đang giải nén dữ liệu vào thư mục cá nhân (~)...", true, false);

        new Thread(() -> {
            boolean success = false;
            String errorMsg = null;
            try {
                String tarCmd = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/tar";
                if (new File(tarCmd).exists()) {
                    ProcessBuilder pb = new ProcessBuilder(
                        tarCmd,
                        "-xzf", archive.getAbsolutePath(),
                        "-C", homeDir.getAbsolutePath()
                    );
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    success = (exitCode == 0);
                } else {
                    errorMsg = "Không tìm thấy lệnh tar để giải nén";
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalMsg = errorMsg;
            activity.runOnUiThread(() -> {
                progress.dismiss();
                if (finalSuccess) {
                    Toast.makeText(activity, R.string.backup_restored_success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "Khôi phục thất bại: " + (finalMsg != null ? finalMsg : "Lỗi không xác định"), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private static boolean backupDirectoryManually(File homeDir, File targetArchive) {
        // Minimal tar implementation or placeholder
        return false;
    }
}
