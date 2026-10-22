package com.termux.view;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VietnameseTelexEngineTest {

    private VietnameseTelexEngine mEngine;

    @Before
    public void setUp() {
        mEngine = new VietnameseTelexEngine();
    }

    private String typeString(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            VietnameseTelexEngine.TransformResult tr = mEngine.processChar(c);
            if (tr != null) {
                if (tr.backspaces > 0) {
                    int deleteFrom = Math.max(0, result.length() - tr.backspaces);
                    result.delete(deleteFrom, result.length());
                }
                result.append(tr.text);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Test
    public void testUndoRepeatedModifierNoDowwn() {
        // Typing 'd', 'o', 'w', 'w', 'n' should produce 'down', NEVER 'dowwn'
        String res = typeString("dowwn");
        Assert.assertEquals("down", res);
    }

    @Test
    public void testDownWordProtection() {
        // Typing 'down' should automatically preserve 'down'
        String res = typeString("down");
        Assert.assertEquals("down", res);
    }

    @Test
    public void testDownloadsCommandProtection() {
        // Typing 'downloads' should never corrupt
        String res = typeString("downloads");
        Assert.assertEquals("downloads", res);
    }

    @Test
    public void testEnglishCommandsProtection() {
        mEngine.reset();
        Assert.assertEquals("status", typeString("status"));
        mEngine.reset();
        Assert.assertEquals("clear", typeString("clear"));
        mEngine.reset();
        Assert.assertEquals("grep", typeString("grep"));
        mEngine.reset();
        Assert.assertEquals("words", typeString("words"));
        mEngine.reset();
        Assert.assertEquals("cards", typeString("cards"));
    }

    @Test
    public void testDiphthongsAndTriphthongs() {
        mEngine.reset();
        Assert.assertEquals("thấy", typeString("thays"));
        mEngine.reset();
        Assert.assertEquals("tuần", typeString("tuanf"));
        mEngine.reset();
        Assert.assertEquals("chuẩn", typeString("chuanr"));
        mEngine.reset();
        Assert.assertEquals("thuế", typeString("thues"));
        mEngine.reset();
        Assert.assertEquals("thuở", typeString("thuowr"));
        mEngine.reset();
        Assert.assertEquals("chuyện", typeString("chuyenj"));
        mEngine.reset();
        Assert.assertEquals("cứu", typeString("cuus"));
        mEngine.reset();
        Assert.assertEquals("hoặc", typeString("hoawcj"));
    }

    @Test
    public void testToneChangingAndRemoval() {
        mEngine.reset();
        // toan + s -> toán
        String res = typeString("toans");
        Assert.assertEquals("toán", res);
        // + f -> đổi thành toàn
        VietnameseTelexEngine.TransformResult tr = mEngine.processChar('f');
        Assert.assertNotNull(tr);
        res = res.substring(0, res.length() - tr.backspaces) + tr.text;
        Assert.assertEquals("toàn", res);
        // + f -> lặp lại phím huyền thì xóa dấu thành toan
        tr = mEngine.processChar('f');
        Assert.assertNotNull(tr);
        res = res.substring(0, res.length() - tr.backspaces) + tr.text;
        Assert.assertEquals("toan", res);
    }
}
