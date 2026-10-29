package com.termux.view;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.termux.view.input.InputModel;
import com.termux.view.input.InputProducer;
import com.termux.view.input.KeyWriter;
import com.termux.view.input.PtyWriter;
import com.termux.view.input.RegionWriter;
import com.termux.view.input.TraceRecorder;
import com.termux.view.input.VietnameseEngine;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-Writer InputConnection for Termux Pro (v3 Hotfix Pipeline).
 *
 * RegionWriter Semantics (Fix Rules):
 * 1. S = text currently displayed that is still EDITABLE by the IME (composing region only).
 * 2. commit(), finishComposingText, boundary (space/Enter/punct), and KeyEvent close the region: S = "" AFTER final text is written.
 * 3. commitText(text) with composing span: final text replaces composing span; update(S -> text) then close region.
 * 4. commitText(text) without composing span: pure append, ZERO DEL.
 * 5. In RAW engine mode: "a" then "g" continues word "ag" with common prefix "a" (0 DELs).
 * 6. Safe Mode: direct hardware-accurate pass-through, no engine, no composing mirror, backspace = single DEL.
 */
public class TerminalInputConnection extends BaseInputConnection {

    public interface TerminalInputListener {
        void onSendKeyEvent(KeyEvent event);
        void onComposingStateChanged(String composingText);
        boolean isBypassInputEngine();
        default boolean isControlOrAltActive() { return false; }
        default void onFallbackToSafeMode() {}
    }

    private final InputModel mInputModel;
    private final RegionWriter mRegionWriter;
    private final KeyWriter mKeyWriter;
    private final InputProducer mInputProducer;
    private final TerminalInputListener mListener;

    private int mBatchDepth = 0;
    private String mBatchPendingTarget = null;
    private boolean mBatchPendingCommit = false;

    private static boolean sDebugLogEnabled = false;
    private static final List<String> sDebugLog = new ArrayList<>();
    private String mActiveImeId = "unknown";
    private int mInputMethodMode = 0; // 0 = app engine, 1 = keyboard ime, 2 = builtin keyboard
    private boolean mSafeMode = false;

    public TerminalInputConnection(View targetView, PtyWriter ptyWriter, TerminalInputListener listener) {
        super(targetView, true);
        this.mListener = listener;
        this.mInputModel = new InputModel();
        this.mRegionWriter = new RegionWriter(ptyWriter);
        this.mKeyWriter = new KeyWriter(this.mRegionWriter, ptyWriter);
        this.mInputProducer = new InputProducer(null, new InputProducer.BypassChecker() {
            @Override
            public boolean isBypassed() {
                if (mSafeMode) return true;
                if (mInputMethodMode == 1) return true; // External keyboard IME owns composition
                return mListener != null && mListener.isBypassInputEngine();
            }

            @Override
            public boolean isControlOrAltActive() {
                return mListener != null && mListener.isControlOrAltActive();
            }
        });
    }

    public void setVietnameseEngine(VietnameseEngine engine) {
        mInputProducer.setVietnameseEngine(engine);
    }

    public VietnameseEngine getVietnameseEngine() {
        return mInputProducer.getVietnameseEngine();
    }

    public RegionWriter getRegionWriter() {
        return mRegionWriter;
    }

    public KeyWriter getKeyWriter() {
        return mKeyWriter;
    }

    public InputModel getInputModel() {
        return mInputModel;
    }

    public InputProducer getInputProducer() {
        return mInputProducer;
    }

    public void setInputMethodMode(int mode) {
        this.mInputMethodMode = mode;
        if (mInputProducer.getVietnameseEngine() != null) {
            // Mode 1: disable internal engine (external keyboard handles everything)
            mInputProducer.getVietnameseEngine().setEnabled(mode != 1 && !mSafeMode);
        }
    }

    public int getInputMethodMode() {
        return mInputMethodMode;
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
        if (safeMode) {
            commitActiveRegion();
            if (mInputProducer.getVietnameseEngine() != null) {
                mInputProducer.getVietnameseEngine().setEnabled(false);
            }
        } else {
            setInputMethodMode(mInputMethodMode);
        }
    }

    public boolean isSafeMode() {
        return mSafeMode;
    }

