package com.termux.view.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure Java English and Command Lexicon (Section B.2.a).
 * Manages >= 20,000 common English words, software terms, and runtime $PREFIX/bin executables.
 * Zero android.* dependencies; fully compatible with pure JVM regression runners.
 */
public final class EnglishLexicon {

    private static final Set<String> sWords = Collections.synchronizedSet(new HashSet<>(25000));
    private static final Set<String> sDevWords = Collections.synchronizedSet(new HashSet<>(2000));
    private static volatile boolean sInitialized = false;

    // Fallback core dev commands and English words always present
    private static final String[] CORE_FALLBACK = {
        "git", "curl", "fix", "exit", "docker", "sudo", "wget", "install", "cargo", "npm", "python",
        "node", "grep", "ssh", "ls", "cd", "cat", "echo", "chmod", "tmux", "vim", "nano", "apt", "pkg",
        "bash", "sh", "zsh", "test", "raw", "reset", "diff", "class", "error", "box", "mix", "six",
        "status", "commit", "push", "pull", "clone", "checkout", "branch", "merge", "rebase", "remote",
        "init", "fetch", "log", "show", "add", "mv", "rm", "cp", "mkdir", "rmdir", "touch", "find",
        "sed", "awk", "gzip", "gunzip", "zip", "unzip", "which", "whereis", "whoami", "uname",
        "df", "du", "top", "htop", "ps", "kill", "pkill", "env", "export", "alias", "unalias", "source",
        "history", "clear", "sleep", "wait", "true", "false", "head", "tail", "less", "more", "wc",
        "sort", "uniq", "cut", "paste", "tr", "tee", "xargs", "date", "cal", "bc", "expr", "printf",
        "read", "enable", "disable", "type", "help", "man", "info", "ping", "netstat", "ip", "ifconfig",
        "route", "traceroute", "nslookup", "dig", "host", "nmap", "nc", "netcat", "socat", "telnet",
        "ftp", "sftp", "scp", "rsync", "ssh-keygen", "ssh-agent", "ssh-add", "gpg", "openssl",
        "clang", "gcc", "g++", "make", "cmake", "ninja", "rustc", "rustup", "go", "java", "javac",
        "jar", "gradle", "mvn", "ruby", "perl", "php", "lua", "kotlin", "swift", "flutter", "dart",
        "yarn", "pnpm", "bun", "pip", "pip3", "gem", "bundle", "composer", "sbt", "ant",
        "zalo", "facebook", "google", "termux", "android", "linux", "github", "gitlab", "ubuntu"
    };

    static {
        initDefault();
    }

    private EnglishLexicon() {}

    public static void initDefault() {
        if (sInitialized && sWords.size() >= 20000) return;
        synchronized (sWords) {
            for (String w : CORE_FALLBACK) {
                String lw = w.toLowerCase(Locale.ROOT);
                sWords.add(lw);
                sDevWords.add(lw);
            }

            // Load dev words from corpus if available
            loadDevCorpus();

            // Try class resource
            try (InputStream is = EnglishLexicon.class.getResourceAsStream("/english_lexicon.txt")) {
                if (is != null) {
                    loadFromStream(is);
                }
            } catch (Throwable ignored) {}

            // Try standard relative file paths
            String[] tryPaths = {
                "tests/english_lexicon.txt",
                "terminal-view/src/main/resources/english_lexicon.txt",
                "app/src/main/assets/english_lexicon.txt",
                "../tests/english_lexicon.txt"
            };
            for (String p : tryPaths) {
                File f = new File(p);
                if (f.exists() && f.canRead()) {
                    try (InputStream is = new FileInputStream(f)) {
                        loadFromStream(is);
                    } catch (Throwable ignored) {}
                }
            }

            // Auto scan default $PREFIX/bin if on Android device
            scanBinDirectory(new File("/data/data/com.termuxpro.app/files/usr/bin"));

            sInitialized = true;
        }
    }

    private static void loadDevCorpus() {
        String[] corpusPaths = {
            "tests/english_corpus.txt",
            "terminal-view/src/main/resources/english_corpus.txt",
            "app/src/main/assets/english_corpus.txt",
            "../tests/english_corpus.txt"
        };
        for (String cp : corpusPaths) {
            File f = new File(cp);
            if (f.exists() && f.canRead()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String w = line.trim().toLowerCase(Locale.ROOT);
                        if (!w.isEmpty() && w.length() >= 2) {
                            sDevWords.add(w);
                            sWords.add(w);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    public static int loadFromStream(InputStream is) {
        if (is == null) return sWords.size();
        int added = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String w = line.trim().toLowerCase(Locale.ROOT);
                if (!w.isEmpty() && w.length() >= 2) {
                    if (sWords.add(w)) {
                        added++;
                    }
                }
            }
        } catch (Throwable ignored) {}
        sInitialized = true;
        return sWords.size();
    }

    public static void scanBinDirectory(File binDir) {
        if (binDir != null && binDir.exists() && binDir.isDirectory()) {
            File[] files = binDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.canExecute()) {
                        String name = f.getName().toLowerCase(Locale.ROOT);
                        if (!name.isEmpty() && name.length() >= 2) {
                            sWords.add(name);
                        }
                    }
                }
            }
        }
    }

    public static boolean isDevWord(String word) {
        if (word == null || word.length() < 2) return false;
        if (!sInitialized) {
            initDefault();
        }
        return sDevWords.contains(word.toLowerCase(Locale.ROOT));
    }

    public static boolean contains(String word) {
        if (word == null || word.length() < 2) return false;
        if (!sInitialized) {
            initDefault();
        }
        String lower = word.toLowerCase(Locale.ROOT);
        return sWords.contains(lower) || sDevWords.contains(lower);
    }

    public static int getWordCount() {
        if (!sInitialized) {
            initDefault();
        }
        return sWords.size();
    }

    public static void clear() {
        sWords.clear();
        sDevWords.clear();
        for (String w : CORE_FALLBACK) {
            String lw = w.toLowerCase(Locale.ROOT);
            sWords.add(lw);
            sDevWords.add(lw);
        }
    }
}
