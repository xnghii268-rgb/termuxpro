package com.termux.app.features;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Storage and management layer for Command History and Command Templates.
 * Enforces:
 * - Hard cap 100 history entries with eviction of oldest UNPINNED entry.
 * - Deduplication on command execution.
 * - Max 5 pins total across history + templates.
 * - Pinned entries are never auto-evicted.
 * - Clear history action that keeps pinned entries.
 */
public class CommandStore {

    private static final String LOG_TAG = "TermuxProCommandStore";
    public static final int MAX_HISTORY_CAP = 100;
    public static final int MAX_TOTAL_PINS = 5;

    private static final String HISTORY_FILE_NAME = "termuxpro_cmd_history.json";
    private static final String TEMPLATES_FILE_NAME = "termuxpro_snippets.json";

    private final List<CommandItem> mHistory = new ArrayList<>();
    private final List<CommandItem> mTemplates = new ArrayList<>();
    private boolean mLoaded = false;

    private static CommandStore sInstance;

    public static synchronized CommandStore getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CommandStore();
            sInstance.load(context);
        }
        return sInstance;
    }

    public static CommandStore createForTesting() {
        return new CommandStore();
    }

    public synchronized void load(Context context) {
        load(context != null ? context.getFilesDir() : null);
    }

    public synchronized void load(File filesDir) {
        mHistory.clear();
        mTemplates.clear();

        if (filesDir != null) {
            loadTemplatesInternal(filesDir);
            loadHistoryInternal(filesDir);
        }
        mLoaded = true;
    }

    private void loadTemplatesInternal(File filesDir) {
        File file = new File(filesDir, TEMPLATES_FILE_NAME);
        if (!file.exists()) {
            // Seed default 5 built-in templates
            mTemplates.add(new CommandItem("pkg update && pkg upgrade", "Cập nhật gói hệ thống", true));
            mTemplates.add(new CommandItem("pkg install python nano git openssh jq -y", "Cài gói lập trình cơ bản", true));
            mTemplates.add(new CommandItem("clipimg", "Lấy đường dẫn ảnh tạm", true));
            mTemplates.add(new CommandItem("lastshot", "Lấy ảnh chụp màn hình mới", true));
            mTemplates.add(new CommandItem("git status", "Kiểm tra trạng thái Git", true));
            saveTemplatesInternal(filesDir);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                CommandItem item = CommandItem.fromJSON(obj);
                if (item != null && !isEmpty(item.getCommand())) {
                    mTemplates.add(item);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void loadHistoryInternal(File filesDir) {
        File file = new File(filesDir, HISTORY_FILE_NAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONArray arr = new JSONArray(sb.toString());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    CommandItem item = CommandItem.fromJSON(obj);
                    if (item != null && !isEmpty(item.getCommand())) {
                        mHistory.add(item);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // If history file was empty or newly created, seed from .bash_history
        if (mHistory.isEmpty()) {
            seedFromBashHistory();
            saveHistoryInternal(filesDir);
        }
    }

    private void seedFromBashHistory() {
        File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        File[] historyFiles = {
            new File(home, ".bash_history"),
            new File(home, ".zsh_history"),
            new File(home, ".history")
        };

        List<String> rawCommands = new ArrayList<>();
        for (File hf : historyFiles) {
            if (hf.exists() && hf.canRead()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(hf), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            if (line.startsWith(": ") && line.contains(";")) {
                                line = line.substring(line.indexOf(';') + 1).trim();
                            }
                            if (!line.isEmpty()) {
                                rawCommands.add(line);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Add most recent commands in reverse order, respecting deduplication and cap 100
        for (int i = rawCommands.size() - 1; i >= 0; i--) {
            addHistoryCommandInternal(rawCommands.get(i), false);
        }
    }

    public synchronized void save(Context context) {
        save(context != null ? context.getFilesDir() : null);
    }

    public synchronized void save(File filesDir) {
        if (filesDir != null) {
            saveTemplatesInternal(filesDir);
            saveHistoryInternal(filesDir);
        }
    }

    private void saveTemplatesInternal(File filesDir) {
        try {
            JSONArray arr = new JSONArray();
            for (CommandItem item : mTemplates) {
                arr.put(item.toJSON());
            }
            File file = new File(filesDir, TEMPLATES_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    private void saveHistoryInternal(File filesDir) {
        try {
            JSONArray arr = new JSONArray();
            for (CommandItem item : mHistory) {
                arr.put(item.toJSON());
            }
            File file = new File(filesDir, HISTORY_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Records an executed command into history.
     * Deduplicates (moves existing command to top and updates timestamp).
     * Enforces MAX_HISTORY_CAP (100) by evicting the oldest unpinned entry.
     */
    public synchronized boolean addHistoryCommand(String command, Context context) {
        return addHistoryCommand(command, context != null ? context.getFilesDir() : null);
    }

    public synchronized boolean addHistoryCommand(String command, File filesDir) {
        boolean changed = addHistoryCommandInternal(command, true);
        if (changed && filesDir != null) {
            saveHistoryInternal(filesDir);
        }
        return changed;
    }

    private boolean addHistoryCommandInternal(String command, boolean updateTimestamp) {
        if (command == null) return false;
        String trimmed = command.trim();
        if (trimmed.isEmpty()) return false;

        // 1. Deduplication: check if already exists
        for (int i = 0; i < mHistory.size(); i++) {
            CommandItem existing = mHistory.get(i);
            if (existing.getCommand().equals(trimmed)) {
                if (updateTimestamp) {
                    existing.setTimestamp(System.currentTimeMillis());
                }
                // Move to top (index 0)
                if (i != 0) {
                    mHistory.remove(i);
                    mHistory.add(0, existing);
                }
                return true;
            }
        }

        // 2. New command: check cap
        if (mHistory.size() >= MAX_HISTORY_CAP) {
            // Evict oldest UNPINNED entry (scan from end to start)
            boolean evicted = false;
            for (int i = mHistory.size() - 1; i >= 0; i--) {
                if (!mHistory.get(i).isPinned()) {
                    mHistory.remove(i);
                    evicted = true;
                    break;
                }
            }
            if (!evicted) {
                // All entries were pinned (theoretically impossible with max 5 pins), evict last
                mHistory.remove(mHistory.size() - 1);
            }
        }

        // 3. Add to front
        CommandItem newItem = new CommandItem(trimmed, "", false);
        mHistory.add(0, newItem);
        return true;
    }

    /**
     * Toggles the pin state of an item.
     * Enforces MAX_TOTAL_PINS (5) across history + templates.
     * @return true if pin state was toggled; false if limit reached.
     */
    public synchronized boolean togglePin(CommandItem item, Context context) {
        return togglePin(item, context != null ? context.getFilesDir() : null);
    }

    public synchronized boolean togglePin(CommandItem item, File filesDir) {
        if (item == null) return false;

        if (item.isPinned()) {
            item.setPinned(false);
            if (filesDir != null) save(filesDir);
            return true;
        } else {
            if (countTotalPins() >= MAX_TOTAL_PINS) {
                return false; // Limit reached!
            }
            item.setPinned(true);
            if (filesDir != null) save(filesDir);
            return true;
        }
    }

    public synchronized int countTotalPins() {
        int count = 0;
        for (CommandItem item : mHistory) {
            if (item.isPinned()) count++;
        }
        for (CommandItem item : mTemplates) {
            if (item.isPinned()) count++;
        }
        return count;
    }

    public synchronized boolean addTemplate(String title, String command, Context context) {
        return addTemplate(title, command, context != null ? context.getFilesDir() : null);
    }

    public synchronized boolean addTemplate(String title, String command, File filesDir) {
        if (isEmpty(title) || isEmpty(command)) return false;
        CommandItem item = new CommandItem(command.trim(), title.trim(), true);
        mTemplates.add(item);
        if (filesDir != null) saveTemplatesInternal(filesDir);
        return true;
    }

    public synchronized boolean updateTemplate(String id, String newTitle, String newCommand, Context context) {
        return updateTemplate(id, newTitle, newCommand, context != null ? context.getFilesDir() : null);
    }

    public synchronized boolean updateTemplate(String id, String newTitle, String newCommand, File filesDir) {
        if (isEmpty(id) || isEmpty(newTitle) || isEmpty(newCommand)) return false;
        for (CommandItem item : mTemplates) {
            if (item.getId().equals(id)) {
                item.setTitle(newTitle.trim());
                item.setCommand(newCommand.trim());
                if (filesDir != null) saveTemplatesInternal(filesDir);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean deleteItem(CommandItem item, Context context) {
        return deleteItem(item, context != null ? context.getFilesDir() : null);
    }

    public synchronized boolean deleteItem(CommandItem item, File filesDir) {
        if (item == null) return false;
        boolean removed = false;
        if (item.isTemplate()) {
            removed = mTemplates.remove(item);
            if (removed && filesDir != null) saveTemplatesInternal(filesDir);
        } else {
            removed = mHistory.remove(item);
            if (removed && filesDir != null) saveHistoryInternal(filesDir);
        }
        return removed;
    }

    public synchronized void restoreItem(CommandItem item, int originalIndex, Context context) {
        restoreItem(item, originalIndex, context != null ? context.getFilesDir() : null);
    }

    public synchronized void restoreItem(CommandItem item, int originalIndex, File filesDir) {
        if (item == null) return;
        if (item.isTemplate()) {
            int index = Math.min(Math.max(0, originalIndex), mTemplates.size());
            mTemplates.add(index, item);
            if (filesDir != null) saveTemplatesInternal(filesDir);
        } else {
            int index = Math.min(Math.max(0, originalIndex), mHistory.size());
            mHistory.add(index, item);
            if (filesDir != null) saveHistoryInternal(filesDir);
        }
    }

    public synchronized void clearHistoryKeepPinned(Context context) {
        clearHistoryKeepPinned(context != null ? context.getFilesDir() : null);
    }

    public synchronized void clearHistoryKeepPinned(File filesDir) {
        mHistory.removeIf(item -> !item.isPinned());
        if (filesDir != null) saveHistoryInternal(filesDir);
    }

    public synchronized List<CommandItem> getHistoryList() {
        return new ArrayList<>(mHistory);
    }

    public synchronized List<CommandItem> getTemplatesList() {
        return new ArrayList<>(mTemplates);
    }

    /**
     * Returns pinned items for the given tab, ordered by pinTimestamp descending (newest pin first).
     */
    public synchronized List<CommandItem> getPinnedItems(boolean isTemplateTab) {
        List<CommandItem> source = isTemplateTab ? mTemplates : mHistory;
        List<CommandItem> pinned = new ArrayList<>();
        for (CommandItem item : source) {
            if (item.isPinned()) pinned.add(item);
        }
        pinned.sort((a, b) -> Long.compare(b.getPinTimestamp(), a.getPinTimestamp()));
        return pinned;
    }

    /**
     * Returns unpinned items for the given tab.
     */
    public synchronized List<CommandItem> getUnpinnedItems(boolean isTemplateTab) {
        List<CommandItem> source = isTemplateTab ? mTemplates : mHistory;
        List<CommandItem> unpinned = new ArrayList<>();
        for (CommandItem item : source) {
            if (!item.isPinned()) unpinned.add(item);
        }
        return unpinned;
    }

    public static List<CommandItem> filter(List<CommandItem> list, String query) {
        if (isEmpty(query)) return new ArrayList<>(list);
        String q = query.toLowerCase().trim();
        List<CommandItem> filtered = new ArrayList<>();
        for (CommandItem item : list) {
            boolean matchCommand = item.getCommand().toLowerCase().contains(q);
            boolean matchTitle = item.getTitle().toLowerCase().contains(q);
            if (matchCommand || matchTitle) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
