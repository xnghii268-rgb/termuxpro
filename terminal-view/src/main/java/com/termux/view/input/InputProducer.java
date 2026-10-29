package com.termux.view.input;

/**
 * Producer that determines the target display text T for the active word region.
 * Automatically classifies active word as RAW (engine transforms raw ASCII keystrokes)
 * or COMPOSER (external IME composes; engine is turned OFF to prevent double transformation).
 */
public class InputProducer {

    public enum Mode {
        RAW,
        COMPOSER
    }

    public interface BypassChecker {
        boolean isBypassed();
        boolean isControlOrAltActive();
    }

    private Mode mCurrentMode = Mode.RAW;
    private final StringBuilder mRawWordBuffer = new StringBuilder();
    private String mPreviousImeText = "";
    private VietnameseEngine mEngine;
    private BypassChecker mBypassChecker;

    public InputProducer(VietnameseEngine engine, BypassChecker bypassChecker) {
        this.mEngine = engine;
        this.mBypassChecker = bypassChecker;
    }

    public void setVietnameseEngine(VietnameseEngine engine) {
        this.mEngine = engine;
    }

    public VietnameseEngine getVietnameseEngine() {
        return mEngine;
    }

    public void setBypassChecker(BypassChecker bypassChecker) {
        this.mBypassChecker = bypassChecker;
    }

    public synchronized Mode getMode() {
        return mCurrentMode;
    }

    public synchronized String getRawBuffer() {
        return mRawWordBuffer.toString();
    }

    /**
     * Computes target text T for a composing update from IME.
     */
    public synchronized String onComposingText(String imeText) {
        if (imeText == null) imeText = "";

        if (mBypassChecker != null && (mBypassChecker.isBypassed() || mBypassChecker.isControlOrAltActive())) {
            mCurrentMode = Mode.RAW;
            mRawWordBuffer.setLength(0);
            mPreviousImeText = imeText;
            return imeText;
        }

        if (imeText.isEmpty()) {
            resetWord();
            return "";
        }

        boolean isAscii = isPureAscii(imeText);
        boolean isAppend = (mPreviousImeText.isEmpty() && imeText.length() == 1)
                || (imeText.startsWith(mPreviousImeText) && imeText.length() == mPreviousImeText.length() + 1);

        if (isAscii && isAppend && mCurrentMode == Mode.RAW) {
            mCurrentMode = Mode.RAW;
            char c = imeText.charAt(imeText.length() - 1);
            mRawWordBuffer.append(c);
            mPreviousImeText = imeText;
            if (mEngine != null && mEngine.isEnabled()) {
                VietnameseEngine.Result res = VietnameseEngine.process(mRawWordBuffer.toString(), mEngine.getOptions());
                return res.text;
            } else {
                return mRawWordBuffer.toString();
            }
        } else {
            // IME performed composition (non-ASCII or rewrite): switch to COMPOSER
            mCurrentMode = Mode.COMPOSER;
            mRawWordBuffer.setLength(0);
            mPreviousImeText = imeText;
            // Engine is OFF for COMPOSER word
            return imeText;
        }
    }

    /**
     * Computes target text T for a direct commit character / word.
     */
    public synchronized String onDirectCommit(String committedText) {
        if (committedText == null || committedText.isEmpty()) return "";

        if (mBypassChecker != null && (mBypassChecker.isBypassed() || mBypassChecker.isControlOrAltActive())) {
            resetWord();
            return committedText;
        }

        boolean isSingleAsciiLetter = (committedText.length() == 1 &&
                committedText.charAt(0) <= 127 &&
                Character.isLetter(committedText.charAt(0)));

        if (isSingleAsciiLetter && mEngine != null && mEngine.isEnabled() && mCurrentMode == Mode.RAW) {
            mRawWordBuffer.append(committedText.charAt(0));
            mPreviousImeText = mRawWordBuffer.toString();
            if (mEngine.isPerWordOverrideRaw()) {
                return mRawWordBuffer.toString();
            }
            VietnameseEngine.Result res = VietnameseEngine.process(mRawWordBuffer.toString(), mEngine.getOptions());
            return res.text;
        } else {
            // Word boundary, symbol, space, or precomposed text commit
            resetWord();
            return committedText;
        }
    }

    /**
     * Handles backspace within a RAW word.
     */
    public synchronized String onBackspace() {
        if (mCurrentMode == Mode.RAW && mRawWordBuffer.length() > 0) {
            mRawWordBuffer.deleteCharAt(mRawWordBuffer.length() - 1);
            mPreviousImeText = mRawWordBuffer.toString();
            if (mRawWordBuffer.length() == 0) {
                return "";
            }
            if (mEngine != null && mEngine.isEnabled()) {
                if (mEngine.isPerWordOverrideRaw()) {
                    return mRawWordBuffer.toString();
                }
                VietnameseEngine.Result res = VietnameseEngine.process(mRawWordBuffer.toString(), mEngine.getOptions());
                return res.text;
            } else {
                return mRawWordBuffer.toString();
            }
        }
        return null;
    }

    public synchronized boolean hasActiveWord() {
        return mRawWordBuffer.length() > 0;
    }

    public synchronized String togglePerWordOverride() {
        if (mEngine == null || mRawWordBuffer.length() == 0) return null;
        VietnameseEngine.TransformResult tr = mEngine.togglePerWordOverride();
        if (tr != null) {
            if (mEngine.isPerWordOverrideRaw()) {
                return mRawWordBuffer.toString();
            } else {
                VietnameseEngine.Result res = VietnameseEngine.process(mRawWordBuffer.toString(), mEngine.getOptions());
                return res.text;
            }
        }
        return null;
    }

    public synchronized void resetWord() {
        mCurrentMode = Mode.RAW;
        mRawWordBuffer.setLength(0);
        mPreviousImeText = "";
    }

    private static boolean isPureAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return false;
        }
        return true;
    }
}
