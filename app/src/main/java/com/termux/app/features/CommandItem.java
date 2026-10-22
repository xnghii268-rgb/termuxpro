package com.termux.app.features;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Model representing a shell command entry in the Command Palette (either history or template).
 */
public class CommandItem {

    private final String mId;
    private String mCommand;
    private String mTitle;
    private final boolean mIsTemplate;
    private boolean mIsPinned;
    private long mTimestamp;
    private long mPinTimestamp;

    public CommandItem(String command, String title, boolean isTemplate) {
        this(UUID.randomUUID().toString(), command, title, isTemplate, false, System.currentTimeMillis(), 0);
    }

    public CommandItem(String id, String command, String title, boolean isTemplate, boolean isPinned, long timestamp, long pinTimestamp) {
        this.mId = id != null ? id : UUID.randomUUID().toString();
        this.mCommand = command != null ? command.trim() : "";
        this.mTitle = title != null ? title.trim() : "";
        this.mIsTemplate = isTemplate;
        this.mIsPinned = isPinned;
        this.mTimestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.mPinTimestamp = pinTimestamp;
    }

    public String getId() {
        return mId;
    }

    public String getCommand() {
        return mCommand;
    }

    public void setCommand(String command) {
        this.mCommand = command != null ? command.trim() : "";
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title != null ? title.trim() : "";
    }

    public boolean isTemplate() {
        return mIsTemplate;
    }

    public boolean isPinned() {
        return mIsPinned;
    }

    public void setPinned(boolean pinned) {
        this.mIsPinned = pinned;
        this.mPinTimestamp = pinned ? System.currentTimeMillis() : 0;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        this.mTimestamp = timestamp;
    }

    public long getPinTimestamp() {
        return mPinTimestamp;
    }

    public void setPinTimestamp(long pinTimestamp) {
        this.mPinTimestamp = pinTimestamp;
    }

    /**
     * Formats the relative timestamp in natural, idiomatic Vietnamese.
     */
    public String getFormattedRelativeTime() {
        long now = System.currentTimeMillis();
        long diff = now - mTimestamp;
        if (diff < 0) diff = 0;

        long seconds = diff / 1000;
        if (seconds < 60) {
            return "Vừa xong";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " phút trước";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " giờ trước";
        }
        long days = hours / 24;
        if (days == 1) {
            return "Hôm qua";
        }
        if (days < 7) {
            return days + " ngày trước";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(mTimestamp));
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", mId);
        obj.put("command", mCommand);
        obj.put("title", mTitle);
        obj.put("isTemplate", mIsTemplate);
        obj.put("isPinned", mIsPinned);
        obj.put("timestamp", mTimestamp);
        obj.put("pinTimestamp", mPinTimestamp);
        return obj;
    }

    public static CommandItem fromJSON(JSONObject obj) {
        if (obj == null) return null;
        String id = obj.optString("id", UUID.randomUUID().toString());
        String command = obj.optString("command", "");
        String title = obj.optString("title", "");
        boolean isTemplate = obj.optBoolean("isTemplate", false);
        boolean isPinned = obj.optBoolean("isPinned", false);
        long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
        long pinTimestamp = obj.optLong("pinTimestamp", 0);
        return new CommandItem(id, command, title, isTemplate, isPinned, timestamp, pinTimestamp);
    }

    @NonNull
    @Override
    public String toString() {
        return mIsTemplate ? mTitle + " (" + mCommand + ")" : mCommand;
    }
}
