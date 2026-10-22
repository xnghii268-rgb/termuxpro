package com.termux.view;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight, smart in-terminal Vietnamese Telex input engine for Termux Pro.
 * Transforms raw keystrokes (e.g. "as" -> "á", "dd" -> "đ", "ow" -> "ơ") seamlessly in real-time
 * while preserving English commands, linux paths, and keywords (e.g. "down", "downloads", "words",
 * "status", "cards", "clear", "add") without corruption, duplicate characters, or unwanted accents.
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

    // Complete Vietnamese diphthongs and triphthongs
    private static final Set<String> VALID_DIPHTHONGS = new HashSet<>(Arrays.asList(
        // Diphthongs
        "ia", "iê", "ya", "yê", "ua", "uô", "ưa", "ươ", "ai", "ao", "au", "ay", "âu", "ây",
        "eo", "êu", "iu", "oi", "ôi", "ơi", "ui", "ưi", "uy", "uơ", "ưu",
        "oa", "oe", "ie", "ye", "uo", "uâ", "oă", "uê",
        // Triphthongs
        "iêu", "yêu", "oai", "oao", "oay", "oeo", "uao", "uay", "uây", "uôi", "ươi", "ươu", "uya", "uyê", "uyu"
    ));

    private static final Set<String> VALID_ENDINGS = new HashSet<>(Arrays.asList(
        "", "c", "ch", "m", "n", "ng", "nh", "p", "t"
    ));

    // Common terminal commands and English keywords that should never be corrupted by Telex
    private static final Set<String> COMMON_ENGLISH_WORDS = new HashSet<>(Arrays.asList(
        "down", "downloads", "download", "words", "cards", "hosts", "diff", "user", "users",
        "sudo", "docker", "chmod", "chown", "curl", "wget", "find", "make", "bash", "nano",
        "install", "reset", "exit", "echo", "true", "false", "null", "undefined",
        "import", "export", "return", "switch", "case", "default", "add", "git", "status"
    ));

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
    private final StringBuilder mRawBuffer = new StringBuilder();
    private char mLastTransformTrigger = 0;
    private String mLastPreTransform = null;

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (!enabled) reset();
    }

    public synchronized void reset() {
        mWordBuffer.setLength(0);
        mRawBuffer.setLength(0);
        mLastTransformTrigger = 0;
        mLastPreTransform = null;
    }

    public synchronized void handleBackspace() {
        if (mWordBuffer.length() > 0) {
            mWordBuffer.deleteCharAt(mWordBuffer.length() - 1);
        }
        if (mRawBuffer.length() > 0) {
            mRawBuffer.deleteCharAt(mRawBuffer.length() - 1);
        }
        mLastTransformTrigger = 0;
        mLastPreTransform = null;
    }

    /**
     * Checks if a word starts with an English/foreign initial consonant cluster
     * that never exists in Vietnamese phonetics (e.g. st, sp, cl, cr, gr, pr, etc.)
     */
    private static boolean startsWithForeignConsonantCluster(String lower) {
        if (lower.length() < 2) return false;
        if (lower.startsWith("str") || lower.startsWith("spr") || lower.startsWith("scr")) return true;
        String c2 = lower.substring(0, 2);
        return c2.equals("st") || c2.equals("sp") || c2.equals("sc") || c2.equals("sk")
            || c2.equals("sl") || c2.equals("sm") || c2.equals("sn") || c2.equals("sw")
            || c2.equals("cl") || c2.equals("cr") || c2.equals("bl") || c2.equals("br")
            || c2.equals("fl") || c2.equals("fr") || c2.equals("gl") || c2.equals("gr")
            || c2.equals("pl") || c2.equals("pr") || c2.equals("dr") || c2.equals("kl")
            || c2.equals("sh") || c2.equals("wh") || c2.equals("wr");
    }

    public synchronized TransformResult processChar(char c) {
        if (!mEnabled) return null;

        if (!Character.isLetter(c)) {
            // Typing non-letter (space, punctuation, slash): check if raw buffer is a known English word
            if (!mWordBuffer.toString().equals(mRawBuffer.toString())) {
                String rawLower = mRawBuffer.toString().toLowerCase();
                if (COMMON_ENGLISH_WORDS.contains(rawLower)) {
                    int bs = mWordBuffer.length();
                    String rep = mRawBuffer.toString();
                    reset();
                    return new TransformResult(bs, rep);
                }
            }
            reset();
            return null;
        }

        // 1. Undo on repeated trigger key (e.g. "á" + 's' -> "as", "đ" + 'd' -> "dd", "dơ" + 'w' -> "dow")
        if (mLastTransformTrigger != 0 && mLastTransformTrigger == c && mLastPreTransform != null) {
            String currentWord = mWordBuffer.toString();
            String targetWord = mLastPreTransform + c;
            int prefixLen = 0;
            int minLen = Math.min(currentWord.length(), targetWord.length());
            while (prefixLen < minLen && currentWord.charAt(prefixLen) == targetWord.charAt(prefixLen)) {
                prefixLen++;
            }
            int bs = currentWord.length() - prefixLen;
            String rep = targetWord.substring(prefixLen);

            mWordBuffer.setLength(0);
            mWordBuffer.append(targetWord);
            mRawBuffer.setLength(0);
            mRawBuffer.append(targetWord); // CRITICAL FIX: Keep raw buffer in exact sync with target word
            mLastTransformTrigger = 0;
            mLastPreTransform = null;
            return new TransformResult(bs, rep);
        }

        // Bypass Vietnamese transform for words starting with foreign consonant clusters (e.g. status, clear, grep)
        String tentativeRawCheck = (mRawBuffer.toString() + c).toLowerCase();
        if (startsWithForeignConsonantCluster(tentativeRawCheck)) {
            mWordBuffer.append(c);
            mRawBuffer.append(c);
            mLastTransformTrigger = 0;
            mLastPreTransform = null;
            return null;
        }

        // 2. Check vowel / roof / horn modifiers (aa, aw, ee, oo, ow, uw, dd, uow)
        String modWord = applyModifier(mWordBuffer.toString(), c);
        if (modWord != null && !modWord.equals(mWordBuffer.toString())) {
            mRawBuffer.append(c);
            return applyChange(modWord, c);
        }

        // 3. Check tone marks (s, f, r, x, j, z)
        String toneWord = applyTone(mWordBuffer.toString(), c);
        if (toneWord != null && !toneWord.equals(mWordBuffer.toString())) {
            mRawBuffer.append(c);
            return applyChange(toneWord, c);
        }

        // 4. Auto-restore raw English keystrokes if typing this char invalidates Vietnamese phonetics or matches known words
        String tentativeWord = mWordBuffer.toString() + c;
        String tentativeRaw = mRawBuffer.toString() + c;
        if (!mWordBuffer.toString().equals(mRawBuffer.toString())) {
            // Immediate match for common English words (e.g. "down", "downloads", "words")
            if (COMMON_ENGLISH_WORDS.contains(tentativeRaw.toLowerCase())) {
                int bs = mWordBuffer.length();
                mWordBuffer.setLength(0);
                mWordBuffer.append(tentativeRaw);
                mRawBuffer.setLength(0);
                mRawBuffer.append(tentativeRaw);
                mLastTransformTrigger = 0;
                mLastPreTransform = null;
                return new TransformResult(bs, tentativeRaw);
            }

            int firstVowel = -1;
            int lastVowel = -1;
            int vCount = 0;
            for (int i = 0; i < tentativeWord.length(); i++) {
                if (CHAR_TO_VOWEL.containsKey(tentativeWord.charAt(i))) {
                    if (firstVowel == -1) firstVowel = i;
                    lastVowel = i;
                    vCount++;
                }
            }
            if (vCount > 0) {
                String ending = tentativeWord.substring(lastVowel + 1).toLowerCase();
                if (!VALID_ENDINGS.contains(ending) && !"g".equals(ending) && !"h".equals(ending)) {
                    // Restore raw English keystrokes
                    int bs = mWordBuffer.length();
                    mWordBuffer.setLength(0);
                    mWordBuffer.append(tentativeRaw);
                    mRawBuffer.setLength(0);
                    mRawBuffer.append(tentativeRaw);
                    mLastTransformTrigger = 0;
                    mLastPreTransform = null;
                    return new TransformResult(bs, tentativeRaw);
                }
            }
        }

        // 5. Normal character
        mWordBuffer.append(c);
        mRawBuffer.append(c);
        mLastTransformTrigger = 0;
        mLastPreTransform = null;
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

        mWordBuffer.setLength(0);
        mWordBuffer.append(newWord);

        return new TransformResult(bs, rep);
    }

    private String applyModifier(String word, char mk) {
        if (word.isEmpty()) return null;
        char last = word.charAt(word.length() - 1);
        char mkl = Character.toLowerCase(mk);

        // dd -> đ, Dd/DD -> Đ (ONLY if no vowel exists yet in the word)
        if ((last == 'd' || last == 'D') && mkl == 'd') {
            boolean hasVowel = false;
            for (int i = 0; i < word.length(); i++) {
                if (CHAR_TO_VOWEL.containsKey(word.charAt(i))) {
                    hasVowel = true;
                    break;
                }
            }
            if (!hasVowel) {
                return word.substring(0, word.length() - 1) + (Character.isUpperCase(last) ? "Đ" : "đ");
            }
            return null;
        }

        // đ + d -> d, Đ + d -> D (toggle back)
        if ((last == 'đ' || last == 'Đ') && mkl == 'd') {
            return word.substring(0, word.length() - 1) + (Character.isUpperCase(last) ? "D" : "d");
        }

        // Ensure vowels in the word are contiguous before applying vowel modifiers
        int firstVowel = -1;
        int lastVowel = -1;
        int vCount = 0;
        for (int i = 0; i < word.length(); i++) {
            if (CHAR_TO_VOWEL.containsKey(word.charAt(i))) {
                if (firstVowel == -1) firstVowel = i;
                lastVowel = i;
                vCount++;
            }
        }
        if (vCount > 0 && (lastVowel - firstVowel + 1 != vCount)) {
            return null; // Consonants between vowels -> foreign word
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
            // ươ + w -> uo (toggle back)
            if (v1 != null && v2 != null && "ư".equals(v1.base) && "ơ".equals(v2.base)) {
                String c1 = VOWEL_TABLE.get("u")[v1.tone];
                String c2 = VOWEL_TABLE.get("o")[v2.tone];
                if (v1.isUpper) c1 = c1.toUpperCase();
                if (v2.isUpper) c2 = c2.toUpperCase();
                return word.substring(0, word.length() - 2) + c1 + c2;
            }
        }

        VowelInfo vi = CHAR_TO_VOWEL.get(last);
        if (vi != null) {
            String newBase = null;
            // Transforming base vowel to accented vowel
            if ("a".equals(vi.base) && mkl == 'a') newBase = "â";
            else if ("a".equals(vi.base) && mkl == 'w') newBase = "ă";
            else if ("e".equals(vi.base) && mkl == 'e') newBase = "ê";
            else if ("o".equals(vi.base) && mkl == 'o') newBase = "ô";
            else if ("o".equals(vi.base) && mkl == 'w') newBase = "ơ";
            else if ("u".equals(vi.base) && mkl == 'w') newBase = "ư";
            // Toggling back (undoing modifier)
            else if ("â".equals(vi.base) && mkl == 'a') newBase = "a";
            else if ("ă".equals(vi.base) && mkl == 'w') newBase = "a";
            else if ("ê".equals(vi.base) && mkl == 'e') newBase = "e";
            else if ("ô".equals(vi.base) && mkl == 'o') newBase = "o";
            else if ("ơ".equals(vi.base) && mkl == 'w') newBase = "o";
            else if ("ư".equals(vi.base) && mkl == 'w') newBase = "u";

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

        int firstVowel = -1;
        int lastVowel = -1;
        int vCount = 0;
        int[] vPos = new int[word.length()];
        for (int i = 0; i < word.length(); i++) {
            if (CHAR_TO_VOWEL.containsKey(word.charAt(i))) {
                if (firstVowel == -1) firstVowel = i;
                lastVowel = i;
                vPos[vCount++] = i;
            }
        }

        if (vCount == 0) return null;

        // Vowels must be contiguous
        if (lastVowel - firstVowel + 1 != vCount) return null;

        // Validate vowel group against Vietnamese diphthongs/triphthongs (rejects "ea", "ae", "ou", etc.)
        if (vCount > 1) {
            StringBuilder baseGroup = new StringBuilder();
            for (int i = firstVowel; i <= lastVowel; i++) {
                VowelInfo vi = CHAR_TO_VOWEL.get(word.charAt(i));
                if (vi != null) baseGroup.append(vi.base);
            }
            if (!VALID_DIPHTHONGS.contains(baseGroup.toString())) {
                return null;
            }
        }

        // Validate final consonants
        String ending = word.substring(lastVowel + 1).toLowerCase();
        if (!VALID_ENDINGS.contains(ending) && !"g".equals(ending) && !"h".equals(ending)) {
            return null;
        }

        // Check if word already has a tone mark
        int currentTone = 0;
        for (int i = firstVowel; i <= lastVowel; i++) {
            VowelInfo vi = CHAR_TO_VOWEL.get(word.charAt(i));
            if (vi != null && vi.tone != 0) {
                currentTone = vi.tone;
                break;
            }
        }

        int targetTone = t;
        if (currentTone != 0) {
            if (targetTone == currentTone || targetTone == 0) {
                // Same tone or 'z': remove tone
                targetTone = 0;
            }
            // else: different tone -> change tone!
        } else {
            if (targetTone == 0) {
                return null; // No tone to remove
            }
        }

        int target;
        if (vCount == 1) {
            target = vPos[0];
        } else if (vCount == 2) {
            boolean hasEndConsonant = vPos[1] < word.length() - 1;
            VowelInfo vi1 = CHAR_TO_VOWEL.get(word.charAt(vPos[0]));
            VowelInfo vi2 = CHAR_TO_VOWEL.get(word.charAt(vPos[1]));
            String pair = (vi1 != null && vi2 != null) ? (vi1.base + vi2.base) : "";

            if (hasEndConsonant) {
                // Words with ending consonants: tone is placed on the second vowel (toán, luật, hoặc, tiến, muốn)
                target = vPos[1];
            } else if ("oa".equals(pair) || "oe".equals(pair) || "uy".equals(pair) || "uê".equals(pair) || "uơ".equals(pair)) {
                // Open diphthongs starting with u/o: tone on the second vowel (hóa, hòe, thúy, thuế, thuở)
                target = vPos[1];
            } else {
                // Open diphthongs (ia, ua, ưa, ai, ao, au, ay, âu, ây, eo, êu, iu, oi, ôi, ơi, ui, ưi, ưu): tone on first vowel
                target = vPos[0];
            }
        } else {
            // Triphthong (vCount >= 3)
            VowelInfo vi1 = CHAR_TO_VOWEL.get(word.charAt(vPos[0]));
            VowelInfo vi2 = CHAR_TO_VOWEL.get(word.charAt(vPos[1]));
            VowelInfo vi3 = CHAR_TO_VOWEL.get(word.charAt(vPos[2]));
            String trip = (vi1 != null && vi2 != null && vi3 != null) ? (vi1.base + vi2.base + vi3.base) : "";

            if ("uyê".equals(trip) || "uya".equals(trip)) {
                // Tone goes on 3rd vowel (ê / a): chuyện, truyền, khuyên, khuyết, khuya
                target = vPos[2];
            } else {
                // All other triphthongs (iêu, yêu, oai, oao, oay, oeo, uao, uay, uây, uôi, ươi, ươu, uyu): tone on middle vowel
                target = vPos[1];
            }
        }

        StringBuilder sb = new StringBuilder(word);
        // Clear old tone
        for (int i = firstVowel; i <= lastVowel; i++) {
            VowelInfo vi = CHAR_TO_VOWEL.get(sb.charAt(i));
            if (vi != null && vi.tone != 0) {
                String baseChar = VOWEL_TABLE.get(vi.base)[0];
                if (vi.isUpper) baseChar = baseChar.toUpperCase();
                sb.replace(i, i + 1, baseChar);
            }
        }

        // Apply new tone if targetTone > 0
        if (targetTone > 0) {
            VowelInfo targetVi = CHAR_TO_VOWEL.get(sb.charAt(target));
            if (targetVi == null) return null;
            String newChar = VOWEL_TABLE.get(targetVi.base)[targetTone];
            if (targetVi.isUpper) newChar = newChar.toUpperCase();
            sb.replace(target, target + 1, newChar);
        }

        return sb.toString();
    }
}
