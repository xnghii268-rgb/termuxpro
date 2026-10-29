package com.termux.view.input;

/**
 * Interface representing the physical sink to the terminal Linux PTY.
 * Used exclusively by RegionWriter (for text and DELs) and KeyWriter (for action keys/escape sequences).
 */
public interface PtyWriter {
    void write(byte[] data);
    void writeCodePoint(boolean altDown, int codePoint);
}