    public void triggerSafeModeFallback() {
        setSafeMode(true);
        if (mListener != null) {
            mListener.onFallbackToSafeMode();
        }
    }

    public void setActiveImeId(String imeId) {
        this.mActiveImeId = (imeId != null) ? imeId : "unknown";
    }

    public String getActiveImeId() {
        return mActiveImeId;
    }

    public static void setDebugLogEnabled(boolean enabled) {
        sDebugLogEnabled = enabled;
        TraceRecorder.setEnabled(enabled);
        if (!enabled) {
            synchronized (sDebugLog) {
                sDebugLog.clear();
            }
        }
    }

    public static boolean isDebugLogEnabled() {
        return sDebugLogEnabled;
    }

    public static List<String> getRecentDebugLogs() {
        synchronized (sDebugLog) {
            return new ArrayList<>(sDebugLog);
        }
    }

    private void logEvent(String msg) {
        if (!sDebugLogEnabled) return;
        synchronized (sDebugLog) {
            if (sDebugLog.size() > 200) sDebugLog.remove(0);
            sDebugLog.add(System.currentTimeMillis() + ": " + msg);
        }
    }

    @Override
    public boolean beginBatchEdit() {
        mBatchDepth++;
        logEvent("beginBatchEdit depth=" + mBatchDepth);
        return super.beginBatchEdit();
    }

    @Override
    public boolean endBatchEdit() {
        mBatchDepth = Math.max(0, mBatchDepth - 1);
        logEvent("endBatchEdit depth=" + mBatchDepth);
        if (mBatchDepth == 0) {
            if (mBatchPendingTarget != null) {
                mRegionWriter.update(mBatchPendingTarget);
                mBatchPendingTarget = null;
            }
            if (mBatchPendingCommit) {
                mRegionWriter.commit();
                mBatchPendingCommit = false;
            }
        }
        return super.endBatchEdit();
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        String str = (text != null) ? text.toString() : "";
        logEvent("setComposingText(\"" + str + "\", " + newCursorPosition + ")");

        if (mSafeMode) {
            // Safe mode: pass-through plain commit
            commitActiveRegion();
            mInputModel.commitText(str, newCursorPosition);
            if (!str.isEmpty()) {
                mRegionWriter.update(str);
                mRegionWriter.commit();
            }
            return true;
        }

        boolean wasComposing = mInputModel.isComposing();
        if (!wasComposing) {
            // Rule 1: A new composing text must NEVER emit DEL for committed text.
            // Close any previous editable region so S = "" before starting new composing text.
            mRegionWriter.commit();
            mInputProducer.resetWord();
        }

        mInputModel.setComposingText(text, newCursorPosition);
        try {
            super.setComposingText(text, newCursorPosition);
        } catch (Throwable ignored) {}

        if (mListener != null && mListener.isControlOrAltActive()) {
            if (!str.isEmpty()) {
                mRegionWriter.commit();
                mRegionWriter.update(str);
                mRegionWriter.commit();
            }
            return true;
        }

        if (mListener != null) {
            mListener.onComposingStateChanged(str);
        }

        String sBefore = mRegionWriter.getActiveRegionText();
        String target = mInputProducer.onComposingText(str);

        if (mBatchDepth > 0) {
            mBatchPendingTarget = target;
        } else {
            mRegionWriter.update(target);
        }

        String sAfter = mRegionWriter.getActiveRegionText();
        TraceRecorder.record("setComposingText(\"" + str + "\")",
                mInputProducer.getMode().name(),
                mInputModel.toString(),
                mInputProducer.getRawBuffer(),
                target,
                sBefore, sAfter,
                sAfter.equals(sBefore) ? "0" : sAfter,
                mRegionWriter.getLastLatencyMs(),
                mListener == null || !mListener.isBypassInputEngine());

        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        logEvent("setComposingRegion(" + start + ", " + end + ")");
        if (mSafeMode) return true;
        mInputModel.setComposingRegion(start, end);
        try {
            super.setComposingRegion(start, end);
        } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public boolean finishComposingText() {
        logEvent("finishComposingText()");
        String sBefore = mRegionWriter.getActiveRegionText();

        mInputModel.finishComposingText();
        try {
            super.finishComposingText();
        } catch (Throwable ignored) {}

        mInputProducer.resetWord();
        mRegionWriter.commit(); // S = "" AFTER final text is written

        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }

        TraceRecorder.record("finishComposingText()",
                mInputProducer.getMode().name(),
                mInputModel.toString(),
                "", "",
                sBefore, "", "commit",
                mRegionWriter.getLastLatencyMs(),
                mListener == null || !mListener.isBypassInputEngine());

        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if (text == null) return false;
        String rawStr = text.toString();
        logEvent("commitText(\"" + rawStr + "\", " + newCursorPosition + ")");

        if (mSafeMode) {
            mRegionWriter.commit();
            mInputProducer.resetWord();
            mInputModel.commitText(text, newCursorPosition);
            try {
                super.commitText(text, newCursorPosition);
            } catch (Throwable ignored) {}
            if (!rawStr.isEmpty()) {
                mRegionWriter.update(rawStr);
                mRegionWriter.commit();
            }
            return true;
        }

        boolean wasComposing = mInputModel.isComposing();
        String sBefore = mRegionWriter.getActiveRegionText();

        mInputModel.commitText(text, newCursorPosition);
        try {
            super.commitText(text, newCursorPosition);
        } catch (Throwable ignored) {}

        if (mListener != null && mListener.isControlOrAltActive()) {
            if (!rawStr.isEmpty()) {
                mRegionWriter.commit();
                mRegionWriter.update(rawStr);
                mRegionWriter.commit();
            }
            return true;
        }

        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }

        if (wasComposing) {
            // Rule 2: commitText while a composing span exists:
            // final text replaces the composing span; write update(S -> text) then close the region.
            if (mBatchDepth > 0) {
                mBatchPendingTarget = rawStr;
                mBatchPendingCommit = true;
            } else {
                mRegionWriter.update(rawStr);
                mRegionWriter.commit();
            }
            mInputProducer.resetWord();
        } else {
            // Rule 2 & 3: commitText with no composing span: pure append, no DEL.
            // For a raw keyboard (engine mode), "a" then "g" with no boundary between is the same word "ag".
            boolean isEngineActive = (mInputMethodMode == 0 || mInputMethodMode == 2)
                    && (mListener == null || !mListener.isBypassInputEngine())
                    && mInputProducer.getVietnameseEngine() != null
                    && mInputProducer.getVietnameseEngine().isEnabled();

            boolean isSingleAsciiLetter = (rawStr.length() == 1 &&
                    rawStr.charAt(0) <= 127 &&
                    Character.isLetter(rawStr.charAt(0)));

            if (isEngineActive && isSingleAsciiLetter) {
                String target = mInputProducer.onDirectCommit(rawStr);
                if (mBatchDepth > 0) {
                    mBatchPendingTarget = target;
                } else {
                    mRegionWriter.update(target);
                }
            } else {
                // Word boundary (space, enter, punct) or engine disabled:
                // Close active word region first (S = "") so committed text is NEVER erased by DEL.
                mRegionWriter.commit();
                mInputProducer.resetWord();

                // Pure append without DEL
                if (!rawStr.isEmpty()) {
                    if (mBatchDepth > 0) {
                        mBatchPendingTarget = rawStr;
                        mBatchPendingCommit = true;
                    } else {
                        mRegionWriter.update(rawStr);
                        mRegionWriter.commit();
                    }
                }
            }
        }

        String sAfter = mRegionWriter.getActiveRegionText();
        TraceRecorder.record("commitText(\"" + rawStr + "\")",
                mInputProducer.getMode().name(),
                mInputModel.toString(),
                mInputProducer.getRawBuffer(),
                rawStr,
                sBefore, sAfter,
                rawStr,
                mRegionWriter.getLastLatencyMs(),
                mListener == null || !mListener.isBypassInputEngine());

