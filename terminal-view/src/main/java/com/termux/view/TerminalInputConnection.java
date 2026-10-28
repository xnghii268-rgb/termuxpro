package com.termux.view;

import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, root-cause Vietnamese Telex/VNI InputConnection for Termux Pro.
 * Preserves full composing buffers, mirrors line context for IME word rewrites,
 * guarantees NFC Unicode normalization, and sends code-point-accurate backspaces.
 */
public class TerminalInputConnection extends BaseInputConnection {

    public interface TerminalInputListener {
        void onCommitText(String text);
        void onDeleteCodePoints(int codePoints);
        void onSendKeyEvent(KeyEvent event);
        void onComposingStateChanged(String composingText);
    }

    private final TerminalInputListener mListener;
    private final SpannableStringBuilder mComposingBuffer = new SpannableStringBuilder();
    private final SpannableStringBuilder mMirroredText = new SpannableStringBuilder();
    private int mBatchDepth = 0;
    private boolean mBatchModified = false;
    private static boolean sDebugLogEnabled = false;
    private static final List<String> sDebugLog = new ArrayList<>();

    public TerminalInputConnection(View targetView, TerminalInputListener listener) {
        super(targetView, true);
        this.mListener = listener;
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

    private String mActiveImeId = "unknown";

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

    @Override
    public Editable getEditable() {
        return mMirroredText;
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

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        String str = (text != null) ? text.toString() : "";
        logEvent("setComposingText(\"" + str + "\", " + newCursorPosition + ")");

        mComposingBuffer.clear();
        if (!str.isEmpty()) {
            mComposingBuffer.append(str);
        }

        if (mListener != null) {
            mListener.onComposingStateChanged(str);
        }

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
        if (mComposingBuffer.length() > 0) {
            commitText(mComposingBuffer.toString(), 1);
        }
        mComposingBuffer.clear();
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

        mComposingBuffer.clear();
        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }

        // Canonical Decomposition & Re-composition into NFC (single-cell Vietnamese glyphs)
        String nfcStr = Normalizer.normalize(rawStr, Normalizer.Form.NFC);

        // Update mirrored context model
        mMirroredText.append(nfcStr);
        if (mMirroredText.length() > 2000) {
            mMirroredText.delete(0, mMirroredText.length() - 1000);
        }
        Selection.setSelection(mMirroredText, mMirroredText.length());

        if (mListener != null && !nfcStr.isEmpty()) {
            mListener.onCommitText(nfcStr);
        }

        mBatchModified = true;
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        logEvent("deleteSurroundingText(" + leftLength + ", " + rightLength + ")");
        if (leftLength <= 0 && rightLength <= 0) return true;

        if (mComposingBuffer.length() > 0) {
            int deleteCount = Math.min(leftLength, mComposingBuffer.length());
            mComposingBuffer.delete(mComposingBuffer.length() - deleteCount, mComposingBuffer.length());
            if (mListener != null) {
                mListener.onComposingStateChanged(mComposingBuffer.toString());
            }
            leftLength -= deleteCount;
        }

        if (leftLength > 0) {
            int codePointsToDelete = 0;
            int mirrorLen = mMirroredText.length();
            int charsRemaining = leftLength;

            while (charsRemaining > 0 && mirrorLen > 0) {
                int cp = Character.codePointBefore(mMirroredText, mirrorLen);
                int charCount = Character.charCount(cp);
                mirrorLen -= charCount;
                charsRemaining -= charCount;
                codePointsToDelete++;
            }

            if (codePointsToDelete == 0) {
                codePointsToDelete = leftLength;
            }

            if (mMirroredText.length() > 0) {
                int delFrom = Math.max(0, mMirroredText.length() - leftLength);
                mMirroredText.delete(delFrom, mMirroredText.length());
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
            if (mMirroredText.length() > 0) {
                int delChars = 0;
                int len = mMirroredText.length();
                for (int i = 0; i < beforeLength && len > 0; i++) {
                    int cp = Character.codePointBefore(mMirroredText, len);
                    int cc = Character.charCount(cp);
                    len -= cc;
                    delChars += cc;
                }
                mMirroredText.delete(Math.max(0, mMirroredText.length() - delChars), mMirroredText.length());
            }
        }
        return true;
    }

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        int len = mMirroredText.length();
        if (len == 0) return "";
        int start = Math.max(0, len - n);
        CharSequence res = mMirroredText.subSequence(start, len);
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
                mMirroredText.clear();
            }
        }
        if (mListener != null) {
            mListener.onSendKeyEvent(event);
        }
        return true;
    }

    public void resetContext() {
        mComposingBuffer.clear();
        mMirroredText.clear();
        if (mListener != null) {
            mListener.onComposingStateChanged("");
        }
    }
}
