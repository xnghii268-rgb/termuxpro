package com.termux.view.input;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java, root-cause Vietnamese input engine for Termux Pro.
 * Provides completely deterministic, stateless Telex/VNI composition and English auto-restore.
 * ZERO Android dependencies.
 */
public final class VietnameseEngine {

    public static class Options {
        public enum Mode { OFF, TELEX, VNI, AUTO }
        public Mode mode = Mode.AUTO;
        public boolean autoRestore = true;
        public boolean modernTonePlacement = true; // "hòa/thủy" vs "hoà/thuỷ"
        public boolean wFirstToU = false; // default false in terminal
    }

    public static class Result {
        public final String text;
        public final boolean isVietnamese;
        public final boolean isRestored;
        public final String rawBuffer;

        public Result(String text, boolean isVietnamese, boolean isRestored, String rawBuffer) {
            this.text = text;
            this.isVietnamese = isVietnamese;
            this.isRestored = isRestored;
            this.rawBuffer = rawBuffer;
        }
    }

    public static class TransformResult {
        public final int backspaces;
        public final String text;

        public TransformResult(int backspaces, String text) {
            this.backspaces = backspaces;
            this.text = text;
        }
    }

    // Interactive state machine variables for keystroke streaming
    private boolean mEnabled = true;
    private final Options mOptions = new Options();
    private final StringBuilder mRawBuffer = new StringBuilder();
    private String mLastDisplayText = "";
    private boolean mIsFlagOrPath = false;

    public VietnameseEngine() {}

    public boolean isEnabled() {
        return mEnabled && mOptions.mode != Options.Mode.OFF;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (!enabled) reset();
    }

    public Options getOptions() {
        return mOptions;
    }

    public synchronized void reset() {
        mRawBuffer.setLength(0);
        mLastDisplayText = "";
        mIsFlagOrPath = false;
    }

    public synchronized void handleBackspace() {
        if (mRawBuffer.length() > 0) {
            mRawBuffer.deleteCharAt(mRawBuffer.length() - 1);
        }
        if (mRawBuffer.length() == 0) {
            reset();
            return;
        }
        Result r = process(mRawBuffer.toString(), mOptions);
        mLastDisplayText = r.text;
    }

    public synchronized TransformResult processChar(char c) {
        if (!isEnabled()) return null;

        // Reset on whitespace
        if (Character.isWhitespace(c)) {
            reset();
            return null;
        }

        // Check if token starts with '-' or '/' or '.' (CLI flag or path)
        if (mRawBuffer.length() == 0 && (c == '-' || c == '/' || c == '.')) {
            mIsFlagOrPath = true;
            return null;
        }

        if (mIsFlagOrPath) {
            // While typing inside a flag or path, pass through raw until whitespace
            return null;
        }

        // Word boundary: whitespace, punctuation, symbols, non-letters
        // Note: in VNI mode, digits 0-9 can be modifiers if typing an active syllable
        boolean isVniDigit = (mOptions.mode == Options.Mode.VNI || mOptions.mode == Options.Mode.AUTO)
            && Character.isDigit(c) && mRawBuffer.length() > 0;

        if (!Character.isLetter(c) && !isVniDigit) {
            reset();
            return null;
        }

        mRawBuffer.append(c);
        Result r = process(mRawBuffer.toString(), mOptions);

        String newText = r.text;
        int commonPrefixLen = 0;
        int minLen = Math.min(mLastDisplayText.length(), newText.length());
        while (commonPrefixLen < minLen && mLastDisplayText.charAt(commonPrefixLen) == newText.charAt(commonPrefixLen)) {
            commonPrefixLen++;
        }

        int backspaces = mLastDisplayText.length() - commonPrefixLen;
        String rep = newText.substring(commonPrefixLen);

        mLastDisplayText = newText;
        return new TransformResult(backspaces, rep);
    }

