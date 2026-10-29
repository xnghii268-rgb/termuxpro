package com.termux.view.input;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;

/**
 * RegionWriter is the SOLE authority permitted to write text and text deltas to the Linux PTY.
 * Maintains S = the active word-region text already written to the terminal screen.
 * Synchronous, zero-hop, latency-measured UTF-8 NFC pipeline.
 */
public class RegionWriter {

    private final PtyWriter mPty;
    private String mS = "";
    private long mMaxLatencyNs = 0;
    private long mLastLatencyNs = 0;

    public RegionWriter(PtyWriter pty) {
        this.mPty = pty;
    }

    /**
     * Updates the terminal display from S to T using code-point accurate DELs and appending.
     * Dispatches as a single atomic UTF-8 byte array write to the PTY.
     *
     * @param targetText The desired active region text T.
     */
    public synchronized void update(String targetText) {
        long startNs = System.nanoTime();
        String T = (targetText != null) ? Normalizer.normalize(targetText, Normalizer.Form.NFC) : "";

        if (mS.equals(T)) {
            recordLatency(startNs);
            return;
        }

        int sCpCount = Character.codePointCount(mS, 0, mS.length());
        int sCharIdx = 0;
        int tCharIdx = 0;
        int commonCp = 0;

        while (sCharIdx < mS.length() && tCharIdx < T.length()) {
            int cpS = Character.codePointAt(mS, sCharIdx);
            int cpT = Character.codePointAt(T, tCharIdx);
            if (cpS != cpT) break;
            commonCp++;
            sCharIdx += Character.charCount(cpS);
            tCharIdx += Character.charCount(cpT);
        }

        int backspaces = sCpCount - commonCp;
        String append = T.substring(tCharIdx);

        if (mPty != null && (backspaces > 0 || !append.isEmpty())) {
            byte[] appendBytes = append.getBytes(StandardCharsets.UTF_8);
            byte[] buffer = new byte[backspaces + appendBytes.length];
            if (backspaces > 0) {
                Arrays.fill(buffer, 0, backspaces, (byte) 0x7F);
            }
            if (appendBytes.length > 0) {
                System.arraycopy(appendBytes, 0, buffer, backspaces, appendBytes.length);
            }
            mPty.write(buffer);
        }

        mS = T;
        recordLatency(startNs);
    }

    /**
     * Closes the active word region upon word boundaries, commits, or action keys.
     */
    public synchronized void commit() {
        mS = "";
    }

    /**
     * Emits DEL (0x7F) bytes for deletions that extend before the current active region.
     */
    public synchronized void deletePastRegion(int extraCodePoints) {
        long startNs = System.nanoTime();
        if (extraCodePoints > 0 && mPty != null) {
            byte[] dels = new byte[extraCodePoints];
            Arrays.fill(dels, (byte) 0x7F);
            mPty.write(dels);
        }
        recordLatency(startNs);
    }

    public synchronized String getActiveRegionText() {
        return mS;
    }

    public synchronized long getMaxLatencyNs() {
        return mMaxLatencyNs;
    }

    public synchronized long getLastLatencyNs() {
        return mLastLatencyNs;
    }

    public synchronized double getMaxLatencyMs() {
        return mMaxLatencyNs / 1_000_000.0;
    }

    public synchronized double getLastLatencyMs() {
        return mLastLatencyNs / 1_000_000.0;
    }

    private void recordLatency(long startNs) {
        long elapsed = System.nanoTime() - startNs;
        mLastLatencyNs = elapsed;
        if (elapsed > mMaxLatencyNs) {
            mMaxLatencyNs = elapsed;
        }
    }
}
