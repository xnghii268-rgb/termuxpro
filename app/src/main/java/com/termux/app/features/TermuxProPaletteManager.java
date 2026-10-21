package com.termux.app.features;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TermuxProPaletteManager {

    private static final String LOG_TAG = "TermuxProPalette";
    private static final String SNIPPETS_FILE_NAME = "termuxpro_snippets.json";

    public static class PaletteItem {
        public final String title;
        public final String command;
        public final boolean isSnippet;

        public PaletteItem(String title, String command, boolean isSnippet) {
            this.title = title;
            this.command = command;
            this.isSnippet = isSnippet;
        }

        @NonNull
        @Override
        public String toString() {
            return isSnippet ? title + "\n  " + command : "  " + command;
        }
    }

    public static List<PaletteItem> loadSnippets(Context context) {
        List<PaletteItem> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), SNIPPETS_FILE_NAME);
        if (!file.exists()) {
            // Default built-in snippets
            list.add(new PaletteItem("Cập nhật gói hệ thống", "pkg update && pkg upgrade", true));
            list.add(new PaletteItem("Cài gói lập trình cơ bản", "pkg install python nano git openssh jq -y", true));
            list.add(new PaletteItem("Lấy đường dẫn ảnh tạm", "clipimg", true));
            list.add(new PaletteItem("Lấy ảnh chụp màn hình mới", "lastshot", true));
            list.add(new PaletteItem("Kiểm tra trạng thái Git", "git status", true));
            list.add(new PaletteItem("Dung lượng bộ nhớ", "df -h $PREFIX $HOME", true));
            saveSnippets(context, list);
            return list;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new PaletteItem(obj.getString("title"), obj.getString("command"), true));
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error loading snippets: " + e.getMessage());
        }
        return list;
    }

    public static void saveSnippets(Context context, List<PaletteItem> snippets) {
        try {
            JSONArray arr = new JSONArray();
            for (PaletteItem item : snippets) {
                if (item.isSnippet) {
                    JSONObject obj = new JSONObject();
                    obj.put("title", item.title);
                    obj.put("command", item.command);
                    arr.put(obj);
                }
            }
            File file = new File(context.getFilesDir(), SNIPPETS_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString(2).getBytes());
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error saving snippets: " + e.getMessage());
        }
    }

    public static List<String> loadHistory() {
        Set<String> set = new LinkedHashSet<>();
        File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        File[] historyFiles = {
            new File(home, ".bash_history"),
            new File(home, ".zsh_history"),
            new File(home, ".history")
        };

        for (File hf : historyFiles) {
            if (hf.exists() && hf.canRead()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(hf)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            // Strip timestamp prefix in zsh if present
                            if (line.startsWith(": ") && line.contains(";")) {
                                line = line.substring(line.indexOf(';') + 1).trim();
                            }
                            set.add(line);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return new ArrayList<>(set);
    }

    public static void showCommandPalette(final TermuxActivity activity) {
        final List<PaletteItem> allItems = new ArrayList<>();
        allItems.addAll(loadSnippets(activity));

        List<String> history = loadHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            allItems.add(new PaletteItem(history.get(i), history.get(i), false));
        }

        final List<PaletteItem> filteredItems = new ArrayList<>(allItems);

        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            com.termux.app.ui.GlassSheet.createBaseSheet(activity);

        float density = activity.getResources().getDisplayMetrics().density;
        String currentThemeId = com.termux.app.theme.TermuxProThemeManager.getCurrentThemeId(activity);
        boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");
        int textColor = isDark ? android.graphics.Color.parseColor("#F3F4F6") : android.graphics.Color.parseColor("#1F2937");
        int subTextColor = isDark ? android.graphics.Color.parseColor("#9CA3AF") : android.graphics.Color.parseColor("#6B7280");
        int accentColor = android.graphics.Color.parseColor("#FF3C70");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(16 * density));

        // Drag handle
        View handle = new View(activity);
        android.graphics.drawable.GradientDrawable handleBg = new android.graphics.drawable.GradientDrawable();
        handleBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? android.graphics.Color.parseColor("#4DFFFFFF") : android.graphics.Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(14 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        layout.addView(handle);

        // Title
        TextView titleView = new TextView(activity);
        titleView.setText("Bảng lệnh tương tác");
        titleView.setTextSize(17);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(12 * density));
        layout.addView(titleView);

        // Search Input
        final EditText searchInput = new EditText(activity);
        searchInput.setHint("Tìm kiếm mẫu lệnh hoặc lịch sử...");
        searchInput.setHintTextColor(subTextColor);
        searchInput.setTextColor(textColor);
        searchInput.setTextSize(14);
        searchInput.setSingleLine(true);
        android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
        inputBg.setCornerRadius(10 * density);
        inputBg.setColor(isDark ? android.graphics.Color.parseColor("#26000000") : android.graphics.Color.parseColor("#14000000"));
        inputBg.setStroke((int)(1 * density), isDark ? android.graphics.Color.parseColor("#33FFFFFF") : android.graphics.Color.parseColor("#22000000"));
        searchInput.setBackground(inputBg);
        searchInput.setPadding((int)(12 * density), (int)(10 * density), (int)(12 * density), (int)(10 * density));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.bottomMargin = (int)(12 * density);
        layout.addView(searchInput, inputParams);

        // List
        final ListView listView = new ListView(activity);
        listView.setDivider(null);
        listView.setDividerHeight((int)(4 * density));
        listView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(320 * density)));

        final ArrayAdapter<PaletteItem> adapter = new ArrayAdapter<PaletteItem>(activity,
            android.R.layout.simple_list_item_2, android.R.id.text1, filteredItems) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                PaletteItem item = getItem(position);
                if (item != null) {
                    if (item.isSnippet) {
                        text1.setText(item.title);
                        text1.setTextColor(textColor);
                        text1.setTypeface(null, android.graphics.Typeface.BOLD);
                        text2.setText(item.command);
                        text2.setTextColor(accentColor);
                    } else {
                        text1.setText(item.command);
                        text1.setTextColor(textColor);
                        text1.setTypeface(null, android.graphics.Typeface.NORMAL);
                        text2.setText("Lịch sử lệnh");
                        text2.setTextColor(subTextColor);
                    }
                }
                return view;
            }
        };

        listView.setAdapter(adapter);
        layout.addView(listView);

        // Buttons
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);
        btnRow.setPadding(0, (int)(12 * density), 0, 0);

        android.widget.Button closeBtn = new android.widget.Button(activity, null, android.R.attr.borderlessButtonStyle);
        closeBtn.setText("Đóng");
        closeBtn.setTextColor(subTextColor);
        closeBtn.setTextSize(14);
        closeBtn.setOnClickListener(v -> sheet.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        closeParams.rightMargin = (int)(8 * density);
        btnRow.addView(closeBtn, closeParams);

        android.widget.Button addBtn = new android.widget.Button(activity);
        addBtn.setText("Thêm mẫu lệnh");
        addBtn.setTextSize(14);
        addBtn.setTextColor(android.graphics.Color.parseColor("#1A0A10"));
        android.graphics.drawable.GradientDrawable addBg = new android.graphics.drawable.GradientDrawable();
        addBg.setCornerRadius(10 * density);
        addBg.setColor(accentColor);
        addBtn.setBackground(addBg);
        addBtn.setOnClickListener(v -> {
            sheet.dismiss();
            showAddSnippetDialog(activity);
        });
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        btnRow.addView(addBtn, addParams);

        layout.addView(btnRow);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                filteredItems.clear();
                for (PaletteItem item : allItems) {
                    if (query.isEmpty() || item.title.toLowerCase().contains(query) || item.command.toLowerCase().contains(query)) {
                        filteredItems.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            PaletteItem selected = adapter.getItem(position);
            if (selected != null) {
                TerminalSession session = activity.getCurrentSession();
                if (session != null && session.isRunning()) {
                    session.write(selected.command);
                }
                sheet.dismiss();
            }
        });

        sheet.setContentView(layout);
        sheet.show();
    }

    public static void showAddSnippetDialog(final TermuxActivity activity) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            com.termux.app.ui.GlassSheet.createBaseSheet(activity);

        float density = activity.getResources().getDisplayMetrics().density;
        String currentThemeId = com.termux.app.theme.TermuxProThemeManager.getCurrentThemeId(activity);
        boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");
        int textColor = isDark ? android.graphics.Color.parseColor("#F3F4F6") : android.graphics.Color.parseColor("#1F2937");
        int subTextColor = isDark ? android.graphics.Color.parseColor("#9CA3AF") : android.graphics.Color.parseColor("#6B7280");
        int accentColor = android.graphics.Color.parseColor("#FF3C70");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(20 * density));

        // Drag handle
        View handle = new View(activity);
        android.graphics.drawable.GradientDrawable handleBg = new android.graphics.drawable.GradientDrawable();
        handleBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? android.graphics.Color.parseColor("#4DFFFFFF") : android.graphics.Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = (int)(16 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        layout.addView(handle);

        // Title
        TextView titleView = new TextView(activity);
        titleView.setText("Thêm mẫu lệnh mới");
        titleView.setTextSize(17);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(12 * density));
        layout.addView(titleView);

        // Title Input
        final EditText titleInput = new EditText(activity);
        titleInput.setHint("Tên mẫu lệnh (ví dụ: Cập nhật hệ thống)");
        titleInput.setHintTextColor(subTextColor);
        titleInput.setTextColor(textColor);
        titleInput.setTextSize(14);
        android.graphics.drawable.GradientDrawable titleBg = new android.graphics.drawable.GradientDrawable();
        titleBg.setCornerRadius(10 * density);
        titleBg.setColor(isDark ? android.graphics.Color.parseColor("#26000000") : android.graphics.Color.parseColor("#14000000"));
        titleBg.setStroke((int)(1 * density), isDark ? android.graphics.Color.parseColor("#33FFFFFF") : android.graphics.Color.parseColor("#22000000"));
        titleInput.setBackground(titleBg);
        titleInput.setPadding((int)(12 * density), (int)(10 * density), (int)(12 * density), (int)(10 * density));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = (int)(12 * density);
        layout.addView(titleInput, titleParams);

        // Cmd Input
        final EditText cmdInput = new EditText(activity);
        cmdInput.setHint("Câu lệnh thực thi (ví dụ: pkg update && pkg upgrade)");
        cmdInput.setHintTextColor(subTextColor);
        cmdInput.setTextColor(textColor);
        cmdInput.setTextSize(14);
        android.graphics.drawable.GradientDrawable cmdBg = new android.graphics.drawable.GradientDrawable();
        cmdBg.setCornerRadius(10 * density);
        cmdBg.setColor(isDark ? android.graphics.Color.parseColor("#26000000") : android.graphics.Color.parseColor("#14000000"));
        cmdBg.setStroke((int)(1 * density), isDark ? android.graphics.Color.parseColor("#33FFFFFF") : android.graphics.Color.parseColor("#22000000"));
        cmdInput.setBackground(cmdBg);
        cmdInput.setPadding((int)(12 * density), (int)(10 * density), (int)(12 * density), (int)(10 * density));
        LinearLayout.LayoutParams cmdParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cmdParams.bottomMargin = (int)(20 * density);
        layout.addView(cmdInput, cmdParams);

        // Buttons
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);

        android.widget.Button negBtn = new android.widget.Button(activity, null, android.R.attr.borderlessButtonStyle);
        negBtn.setText("Hủy");
        negBtn.setTextColor(subTextColor);
        negBtn.setTextSize(14);
        negBtn.setOnClickListener(v -> sheet.dismiss());
        LinearLayout.LayoutParams negParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        negParams.rightMargin = (int)(8 * density);
        btnRow.addView(negBtn, negParams);

        android.widget.Button posBtn = new android.widget.Button(activity);
        posBtn.setText("Lưu");
        posBtn.setTextSize(14);
        posBtn.setTextColor(android.graphics.Color.parseColor("#1A0A10"));
        android.graphics.drawable.GradientDrawable posBg = new android.graphics.drawable.GradientDrawable();
        posBg.setCornerRadius(10 * density);
        posBg.setColor(accentColor);
        posBtn.setBackground(posBg);
        posBtn.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String cmd = cmdInput.getText().toString().trim();
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(cmd)) {
                List<PaletteItem> snippets = loadSnippets(activity);
                snippets.add(new PaletteItem(title, cmd, true));
                saveSnippets(activity, snippets);
                Toast.makeText(activity, "Đã lưu mẫu lệnh", Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            }
        });
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(44 * density));
        btnRow.addView(posBtn, posParams);

        layout.addView(btnRow);

        sheet.setContentView(layout);
        sheet.show();
    }
}
