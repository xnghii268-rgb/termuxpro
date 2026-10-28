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
        });
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
}
