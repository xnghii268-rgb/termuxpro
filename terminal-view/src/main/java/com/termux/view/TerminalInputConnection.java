package com.termux.view;

import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

import com.termux.view.input.VietnameseEngine;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, root-cause Vietnamese Telex/VNI InputConnection for Termux Pro.
 * Preserves native AOSP BaseInputConnection Editable synchronization to prevent IME desynchronization,
 * bridges live composing and direct commit deltas to PTY, and auto-restores English words.
 */
public class TerminalInputConnection extends BaseInputConnection {

    public interface TerminalInputListener {
        void onCommitText(String text);
        void onCommitTextRaw(String text);
        void onDeleteCodePoints(int codePoints);
        void onSendKeyEvent(KeyEvent event);
        void onComposingStateChanged(String composingText);
        boolean isBypassInputEngine();
        default boolean isControlOrAltActive() { return false; }
    }

    private final TerminalInputListener mListener;
    private final StringBuilder mCommitWordBuffer = new StringBuilder();
    private String mLastCommittedWordDisplay = "";
    private String mComposingOnPty = "";
    private VietnameseEngine mEngine;
    private int mBatchDepth = 0;
    private boolean mBatchModified = false;
    private static boolean sDebugLogEnabled = false;
    private static final List<String> sDebugLog = new ArrayList<>();
    private String mActiveImeId = "unknown";
    private final StringBuilder mCommittedHistory = new StringBuilder();

    public TerminalInputConnection(View targetView, TerminalInputListener listener) {
        super(targetView, true);
        this.mListener = listener;
    }

    public void setVietnameseEngine(VietnameseEngine engine) {
        this.mEngine = engine;
    }

    public VietnameseEngine getVietnameseEngine() {
        return mEngine;
    }

    public static void setDebugLogEnabled(boolean enabled) {
        sDebugLogEnabled = enabled;
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

    public void setActiveImeId(String imeId) {
        this.mActiveImeId = (imeId != null) ? imeId : "unknown";
    }

    public String getActiveImeId() {
        return mActiveImeId;
    }

    public void logDebugState(String rawBuffer, String engineOutput, boolean isVietnamese, boolean isRestored, int bytesWritten, boolean echoOn) {
        if (!sDebugLogEnabled) return;
        if (!echoOn) {
            logEvent("IME State: [PASSWORD_ECHO_OFF] bytesWritten=" + bytesWritten + " ime=" + mActiveImeId);
        } else {
            logEvent("IME State: raw=\"" + rawBuffer + "\" out=\"" + engineOutput + "\" vi=" + isVietnamese + " restored=" + isRestored + " bytes=" + bytesWritten + " ime=" + mActiveImeId);
        }
    }

    private void logEvent(String msg) {
        if (!sDebugLogEnabled) return;
        synchronized (sDebugLog) {
            if (sDebugLog.size() > 200) sDebugLog.remove(0);
            sDebugLog.add(System.currentTimeMillis() + ": " + msg);
        }
    }

    private void clearNativeEditable() {
        try {
            Editable e = super.getEditable();
            if (e != null) {
                e.clear();
            }
        } catch (Throwable ignored) {}
    }

    private void trimHistory() {
        if (mCommittedHistory.length() > 1000) {
            mCommittedHistory.delete(0, mCommittedHistory.length() - 500);
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
        if (mBatchDepth == 0 && mBatchModified) {
            mBatchModified = false;
        }
        return super.endBatchEdit();
    }

    public String transformComposingText(String input) {
        if (input == null || input.isEmpty()) return "";
        String nfc = Normalizer.normalize(input, Normalizer.Form.NFC);
        if (mListener != null && mListener.isBypassInputEngine()) {
            return nfc;
        }
        if (mEngine == null || !mEngine.isEnabled()) {
            return nfc;
        }
        // If input contains non-ASCII characters and doesn't end with an ASCII letter modifier,
        // it is already a finalized/composed word from an external IME.
        if (!isPureAscii(nfc)) {
            char last = nfc.charAt(nfc.length() - 1);
            if (last > 127 || !Character.isLetter(last)) {
                return nfc;
            }
        }
        int end = nfc.length();
        while (end > 0 && Character.isWhitespace(nfc.charAt(end - 1))) {
            end--;
        }
        String wordPart = nfc.substring(0, end);
        String wsPart = nfc.substring(end);
        VietnameseEngine.Result r = VietnameseEngine.process(wordPart, mEngine.getOptions());
        return r.text + wsPart;
    }

    private static boolean isPureAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return false;
        }
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        String str = (text != null) ? text.toString() : "";
        logEvent("setComposingText(\"" + str + "\", " + newCursorPosition + ")");

        // Update Android native Editable with the raw text sent by IME.
        // This guarantees Gboard's internal model stays 100% in sync without desync or span corruption.
        try {
            super.setComposingText(text, newCursorPosition);
        } catch (Throwable ignored) {}

        if (mListener != null && mListener.isControlOrAltActive()) {
            if (!str.isEmpty()) {
                mListener.onCommitTextRaw(str);
            }
            return true;
        }

        mCommitWordBuffer.setLength(0);
        mLastCommittedWordDisplay = "";

        if (mListener != null) {
            mListener.onComposingStateChanged(str);
        }

        String target = transformComposingText(str);

        int common = 0;
        int min = Math.min(mComposingOnPty.length(), target.length());
        while (common < min && mComposingOnPty.charAt(common) == target.charAt(common)) {
            common++;
        }

        int backspaces = mComposingOnPty.length() - common;
        String append = target.substring(common);

        if (mListener != null) {
            if (backspaces > 0) {
                mListener.onDeleteCodePoints(backspaces);
            }
            if (!append.isEmpty()) {
                mListener.onCommitTextRaw(append);
            }
        }

        mComposingOnPty = target;
        mBatchModified = true;
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        logEvent("setComposingRegion(" + start + ", " + end + ")");
        return super.setComposingRegion(start, end);
    }