        return true;
    }

    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        logEvent("deleteSurroundingText(" + leftLength + ", " + rightLength + ")");
        if (leftLength <= 0 && rightLength <= 0) return true;

        if (mSafeMode) {
            mRegionWriter.commit();
            mInputProducer.resetWord();
            mInputModel.deleteSurroundingText(leftLength, rightLength);
            mRegionWriter.deletePastRegion(leftLength);
            return true;
        }

        boolean wasComposing = mInputModel.isComposing();
        mInputModel.deleteSurroundingText(leftLength, rightLength);
        try {
            super.deleteSurroundingText(leftLength, rightLength);
        } catch (Throwable ignored) {}

        String sBefore = mRegionWriter.getActiveRegionText();

        if (wasComposing) {
            int activeCp = Character.codePointCount(sBefore, 0, sBefore.length());
            if (leftLength <= activeCp) {
                int targetCp = activeCp - leftLength;
                int targetEnd = 0;
                for (int i = 0; i < targetCp; i++) {
                    targetEnd += Character.charCount(Character.codePointAt(sBefore, targetEnd));
                }
                String target = sBefore.substring(0, targetEnd);
                if (mBatchDepth > 0) {
                    mBatchPendingTarget = target;
                } else {
                    mRegionWriter.update(target);
                }
            } else {
                int extra = leftLength - activeCp;
                mRegionWriter.update("");
                mRegionWriter.commit();
                mRegionWriter.deletePastRegion(extra);
            }
        } else {
            // Not composing
            if (mInputProducer.getMode() == InputProducer.Mode.RAW && mInputProducer.getRawBuffer().length() > 0) {
                String target = mInputProducer.onBackspace();
                if (target != null) {
                    if (mBatchDepth > 0) {
                        mBatchPendingTarget = target;
                    } else {
                        mRegionWriter.update(target);
                    }
                } else {
                    mRegionWriter.commit();
                    mRegionWriter.deletePastRegion(leftLength);
                }
            } else {
                // Plain committed text deletion
                mRegionWriter.commit();
                mRegionWriter.deletePastRegion(leftLength);
            }
        }

        if (mListener != null) {
            mListener.onComposingStateChanged(mRegionWriter.getActiveRegionText());
        }

        String sAfter = mRegionWriter.getActiveRegionText();
        TraceRecorder.record("deleteSurroundingText(" + leftLength + ", " + rightLength + ")",
                mInputProducer.getMode().name(),
                mInputModel.toString(),
                mInputProducer.getRawBuffer(),
                sAfter,
                sBefore, sAfter,
                "DEL x" + leftLength,
                mRegionWriter.getLastLatencyMs(),
                mListener == null || !mListener.isBypassInputEngine());

        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        logEvent("deleteSurroundingTextInCodePoints(" + beforeLength + ", " + afterLength + ")");
        mInputModel.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
        return deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean setSelection(int start, int end) {
        logEvent("setSelection(" + start + ", " + end + ")");
        mInputModel.setSelection(start, end);
        try {
            super.setSelection(start, end);
        } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        if (n <= 0) return "";
        CharSequence res = mInputModel.getTextBeforeCursor(n, flags);
        logEvent("getTextBeforeCursor(" + n + ") -> \"" + res + "\"");
        return res;
    }

    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        if (n <= 0) return "";
        return mInputModel.getTextAfterCursor(n, flags);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        return mInputModel.getSelectedText(flags);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        ExtractedText et = new ExtractedText();
        et.text = mInputModel.toString();
        et.startOffset = 0;
        et.selectionStart = mInputModel.getSelectionStart();
        et.selectionEnd = mInputModel.getSelectionEnd();
        et.flags = 0;
        return et;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        logEvent("sendKeyEvent(action=" + event.getAction() + ", code=" + event.getKeyCode() + ")");
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                return deleteSurroundingText(1, 0);
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                commitActiveRegion();
                mKeyWriter.writeCodePoint(false, '\r');
                return true;
            }
        }
        commitActiveRegion();
        if (mListener != null) {
            mListener.onSendKeyEvent(event);
        }
        return true;
    }

    /**
     * Commits the active region and resets word context upon boundaries, action keys, or focus changes.
     */
    public synchronized void commitActiveRegion() {
        mRegionWriter.commit();
        mInputProducer.resetWord();
        mInputModel.finishComposingText();
        try {
            Editable e = super.getEditable();
            if (e != null) e.clear();
        } catch (Throwable ignored) {}
        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }
    }

    public void resetContext() {
        commitActiveRegion();
        mInputModel.clear();
    }
}
