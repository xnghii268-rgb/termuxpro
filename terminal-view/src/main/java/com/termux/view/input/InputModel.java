package com.termux.view.input;

/**
 * Faithful pure-data mirror of Android Editable with composing span and selection.
 * Guarantees consistent state for all InputConnection read and write operations.
 * Pure JVM, zero Android dependencies, fully unit-testable.
 */
public class InputModel {

    private final StringBuilder mText = new StringBuilder();
    private int mComposingStart = -1;
    private int mComposingEnd = -1;
    private int mSelectionStart = 0;
    private int mSelectionEnd = 0;

    public synchronized void setComposingText(CharSequence text, int newCursorPosition) {
        String str = (text != null) ? text.toString() : "";
        if (mComposingStart != -1 && mComposingEnd != -1) {
            int start = Math.min(mComposingStart, mText.length());
            int end = Math.min(mComposingEnd, mText.length());
            mText.replace(start, end, str);
            mComposingStart = start;
            mComposingEnd = start + str.length();
        } else {
            int a = Math.min(mSelectionStart, mSelectionEnd);
            int b = Math.max(mSelectionStart, mSelectionEnd);
            a = Math.max(0, Math.min(a, mText.length()));
            b = Math.max(0, Math.min(b, mText.length()));
            mText.replace(a, b, str);
            mComposingStart = a;
            mComposingEnd = a + str.length();
        }

        if (newCursorPosition > 0) {
            mSelectionStart = mSelectionEnd = mComposingEnd + (newCursorPosition - 1);
        } else {
            mSelectionStart = mSelectionEnd = mComposingStart + newCursorPosition;
        }
        clampSelection();
    }

    public synchronized void setComposingRegion(int start, int end) {
        if (start < 0 || end < 0 || start == end) {
            mComposingStart = -1;
            mComposingEnd = -1;
            return;
        }
        int a = Math.max(0, Math.min(Math.min(start, end), mText.length()));
        int b = Math.max(0, Math.min(Math.max(start, end), mText.length()));
        mComposingStart = a;
        mComposingEnd = b;
    }

    public synchronized void finishComposingText() {
        mComposingStart = -1;
        mComposingEnd = -1;
        trimIfTooLong();
    }

    public synchronized void commitText(CharSequence text, int newCursorPosition) {
        String str = (text != null) ? text.toString() : "";
        int insertedEnd;
        if (mComposingStart != -1 && mComposingEnd != -1) {
            int start = Math.min(mComposingStart, mText.length());
            int end = Math.min(mComposingEnd, mText.length());
            mText.replace(start, end, str);
            insertedEnd = start + str.length();
            mComposingStart = -1;
            mComposingEnd = -1;
        } else {
            int a = Math.min(mSelectionStart, mSelectionEnd);
            int b = Math.max(mSelectionStart, mSelectionEnd);
            a = Math.max(0, Math.min(a, mText.length()));
            b = Math.max(0, Math.min(b, mText.length()));
            mText.replace(a, b, str);
            insertedEnd = a + str.length();
        }

        if (newCursorPosition > 0) {
            mSelectionStart = mSelectionEnd = insertedEnd + (newCursorPosition - 1);
        } else {
            mSelectionStart = mSelectionEnd = (insertedEnd - str.length()) + newCursorPosition;
        }
        clampSelection();
        trimIfTooLong();
    }

    public synchronized int deleteSurroundingText(int leftLength, int rightLength) {
        if (leftLength <= 0 && rightLength <= 0) return 0;
        int selStart = Math.min(mSelectionStart, mSelectionEnd);
        int selEnd = Math.max(mSelectionStart, mSelectionEnd);
        int delStart = Math.max(0, selStart - leftLength);
        int delEnd = Math.min(mText.length(), selEnd + rightLength);
        int deletedChars = 0;

        if (delEnd > delStart) {
            deletedChars = delEnd - delStart;
            mText.delete(delStart, delEnd);
            mSelectionStart = mSelectionEnd = delStart;
            if (mComposingStart != -1) {
                if (delStart <= mComposingStart) {
                    mComposingStart = -1;
                    mComposingEnd = -1;
                } else {
                    mComposingEnd = Math.max(mComposingStart, mComposingEnd - deletedChars);
                    if (mComposingEnd == mComposingStart) {
                        mComposingStart = -1;
                        mComposingEnd = -1;
                    }
                }
            }
        }
        return deletedChars;
    }