    @Override
    public boolean finishComposingText() {
        logEvent("finishComposingText()");
        try {
            super.finishComposingText();
        } catch (Throwable ignored) {}

        if (mComposingOnPty.length() > 0) {
            mCommittedHistory.append(mComposingOnPty);
            trimHistory();
            mComposingOnPty = "";
        }
        mCommitWordBuffer.setLength(0);
        mLastCommittedWordDisplay = "";
        clearNativeEditable();

        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }
        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if (text == null) return false;
        String rawStr = text.toString();
        logEvent("commitText(\"" + rawStr + "\", " + newCursorPosition + ")");

        try {
            super.commitText(text, newCursorPosition);
        } catch (Throwable ignored) {}

        if (mListener != null && mListener.isControlOrAltActive()) {
            if (!rawStr.isEmpty()) {
                mListener.onCommitTextRaw(rawStr);
            }
            clearNativeEditable();
            return true;
        }

        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }

        if (mComposingOnPty.length() > 0) {
            mCommitWordBuffer.setLength(0);
            mLastCommittedWordDisplay = "";
            String target = transformComposingText(rawStr);
            int common = 0;
            int min = Math.min(mComposingOnPty.length(), target.length());
            while (common < min && mComposingOnPty.charAt(common) == target.charAt(common)) {
                common++;
            }
            int backspaces = mComposingOnPty.length() - common;
            String append = target.substring(common);

            if (mListener != null) {
                if (backspaces > 0) {
                    mListener.onDeleteCodePoints(backspaces);
                }
                if (!append.isEmpty()) {
                    mListener.onCommitTextRaw(append);
                }
            }
            mComposingOnPty = "";

            mCommittedHistory.append(target);
            trimHistory();
            clearNativeEditable();
        } else {
            // Direct commit mode:
            // Only buffer single pure-ASCII letters (for Telex composition without IME)
            boolean isSingleAsciiLetter = (rawStr.length() == 1 && rawStr.charAt(0) <= 127 && Character.isLetter(rawStr.charAt(0)));
            if (isSingleAsciiLetter && mEngine != null && mEngine.isEnabled() && (mListener == null || !mListener.isBypassInputEngine())) {
                if (mCommitWordBuffer.length() == 0) {
                    mLastCommittedWordDisplay = "";
                }
                mCommitWordBuffer.append(rawStr.charAt(0));
                String targetWord = transformComposingText(mCommitWordBuffer.toString());
                int common = 0;
                int min = Math.min(mLastCommittedWordDisplay.length(), targetWord.length());
                while (common < min && mLastCommittedWordDisplay.charAt(common) == targetWord.charAt(common)) {
                    common++;
                }
                int backspaces = mLastCommittedWordDisplay.length() - common;
                String append = targetWord.substring(common);

                if (mListener != null) {
                    if (backspaces > 0) {
                        mListener.onDeleteCodePoints(backspaces);
                    }
                    if (!append.isEmpty()) {
                        mListener.onCommitTextRaw(append);
                    }
                }
                mLastCommittedWordDisplay = targetWord;

                mCommittedHistory.append(append);
                trimHistory();
            } else {
                // Word commit from suggestion or space/enter/non-letter/already-composed text (á, đ, etc.)
                mCommitWordBuffer.setLength(0);
                mLastCommittedWordDisplay = "";
                String target = Normalizer.normalize(rawStr, Normalizer.Form.NFC);
                if (mListener != null && !target.isEmpty()) {
                    mListener.onCommitTextRaw(target);
                }
                mCommittedHistory.append(target);
                trimHistory();
                clearNativeEditable();
            }
        }

        mBatchModified = true;
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        logEvent("deleteSurroundingText(" + leftLength + ", " + rightLength + ")");
        try {
            super.deleteSurroundingText(leftLength, rightLength);
        } catch (Throwable ignored) {}
        if (leftLength <= 0 && rightLength <= 0) return true;

        if (mComposingOnPty.length() > 0) {
            int del = Math.min(leftLength, mComposingOnPty.length());
            String newTarget = mComposingOnPty.substring(0, mComposingOnPty.length() - del);
            if (mListener != null && del > 0) {
                mListener.onDeleteCodePoints(del);
            }
            mComposingOnPty = newTarget;
            if (mListener != null) {
                mListener.onComposingStateChanged(newTarget);
            }
            leftLength -= del;
        } else if (mCommitWordBuffer.length() > 0) {
            mCommitWordBuffer.deleteCharAt(mCommitWordBuffer.length() - 1);
            if (mCommitWordBuffer.length() == 0) {
                mLastCommittedWordDisplay = "";
                if (mListener != null) {
                    mListener.onDeleteCodePoints(1);
                }
            } else {
                String targetWord = transformComposingText(mCommitWordBuffer.toString());
                int common = 0;
                int min = Math.min(mLastCommittedWordDisplay.length(), targetWord.length());
                while (common < min && mLastCommittedWordDisplay.charAt(common) == targetWord.charAt(common)) {
                    common++;
                }
                int backspaces = mLastCommittedWordDisplay.length() - common;
                String append = targetWord.substring(common);
                if (mListener != null) {
                    if (backspaces > 0) {
                        mListener.onDeleteCodePoints(backspaces);
                    }
                    if (!append.isEmpty()) {
                        mListener.onCommitTextRaw(append);
                    }
                }
                mLastCommittedWordDisplay = targetWord;
            }
            leftLength -= 1;
        }

        if (leftLength > 0) {
            int codePointsToDelete = 0;
            int histLen = mCommittedHistory.length();
            int charsRemaining = leftLength;

            while (charsRemaining > 0 && histLen > 0) {
                int cp = Character.codePointBefore(mCommittedHistory, histLen);
                int charCount = Character.charCount(cp);
                histLen -= charCount;
                charsRemaining -= charCount;
                codePointsToDelete++;
            }

            if (codePointsToDelete == 0) {
                codePointsToDelete = leftLength;
            }

            if (mCommittedHistory.length() > 0) {
                int delFrom = Math.max(0, mCommittedHistory.length() - leftLength);
                mCommittedHistory.delete(delFrom, mCommittedHistory.length());
            }

            if (mListener != null) {
                mListener.onDeleteCodePoints(codePointsToDelete);
            }
        }

        mBatchModified = true;
        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        logEvent("deleteSurroundingTextInCodePoints(" + beforeLength + ", " + afterLength + ")");
        if (beforeLength > 0 && mListener != null) {
            mListener.onDeleteCodePoints(beforeLength);
            if (mCommittedHistory.length() > 0) {
                int delChars = 0;
                int len = mCommittedHistory.length();
                for (int i = 0; i < beforeLength && len > 0; i++) {
                    int cp = Character.codePointBefore(mCommittedHistory, len);
                    int cc = Character.charCount(cp);
                    len -= cc;
                    delChars += cc;
                }
                mCommittedHistory.delete(Math.max(0, mCommittedHistory.length() - delChars), mCommittedHistory.length());
            }
        }
        return true;
    }

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        if (n <= 0) return "";
        CharSequence superText = null;
        try {
            superText = super.getTextBeforeCursor(n, flags);
        } catch (Throwable ignored) {}
        if (superText != null && superText.length() > 0) {
            logEvent("getTextBeforeCursor(" + n + ") [super] -> \"" + superText + "\"");
            return superText;
        }
        String combined = (mComposingOnPty.length() > 0)
                ? mCommittedHistory.toString() + mComposingOnPty
                : mCommittedHistory.toString();
        int len = combined.length();
        if (len == 0) return "";
        int start = Math.max(0, len - n);
        CharSequence res = combined.subSequence(start, len);
        logEvent("getTextBeforeCursor(" + n + ") -> \"" + res + "\"");
        return res;
    }

    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        return "";
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        return "";
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        logEvent("sendKeyEvent(action=" + event.getAction() + ", code=" + event.getKeyCode() + ")");
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                deleteSurroundingText(1, 0);
                return true;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                finishComposingText();
                mCommittedHistory.setLength(0);
                clearNativeEditable();
            }
        }
        if (mListener != null) {
            mListener.onSendKeyEvent(event);
        }
        return true;
    }

    public void resetContext() {
        mComposingOnPty = "";
        mCommitWordBuffer.setLength(0);
        mLastCommittedWordDisplay = "";
        mCommittedHistory.setLength(0);
        clearNativeEditable();
        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }
    }
}
