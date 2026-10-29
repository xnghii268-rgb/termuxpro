package com.termux.view.input;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * High-precision trace recorder for IME input events, producer states, and PTY writes.
 * Active exclusively when debug logging is enabled and echo is ON (never records passwords).
 */
public class TraceRecorder {

    public static class TraceEntry {
        public final long timestamp;
        public final String imeCall;
        public final String producer;
        public final String modelState;
        public final String engineInput;
        public final String engineOutput;
        public final String sBefore;
        public final String sAfter;
        public final String bytesWritten;
        public final double latencyMs;

        public TraceEntry(String imeCall, String producer, String modelState,
                          String engineInput, String engineOutput,
                          String sBefore, String sAfter, String bytesWritten, double latencyMs) {
            this.timestamp = System.currentTimeMillis();
            this.imeCall = imeCall;
            this.producer = producer;
            this.modelState = modelState;
            this.engineInput = engineInput;
            this.engineOutput = engineOutput;
            this.sBefore = sBefore;
            this.sAfter = sAfter;
            this.bytesWritten = bytesWritten;
            this.latencyMs = latencyMs;
        }

        public String toJson() {
            return "{" +
                    "\"timestamp\":" + timestamp + "," +
                    "\"imeCall\":\"" + escape(imeCall) + "\"," +
                    "\"producer\":\"" + escape(producer) + "\"," +
                    "\"modelState\":\"" + escape(modelState) + "\"," +
                    "\"engineInput\":\"" + escape(engineInput) + "\"," +
                    "\"engineOutput\":\"" + escape(engineOutput) + "\"," +
                    "\"sBefore\":\"" + escape(sBefore) + "\"," +
                    "\"sAfter\":\"" + escape(sAfter) + "\"," +
                    "\"bytesWritten\":\"" + escape(bytesWritten) + "\"," +
                    "\"latencyMs\":" + latencyMs +
                    "}";
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }
    }

    private static boolean sEnabled = false;
    private static final List<TraceEntry> sEntries = new ArrayList<>();
    private static final int MAX_ENTRIES = 1000;

    public static synchronized void setEnabled(boolean enabled) {
        sEnabled = enabled;
        if (!enabled) {
            sEntries.clear();
        }
    }

    public static synchronized boolean isEnabled() {
        return sEnabled;
    }

    public static synchronized void record(String imeCall, String producer, String modelState,
                                          String engineInput, String engineOutput,
                                          String sBefore, String sAfter, String bytesWritten,
                                          double latencyMs, boolean echoOn) {
        if (!sEnabled || !echoOn) return;
        if (sEntries.size() >= MAX_ENTRIES) {
            sEntries.remove(0);
        }
        sEntries.add(new TraceEntry(imeCall, producer, modelState, engineInput, engineOutput,
                sBefore, sAfter, bytesWritten, latencyMs));
    }

    public static synchronized List<TraceEntry> getEntries() {
        return new ArrayList<>(sEntries);
    }

    public static synchronized void clear() {
        sEntries.clear();
    }

    public static synchronized boolean exportToFile(File file) {
        if (file == null) return false;
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write("[\n");
                for (int i = 0; i < sEntries.size(); i++) {
                    writer.write("  " + sEntries.get(i).toJson());
                    if (i < sEntries.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("]\n");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
