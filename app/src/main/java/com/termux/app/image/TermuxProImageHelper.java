package com.termux.app.image;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class TermuxProImageHelper {

    private static final String LOG_TAG = "TermuxProImageHelper";
    public static final int REQUEST_CODE_READ_IMAGES = 2001;
    public static final int REQUEST_CODE_PICK_IMAGE = 2002;

    public static final int MAX_LONG_SIDE = 1568;
    public static final int MAX_SAVED_CLIP_IMAGES = 30;

    private static final String PREF_DOWNSCALE_ENABLED = "image_downscale_enabled";

    public static boolean isDownscaleEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("termuxpro_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_DOWNSCALE_ENABLED, true);
    }

    public static void setDownscaleEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences("termuxpro_preferences", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_DOWNSCALE_ENABLED, enabled).apply();
    }

    public static File getClipDirectory() {
        File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        File clipDir = new File(home, ".clip");
        if (!clipDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            clipDir.mkdirs();
        }
        return clipDir;
    }

    public static void cleanOldClipImages() {
        try {
            File clipDir = getClipDirectory();
            File[] files = clipDir.listFiles();
            if (files != null && files.length > MAX_SAVED_CLIP_IMAGES) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (int i = MAX_SAVED_CLIP_IMAGES; i < files.length; i++) {
                    //noinspection ResultOfMethodCallIgnored
                    files[i].delete();
                }
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error cleaning old clip images: " + e.getMessage());
        }
    }

    public static void insertPathToTerminal(TermuxActivity activity, String absolutePath) {
        if (activity == null || absolutePath == null) return;
        TerminalSession session = activity.getCurrentSession();
        if (session != null && session.isRunning()) {
            // Insert path directly without trailing newline
            session.write(absolutePath + " ");
        }
    }

    public static boolean handleCommittedImageUri(TermuxActivity activity, Uri uri) {
        if (activity == null || uri == null) return false;
        try {
            String savedPath = saveImageFromUri(activity, uri);
            if (savedPath != null) {
                insertPathToTerminal(activity, savedPath);
                activity.runOnUiThread(() -> Toast.makeText(activity, "Đã dán ảnh", Toast.LENGTH_SHORT).show());
                return true;
            } else {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Không thể đọc hoặc xử lý ảnh đã chọn", Toast.LENGTH_SHORT).show());
                return false;
            }
        } catch (SecurityException se) {
            Logger.logError(LOG_TAG, "SecurityException reading committed image: " + se.getMessage());
            activity.runOnUiThread(() -> Toast.makeText(activity, "Lỗi quyền truy cập tệp ảnh", Toast.LENGTH_SHORT).show());
            return false;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error handling committed image: " + e.getMessage());
            activity.runOnUiThread(() -> Toast.makeText(activity, "Lỗi dán ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return false;
        }
    }

    public static void pasteImageFromClipboard(TermuxActivity activity) {
        try {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) {
                Toast.makeText(activity, "Bảng nhớ tạm không có ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                Toast.makeText(activity, "Bảng nhớ tạm không có ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < clip.getItemCount(); i++) {
                ClipData.Item item = clip.getItemAt(i);
                Uri uri = item.getUri();

                if (uri != null) {
                    String savedPath = saveImageFromUri(activity, uri);
                    if (savedPath != null) {
                        insertPathToTerminal(activity, savedPath);
                        Toast.makeText(activity, "Đã dán ảnh", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Check if clip contains text that is an image URI or path
                CharSequence text = item.getText();
                if (text != null) {
                    String str = text.toString().trim();
                    if (str.startsWith("content://") || str.startsWith("file://")) {
                        try {
                            Uri parsedUri = Uri.parse(str);
                            String savedPath = saveImageFromUri(activity, parsedUri);
                            if (savedPath != null) {
                                insertPathToTerminal(activity, savedPath);
                                Toast.makeText(activity, "Đã dán ảnh", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            Toast.makeText(activity, "Bảng nhớ tạm không có ảnh", Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {
            Logger.logError(LOG_TAG, "SecurityException reading clipboard: " + se.getMessage());
            Toast.makeText(activity, "Lỗi bảo mật khi đọc bảng nhớ tạm", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error pasting image from clipboard: " + e.getMessage());
            Toast.makeText(activity, "Bảng nhớ tạm không có ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    public static void fetchLatestImage(TermuxActivity activity) {
        String permission;
        if (Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            com.termux.app.ui.GlassSheet.showConfirmationSheet(
                activity,
                activity.getString(R.string.action_latest_image),
                activity.getString(R.string.image_permission_rationale),
                "Đồng ý",
                () -> ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_CODE_READ_IMAGES),
                "Hủy",
                null
            );
            return;
        }

        queryAndInsertLatestImage(activity);
    }

    public static void queryAndInsertLatestImage(TermuxActivity activity) {
        ContentResolver cr = activity.getContentResolver();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        };
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Uri targetUri = null;

        // First attempt: try finding latest in Screenshots
        String selection = MediaStore.Images.Media.DATA + " LIKE '%Screenshots%'";
        try (Cursor cursor = cr.query(collection, projection, selection, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                targetUri = Uri.withAppendedPath(collection, String.valueOf(id));
            }
        } catch (Exception ignored) {}

        // Fallback: any latest image
        if (targetUri == null) {
            try (Cursor cursor = cr.query(collection, projection, null, null, sortOrder)) {
                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                    targetUri = Uri.withAppendedPath(collection, String.valueOf(id));
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error querying latest image: " + e.getMessage());
            }
        }

        if (targetUri != null) {
            String savedPath = saveImageFromUri(activity, targetUri);
            if (savedPath != null) {
                insertPathToTerminal(activity, savedPath);
                Toast.makeText(activity, R.string.image_saved_inserted, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(activity, "Không tìm thấy ảnh nào trên thiết bị", Toast.LENGTH_SHORT).show();
    }

    public static void openSystemPhotoPicker(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            activity.startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), REQUEST_CODE_PICK_IMAGE);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error launching photo picker: " + e.getMessage());
        }
    }

    public static String saveImageFromUri(Context context, Uri uri) {
        try {
            ContentResolver cr = context.getContentResolver();
            String mimeType = cr.getType(uri);
            boolean isJpeg = mimeType != null && (mimeType.contains("jpeg") || mimeType.contains("jpg"));

            File clipDir = getClipDirectory();
            cleanOldClipImages();

            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            String ext = isJpeg ? ".jpg" : ".png";
            File outFile = new File(clipDir, "img-" + timeStamp + ext);

            boolean downscale = isDownscaleEnabled(context);

            if (downscale) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                try (InputStream is = cr.openInputStream(uri)) {
                    BitmapFactory.decodeStream(is, null, opts);
                }

                int maxSide = Math.max(opts.outWidth, opts.outHeight);
                int sampleSize = 1;
                while (maxSide / (sampleSize * 2) >= MAX_LONG_SIDE) {
                    sampleSize *= 2;
                }

                opts.inJustDecodeBounds = false;
                opts.inSampleSize = sampleSize;
                Bitmap bmp;
                try (InputStream is = cr.openInputStream(uri)) {
                    bmp = BitmapFactory.decodeStream(is, null, opts);
                }

                if (bmp != null) {
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    int currentMax = Math.max(w, h);
                    if (currentMax > MAX_LONG_SIDE) {
                        float scale = (float) MAX_LONG_SIDE / currentMax;
                        int newW = Math.round(w * scale);
                        int newH = Math.round(h * scale);
                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true);
                        if (scaled != bmp) bmp.recycle();
                        bmp = scaled;
                    }

                    try (OutputStream os = new FileOutputStream(outFile)) {
                        if (isJpeg) {
                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, os);
                        } else {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                        }
                    }
                    bmp.recycle();
                    return outFile.getAbsolutePath();
                }
            }

            // Direct stream copy fallback
            try (InputStream is = cr.openInputStream(uri);
                 OutputStream os = new FileOutputStream(outFile)) {
                if (is != null) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }
                    return outFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error saving image from uri: " + e.getMessage());
        }
        return null;
    }
}