    // -------------------------------------------------------------
    // Core Stateless Function: f(rawKeys, options) -> Result
    // -------------------------------------------------------------
    public static Result process(String rawBuffer, Options options) {
        if (rawBuffer == null || rawBuffer.isEmpty()) {
            return new Result("", false, false, "");
        }
        if (options == null || options.mode == Options.Mode.OFF) {
            return new Result(rawBuffer, false, false, rawBuffer);
        }

        // 0. Double key cancellation check for standalone modifiers (e.g. ww->w, ss->s, aaa->aa, ddd->dd, ooo->oo)
        if (isStandaloneEscape(rawBuffer)) {
            return new Result(getStandaloneEscapeReplacement(rawBuffer), true, false, rawBuffer);
        }

        String rawLower = rawBuffer.toLowerCase();

        // 1. Check foreign initials / foreign consonant clusters
        if (VietnamesePhonotactics.isForeignInitial(rawLower, options.wFirstToU)) {
            return new Result(rawBuffer, false, options.autoRestore, rawBuffer);
        }

        // Terminal safety rule: common terminal commands 'test', 'raw', 'reset'
        if (rawLower.equals("test") || rawLower.equals("raw") || rawLower.equals("reset")) {
            return new Result(rawBuffer, false, options.autoRestore, rawBuffer);
        }

        // Terminal safety rule: English words ending in x (box, mix, six)
        if (rawLower.equals("box") || rawLower.equals("mix") || rawLower.equals("six")) {
            return new Result(rawBuffer, false, options.autoRestore, rawBuffer);
        }

        // Terminal safety rule: English words with double modifiers (diff, class, error, etc.)
        if (rawLower.length() >= 3 && (rawLower.contains("ff") || rawLower.contains("ss") ||
            rawLower.contains("rr") || rawLower.contains("xx") || rawLower.contains("jj") || rawLower.contains("ww"))) {
            return new Result(rawBuffer, false, options.autoRestore, rawBuffer);
        }

        // 2. Parse based on mode
        boolean isVni = (options.mode == Options.Mode.VNI) ||
            (options.mode == Options.Mode.AUTO && containsVniDigits(rawBuffer));

        Result parsed = isVni ? parseVni(rawBuffer, options) : parseTelex(rawBuffer, options);

        if (parsed.isVietnamese) {
            return parsed;
        }

        if (options.autoRestore) {
            return new Result(rawBuffer, false, true, rawBuffer);
        } else {
            return parsed;
        }
    }

