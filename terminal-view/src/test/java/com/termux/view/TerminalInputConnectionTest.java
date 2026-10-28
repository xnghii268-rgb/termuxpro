package com.termux.view;

import android.view.KeyEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class TerminalInputConnectionTest {

    private StringBuilder mTerminalBuffer;
    private TerminalInputConnection mConnection;
    private final List<Integer> mDeletedPoints = new ArrayList<>();

    @Before
    public void setUp() {
        mTerminalBuffer = new StringBuilder();
        mDeletedPoints.clear();
        mConnection = new TerminalInputConnection(null, new TerminalInputConnection.TerminalInputListener() {
            @Override
            public void onCommitText(String text) {
                mTerminalBuffer.append(text);
            }

            @Override
            public void onCommitTextRaw(String text) {
                mTerminalBuffer.append(text);
            }

            @Override
            public void onDeleteCodePoints(int codePoints) {
                mDeletedPoints.add(codePoints);
                for (int i = 0; i < codePoints && mTerminalBuffer.length() > 0; i++) {
                    int cp = Character.codePointBefore(mTerminalBuffer, mTerminalBuffer.length());
                    mTerminalBuffer.delete(mTerminalBuffer.length() - Character.charCount(cp), mTerminalBuffer.length());
                }
            }

            @Override
            public void onSendKeyEvent(KeyEvent event) {
                // Mock
            }

            @Override
            public void onComposingStateChanged(String composingText) {
                // Mock
            }

            @Override
            public boolean isBypassInputEngine() {
                return false;
            }
        });
        mConnection.setVietnameseEngine(new com.termux.view.input.VietnameseEngine());
    }

    @Test
    public void testDirectCommitTextNFC() {
        mConnection.commitText("người", 1);
        Assert.assertEquals("người", mTerminalBuffer.toString());
        // Verify canonical NFC
        Assert.assertTrue(Normalizer.isNormalized(mTerminalBuffer.toString(), Normalizer.Form.NFC));
    }

    @Test
    public void testComposingSequenceAndFinish() {
        // Gboard Telex: "ng" -> "ngu" -> "nguo" -> "nguoi" -> "người"
        mConnection.setComposingText("ng", 1);
        mConnection.setComposingText("ngu", 1);
        mConnection.setComposingText("nguo", 1);
        mConnection.setComposingText("nguoi", 1);
        mConnection.setComposingText("người", 1);
        mConnection.finishComposingText();

        Assert.assertEquals("người", mTerminalBuffer.toString());
    }

    @Test
    public void testRewriteWithDeleteSurroundingText() {
        // Unikey style: commits "nguoi", then user types "w", IME reads context, deletes 5 chars, commits "người"
        mConnection.commitText("nguoi", 1);
        Assert.assertEquals("nguoi", mConnection.getTextBeforeCursor(5, 0).toString());

        mConnection.deleteSurroundingText(5, 0);
        mConnection.commitText("người", 1);

        Assert.assertEquals("người", mTerminalBuffer.toString());
    }

    @Test
    public void testSixHistoricalCriticalWords() {
        String[] words = new String[]{"người", "được", "trường", "những", "Việt", "quốc"};
        for (String word : words) {
            setUp();
            // Test composition cycle
            mConnection.setComposingText(word.substring(0, Math.min(2, word.length())), 1);
            mConnection.setComposingText(word, 1);
            mConnection.finishComposingText();
            Assert.assertEquals("Failed for critical word: " + word, word, mTerminalBuffer.toString());
            Assert.assertTrue(Normalizer.isNormalized(mTerminalBuffer.toString(), Normalizer.Form.NFC));
        }
    }

    @Test
    public void testNFDInputNormalizedToNFC() {
        // Decomposed NFD for "Việt"
        String nfdViet = Normalizer.normalize("Việt", Normalizer.Form.NFD);
        mConnection.commitText(nfdViet, 1);
        Assert.assertEquals("Việt", mTerminalBuffer.toString());
        Assert.assertTrue(Normalizer.isNormalized(mTerminalBuffer.toString(), Normalizer.Form.NFC));
    }

    @Test
    public void testCodePointAccurateBackspace() {
        mConnection.commitText("người", 1);
        // Backspace 1 character should delete "i", leaving "ngườ"
        mConnection.deleteSurroundingText(1, 0);
        Assert.assertEquals("ngườ", mTerminalBuffer.toString());

        // Backspace another character should delete "ờ", leaving "ngư"
        mConnection.deleteSurroundingText(1, 0);
        Assert.assertEquals("ngư", mTerminalBuffer.toString());
    }

    @Test
    public void testLiveComposingVietnameseGboardDau() {
        setUp();
        mConnection.setComposingText("d", 1);
        Assert.assertEquals("d", mTerminalBuffer.toString());

        mConnection.setComposingText("da", 1);
        Assert.assertEquals("da", mTerminalBuffer.toString());

        mConnection.setComposingText("dâ", 1);
        Assert.assertEquals("dâ", mTerminalBuffer.toString());

        mConnection.setComposingText("dâu", 1);
        Assert.assertEquals("dâu", mTerminalBuffer.toString());

        mConnection.setComposingText("dấu", 1);
        Assert.assertEquals("dấu", mTerminalBuffer.toString());

        mConnection.commitText("dấu ", 1);
        Assert.assertEquals("dấu ", mTerminalBuffer.toString());
    }

    @Test
    public void testLiveComposingEnglishTelexDaaus() {
        setUp();
        mConnection.setComposingText("d", 1);
        Assert.assertEquals("d", mTerminalBuffer.toString());

        mConnection.setComposingText("da", 1);
        Assert.assertEquals("da", mTerminalBuffer.toString());

        mConnection.setComposingText("daa", 1);
        Assert.assertEquals("dâ", mTerminalBuffer.toString());

        mConnection.setComposingText("daau", 1);
        Assert.assertEquals("dâu", mTerminalBuffer.toString());

        mConnection.setComposingText("daaus", 1);
        Assert.assertEquals("dấu", mTerminalBuffer.toString());

        mConnection.commitText("daaus ", 1);
        Assert.assertEquals("dấu ", mTerminalBuffer.toString());
    }

    @Test
    public void testLiveComposingDdToDe() {
        setUp();
        mConnection.setComposingText("d", 1);
        Assert.assertEquals("d", mTerminalBuffer.toString());

        mConnection.setComposingText("dd", 1);
        Assert.assertEquals("đ", mTerminalBuffer.toString());

        mConnection.setComposingText("dde", 1);
        Assert.assertEquals("đe", mTerminalBuffer.toString());

        mConnection.setComposingText("ddee", 1);
        Assert.assertEquals("đê", mTerminalBuffer.toString());

        mConnection.setComposingText("ddeer", 1);
        Assert.assertEquals("để", mTerminalBuffer.toString());

        mConnection.commitText("ddeer ", 1);
        Assert.assertEquals("để ", mTerminalBuffer.toString());
    }

    @Test
    public void testLiveComposingMixedPrecomposedDe() {
        setUp();
        // Emulates Gboard composing "đ" then adding "ee" then "r"
        mConnection.setComposingText("đ", 1);
        Assert.assertEquals("đ", mTerminalBuffer.toString());

        mConnection.setComposingText("đe", 1);
        Assert.assertEquals("đe", mTerminalBuffer.toString());

        mConnection.setComposingText("đee", 1);
        Assert.assertEquals("đê", mTerminalBuffer.toString());

        mConnection.setComposingText("đeer", 1);
        Assert.assertEquals("để", mTerminalBuffer.toString());

        mConnection.commitText("đeer", 1);
        Assert.assertEquals("để", mTerminalBuffer.toString());
    }

    @Test
    public void testPerCharacterDirectCommitDau() {
        setUp();
        String[] keys = {"d", "a", "a", "u", "s", " "};
        for (String k : keys) {
            mConnection.commitText(k, 1);
        }
        Assert.assertEquals("dấu ", mTerminalBuffer.toString());
    }

    @Test
    public void testPerCharacterDirectCommitThuong() {
        setUp();
        String[] keys = {"t", "h", "u", "o", "n", "g", "w", " "};
        for (String k : keys) {
            mConnection.commitText(k, 1);
        }
        Assert.assertEquals("thương ", mTerminalBuffer.toString());
    }

    @Test
    public void testPerCharacterDirectCommitTriet() {
        setUp();
        String[] keys = {"t", "r", "i", "e", "e", "j", "t", " "};
        for (String k : keys) {
            mConnection.commitText(k, 1);
        }
        Assert.assertEquals("triệt ", mTerminalBuffer.toString());
    }

    @Test
    public void testPerCharacterDirectCommitEnglish() {
        setUp();
        String[] keys = {"f", "i", "x", " "};
        for (String k : keys) {
            mConnection.commitText(k, 1);
        }
        Assert.assertEquals("fix ", mTerminalBuffer.toString());
    }
}
