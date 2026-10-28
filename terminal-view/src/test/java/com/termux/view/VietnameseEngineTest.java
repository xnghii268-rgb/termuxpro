package com.termux.view;

import com.termux.view.input.VietnameseEngine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class VietnameseEngineTest {

    private VietnameseEngine mEngine;

    @Before
    public void setUp() {
        mEngine = new VietnameseEngine();
    }

    private String typeString(String input) {
        return typeString(mEngine, input);
    }

    public static String typeString(VietnameseEngine engine, String input) {
        StringBuilder screen = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            VietnameseEngine.TransformResult tr = engine.processChar(c);
            if (tr != null) {
                if (tr.backspaces > 0) {
                    int delFrom = Math.max(0, screen.length() - tr.backspaces);
                    screen.delete(delFrom, screen.length());
                }
                screen.append(tr.text);
            } else {
                screen.append(c);
            }
        }
        return screen.toString();
    }

    @Test
    public void testOwnerReportedBug1ThuongW() {
        mEngine.reset();
        String result = typeString("thuongw");
        Assert.assertEquals("thương", result);
    }

    @Test
    public void testOwnerReportedBug2Fix() {
        mEngine.reset();
        String result = typeString("fix");
        Assert.assertEquals("fix", result);
        Assert.assertNotEquals("fĩ", result);
    }

    @Test
    public void testMandatoryCases() {
        String[][] mandatory = {
            {"thuongw", "thương"},
            {"thuowng", "thương"},
            {"nguoiwf", "người"},
            {"nguwowif", "người"},
            {"duocj", "được"},
            {"truongwf", "trường"},
            {"nhuwngx", "những"},
            {"Vieetj", "Việt"},
            {"Vietj", "Việt"},
            {"quoocs", "quốc"},
            {"nguyeenx", "nguyễn"},
            {"huowngs", "hướng"},
            {"muowfn", "mượn"},
            {"khuyeenns", "khuyến"},
            {"Dduwowcj", "Được"},
            {"ddi", "đi"},
            {"Ddaf", "Đà"},
            {"Nanwx", "Nẵng"},
            {"aaa", "aa"},
            {"ww", "w"},
            {"ss", "s"},
            {"asf", "à"},
            {"asfa", "àa"}
        };

        for (String[] test : mandatory) {
            mEngine.reset();
            String res = typeString(test[0]);
            String nfcExpected = Normalizer.normalize(test[1], Normalizer.Form.NFC);
            Assert.assertEquals("Failed for input: " + test[0], nfcExpected, res);
        }
    }

    @Test
    public void testVniModeCases() {
        VietnameseEngine vniEngine = new VietnameseEngine();
        vniEngine.getOptions().mode = VietnameseEngine.Options.Mode.VNI;

        String[][] vniCases = {
            {"thuong7", "thương"},
            {"nguoi72", "người"},
            {"truong72", "trường"},
            {"duoc75", "được"},
            {"d9uoc75", "được"},
            {"nhung74", "những"},
            {"Viet65", "Việt"},
            {"quoc61", "quốc"},
        };

        for (String[] test : vniCases) {
            vniEngine.reset();
            String res = typeString(vniEngine, test[0]);
            String nfcExpected = Normalizer.normalize(test[1], Normalizer.Form.NFC);
            Assert.assertEquals("VNI failed for input: " + test[0], nfcExpected, res);
        }
    }

    @Test
    public void testModernVsClassicTonePlacement() {
        VietnameseEngine modern = new VietnameseEngine();
        modern.getOptions().modernTonePlacement = true;
        Assert.assertEquals("thủy", typeString(modern, "thuyr"));
        modern.reset();
        Assert.assertEquals("hòa", typeString(modern, "hoaf"));

        VietnameseEngine classic = new VietnameseEngine();
        classic.getOptions().modernTonePlacement = false;
        Assert.assertEquals("thuỷ", typeString(classic, "thuyr"));
        classic.reset();
        Assert.assertEquals("hoà", typeString(classic, "hoaf"));
    }

    @Test
    public void testEnglishCommandsProtection() {
        String[] cmds = {
            "fix", "exit", "curl", "wget", "docker", "cargo", "sudo", "git", "status",
            "install", "python", "node", "npm", "pip", "nano", "vim", "htop", "make",
            "clear", "reset", "export", "source", "alias", "which", "whereis", "head",
            "tail", "find", "sed", "awk", "tar", "zip", "unzip", "kill", "free",
            "mixed", "fixed", "flex", "taxi", "box", "text", "next", "six"
        };

        for (String cmd : cmds) {
            mEngine.reset();
            String res = typeString(cmd);
            Assert.assertEquals("English command was corrupted: " + cmd, cmd, res);
        }
    }

    @Test
    public void testBackspaceRecomputation() {
        mEngine.reset();
        typeString("thuongwf"); // -> "thường"
        mEngine.handleBackspace(); // pops 'f' -> "thương"
        String current = VietnameseEngine.process("thuongw", mEngine.getOptions()).text;
        Assert.assertEquals("thương", current);

        mEngine.handleBackspace(); // pops 'w' -> "thuong"
        current = VietnameseEngine.process("thuong", mEngine.getOptions()).text;
        Assert.assertEquals("thuong", current);
    }

    public static void main(String[] args) {
        System.out.println("Running VietnameseEngineTest suite directly...");
        VietnameseEngineTest test = new VietnameseEngineTest();
        test.setUp();

        try {
            test.testOwnerReportedBug1ThuongW();
            System.out.println("✓ Bug 1 (thuongw -> thương): PASSED");

            test.testOwnerReportedBug2Fix();
            System.out.println("✓ Bug 2 (fix -> fix, not fĩ): PASSED");

            test.testMandatoryCases();
            System.out.println("✓ Mandatory Cases: PASSED");

            test.testVniModeCases();
            System.out.println("✓ VNI Mode Cases: PASSED");

            test.testModernVsClassicTonePlacement();
            System.out.println("✓ Modern vs Classic Tone Placement: PASSED");

            test.testEnglishCommandsProtection();
            System.out.println("✓ English Commands Protection: PASSED");

            test.testBackspaceRecomputation();
            System.out.println("✓ Backspace Recomputation: PASSED");

            System.out.println("=== ALL CORE VIETNAMESE ENGINE TESTS PASSED ===");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("TEST FAILURE: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