    public synchronized int deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        int selStart = Math.min(mSelectionStart, mSelectionEnd);
        int leftChars = 0;
        int curr = selStart;
        for (int i = 0; i < beforeLength && curr > 0; i++) {
            int cp = Character.codePointBefore(mText, curr);
            int cc = Character.charCount(cp);
            curr -= cc;
            leftChars += cc;
        }

        int selEnd = Math.max(mSelectionStart, mSelectionEnd);
        int rightChars = 0;
        curr = selEnd;
        for (int i = 0; i < afterLength && curr < mText.length(); i++) {
            int cp = Character.codePointAt(mText, curr);
            int cc = Character.charCount(cp);
            curr += cc;
            rightChars += cc;
        }

        return deleteSurroundingText(leftChars, rightChars);
    }

    public synchronized void setSelection(int start, int end) {
        mSelectionStart = start;
        mSelectionEnd = end;
        clampSelection();
    }

    public synchronized CharSequence getTextBeforeCursor(int n, int flags) {
        if (n <= 0) return "";
        int pos = Math.min(mSelectionStart, mSelectionEnd);
        pos = Math.max(0, Math.min(pos, mText.length()));
        int start = Math.max(0, pos - n);
        return mText.substring(start, pos);
    }

    public synchronized CharSequence getTextAfterCursor(int n, int flags) {
        if (n <= 0) return "";
        int pos = Math.max(mSelectionStart, mSelectionEnd);
        pos = Math.max(0, Math.min(pos, mText.length()));
        int end = Math.min(mText.length(), pos + n);
        return mText.substring(pos, end);
    }

    public synchronized CharSequence getSelectedText(int flags) {
        int a = Math.min(mSelectionStart, mSelectionEnd);
        int b = Math.max(mSelectionStart, mSelectionEnd);
        if (a == b) return "";
        a = Math.max(0, Math.min(a, mText.length()));
        b = Math.max(0, Math.min(b, mText.length()));
        return mText.substring(a, b);
    }

    public synchronized String getComposingText() {
        if (mComposingStart != -1 && mComposingEnd != -1 && mComposingEnd <= mText.length()) {
            return mText.substring(mComposingStart, mComposingEnd);
        }
        return "";
    }

    public synchronized boolean isComposing() {
        return mComposingStart != -1 && mComposingEnd >= mComposingStart;
    }

    public synchronized void clear() {
        mText.setLength(0);
        mComposingStart = -1;
        mComposingEnd = -1;
        mSelectionStart = 0;
        mSelectionEnd = 0;
    }

    public synchronized int length() {
        return mText.length();
    }

    public synchronized int getSelectionStart() {
        return mSelectionStart;
    }

    public synchronized int getSelectionEnd() {
        return mSelectionEnd;
    }

    public synchronized int getComposingStart() {
        return mComposingStart;
    }

    public synchronized int getComposingEnd() {
        return mComposingEnd;
    }

    @Override
    public synchronized String toString() {
        return mText.toString();
    }

    private void clampSelection() {
        int len = mText.length();
        mSelectionStart = Math.max(0, Math.min(mSelectionStart, len));
        mSelectionEnd = Math.max(0, Math.min(mSelectionEnd, len));
    }

    private void trimIfTooLong() {
        if (!isComposing() && mText.length() > 2000) {
            int deleteCount = mText.length() - 1000;
            mText.delete(0, deleteCount);
            mSelectionStart = Math.max(0, mSelectionStart - deleteCount);
            mSelectionEnd = Math.max(0, mSelectionEnd - deleteCount);
        }
    }
}
