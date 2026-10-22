package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.system.Os;
import android.util.Pair;
import android.view.WindowManager;

import com.termux.R;
import com.termux.shared.file.FileUtils;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.interact.MessageDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.markdown.MarkdownUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.android.PackageUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR_PATH;

/**
 * Install the Termux bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX directory below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging directory, $STAGING_PREFIX, is cleared if left over from broken installation below.
 * <p/>
 * (4) The zip file is loaded from a shared library.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 */
final class TermuxInstaller {

    private static final String LOG_TAG = "TermuxInstaller";

    /** Performs bootstrap setup if necessary. */
    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        String bootstrapErrorMessage;
        Error filesDirectoryAccessibleError;

        // This will also call Context.getFilesDir(), which should ensure that termux files directory
        // is created if it does not already exist
        filesDirectoryAccessibleError = TermuxFileUtils.isTermuxFilesDirectoryAccessible(activity, true, true);
        boolean isFilesDirectoryAccessible = filesDirectoryAccessibleError == null;

        // Termux can only be run as the primary user (device owner) since only that
        // account has the expected file system paths. Verify that:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !PackageUtils.isCurrentUserThePrimaryUser(activity)) {
            bootstrapErrorMessage = activity.getString(R.string.bootstrap_error_not_primary_user_message,
                MarkdownUtils.getMarkdownCodeForString(TERMUX_PREFIX_DIR_PATH, false));
            Logger.logError(LOG_TAG, "isFilesDirectoryAccessible: " + isFilesDirectoryAccessible);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.exitAppWithErrorMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage);
            return;
        }

        if (!isFilesDirectoryAccessible) {
            bootstrapErrorMessage = Error.getMinimalErrorString(filesDirectoryAccessibleError);
            //noinspection SdCardPath
            if (PackageUtils.isAppInstalledOnExternalStorage(activity) &&
                !TermuxConstants.TERMUX_FILES_DIR_PATH.equals(activity.getFilesDir().getAbsolutePath().replaceAll("^/data/user/0/", "/data/data/"))) {
                bootstrapErrorMessage += "\n\n" + activity.getString(R.string.bootstrap_error_installed_on_portable_sd,
                    MarkdownUtils.getMarkdownCodeForString(TERMUX_PREFIX_DIR_PATH, false));
            }

            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.showMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage, null);
            return;
        }

        // If prefix directory exists and has the v6 marker, and key binaries exist, assume that it is correct and be done
        File v6Marker = new File(TERMUX_PREFIX_DIR_PATH, ".termuxpro-v6");
        File loginBin = new File(TERMUX_PREFIX_DIR_PATH, "bin/login");
        File bashBin = new File(TERMUX_PREFIX_DIR_PATH, "bin/bash");
        File dashBin = new File(TERMUX_PREFIX_DIR_PATH, "bin/dash");
        File shBin = new File(TERMUX_PREFIX_DIR_PATH, "bin/sh");
        File libDir = new File(TERMUX_PREFIX_DIR_PATH, "lib");
        File etcDir = new File(TERMUX_PREFIX_DIR_PATH, "etc");

        boolean prefixHealthy = FileUtils.directoryFileExists(TERMUX_PREFIX_DIR_PATH, true) &&
            v6Marker.exists() &&
            loginBin.exists() && loginBin.canExecute() &&
            bashBin.exists() && bashBin.canExecute() &&
            (dashBin.exists() || shBin.exists()) &&
            libDir.isDirectory() &&
            etcDir.isDirectory();

        if (prefixHealthy && !TermuxFileUtils.isTermuxPrefixDirectoryEmpty()) {
            setupDefaultShellConfigFiles(activity);
            whenDone.run();
            return;
        }

        Logger.logInfo(LOG_TAG, "Prefix not healthy (v6Marker=" + v6Marker.exists() +
            ", login=" + loginBin.exists() + ", bash=" + bashBin.exists() + ", dash=" + dashBin.exists() + ", sh=" + shBin.exists() +
            "), (re-)installing bootstrap packages.");

        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    Logger.logInfo(LOG_TAG, "Installing " + TermuxConstants.TERMUX_APP_NAME + " bootstrap packages.");

                    Error error;

                    // Delete prefix staging directory or any file at its destination
                    error = FileUtils.deleteFile("termux prefix staging directory", TERMUX_STAGING_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Delete prefix directory or any file at its destination
                    error = FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix staging directory if it does not already exist and set required permissions
                    error = TermuxFileUtils.isTermuxPrefixStagingDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Ensure files directory exists
                    File filesDir = new File(TermuxConstants.TERMUX_FILES_DIR_PATH);
                    if (!filesDir.exists()) filesDir.mkdirs();

                    Logger.logInfo(LOG_TAG, "Extracting bootstrap zip to prefix staging directory \"" + TERMUX_STAGING_PREFIX_DIR_PATH + "\".");

                    final byte[] buffer = new byte[8096];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(50);

                    final byte[] zipBytes = loadZipBytes();
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = symlinksReader.readLine()) != null) {
                                    String[] parts = line.split("←");
                                    if (parts.length != 2)
                                        throw new RuntimeException("Malformed symlink line: " + line);
                                    String oldPath = parts[0];
                                    if (oldPath.contains("com.termuxpro.apppro.app")) {
                                        oldPath = oldPath.replace("com.termuxpro.apppro.app", "com.termuxpro.app");
                                    }
                                    if (oldPath.contains("/data/data/com.termux/")) {
                                        oldPath = oldPath.replace("/data/data/com.termux/", "/data/data/com.termuxpro.app/");
                                    }
                                    String newPath = TERMUX_STAGING_PREFIX_DIR_PATH + "/" + parts[1];
                                    symlinks.add(Pair.create(oldPath, newPath));

                                    error = ensureDirectoryExists(new File(newPath).getParentFile());
                                    if (error != null) {
                                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                        return;
                                    }
                                }
                            } else {
                                String zipEntryName = zipEntry.getName();
                                File targetFile = new File(TERMUX_STAGING_PREFIX_DIR_PATH, zipEntryName);
                                boolean isDirectory = zipEntry.isDirectory();

                                error = ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());
                                if (error != null) {
                                    showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                    return;
                                }

                                if (!isDirectory) {
                                    try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                        int readBytes;
                                        while ((readBytes = zipInput.read(buffer)) != -1)
                                            outStream.write(buffer, 0, readBytes);
                                    }
                                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") ||
                                        zipEntryName.startsWith("lib/apt/apt-helper") || zipEntryName.startsWith("lib/apt/methods")) {
                                        //noinspection OctalInteger
                                        try { Os.chmod(targetFile.getAbsolutePath(), 0700); } catch (Exception ignored) {}
                                    } else if (zipEntryName.endsWith(".sh") || zipEntryName.equals("etc/motd.sh")) {
                                        //noinspection OctalInteger
                                        try { Os.chmod(targetFile.getAbsolutePath(), 0755); } catch (Exception ignored) {}
                                    }
                                }
                            }
                        }
                    }

                    if (symlinks.isEmpty())
                        throw new RuntimeException("No SYMLINKS.txt encountered");
                    for (Pair<String, String> symlink : symlinks) {
                        try {
                            File dest = new File(symlink.second);
                            if (dest.exists()) dest.delete();
                            Os.symlink(symlink.first, symlink.second);
                        } catch (Exception e) {
                            Logger.logError(LOG_TAG, "Failed creating symlink " + symlink.second + " -> " + symlink.first + ": " + e.getMessage());
                        }
                    }

                    // Ensure bin/sh exists (links to dash or bash)
                    File stagingSh = new File(TERMUX_STAGING_PREFIX_DIR_PATH, "bin/sh");
                    if (!stagingSh.exists()) {
                        File stagingDash = new File(TERMUX_STAGING_PREFIX_DIR_PATH, "bin/dash");
                        File stagingBash = new File(TERMUX_STAGING_PREFIX_DIR_PATH, "bin/bash");
                        try {
                            if (stagingDash.exists()) {
                                Os.symlink("dash", stagingSh.getAbsolutePath());
                            } else if (stagingBash.exists()) {
                                Os.symlink("bash", stagingSh.getAbsolutePath());
                            }
                        } catch (Exception ignored) {}
                    }

                    // Chmod 700 and sanitize shebangs for all files in staging/bin
                    File stagingBin = new File(TERMUX_STAGING_PREFIX_DIR_PATH, "bin");
                    File[] binFiles = stagingBin.listFiles();
                    if (binFiles != null) {
                        for (File f : binFiles) {
                            try {
                                Os.chmod(f.getAbsolutePath(), 0700);
                            } catch (Exception ignored) {}
                            if (f.isFile() && f.length() > 2 && f.length() < 1048576) {
                                try {
                                    byte[] head = new byte[256];
                                    int r;
                                    try (FileInputStream fis = new FileInputStream(f)) {
                                        r = fis.read(head);
                                    }
                                    if (r > 2 && head[0] == '#' && head[1] == '!') {
                                        String content = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                                        if (content.contains("com.termuxpro.apppro.app") || content.contains("/data/data/com.termux/")) {
                                            content = content.replace("com.termuxpro.apppro.app", "com.termuxpro.app")
                                                             .replace("/data/data/com.termux/", "/data/data/com.termuxpro.app/");
                                            try (FileOutputStream fos = new FileOutputStream(f)) {
                                                fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                            }
                                            Os.chmod(f.getAbsolutePath(), 0700);
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Moving termux prefix staging to prefix directory.");

                    // Delete prefix directory before rename
                    FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                    if (!TERMUX_STAGING_PREFIX_DIR.renameTo(TERMUX_PREFIX_DIR)) {
                        throw new RuntimeException("Moving termux prefix staging to prefix directory failed");
                    }

                    try {
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v8").createNewFile();
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v7").createNewFile();
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v6").createNewFile();
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v5").createNewFile();
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v4").createNewFile();
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v3").createNewFile();
                        new File(TERMUX_PREFIX_DIR, ".termuxpro-v2").createNewFile();
                    } catch (Exception ignored) {}

                    // Write debug log to file
                    try {
                        File homeDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
                        if (!homeDir.exists()) homeDir.mkdirs();
                        File termuxHome = new File(homeDir, ".termux");
                        if (!termuxHome.exists()) termuxHome.mkdirs();
                        File logFile = new File(termuxHome, "bootstrap-install.log");
                        String logInfo = "Bootstrap installation successful at " + new java.util.Date() + "\n" +
                            "prefix: " + TERMUX_PREFIX_DIR.getAbsolutePath() + "\n" +
                            "login: " + new File(TERMUX_PREFIX_DIR, "bin/login").exists() + "\n" +
                            "bash: " + new File(TERMUX_PREFIX_DIR, "bin/bash").exists() + "\n" +
                            "sh: " + new File(TERMUX_PREFIX_DIR, "bin/sh").exists() + "\n";
                        java.nio.file.Files.write(logFile.toPath(), logInfo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception ignored) {}

                    Logger.logInfo(LOG_TAG, "Bootstrap packages installed successfully.");

                    // Recreate env file since termux prefix was wiped earlier
                    TermuxShellEnvironment.writeEnvironmentToFile(activity);

                    setupDefaultShellConfigFiles(activity);

                    activity.runOnUiThread(whenDone);

                } catch (final Exception e) {
                    showBootstrapErrorDialog(activity, whenDone, Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)));

                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    public static void setupDefaultShellConfigFiles(Context context) {
        try {
            File homeDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
            if (!homeDir.exists()) {
                homeDir.mkdirs();
            }

            File bashrcFile = new File(homeDir, ".bashrc");
            if (!bashrcFile.exists()) {
                String bashrcContent = "# ~/.bashrc for Termux Pro\n" +
                    "if [ -f \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/bash.bashrc\" ]; then\n" +
                    "    . \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/bash.bashrc\"\n" +
                    "fi\n";
                try (FileOutputStream fos = new FileOutputStream(bashrcFile)) {
                    fos.write(bashrcContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }

            File profileFile = new File(homeDir, ".profile");
            if (!profileFile.exists()) {
                String profileContent = "# ~/.profile for Termux Pro\n" +
                    "if [ -f \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/profile\" ]; then\n" +
                    "    . \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/profile\"\n" +
                    "fi\n" +
                    "if [ -f \"$HOME/.bashrc\" ]; then\n" +
                    "    . \"$HOME/.bashrc\"\n" +
                    "fi\n";
                try (FileOutputStream fos = new FileOutputStream(profileFile)) {
                    fos.write(profileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }

            // Ensure libtermuxpro-path.so is installed in $PREFIX/lib
            try {
                File nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
                File apkPathLib = new File(nativeLibDir, "libtermuxpro-path.so");
                File prefixLib = new File(TermuxConstants.TERMUX_LIB_PREFIX_DIR_PATH);
                File destPathLib = new File(prefixLib, "libtermuxpro-path.so");
                File v8Marker = new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v8");
                if (apkPathLib.exists() && (!destPathLib.exists() || destPathLib.length() != apkPathLib.length() || !v8Marker.exists())) {
                    if (!prefixLib.exists()) prefixLib.mkdirs();
                    try (java.io.InputStream in = new java.io.FileInputStream(apkPathLib);
                         FileOutputStream out = new FileOutputStream(destPathLib)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                    destPathLib.setReadable(true, false);
                    destPathLib.setExecutable(true, false);
                    Logger.logInfo(LOG_TAG, "Installed libtermuxpro-path.so to " + destPathLib.getAbsolutePath());
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error installing libtermuxpro-path.so: " + e.getMessage());
            }

            // Ensure libtermux-exec.so and all its variants are symlinks pointing directly to libtermuxpro-path.so
            try {
                File prefixLib = new File(TermuxConstants.TERMUX_LIB_PREFIX_DIR_PATH);
                File pathLib = new File(prefixLib, "libtermuxpro-path.so");
                if (pathLib.exists()) {
                    String[] execNames = new String[] {
                        "libtermux-exec.so",
                        "libtermux-exec-ld-preload.so",
                        "libtermux-exec-direct-ld-preload.so",
                        "libtermux-exec-linker-ld-preload.so"
                    };
                    for (String name : execNames) {
                        File targetFile = new File(prefixLib, name);
                        try {
                            targetFile.delete();
                            android.system.Os.symlink("libtermuxpro-path.so", targetFile.getAbsolutePath());
                        } catch (Exception e) {
                            try (java.io.InputStream in = new java.io.FileInputStream(pathLib);
                                 FileOutputStream out = new FileOutputStream(targetFile)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                            } catch (Exception ignored) {}
                        }
                        targetFile.setReadable(true, false);
                        targetFile.setExecutable(true, false);
                    }
                    Logger.logInfo(LOG_TAG, "Redirected libtermux-exec variants to libtermuxpro-path.so");
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error ensuring libtermux-exec links: " + e.getMessage());
            }

            // Ensure $PREFIX/bin/login exists, is executable, does not unset LD_PRELOAD and uses Termux Pro paths
            try {
                File loginFile = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "login");
                if (!loginFile.exists()) {
                    File binDir = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH);
                    if (!binDir.exists()) binDir.mkdirs();
                    String fallbackLogin = "#!/data/data/com.termuxpro.app/files/usr/bin/sh\n" +
                        "if [ -x /data/data/com.termuxpro.app/files/usr/bin/bash ]; then\n" +
                        "    export SHELL=/data/data/com.termuxpro.app/files/usr/bin/bash\n" +
                        "elif [ -x /data/data/com.termuxpro.app/files/usr/bin/sh ]; then\n" +
                        "    export SHELL=/data/data/com.termuxpro.app/files/usr/bin/sh\n" +
                        "else\n" +
                        "    export SHELL=/system/bin/sh\n" +
                        "fi\n" +
                        "PRELOAD=\"\"\n" +
                        "if [ -f \"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\" ]; then\n" +
                        "    PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\"\n" +
                        "fi\n" +
                        "if [ -n \"$PRELOAD\" ]; then\n" +
                        "    export LD_PRELOAD=\"$PRELOAD\"\n" +
                        "fi\n" +
                        "if [ -f /data/data/com.termuxpro.app/files/usr/etc/profile ]; then\n" +
                        "    . /data/data/com.termuxpro.app/files/usr/etc/profile\n" +
                        "fi\n" +
                        "if [ -n \"$TERM\" ]; then\n" +
                        "    exec \"$SHELL\" -l \"$@\"\n" +
                        "else\n" +
                        "    exec \"$SHELL\" \"$@\"\n" +
                        "fi\n";
                    try (FileOutputStream fos = new FileOutputStream(loginFile)) {
                        fos.write(fallbackLogin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    loginFile.setReadable(true, false);
                    loginFile.setExecutable(true, false);
                    Logger.logInfo(LOG_TAG, "Created fallback $PREFIX/bin/login.");
                } else {
                    String loginContent = new String(java.nio.file.Files.readAllBytes(loginFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    boolean modified = false;
                    if (loginContent.contains("com.termuxpro.apppro.app")) {
                        loginContent = loginContent.replace("com.termuxpro.apppro.app", "com.termuxpro.app");
                        modified = true;
                    }
                    if (loginContent.contains("/data/data/com.termux/")) {
                        loginContent = loginContent.replace("/data/data/com.termux/", "/data/data/com.termuxpro.app/");
                        modified = true;
                    }
                    if (loginContent.startsWith("#!")) {
                        int nl = loginContent.indexOf('\n');
                        if (nl != -1) {
                            String firstLine = loginContent.substring(0, nl);
                            if (firstLine.contains("apppro.app") || firstLine.contains("/com.termux/")) {
                                loginContent = "#!/data/data/com.termuxpro.app/files/usr/bin/sh" + loginContent.substring(nl);
                                modified = true;
                            }
                        }
                    }
                    if (loginContent.contains("unset LD_PRELOAD")) {
                        loginContent = loginContent.replace("unset LD_PRELOAD", "true # keep LD_PRELOAD for termux pro");
                        modified = true;
                    }
                    if (loginContent.contains("libtermux-exec.so") || loginContent.contains("libtermux-exec-ld-preload.so")) {
                        loginContent = loginContent.replace(
                            "export LD_PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so:/data/data/com.termuxpro.app/files/usr/lib/libtermux-exec.so\"",
                            "export LD_PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\""
                        );
                        loginContent = loginContent.replace(
                            "export LD_PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermux-exec.so\"",
                            "export LD_PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\""
                        );
                        loginContent = loginContent.replace(
                            "export LD_PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermux-exec-ld-preload.so\"",
                            "export LD_PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\""
                        );
                        loginContent = loginContent.replaceAll(":[^\"]*libtermux-exec[^\"]*\\.so", "");
                        loginContent = loginContent.replaceAll("[^\"]*libtermux-exec[^\"]*\\.so:?", "");
                        modified = true;
                    }
                    if (modified) {
                        try (FileOutputStream fos = new FileOutputStream(loginFile)) {
                            fos.write(loginContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                        Logger.logInfo(LOG_TAG, "Patched $PREFIX/bin/login for Termux Pro compatibility.");
                    }
                    loginFile.setReadable(true, false);
                    loginFile.setExecutable(true, false);
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error checking/patching login script: " + e.getMessage());
            }

            // Ensure $PREFIX/etc/profile is clean, valid and uses Termux Pro configuration
            try {
                File etcProfileFile = new File(TermuxConstants.TERMUX_PREFIX_DIR, "etc/profile");
                String masterProfile = "# System-wide profile for login shells in Termux Pro.\n" +
                    "# Configured for prefix: /data/data/com.termuxpro.app/files/usr\n\n" +
                    "export PREFIX=\"/data/data/com.termuxpro.app/files/usr\"\n" +
                    "export HOME=\"/data/data/com.termuxpro.app/files/home\"\n" +
                    "export PATH=\"/data/data/com.termuxpro.app/files/usr/bin\"\n" +
                    "export LD_LIBRARY_PATH=\"/data/data/com.termuxpro.app/files/usr/lib\"\n" +
                    "export TMPDIR=\"/data/data/com.termuxpro.app/files/usr/tmp\"\n" +
                    "export APT_CONFIG=\"/data/data/com.termuxpro.app/files/usr/etc/apt/apt.conf\"\n" +
                    "export DPKG_ADMINDIR=\"/data/data/com.termuxpro.app/files/usr/var/lib/dpkg\"\n" +
                    "export TERMUX__PREFIX=\"/data/data/com.termuxpro.app/files/usr\"\n" +
                    "export TERMUX_APP__DATA_DIR=\"/data/data/com.termuxpro.app\"\n" +
                    "export TERMUX_APP__LEGACY_DATA_DIR=\"/data/data/com.termuxpro.app\"\n" +
                    "export SSL_CERT_FILE=\"/data/data/com.termuxpro.app/files/usr/etc/tls/cert.pem\"\n" +
                    "export CURL_CA_BUNDLE=\"/data/data/com.termuxpro.app/files/usr/etc/tls/cert.pem\"\n" +
                    "export OPENSSL_CONF=\"/data/data/com.termuxpro.app/files/usr/etc/tls/openssl.cnf\"\n" +
                    "export GIT_SSL_CAINFO=\"/data/data/com.termuxpro.app/files/usr/etc/tls/cert.pem\"\n\n" +
                    "# Terminfo and locale configuration\n" +
                    "export TERMINFO=\"/data/data/com.termuxpro.app/files/usr/share/terminfo\"\n" +
                    "export TERMINFO_DIRS=\"/data/data/com.termuxpro.app/files/usr/share/terminfo:/data/data/com.termuxpro.app/files/home/.terminfo\"\n" +
                    "export LANG=\"C.UTF-8\"\n" +
                    "export LC_ALL=\"C.UTF-8\"\n\n" +
                    "PRELOAD=\"\"\n" +
                    "if [ -f \"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\" ]; then\n" +
                    "    PRELOAD=\"/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so\"\n" +
                    "fi\n" +
                    "if [ -n \"$PRELOAD\" ]; then\n" +
                    "    export LD_PRELOAD=\"$PRELOAD\"\n" +
                    "fi\n\n" +
                    "# Source bash.bashrc for bash login shells\n" +
                    "if [ -n \"$BASH_VERSION\" ] && [ -f \"/data/data/com.termuxpro.app/files/usr/etc/bash.bashrc\" ]; then\n" +
                    "    . \"/data/data/com.termuxpro.app/files/usr/etc/bash.bashrc\"\n" +
                    "fi\n";
                File etcDir = new File(TermuxConstants.TERMUX_PREFIX_DIR, "etc");
                if (!etcDir.exists()) etcDir.mkdirs();
                java.nio.file.Files.write(etcProfileFile.toPath(), masterProfile.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                etcProfileFile.setReadable(true, false);
                Logger.logInfo(LOG_TAG, "Ensured clean $PREFIX/etc/profile.");
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error ensuring etc/profile: " + e.getMessage());
            }

            // Ensure $PREFIX/etc/bash.bashrc is clean and free from duplicate/broken PRELOAD blocks
            try {
                File etcBashrcFile = new File(TermuxConstants.TERMUX_PREFIX_DIR, "etc/bash.bashrc");
                String masterBashrc = "# System-wide .bashrc file for interactive bash(1) shells in Termux Pro.\n" +
                    "# Configured for prefix: /data/data/com.termuxpro.app/files/usr\n\n" +
                    "export TERMINFO=\"/data/data/com.termuxpro.app/files/usr/share/terminfo\"\n" +
                    "export TERMINFO_DIRS=\"/data/data/com.termuxpro.app/files/usr/share/terminfo:/data/data/com.termuxpro.app/files/home/.terminfo\"\n" +
                    "[ -z \"$LANG\" ] && export LANG=\"C.UTF-8\"\n" +
                    "[ -z \"$LC_ALL\" ] && export LC_ALL=\"C.UTF-8\"\n\n" +
                    "if [ \"$TERM\" = \"dumb\" ] || [ -n \"$NO_COLOR\" ]; then\n" +
                    "    PS1='\\w \\$ '\n" +
                    "else\n" +
                    "    PS1='\\[\\033[01;36m\\]\\w\\[\\033[00m\\] \\$ '\n" +
                    "fi\n\n" +
                    "# Source profile.d directory scripts if present\n" +
                    "if [ -d \"/data/data/com.termuxpro.app/files/usr/etc/profile.d\" ]; then\n" +
                    "    for f in /data/data/com.termuxpro.app/files/usr/etc/profile.d/*.sh; do\n" +
                    "        [ -r \"$f\" ] && . \"$f\"\n" +
                    "    done\n" +
                    "    unset f\n" +
                    "fi\n";
                java.nio.file.Files.write(etcBashrcFile.toPath(), masterBashrc.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                etcBashrcFile.setReadable(true, false);
                Logger.logInfo(LOG_TAG, "Ensured clean $PREFIX/etc/bash.bashrc.");
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error ensuring etc/bash.bashrc: " + e.getMessage());
            }

            // Ensure $PREFIX/etc/apt/apt.conf is properly configured without problematic --instdir
            try {
                File aptConfDir = new File(TermuxConstants.TERMUX_PREFIX_DIR, "etc/apt");
                if (!aptConfDir.exists()) aptConfDir.mkdirs();
                File aptConfFile = new File(aptConfDir, "apt.conf");
                String aptConfContent = "Dir \"/data/data/com.termuxpro.app/files/usr\";\n" +
                    "Dir::State \"var/lib/apt\";\n" +
                    "Dir::State::status \"/data/data/com.termuxpro.app/files/usr/var/lib/dpkg/status\";\n" +
                    "Dir::Cache \"/data/data/com.termuxpro.app/cache/apt\";\n" +
                    "Dir::Cache::archives \"archives/\";\n" +
                    "Dir::Etc \"etc/apt\";\n" +
                    "Dir::Etc::parts \"apt.conf.d\";\n" +
                    "Dir::Etc::sourcelist \"sources.list\";\n" +
                    "Dir::Etc::sourceparts \"sources.list.d\";\n" +
                    "Dir::Bin::methods \"/data/data/com.termuxpro.app/files/usr/lib/apt/methods\";\n" +
                    "Dir::Bin::dpkg \"/data/data/com.termuxpro.app/files/usr/bin/dpkg\";\n" +
                    "Dir::Log \"var/log/apt\";\n" +
                    "DPkg::Path \"/data/data/com.termuxpro.app/files/usr/bin\";\n" +
                    "DPkg::Options {\n" +
                    "   \"--admindir=/data/data/com.termuxpro.app/files/usr/var/lib/dpkg\";\n" +
                    "};\n";
                java.nio.file.Files.write(aptConfFile.toPath(), aptConfContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                aptConfFile.setReadable(true, false);

                // Add apt.conf.d hook to automatically rewrite maintainer scripts after deb unpack
                File aptConfPartsDir = new File(aptConfDir, "apt.conf.d");
                if (!aptConfPartsDir.exists()) aptConfPartsDir.mkdirs();
                File hookFile = new File(aptConfPartsDir, "01termuxpro-shebang-fix");
                String hookContent = "DPkg::Pre-Install-Pkgs { \"sed -i 's|/data/data/com.termux/|/data/data/com.termuxpro.app/|g; s|/data/user/0/com.termux/|/data/data/com.termuxpro.app/|g' /data/data/com.termuxpro.app/files/usr/var/lib/dpkg/info/* 2>/dev/null || true\"; };\n" +
                    "DPkg::Post-Invoke { \"sed -i 's|/data/data/com.termux/|/data/data/com.termuxpro.app/|g; s|/data/user/0/com.termux/|/data/data/com.termuxpro.app/|g' /data/data/com.termuxpro.app/files/usr/var/lib/dpkg/info/* 2>/dev/null || true\"; };\n";
                java.nio.file.Files.write(hookFile.toPath(), hookContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                hookFile.setReadable(true, false);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error ensuring apt.conf: " + e.getMessage());
            }

            // Auto-patch any dpkg maintainer scripts that may contain legacy package prefix
            patchDpkgMaintainerScripts(context);

            try {
                new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v9").createNewFile();
                new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v8").createNewFile();
                new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v7").createNewFile();
                new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v6").createNewFile();
                new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v5").createNewFile();
                new File(TermuxConstants.TERMUX_PREFIX_DIR, ".termuxpro-v4").createNewFile();
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error setting up default shell config files: " + e.getMessage());
        }
    }

    public static void patchDpkgMaintainerScripts(Context context) {
        try {
            File dpkgInfoDir = new File(TermuxConstants.TERMUX_PREFIX_DIR, "var/lib/dpkg/info");
            if (dpkgInfoDir.exists() && dpkgInfoDir.isDirectory()) {
                File[] files = dpkgInfoDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (!f.isFile()) continue;
                        String name = f.getName();
                        if (name.endsWith(".postinst") || name.endsWith(".preinst") ||
                            name.endsWith(".prerm") || name.endsWith(".postrm") ||
                            name.endsWith(".config")) {
                            try {
                                byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                                String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                                boolean changed = false;
                                if (content.contains("com.termuxpro.apppro.app")) {
                                    content = content.replace("com.termuxpro.apppro.app", "com.termuxpro.app");
                                    changed = true;
                                }
                                if (content.contains("/data/data/com.termux/")) {
                                    content = content.replace("/data/data/com.termux/", "/data/data/com.termuxpro.app/");
                                    changed = true;
                                }
                                if (content.contains("/data/user/0/com.termux/")) {
                                    content = content.replace("/data/user/0/com.termux/", "/data/data/com.termuxpro.app/");
                                    changed = true;
                                }
                                if (changed) {
                                    java.nio.file.Files.write(f.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                    Logger.logInfo(LOG_TAG, "Auto-patched dpkg maintainer script: " + name);
                                }
                                f.setReadable(true, false);
                                f.setExecutable(true, false);
                            } catch (Exception e) {
                                Logger.logError(LOG_TAG, "Error patching script " + name + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error in patchDpkgMaintainerScripts: " + e.getMessage());
        }
    }

    public static void showBootstrapErrorDialog(Activity activity, Runnable whenDone, String message) {
        Logger.logErrorExtended(LOG_TAG, "Bootstrap Error:\n" + message);

        // Send a notification with the exception so that the user knows why bootstrap setup failed
        sendBootstrapCrashReportNotification(activity, message);

        activity.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_body)
                    .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                        dialog.dismiss();
                        activity.finish();
                    })
                    .setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                        dialog.dismiss();
                        FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                        TermuxInstaller.setupBootstrapIfNeeded(activity, whenDone);
                    }).show();
            } catch (WindowManager.BadTokenException e1) {
                // Activity already dismissed - ignore.
            }
        });
    }

    private static void sendBootstrapCrashReportNotification(Activity activity, String message) {
        final String title = TermuxConstants.TERMUX_APP_NAME + " Bootstrap Error";

        // Add info of all install Termux plugin apps as well since their target sdk or installation
        // on external/portable sd card can affect Termux app files directory access or exec.
        TermuxCrashUtils.sendCrashReportNotification(activity, LOG_TAG,
            title, null, "## " + title + "\n\n" + message + "\n\n" +
                TermuxUtils.getTermuxDebugMarkdownString(activity),
            true, false, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES, true);
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "termux-storage";
        final String title = TermuxConstants.TERMUX_APP_NAME + " Setup Storage Error";

        Logger.logInfo(LOG_TAG, "Setting up storage symlinks.");

        new Thread() {
            public void run() {
                try {
                    Error error;
                    File storageDir = TermuxConstants.TERMUX_STORAGE_HOME_DIR;

                    error = FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    if (error != null) {
                        Logger.logErrorAndShowToast(context, LOG_TAG, error.getMessage());
                        Logger.logErrorExtended(LOG_TAG, "Setup Storage Error\n" + error.toString());
                        TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                            "## " + title + "\n\n" + Error.getErrorMarkdownString(error),
                            true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/shared, ~/storage/downloads, ~/storage/dcim, ~/storage/pictures, ~/storage/music and ~/storage/movies for directories in \"" + Environment.getExternalStorageDirectory().getAbsolutePath() + "\".");

                    // Get primary storage root "/storage/emulated/0" symlink
                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    Os.symlink(documentsDir.getAbsolutePath(), new File(storageDir, "documents").getAbsolutePath());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    Os.symlink(downloadsDir.getAbsolutePath(), new File(storageDir, "downloads").getAbsolutePath());

                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    Os.symlink(dcimDir.getAbsolutePath(), new File(storageDir, "dcim").getAbsolutePath());

                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Os.symlink(picturesDir.getAbsolutePath(), new File(storageDir, "pictures").getAbsolutePath());

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    Os.symlink(musicDir.getAbsolutePath(), new File(storageDir, "music").getAbsolutePath());

                    File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    Os.symlink(moviesDir.getAbsolutePath(), new File(storageDir, "movies").getAbsolutePath());

                    File podcastsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
                    Os.symlink(podcastsDir.getAbsolutePath(), new File(storageDir, "podcasts").getAbsolutePath());

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        File audiobooksDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS);
                        Os.symlink(audiobooksDir.getAbsolutePath(), new File(storageDir, "audiobooks").getAbsolutePath());
                    }

                    // Dir 0 should ideally be for primary storage
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ContextImpl.java;l=818
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=219
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=181
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/StorageManagerService.java;l=3796
                    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/services/core/java/com/android/server/MountService.java;l=3053

                    // Create "Android/data/com.termux" symlinks
                    File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    // Create "Android/media/com.termux" symlinks
                    dirs = context.getExternalMediaDirs();
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "media-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Storage symlinks created successfully.");
                } catch (Exception e) {
                    Logger.logErrorAndShowToast(context, LOG_TAG, e.getMessage());
                    Logger.logStackTraceWithMessage(LOG_TAG, "Setup Storage Error: Error setting up link", e);
                    TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                        "## " + title + "\n\n" + Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)),
                        true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                }
            }
        }.start();
    }

    private static Error ensureDirectoryExists(File directory) {
        return FileUtils.createDirectoryFile(directory.getAbsolutePath());
    }

    public static byte[] loadZipBytes() {
        // Only load the shared library when necessary to save memory usage.
        System.loadLibrary("termux-bootstrap");
        return getZip();
    }

    public static native byte[] getZip();

}
