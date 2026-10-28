package com.termux.view;

import com.termux.view.input.VietnameseEngine;

/**
 * Backward-compatible wrapper delegating to the unified, root-cause VietnameseEngine.
 * Provides deterministic Telex/VNI processing and English auto-restore.
 */
public final class VietnameseTelexEngine {

    public static class TransformResult {
        public final int backspaces;
        public final String text;

        public TransformResult(int backspaces, String text) {
            this.backspaces = backspaces;
            this.text = text;
        }
    }

    private final VietnameseEngine mEngine = new VietnameseEngine();

    public VietnameseTelexEngine() {}

    public VietnameseEngine getUnderlyingEngine() {
        return mEngine;
    }

    public boolean isEnabled() {
        return mEngine.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        mEngine.setEnabled(enabled);
    }

    public synchronized void reset() {
        mEngine.reset();
    }

    public synchronized void handleBackspace() {
        mEngine.handleBackspace();
    }

    public synchronized TransformResult processChar(char c) {
        VietnameseEngine.TransformResult tr = mEngine.processChar(c);
        if (tr == null) return null;
        return new TransformResult(tr.backspaces, tr.text);
    }
}
