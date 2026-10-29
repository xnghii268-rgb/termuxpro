package com.termux.view.input;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pure Java phonotactic validator for Vietnamese syllables.
 * Adheres strictly to Vietnamese orthography and phonotactics without Android dependencies.
 */
public final class VietnamesePhonotactics {

    private static final Set<String> VALID_INITIALS = new HashSet<>(Arrays.asList(
        "", "b", "c", "ch", "d", "đ", "g", "gh", "gi", "h", "k", "kh",
        "l", "m", "n", "ng", "ngh", "nh", "p", "ph", "qu", "r", "s",
        "t", "th", "tr", "v", "x"
    ));

    private static final Set<String> VALID_FINALS = new HashSet<>(Arrays.asList(
        "", "c", "ch", "m", "n", "ng", "nh", "p", "t"
    ));

    private static final Set<String> VALID_NUCLEI = new HashSet<>(Arrays.asList(
        // Monophthongs
        "a", "ă", "â", "e", "ê", "i", "o", "ô", "ơ", "u", "ư", "y",
        // Diphthongs
        "ia", "iê", "yê", "ya", "ua", "uô", "ưa", "ươ", "ai", "ao", "au", "ay", "âu", "ây",
        "eo", "êu", "iu", "oi", "ôi", "ơi", "ui", "ưi", "ưu", "uy", "uơ", "uo", "oa", "oe",
        "oă", "uâ", "uê", "ye", "ie",
        // Triphthongs
        "iêu", "yêu", "oai", "oao", "oay", "oeo", "uao", "uay", "uây", "uôi", "ươi", "ươu", "uya", "uyê", "uyu",
        "uoi", "ieu", "yeu", "uou", "uye"
    ));

    private static final Set<String> FOREIGN_CLUSTERS = new HashSet<>(Arrays.asList(
        "bl", "br", "cl", "cr", "dr", "fl", "fr", "gl", "gr", "pl", "pr",
        "sd", "sk", "sl", "sm", "sn", "sp", "st", "sw", "sc", "sh", "sq",
        "tw", "dw", "kw", "qw", "wh", "wr", "tm", "ts", "ps", "kl", "kn"
    ));

    private VietnamesePhonotactics() {}

    public static boolean isForeignInitial(String lower, boolean wFirstToU) {
        if (lower == null || lower.isEmpty()) return false;
        char first = lower.charAt(0);
        if (first == 'f' || first == 'j' || first == 'z') return true;
        if (first == 'w' && !wFirstToU) return true;

        if (lower.length() >= 2) {
            String c2 = lower.substring(0, 2);
            if (FOREIGN_CLUSTERS.contains(c2)) return true;
            if (lower.length() >= 3) {
                String c3 = lower.substring(0, 3);
                if (c3.equals("str") || c3.equals("spr") || c3.equals("scr")) return true;
            }
        }
        return false;
    }

    public static boolean isValidInitial(String initial) {
        return initial != null && VALID_INITIALS.contains(initial.toLowerCase());
    }

    public static boolean isValidFinal(String fin) {
        return fin != null && VALID_FINALS.contains(fin.toLowerCase());
    }

    public static boolean isValidNucleus(String nucleus) {
        return nucleus != null && VALID_NUCLEI.contains(nucleus.toLowerCase());
    }

    /**
     * Syllables ending with stop consonants c, ch, p, t can only carry sắc (1) or nặng (5) tones.
     */
    public static boolean isToneAllowedWithFinal(String fin, int tone) {
        if (fin == null || fin.isEmpty()) return true;
        String f = fin.toLowerCase();
        if (f.equals("c") || f.equals("ch") || f.equals("p") || f.equals("t")) {
            return tone == 1 || tone == 5;
        }
        return true;
    }

    /**
     * Validate full syllable phonotactics.
     */
    public static boolean isValidSyllable(String initial, String nucleus, String fin, int tone) {
        if (!isValidInitial(initial)) return false;
        if (!isValidNucleus(nucleus)) return false;
        if (!isValidFinal(fin)) return false;
        if (!isToneAllowedWithFinal(fin, tone)) return false;

        String initLower = initial.toLowerCase();
        String nucLower = nucleus.toLowerCase();

        // Open diphthongs (ia, ya, ua, ưa) can never take final consonants.
        // In closed syllables with finals, they must be iê, yê, uô, ươ.
        if (fin != null && !fin.isEmpty()) {
            if (nucLower.equals("ia") || nucLower.equals("ya") || nucLower.equals("ua") || nucLower.equals("ưa")) {
                return false;
            }
        }

        // Spelling rules: k, gh, ngh only precede front vowels (e, ê, i, y, and diphthongs starting with them)
        char firstVowel = nucLower.charAt(0);
        if (initLower.equals("k") || initLower.equals("gh") || initLower.equals("ngh")) {
            if (firstVowel != 'e' && firstVowel != 'ê' && firstVowel != 'i' && firstVowel != 'y') {
                return false;
            }
        } else if (initLower.equals("c") || initLower.equals("g") || initLower.equals("ng")) {
            // c, g, ng precede back/central vowels (except gi which is handled as initial)
            if (firstVowel == 'e' || firstVowel == 'ê' || (firstVowel == 'i' && !initLower.equals("g"))) {
                return false;
            }
        } else if (initLower.equals("qu")) {
            // qu precedes vowels (u acts as glide)
            if (firstVowel == 'u' || firstVowel == 'ư') {
                return false;
            }
        }

        return true;
    }

    public static boolean isVietnameseDiacritic(char c) {
        char l = Character.toLowerCase(c);
        if (l == 'đ') return true;
        String s = "áàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ";
        return s.indexOf(l) != -1;
    }

    public static boolean hasExplicitTransform(String rawLower, String parsedText) {
        if (rawLower == null || rawLower.isEmpty()) return false;
        char c0 = rawLower.charAt(0);
        if (c0 == 'f' || c0 == 'j' || c0 == 'z') return false;

        return rawLower.contains("dd") || rawLower.contains("aa") || rawLower.contains("ee") ||
               rawLower.contains("oo") || rawLower.contains("aw") || rawLower.contains("ow") ||
               rawLower.contains("uw") || rawLower.contains("d9") || rawLower.contains("a6") ||
               rawLower.contains("e6") || rawLower.contains("o6") || rawLower.contains("a8") ||
               rawLower.contains("o7") || rawLower.contains("u7");
    }
}
