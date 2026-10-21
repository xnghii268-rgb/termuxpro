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
            return isSnippet ? "★ " + title + "\n  " + command : "  " + command;
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

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 16);

        final EditText searchInput = new EditText(activity);
        searchInput.setHint(R.string.command_palette_search_hint);
        searchInput.setSingleLine(true);
        layout.addView(searchInput);

        final ListView listView = new ListView(activity);
        listView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800));

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
                        text1.setText("★ " + item.title);
                        text2.setText(item.command);
                    } else {
                        text1.setText(item.command);
                        text2.setText("Lịch sử lệnh");
                    }
                }
                return view;
            }
        };

        listView.setAdapter(adapter);
        layout.addView(listView);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.command_palette_title)
            .setView(layout)
            .setPositiveButton("Thêm lệnh mẫu", (d, which) -> showAddSnippetDialog(activity))
            .setNegativeButton("Đóng", null)
            .create();

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
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public static void showAddSnippetDialog(final TermuxActivity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 16);

        final EditText titleInput = new EditText(activity);
        titleInput.setHint(R.string.snippets_name_hint);
        layout.addView(titleInput);

        final EditText cmdInput = new EditText(activity);
        cmdInput.setHint(R.string.snippets_cmd_hint);
        layout.addView(cmdInput);

        new AlertDialog.Builder(activity)
            .setTitle(R.string.snippets_add)
            .setView(layout)
            .setPositiveButton("Lưu", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                String cmd = cmdInput.getText().toString().trim();
                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(cmd)) {
                    List<PaletteItem> snippets = loadSnippets(activity);
                    snippets.add(new PaletteItem(title, cmd, true));
                    saveSnippets(activity, snippets);
                    Toast.makeText(activity, "Đã lưu lệnh mẫu", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
