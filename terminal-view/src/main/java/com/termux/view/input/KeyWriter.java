package com.termux.view.input;

import java.nio.charset.StandardCharsets;

/**
 * Handles action keys (Enter, Tab, Esc, navigation arrows, Ctrl/Alt chords).
 * Guarantees that the active word region is committed first before emitting control sequences.
 */
public class KeyWriter {

    private final RegionWriter mRegionWriter;
    private final PtyWriter mPty;

    public KeyWriter(RegionWriter regionWriter, PtyWriter pty) {
        this.mRegionWriter = regionWriter;
        this.mPty = pty;
    }

    public synchronized void writeActionKey(String escapeSequence) {
        if (mRegionWriter != null) {
            mRegionWriter.commit();
        }
        if (mPty != null && escapeSequence != null && !escapeSequence.isEmpty()) {
            mPty.write(escapeSequence.getBytes(StandardCharsets.UTF_8));
        }
    }

    public synchronized void writeCodePoint(boolean altDown, int codePoint) {
        if (mRegionWriter != null) {
            mRegionWriter.commit();
        }
        if (mPty != null) {
            mPty.writeCodePoint(altDown, codePoint);
        }
    }
}