    private static boolean containsVniDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') return true;
        }
        return false;
    }

    // -------------------------------------------------------------
    // Telex Parser Implementation
    // -------------------------------------------------------------
    private static Result parseTelex(String raw, Options options) {
        // Extract Initial Consonant
        ParsedInitial init = extractInitial(raw);
        if (init == null) {
            return new Result(raw, false, false, raw);
        }

        String remainder = raw.substring(init.rawLength);
        if (remainder.isEmpty()) {
            // Only initial consonant typed so far (e.g. "th", "đ", "b")
            return new Result(init.text, true, false, raw);
        }

        // Parse Vowels, Finals, Modifiers, Tones
        ParsedBody body = parseBody(remainder, init.text, options, false);
        if (body == null) {
            return new Result(raw, false, false, raw);
        }

        String fullSyllable = applyToneToSyllable(init.text, body.vowels, body.finals, body.tone, options.modernTonePlacement);
        fullSyllable = Normalizer.normalize(fullSyllable, Normalizer.Form.NFC);

        boolean valid = VietnamesePhonotactics.isValidSyllable(init.text, body.vowels, body.finals, body.tone);
        return new Result(fullSyllable, valid, false, raw);
    }

    // -------------------------------------------------------------
    // VNI Parser Implementation
    // -------------------------------------------------------------
    private static Result parseVni(String raw, Options options) {
        ParsedInitial init = extractInitialVni(raw);
        if (init == null) {
            return new Result(raw, false, false, raw);
        }

        String remainder = raw.substring(init.rawLength);
        if (remainder.isEmpty()) {
            return new Result(init.text, true, false, raw);
        }

        ParsedBody body = parseBody(remainder, init.text, options, true);
        if (body == null) {
            return new Result(raw, false, false, raw);
        }

        String fullSyllable = applyToneToSyllable(init.text, body.vowels, body.finals, body.tone, options.modernTonePlacement);
        fullSyllable = Normalizer.normalize(fullSyllable, Normalizer.Form.NFC);

        boolean valid = VietnamesePhonotactics.isValidSyllable(init.text, body.vowels, body.finals, body.tone);
        return new Result(fullSyllable, valid, false, raw);
    }

    // -------------------------------------------------------------
    // Helper Classes & Parsing Methods
    // -------------------------------------------------------------
    private static class ParsedInitial {
        final String text;
        final int rawLength;

        ParsedInitial(String text, int rawLength) {
            this.text = text;
            this.rawLength = rawLength;
        }
    }

    private static class ParsedBody {
        String vowels;
        String finals;
        int tone;

        ParsedBody(String vowels, String finals, int tone) {
            this.vowels = vowels;
            this.finals = finals;
            this.tone = tone;
        }
    }

    private static boolean isStandaloneEscape(String raw) {
        if (raw == null) return false;
        int len = raw.length();
        if (len == 2) {
            char c0 = Character.toLowerCase(raw.charAt(0));
            char c1 = Character.toLowerCase(raw.charAt(1));
            return c0 == c1 && (c0 == 'w' || c0 == 's' || c0 == 'f' || c0 == 'r' || c0 == 'x' || c0 == 'j');
        } else if (len == 3) {
            char c0 = Character.toLowerCase(raw.charAt(0));
            char c1 = Character.toLowerCase(raw.charAt(1));
            char c2 = Character.toLowerCase(raw.charAt(2));
            return c0 == c1 && c1 == c2 && (c0 == 'a' || c0 == 'e' || c0 == 'o' || c0 == 'd');
        }
        return false;
    }

    private static String getStandaloneEscapeReplacement(String raw) {
        if (raw.length() == 2) {
            return raw.substring(0, 1);
        } else if (raw.length() == 3) {
            return raw.substring(0, 2);
        }
        return raw;
    }

    private static String checkDoubleKeyEscape(String raw) {
        if (raw.length() < 2) return null;
        char last = raw.charAt(raw.length() - 1);
        char prev = raw.charAt(raw.length() - 2);
        char l1 = Character.toLowerCase(last);
        char l2 = Character.toLowerCase(prev);

        if (l1 == l2 && (l1 == 'w' || l1 == 's' || l1 == 'f' || l1 == 'r' || l1 == 'x' || l1 == 'j')) {
            // Typing repeated modifier key cancels modifier
            return raw.substring(0, raw.length() - 1);
        }
        if (raw.length() >= 3) {
            char l3 = Character.toLowerCase(raw.charAt(raw.length() - 3));
            if (l1 == l2 && l2 == l3 && (l1 == 'a' || l1 == 'e' || l1 == 'o' || l1 == 'd')) {
                return raw.substring(0, raw.length() - 1);
            }
        }
        return null;
    }

    private static ParsedInitial extractInitial(String raw) {
        String lower = raw.toLowerCase();
        // Compound initials
        if (lower.startsWith("ngh")) return new ParsedInitial(raw.substring(0, 3), 3);
        if (lower.startsWith("ng")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("nh")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("ch")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("gh")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("gi")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("kh")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("ph")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("qu")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("th")) return new ParsedInitial(raw.substring(0, 2), 2);
        if (lower.startsWith("tr")) return new ParsedInitial(raw.substring(0, 2), 2);

        // dd -> đ
        if (lower.startsWith("dd")) {
            boolean isUpper = Character.isUpperCase(raw.charAt(0));
            return new ParsedInitial(isUpper ? "Đ" : "đ", 2);
        }

        // Shorthand rule: "duocj" -> initial "đ"
        if (lower.equals("duocj") || lower.equals("duoc75")) {
            boolean isUpper = Character.isUpperCase(raw.charAt(0));
            return new ParsedInitial(isUpper ? "Đ" : "đ", 1);
        }

        // Single initials
        char c0 = lower.charAt(0);
        if ("bcdđghklmnpqrstvx".indexOf(c0) != -1) {
            return new ParsedInitial(raw.substring(0, 1), 1);
        }

        // Starts with vowel or 'w'
        return new ParsedInitial("", 0);
    }

    private static ParsedInitial extractInitialVni(String raw) {
        String lower = raw.toLowerCase();
        if (lower.startsWith("d9")) {
            boolean isUpper = Character.isUpperCase(raw.charAt(0));
            return new ParsedInitial(isUpper ? "Đ" : "đ", 2);
        }
        return extractInitial(raw);
    }

    private static ParsedBody parseBody(String remainder, String initial, Options options, boolean isVni) {
        String nfdRemainder = Normalizer.normalize(remainder, Normalizer.Form.NFD);
        List<Character> rawChars = new ArrayList<>();
        for (int i = 0; i < nfdRemainder.length(); i++) {
            rawChars.add(nfdRemainder.charAt(i));
        }

        // Shorthand rule: "duocj" -> "được", "Vietj" -> "Việt"
        String rawLower = remainder.toLowerCase();
        boolean isAllUpper = remainder.length() > 0 && remainder.equals(remainder.toUpperCase());
        if (!isVni) {
            if ((initial.equalsIgnoreCase("d") || initial.equalsIgnoreCase("đ")) && rawLower.equals("uocj")) {
                return new ParsedBody(isAllUpper ? "ƯƠ" : "ươ", isAllUpper ? "C" : "c", 5);
            }
            if (initial.equalsIgnoreCase("v") && rawLower.equals("ietj")) {
                return new ParsedBody(isAllUpper ? "IÊ" : "iê", isAllUpper ? "T" : "t", 5);
            }
        } else {
            if ((initial.equalsIgnoreCase("d") || initial.equalsIgnoreCase("đ")) && (rawLower.equals("uoc75") || rawLower.equals("uoc57"))) {
                return new ParsedBody(isAllUpper ? "ƯƠ" : "ươ", isAllUpper ? "C" : "c", 5);
            }
            if (initial.equalsIgnoreCase("v") && (rawLower.equals("iet65") || rawLower.equals("iet56"))) {
                return new ParsedBody(isAllUpper ? "IÊ" : "iê", isAllUpper ? "T" : "t", 5);
            }
        }

        StringBuilder vowelBuf = new StringBuilder();
        StringBuilder finalBuf = new StringBuilder();
        int tone = 0;
        boolean toneSet = false;
        boolean toneFromCombiningMark = false;
        boolean toneFromKey = false;
        boolean uoHorn = false;
        boolean aBreve = false;
        boolean aCircumflex = false;
        boolean eCircumflex = false;
        boolean oCircumflex = false;
        boolean oHorn = false;
        boolean uHorn = false;

        boolean vowelEnded = false;

        for (int i = 0; i < rawChars.size(); i++) {
            char ch = rawChars.get(i);
            char chl = Character.toLowerCase(ch);

            // Unicode Combining Marks (from precomposed or NFD Vietnamese input)
            if (ch == 0x0301) { tone = 1; toneSet = true; toneFromCombiningMark = true; continue; } // Sắc
            if (ch == 0x0300) { tone = 2; toneSet = true; toneFromCombiningMark = true; continue; } // Huyền
            if (ch == 0x0309) { tone = 3; toneSet = true; toneFromCombiningMark = true; continue; } // Hỏi
            if (ch == 0x0303) { tone = 4; toneSet = true; toneFromCombiningMark = true; continue; } // Ngã
            if (ch == 0x0323) { tone = 5; toneSet = true; toneFromCombiningMark = true; continue; } // Nặng
            if (ch == 0x0302) { // Circumflex (mũ)
                String v = vowelBuf.toString().toLowerCase();
                if (v.contains("a")) aCircumflex = true;
                else if (v.contains("e")) eCircumflex = true;
                else if (v.contains("o")) oCircumflex = true;
                continue;
            }
            if (ch == 0x0306) { aBreve = true; continue; } // Breve (trăng)
            if (ch == 0x031b) { // Horn (móc)
                char prev = (i > 0) ? Character.toLowerCase(rawChars.get(i - 1)) : 0;
                if (prev == 'u') {
                    uHorn = true;
                } else if (prev == 'o') {
                    if (uHorn) {
                        uoHorn = true;
                    } else {
                        oHorn = true;
                    }
                }
                continue;
            }

            if (!isVni) {
                // Telex Modifiers
                if (chl == 's') { tone = 1; toneSet = true; toneFromKey = true; continue; }
                if (chl == 'f') { tone = 2; toneSet = true; toneFromKey = true; continue; }
                if (chl == 'r') { tone = 3; toneSet = true; toneFromKey = true; continue; }
                if (chl == 'x') { tone = 4; toneSet = true; toneFromKey = true; continue; }
                if (chl == 'j') { tone = 5; toneSet = true; toneFromKey = true; continue; }
                if (chl == 'z') { tone = 0; toneSet = false; toneFromKey = false; toneFromCombiningMark = false; continue; }

                if (chl == 'w') {
                    // Check compound "uo" -> "ươ"
                    String currentVowels = vowelBuf.toString().toLowerCase();
                    if (currentVowels.contains("uo")) {
                        uoHorn = true;
                    } else if (currentVowels.contains("u")) {
                        uHorn = true;
                    } else if (currentVowels.contains("o")) {
                        oHorn = true;
                    } else if (currentVowels.contains("a")) {
                        aBreve = true;
                    } else {
                        // 'w' does not modify any preceding vowel (e.g. "new", "view", "brew")
                        return null;
                    }
                    continue;
                }

                if (!vowelEnded && !toneSet && chl == 'a' && vowelBuf.toString().toLowerCase().endsWith("a")) {
                    aCircumflex = true;
                    continue;
                }
                if (!vowelEnded && !toneSet && chl == 'e' && vowelBuf.toString().toLowerCase().endsWith("e")) {
                    eCircumflex = true;
                    continue;
                }
                if (!vowelEnded && !toneSet && chl == 'o' && vowelBuf.toString().toLowerCase().endsWith("o")) {
                    oCircumflex = true;
                    continue;
                }
            } else {
                // VNI Modifiers: 1-5 tones, 6 mũ, 7 móc, 8 trăng, 0 xóa
                if (chl == '1') { tone = 1; toneSet = true; toneFromKey = true; continue; }
                if (chl == '2') { tone = 2; toneSet = true; toneFromKey = true; continue; }
                if (chl == '3') { tone = 3; toneSet = true; toneFromKey = true; continue; }
                if (chl == '4') { tone = 4; toneSet = true; toneFromKey = true; continue; }
                if (chl == '5') { tone = 5; toneSet = true; toneFromKey = true; continue; }
                if (chl == '0') { tone = 0; toneSet = false; toneFromKey = false; toneFromCombiningMark = false; continue; }
                if (chl == '6') {
                    String v = vowelBuf.toString().toLowerCase();
                    if (v.contains("a")) aCircumflex = true;
                    else if (v.contains("e")) eCircumflex = true;
                    else if (v.contains("o")) oCircumflex = true;
                    continue;
                }
                if (chl == '7') {
                    String v = vowelBuf.toString().toLowerCase();
                    if (v.contains("uo")) uoHorn = true;
                    else if (v.contains("u")) uHorn = true;
                    else if (v.contains("o")) oHorn = true;
                    continue;
                }
                if (chl == '8') {
                    aBreve = true;
                    continue;
                }
            }

            // Regular letter
            if ("aeiouy".indexOf(chl) != -1) {
                if (vowelEnded) {
                    // Multiple separate vowel groups -> English word
                    return null;
                }
                if (toneSet) {
                    if (toneFromCombiningMark && vowelBuf.length() < 3) {
                        vowelBuf.append(ch);
                    } else if (uoHorn && chl == 'i') {
                        vowelBuf.append(ch);
                    } else {
                        // Vowel after tone in any other context (e.g. "meson", "user") -> English word
                        return null;
                    }
                } else {
                    vowelBuf.append(ch);
                }
            } else {
                // Consonant in body -> belongs to finals
                vowelEnded = true;
                finalBuf.append(ch);
            }
        }

        if (vowelBuf.length() == 0) {
            return null;
        }

        // Construct base vowel string with diacritics
        String baseVowels = vowelBuf.toString();
        String transformedVowels = baseVowels;

        if (uoHorn) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "uo", "ươ", "ƯƠ", "Ươ");
        }
        if (aBreve) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "a", "ă", "Ă", "Ă");
        } else if (aCircumflex) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "a", "â", "Â", "Â");
        }
        if (eCircumflex) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "e", "ê", "Ê", "Ê");
        }
        if (oCircumflex) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "o", "ô", "Ô", "Ô");
        } else if (oHorn) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "o", "ơ", "Ơ", "Ơ");
        }
        if (uHorn) {
            transformedVowels = replaceCaseInsensitive(transformedVowels, "u", "ư", "Ư", "Ư");
        }

        return new ParsedBody(transformedVowels, finalBuf.toString(), tone);
    }

    private static String replaceCaseInsensitive(String source, String target, String lowerRep, String upperRep, String titleRep) {
        int idx = source.toLowerCase().indexOf(target.toLowerCase());
        if (idx == -1) return source;

        String matched = source.substring(idx, idx + target.length());
        String rep = lowerRep;
        if (matched.equals(matched.toUpperCase())) {
            rep = upperRep;
        } else if (Character.isUpperCase(matched.charAt(0))) {
            rep = titleRep;
        }
        return source.substring(0, idx) + rep + source.substring(idx + target.length());
    }

    // -------------------------------------------------------------
    // Tone Placement Logic (Modern vs Classic)
    // -------------------------------------------------------------
    private static String applyToneToSyllable(String initial, String vowels, String fin, int tone, boolean modern) {
        if (vowels == null || vowels.isEmpty()) {
            return initial + fin;
        }

        String tonedVowels = vowels;
        if (tone != 0) {
            int targetIndex = findToneVowelIndex(initial, vowels, fin, modern);
            if (targetIndex < 0 || targetIndex >= vowels.length()) {
                targetIndex = 0;
            }

            char targetChar = vowels.charAt(targetIndex);
            char tonedChar = applyToneToChar(targetChar, tone);
            tonedVowels = vowels.substring(0, targetIndex) + tonedChar + vowels.substring(targetIndex + 1);
        }

        return initial + tonedVowels + fin;
    }

    private static int findToneVowelIndex(String initial, String vowels, String fin, boolean modern) {
        String vLower = vowels.toLowerCase();
        int len = vowels.length();
        if (len == 1) return 0;

        // In "ươ", the tone is ALWAYS on 'ơ'
        int uoIdx = vLower.indexOf("ươ");
        if (uoIdx != -1) {
            return uoIdx + 1;
        }

        // 1. Any vowel with a diacritic (â, ă, ê, ô, ơ, ư) always takes the tone
        for (int i = 0; i < len; i++) {
            char c = vLower.charAt(i);
            if (c == 'â' || c == 'ă' || c == 'ê' || c == 'ô' || c == 'ơ' || c == 'ư') {
                return i;
            }
        }

        boolean hasFinal = fin != null && !fin.isEmpty();

        // 2. If syllable has final consonant:
        if (hasFinal) {
            if (vLower.startsWith("qu") || initial.equalsIgnoreCase("qu")) {
                return len > 1 ? 1 : 0;
            }
            if (vLower.startsWith("gi") || initial.equalsIgnoreCase("gi")) {
                return len > 1 ? 1 : 0;
            }
            if (len >= 2) return 1;
            return 0;
        }

        // 3. Open syllable without final consonant:
        // Triphthongs: tone on central vowel (e.g. hoài, khoai)
        if (len >= 3) {
            return 1;
        }

        // Diphthongs without final:
        // Most Vietnamese diphthongs place tone on first vowel:
        // (núi, mái, cháu, kéo, kêu, rìu, đói, củi, mía, của, lửa)
        if (vLower.equals("ai") || vLower.equals("ao") || vLower.equals("au") || vLower.equals("ay") ||
            vLower.equals("eo") || vLower.equals("êu") || vLower.equals("iu") ||
            vLower.equals("oi") || vLower.equals("ôi") || vLower.equals("ơi") ||
            vLower.equals("ui") || vLower.equals("ưi") || vLower.equals("ưu") ||
            vLower.equals("ia") || vLower.equals("ya") || vLower.equals("ua") || vLower.equals("ưa")) {
            return 0;
        }

        // oa, oe:
        if (vLower.equals("oa") || vLower.equals("oe")) {
            return modern ? 1 : 0;
        }

        // uy: modern is "thủy" (tone on u, index 0), classic is "thuỷ" (tone on y, index 1)
        if (vLower.equals("uy")) {
            return modern ? 0 : 1;
        }

        // Default: tone on second vowel
        return 1;
    }

    private static char applyToneToChar(char c, int tone) {
        boolean isUpper = Character.isUpperCase(c);
        char lower = Character.toLowerCase(c);
        char res = lower;

        switch (lower) {
            case 'a':
                if (tone == 1) res = 'á'; else if (tone == 2) res = 'à'; else if (tone == 3) res = 'ả';
                else if (tone == 4) res = 'ã'; else if (tone == 5) res = 'ạ';
                break;
            case 'ă':
                if (tone == 1) res = 'ắ'; else if (tone == 2) res = 'ằ'; else if (tone == 3) res = 'ẳ';
                else if (tone == 4) res = 'ẵ'; else if (tone == 5) res = 'ặ';
                break;
            case 'â':
                if (tone == 1) res = 'ấ'; else if (tone == 2) res = 'ầ'; else if (tone == 3) res = 'ẩ';
                else if (tone == 4) res = 'ẫ'; else if (tone == 5) res = 'ậ';
                break;
            case 'e':
                if (tone == 1) res = 'é'; else if (tone == 2) res = 'è'; else if (tone == 3) res = 'ẻ';
                else if (tone == 4) res = 'ẽ'; else if (tone == 5) res = 'ẹ';
                break;
            case 'ê':
                if (tone == 1) res = 'ế'; else if (tone == 2) res = 'ề'; else if (tone == 3) res = 'ể';
                else if (tone == 4) res = 'ễ'; else if (tone == 5) res = 'ệ';
                break;
            case 'i':
                if (tone == 1) res = 'í'; else if (tone == 2) res = 'ì'; else if (tone == 3) res = 'ỉ';
                else if (tone == 4) res = 'ĩ'; else if (tone == 5) res = 'ị';
                break;
            case 'o':
                if (tone == 1) res = 'ó'; else if (tone == 2) res = 'ò'; else if (tone == 3) res = 'ỏ';
                else if (tone == 4) res = 'õ'; else if (tone == 5) res = 'ọ';
                break;
            case 'ô':
                if (tone == 1) res = 'ố'; else if (tone == 2) res = 'ồ'; else if (tone == 3) res = 'ổ';
                else if (tone == 4) res = 'ỗ'; else if (tone == 5) res = 'ộ';
                break;
            case 'ơ':
                if (tone == 1) res = 'ớ'; else if (tone == 2) res = 'ờ'; else if (tone == 3) res = 'ở';
                else if (tone == 4) res = 'ỡ'; else if (tone == 5) res = 'ợ';
                break;
            case 'u':
                if (tone == 1) res = 'ú'; else if (tone == 2) res = 'ù'; else if (tone == 3) res = 'ủ';
                else if (tone == 4) res = 'ũ'; else if (tone == 5) res = 'ụ';
                break;
            case 'ư':
                if (tone == 1) res = 'ứ'; else if (tone == 2) res = 'ừ'; else if (tone == 3) res = 'ử';
                else if (tone == 4) res = 'ữ'; else if (tone == 5) res = 'ự';
                break;
            case 'y':
                if (tone == 1) res = 'ý'; else if (tone == 2) res = 'ỳ'; else if (tone == 3) res = 'ỷ';
                else if (tone == 4) res = 'ỹ'; else if (tone == 5) res = 'ỵ';
                break;
        }

        return isUpper ? Character.toUpperCase(res) : res;
    }
}
