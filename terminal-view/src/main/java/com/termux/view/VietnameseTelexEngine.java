package com.termux.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight in-terminal Vietnamese Telex input engine for Termux Pro.
 * Transforms raw keystrokes (e.g. "as" -> "á", "dd" -> "đ") seamlessly in real-time.
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

    private static final Map<String, String[]> VOWEL_TABLE = new HashMap<>();
    private static final Map<Character, VowelInfo> CHAR_TO_VOWEL = new HashMap<>();

    private static class VowelInfo {
        final String base;
        final int tone;
        final boolean isUpper;

        VowelInfo(String base, int tone, boolean isUpper) {
            this.base = base;
            this.tone = tone;
            this.isUpper = isUpper;
        }
    }

    static {
        registerVowels("a", new String[]{"a", "á", "à", "ả", "ã", "ạ"});
        registerVowels("â", new String[]{"â", "ấ", "ầ", "ẩ", "ẫ", "ậ"});
        registerVowels("ă", new String[]{"ă", "ắ", "ằ", "ẳ", "ẵ", "ặ"});
        registerVowels("e", new String[]{"e", "é", "è", "ẻ", "ẽ", "ẹ"});
        registerVowels("ê", new String[]{"ê", "ế", "ề", "ể", "ễ", "ệ"});
        registerVowels("i", new String[]{"i", "í", "ì", "ỉ", "ĩ", "ị"});
        registerVowels("o", new String[]{"o", "ó", "ò", "ỏ", "õ", "ọ"});
        registerVowels("ô", new String[]{"ô", "ố", "ồ", "ổ", "ỗ", "ộ"});
        registerVowels("ơ", new String[]{"ơ", "ớ", "ờ", "ở", "ỡ", "ợ"});
        registerVowels("u", new String[]{"u", "ú", "ù", "ủ", "ũ", "ụ"});
        registerVowels("ư", new String[]{"ư", "ứ", "ừ", "ử", "ữ", "ự"});
        registerVowels("y", new String[]{"y", "ý", "ỳ", "ỷ", "ỹ", "ỵ"});
    }

    private static void registerVowels(String base, String[] variants) {
        VOWEL_TABLE.put(base, variants);
        for (int tone = 0; tone < variants.length; tone++) {
            char lower = variants[tone].charAt(0);
            char upper = Character.toUpperCase(lower);
            CHAR_TO_VOWEL.put(lower, new VowelInfo(base, tone, false));
            CHAR_TO_VOWEL.put(upper, new VowelInfo(base, tone, true));
        }
    }

    private boolean mEnabled = true;
    private final StringBuilder mWordBuffer = new StringBuilder();
    private char mLastTransformTrigger = 0;
    private String mLastPreTransform = null;
    private int mLastTransLen = 0;

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (!enabled) reset();
    }

    public synchronized void reset() {
        mWordBuffer.setLength(0);
        mLastTransformTrigger = 0;
        mLastPreTransform = null;
        mLastTransLen = 0;
    }

    public synchronized void handleBackspace() {
        if (mWordBuffer.length() > 0) {
            mWordBuffer.deleteCharAt(mWordBuffer.length() - 1);
        }
        mLastTransformTrigger = 0;
        mLastPreTransform = null;
        mLastTransLen = 0;
    }

    public synchronized TransformResult processChar(char c) {
        if (!mEnabled) return null;

        if (!Character.isLetter(c)) {
            reset();
            return null;
        }

        // 1. Undo on repeated trigger key (e.g. "á" + 's' -> "as", "đ" + 'd' -> "dd")
        if (mLastTransformTrigger != 0 && mLastTransformTrigger == c && mLastPreTransform != null) {
            String revert = mLastPreTransform + c;
            int bs = mLastTransLen;
            mWordBuffer.setLength(0);
            mWordBuffer.append(revert);
            mLastTransformTrigger = 0;
            mLastPreTransform = null;
            mLastTransLen = 0;
            return new TransformResult(bs, revert);
        }

        // 2. Check vowel / roof / horn modifiers (aa, aw, ee, oo, ow, uw, dd, uow)
        String modWord = applyModifier(mWordBuffer.toString(), c);
        if (modWord != null && !modWord.equals(mWordBuffer.toString())) {
            return applyChange(modWord, c);
        }

        // 3. Check tone marks (s, f, r, x, j, z)
        String toneWord = applyTone(mWordBuffer.toString(), c);
        if (toneWord != null && !toneWord.equals(mWordBuffer.toString())) {
            return applyChange(toneWord, c);
        }

        // 4. Normal character
        mWordBuffer.append(c);
        mLastTransformTrigger = 0;
        mLastPreTransform = null;
        mLastTransLen = 0;
        return null;
    }

    private TransformResult applyChange(String newWord, char trigger) {
        String oldWord = mWordBuffer.toString();
        int prefixLen = 0;
        int minLen = Math.min(oldWord.length(), newWord.length());
        while (prefixLen < minLen && oldWord.charAt(prefixLen) == newWord.charAt(prefixLen)) {
            prefixLen++;
        }

        int bs = oldWord.length() - prefixLen;
        String rep = newWord.substring(prefixLen);

        mLastPreTransform = oldWord;
        mLastTransformTrigger = trigger;
        mLastTransLen = rep.length();

        mWordBuffer.setLength(0);
        mWordBuffer.append(newWord);

        return new TransformResult(bs, rep);
    }

    private String applyModifier(String word, char mk) {
        if (word.isEmpty()) return null;
        char last = word.charAt(word.length() - 1);
        char mkl = Character.toLowerCase(mk);

        // dd -> đ, Dd/DD -> Đ
        if ((last == 'd' || last == 'D') && mkl == 'd') {
            return word.substring(0, word.length() - 1) + (Character.isUpperCase(last) ? "Đ" : "đ");
        }

        // uo + w -> ươ
        if (word.length() >= 2 && mkl == 'w') {
            char penult = word.charAt(word.length() - 2);
            VowelInfo v1 = CHAR_TO_VOWEL.get(penult);
            VowelInfo v2 = CHAR_TO_VOWEL.get(last);
            if (v1 != null && v2 != null && "u".equals(v1.base) && "o".equals(v2.base)) {
                String c1 = VOWEL_TABLE.get("ư")[v1.tone];
                String c2 = VOWEL_TABLE.get("ơ")[v2.tone];
                if (v1.isUpper) c1 = c1.toUpperCase();
                if (v2.isUpper) c2 = c2.toUpperCase();
                return word.substring(0, word.length() - 2) + c1 + c2;
            }
        }

        VowelInfo vi = CHAR_TO_VOWEL.get(last);
        if (vi != null) {
            String newBase = null;
            if ("a".equals(vi.base) && mkl == 'a') newBase = "â";
            else if ("a".equals(vi.base) && mkl == 'w') newBase = "ă";
            else if ("e".equals(vi.base) && mkl == 'e') newBase = "ê";
            else if ("o".equals(vi.base) && mkl == 'o') newBase = "ô";
            else if ("o".equals(vi.base) && mkl == 'w') newBase = "ơ";
            else if ("u".equals(vi.base) && mkl == 'w') newBase = "ư";

            if (newBase != null) {
                String newChar = VOWEL_TABLE.get(newBase)[vi.tone];
                if (vi.isUpper) newChar = newChar.toUpperCase();
                return word.substring(0, word.length() - 1) + newChar;
            }
        }

        return null;
    }

    private String applyTone(String word, char tk) {
        int t = -1;
        char tkl = Character.toLowerCase(tk);
        switch (tkl) {
            case 's': t = 1; break; // Sắc
            case 'f': t = 2; break; // Huyền
            case 'r': t = 3; break; // Hỏi
            case 'x': t = 4; break; // Ngã
            case 'j': t = 5; break; // Nặng
            case 'z': t = 0; break; // Xóa dấu
            default: return null;
        }

        int[] vPos = new int[word.length()];
        int vCount = 0;
        for (int i = 0; i < word.length(); i++) {
            if (CHAR_TO_VOWEL.containsKey(word.charAt(i))) {
                vPos[vCount++] = i;
            }
        }

        if (vCount == 0) return null;

        int target;
        if (vCount == 1) {
            target = vPos[0];
        } else if (vCount == 2) {
            boolean hasEndConsonant = vPos[1] < word.length() - 1;
            VowelInfo vi1 = CHAR_TO_VOWEL.get(word.charAt(vPos[0]));
            VowelInfo vi2 = CHAR_TO_VOWEL.get(word.charAt(vPos[1]));
            String pair = (vi1 != null && vi2 != null) ? (vi1.base + vi2.base) : "";

            if ("oa".equals(pair) || "oe".equals(pair) || "uy".equals(pair)) {
                target = vPos[1];
            } else if ("ia".equals(pair) || "ya".equals(pair) || "ua".equals(pair)) {
                target = vPos[0];
            } else if ("iê".equals(pair) || "yê".equals(pair) || "uô".equals(pair) || "ươ".equals(pair)) {
                target = vPos[1];
            } else if (hasEndConsonant) {
                target = vPos[1];
            } else {
                target = vPos[0];
            }
        } else {
            target = vPos[1]; // middle vowel
        }

        VowelInfo vi = CHAR_TO_VOWEL.get(word.charAt(target));
        if (vi == null) return null;

        String newChar = VOWEL_TABLE.get(vi.base)[t];
        if (vi.isUpper) newChar = newChar.toUpperCase();

        return word.substring(0, target) + newChar + word.substring(target + 1);
    }
}
