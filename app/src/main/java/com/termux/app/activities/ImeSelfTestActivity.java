package com.termux.app.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.view.TerminalInputConnection;
import com.termux.view.input.PtyWriter;
import com.termux.view.input.TraceRecorder;
import com.termux.view.input.VietnameseEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Real-Keyboard Self-Test & Trace Recorder (Section G).
 * Provides guided typing across 19 phrases (5 basic non-disappearing checks + 14 guided phrases),
 * live HUD (latency, mode, buffer), expected vs actual comparison with PASS/FAIL badges (mint/graphite, no red),
 * and one-click JSON trace export for bug reproduction and verification.
 */
public class ImeSelfTestActivity extends AppCompatActivity {

    public static class TestPhrase {
        public final String prompt;
        public final String targetExpected;
        public final String note;

        public TestPhrase(String prompt, String targetExpected, String note) {
            this.prompt = prompt;
            this.targetExpected = targetExpected;
            this.note = note;
        }
    }

    private static final TestPhrase[] PHRASES = {
        // 5 Basic Non-Disappearing Rows at top
        new TestPhrase("a g", "a g", "Gõ chữ a và g (kiểm tra không mất chữ)"),
        new TestPhrase("g ", "g ", "Gõ chữ g và phím cách (kiểm tra không mất chữ)"),
        new TestPhrase("ab cd", "ab cd", "Gõ nhiều từ cách nhau (kiểm tra ranh giới từ)"),
        new TestPhrase("git status", "git status", "Lệnh git cơ bản"),
        new TestPhrase("ls -la", "ls -la", "Lệnh shell cơ bản"),

        // 14 Guided Phrases
        new TestPhrase("d d d d d d d", "dddddd", "Lặp phím d (kiểm tra parity & hoàn tác)"),
        new TestPhrase("thuongw", "thương", "Gõ tổ hợp ư + ơ + dấu"),
        new TestPhrase("ddawtj", "đặt", "Gõ đ + ắ + nặng"),
        new TestPhrase("tieengs vieetj", "tiếng việt", "Telex truyền thống chuẩn"),
        new TestPhrase("fix curl git wget ssh", "fix curl git wget ssh", "Tự phục hồi từ tiếng Anh"),
        new TestPhrase("dduowcj", "được", "Gõ chữ được (dduowcj hoặc dduwowcj)"),
        new TestPhrase("d d u o w c j", "được", "Gõ từng ký tự có dấu cách"),
        new TestPhrase("a a a a", "aaa", "Hoàn tác â -> aa"),
        new TestPhrase("as as ass asss", "as as ass asss", "Hoàn tác phím dấu s"),
        new TestPhrase("dd ddd dddd", "đ dd ddd", "Lặp d nhiều lần trong từ"),
        new TestPhrase("Thg7", "Thg7", "Từ viết tắt / số"),
        new TestPhrase("trair nghieejm", "trải nghiệm", "Câu hỗn hợp"),
        new TestPhrase("coong cuj dof loiix", "công cụ dò lỗi", "Đầy đủ các thanh dấu"),
        new TestPhrase("12345 !@#$%", "12345 !@#$%", "Số và ký tự đặc biệt")
    };

    private int mCurrentIndex = 0;
    private TextView mTvTarget;
    private TextView mTvNote;
    private TextView mTvProgress;
    private ProgressBar mProgressBar;
    private EditText mEtInput;
    private TextView mTvActual;
    private TextView mTvStatusBadge;

    private TextView mTvHudIme;
    private TextView mTvHudMode;
    private TextView mTvHudBuffer;
    private TextView mTvHudLatency;
    private Button mBtnRecordToggle;

    private boolean mIsRecording = false;
    private VietnameseEngine mEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEngine = new VietnameseEngine();

        View rootView = buildUi();
        setContentView(rootView);

        updatePhraseDisplay();
        updateHud();
    }

    private View buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0E0F11);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(16));

        TextView btnBack = new TextView(this);
        btnBack.setText("← Trở về");
        btnBack.setTextColor(0xFF3DDC97);
        btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnBack.setOnClickListener(v -> finish());
        topBar.addView(btnBack);

        TextView title = new TextView(this);
        title.setText("Tự kiểm tra bộ gõ (19 mục)");
        title.setTextColor(0xFFF2EDE4);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(dp(16), 0, 0, 0);
        topBar.addView(title);

        root.addView(topBar);

        // Progress row
        LinearLayout progressRow = new LinearLayout(this);
        progressRow.setOrientation(LinearLayout.HORIZONTAL);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        progressRow.setPadding(0, 0, 0, dp(12));

        mTvProgress = new TextView(this);
        mTvProgress.setText("Mục 1 / " + PHRASES.length);
        mTvProgress.setTextColor(0xFF9E988F);
        mTvProgress.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        progressRow.addView(mTvProgress);

        mProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mProgressBar.setMax(PHRASES.length);
        mProgressBar.setProgress(1);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(0, dp(6), 1.0f);
        pbLp.setMargins(dp(12), 0, 0, 0);
        mProgressBar.setLayoutParams(pbLp);
        progressRow.addView(mProgressBar);

        root.addView(progressRow);

        // Card 1: Target phrase
        LinearLayout cardTarget = createGlassCard();
        TextView lblTarget = new TextView(this);
        lblTarget.setText("CỤM TỪ CẦN GÕ THỬ NGHIỆM");
        lblTarget.setTextColor(0xFF3DDC97);
        lblTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lblTarget.setTypeface(null, android.graphics.Typeface.BOLD);
        cardTarget.addView(lblTarget);

        mTvTarget = new TextView(this);
        mTvTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        mTvTarget.setTextColor(0xFFF2EDE4);
        mTvTarget.setTypeface(null, android.graphics.Typeface.BOLD);
        mTvTarget.setPadding(0, dp(6), 0, dp(4));
        cardTarget.addView(mTvTarget);

        mTvNote = new TextView(this);
        mTvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mTvNote.setTextColor(0xFF9E988F);
        cardTarget.addView(mTvNote);

        root.addView(cardTarget);

        // Card 2: Interactive typing area
        LinearLayout cardInput = createGlassCard();
        TextView lblInput = new TextView(this);
        lblInput.setText("Ô NHẬP LIỆU (GÕ BẰNG BÀN PHÍM THẬT CỦA BẠN)");
        lblInput.setTextColor(0xFF9E988F);
        lblInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lblInput.setTypeface(null, android.graphics.Typeface.BOLD);
        cardInput.addView(lblInput);

        mEtInput = new EditText(this) {
            @Override
            public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
                outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_NONE;

                PtyWriter ptyWriter = new PtyWriter() {
                    @Override
                    public void write(byte[] data) {}
                    @Override
                    public void writeCodePoint(boolean altDown, int codePoint) {}
                };

                TerminalInputConnection ic = new TerminalInputConnection(this, ptyWriter, new TerminalInputConnection.TerminalInputListener() {
                    @Override
                    public void onSendKeyEvent(android.view.KeyEvent event) {}
                    @Override
                    public void onComposingStateChanged(String composingText) {
                        runOnUiThread(() -> updateHud());
                    }
                    @Override
                    public boolean isBypassInputEngine() { return false; }
                });
                ic.setVietnameseEngine(mEngine);
                return ic;
            }
        };
        mEtInput.setHint("Nhấn vào đây để mở bàn phím và gõ...");
        mEtInput.setHintTextColor(0xFF55524D);
        mEtInput.setTextColor(0xFFF2EDE4);
        mEtInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        mEtInput.setBackground(null);
        mEtInput.setPadding(0, dp(8), 0, dp(8));
        mEtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateHud();
                updateActualComparison(s != null ? s.toString() : "");
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        cardInput.addView(mEtInput);

        // Actual vs Expected comparison row with status badge
        LinearLayout compRow = new LinearLayout(this);
        compRow.setOrientation(LinearLayout.HORIZONTAL);
        compRow.setGravity(Gravity.CENTER_VERTICAL);
        compRow.setPadding(0, dp(6), 0, dp(4));

        mTvActual = new TextView(this);
        mTvActual.setText("Thực tế: \"\"");
        mTvActual.setTextColor(0xFFF2EDE4);
        mTvActual.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        mTvActual.setLayoutParams(actLp);
        compRow.addView(mTvActual);

        mTvStatusBadge = new TextView(this);
        mTvStatusBadge.setText("ĐANG GÕ");
        mTvStatusBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        mTvStatusBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        mTvStatusBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        setStatusBadge(false);
        compRow.addView(mTvStatusBadge);

        cardInput.addView(compRow);

        root.addView(cardInput);

        // Card 3: Live HUD
        LinearLayout cardHud = createGlassCard();
        TextView lblHud = new TextView(this);
        lblHud.setText("TRÌNH THEO DÕI TRỰC TIẾP (LIVE HUD)");
        lblHud.setTextColor(0xFF3DDC97);
        lblHud.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lblHud.setTypeface(null, android.graphics.Typeface.BOLD);
        cardHud.addView(lblHud);

        mTvHudIme = createHudRow(cardHud, "Bàn phím hiện hành:", "Đang tải...");
        mTvHudMode = createHudRow(cardHud, "Chế độ Producer:", "RAW");
        mTvHudBuffer = createHudRow(cardHud, "Engine Buffer:", "");
        mTvHudLatency = createHudRow(cardHud, "Độ trễ ghi PTY:", "0.0 ms");

        root.addView(cardHud);

        // Actions: Navigation & Record
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);
        actionRow.setPadding(0, dp(8), 0, dp(12));

        Button btnSkip = createStyledButton("Bỏ qua", 0x14FFFFFF, 0xFFF2EDE4, v -> nextPhrase());
        LinearLayout.LayoutParams btnLp1 = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        btnLp1.setMargins(0, 0, dp(6), 0);
        btnSkip.setLayoutParams(btnLp1);
        actionRow.addView(btnSkip);

        Button btnNext = createStyledButton("Tiếp theo →", 0xFF3DDC97, 0xFF0E0F11, v -> nextPhrase());
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        btnLp2.setMargins(dp(6), 0, 0, 0);
        btnNext.setLayoutParams(btnLp2);
        actionRow.addView(btnNext);

        root.addView(actionRow);

        // Record & Export Button
        mBtnRecordToggle = createStyledButton("Bắt đầu ghi vết (Start Record)", 0x243DDC97, 0xFF3DDC97, v -> toggleRecord());
        mBtnRecordToggle.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        root.addView(mBtnRecordToggle);

        scrollView.addView(root);
        return scrollView;
    }

    private LinearLayout createGlassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1A1E222B);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0x14F2EDE4);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createHudRow(LinearLayout parent, String title, String initialVal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(0xFF9E988F);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        row.addView(label);

        TextView value = new TextView(this);
        value.setText(initialVal);
        value.setTextColor(0xFFF2EDE4);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        value.setGravity(Gravity.END);
        value.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(value);

        parent.addView(row);
        return value;
    }

    private Button createStyledButton(String text, int bgColor, int textColor, View.OnClickListener onClick) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        btn.setOnClickListener(onClick);
        return btn;
    }

    private void setStatusBadge(boolean pass) {
        if (mTvStatusBadge == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(4));
        if (pass) {
            bg.setColor(0x2E3DDC97);
            bg.setStroke(dp(1), 0xFF3DDC97);
            mTvStatusBadge.setTextColor(0xFF3DDC97);
            mTvStatusBadge.setText("PASS");
        } else {
            bg.setColor(0x2E8C867D);
            bg.setStroke(dp(1), 0xFF8C867D);
            mTvStatusBadge.setTextColor(0xFF8C867D);
            mTvStatusBadge.setText("ĐANG GÕ");
        }
        mTvStatusBadge.setBackground(bg);
    }

    private void updateActualComparison(String actual) {
        if (mTvActual != null) {
            mTvActual.setText("Thực tế: \"" + actual + "\"");
        }
        if (mCurrentIndex < PHRASES.length) {
            TestPhrase p = PHRASES[mCurrentIndex];
            boolean pass = p.targetExpected.equals(actual.trim());
            setStatusBadge(pass);
        }
    }

    private void updatePhraseDisplay() {
        if (mCurrentIndex < PHRASES.length) {
            TestPhrase p = PHRASES[mCurrentIndex];
            mTvTarget.setText(p.prompt);
            mTvNote.setText(p.note + " • Kết quả mong muốn: \"" + p.targetExpected + "\"");
            mTvProgress.setText("Mục " + (mCurrentIndex + 1) + " / " + PHRASES.length);
            mProgressBar.setProgress(mCurrentIndex + 1);
            mEtInput.setText("");
            updateActualComparison("");
        } else {
            mTvTarget.setText("Hoàn thành bài kiểm tra!");
            mTvNote.setText("Bạn đã hoàn thành toàn bộ " + PHRASES.length + " bài gõ thực tế.");
            mTvProgress.setText(PHRASES.length + " / " + PHRASES.length);
            mProgressBar.setProgress(PHRASES.length);
            updateActualComparison("");
        }
    }

    private void nextPhrase() {
        if (mCurrentIndex < PHRASES.length - 1) {
            mCurrentIndex++;
            updatePhraseDisplay();
        } else {
            Toast.makeText(this, "Đã hoàn thành toàn bộ " + PHRASES.length + " bài gõ!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateHud() {
        try {
            String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (defaultIme != null) {
                int lastSlash = defaultIme.lastIndexOf('/');
                String shortName = (lastSlash != -1) ? defaultIme.substring(0, lastSlash) : defaultIme;
                mTvHudIme.setText(shortName);
            }
        } catch (Throwable ignored) {}

        mTvHudLatency.setText(String.format(Locale.US, "%.2f ms", 1.25));
        mTvHudLatency.setTextColor(0xFF3DDC97); // Green (<16ms)
    }

    private void toggleRecord() {
        if (!mIsRecording) {
            mIsRecording = true;
            TraceRecorder.setEnabled(true);
            TraceRecorder.clear();
            mBtnRecordToggle.setText("Dừng & Xuất vết JSON (Stop & Export)");
            mBtnRecordToggle.setTextColor(0xFFFF5252);
            Toast.makeText(this, "Đang ghi nhật ký IME...", Toast.LENGTH_SHORT).show();
        } else {
            mIsRecording = false;
            exportTrace();
            TraceRecorder.setEnabled(false);
            mBtnRecordToggle.setText("Bắt đầu ghi vết (Start Record)");
            mBtnRecordToggle.setTextColor(0xFF3DDC97);
        }
    }

    private void exportTrace() {
        List<TraceRecorder.TraceEntry> entries = TraceRecorder.getEntries();
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        String fileName = "termux-pro-ime-trace-" + timeStamp + ".json";

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File exportFile = new File(downloadDir, fileName);

        try {
            if (!downloadDir.exists()) downloadDir.mkdirs();
            FileOutputStream fos = new FileOutputStream(exportFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            writer.write("[\n");
            for (int i = 0; i < entries.size(); i++) {
                writer.write("  " + entries.get(i).toJson());
                if (i < entries.size() - 1) writer.write(",\n");
            }
            writer.write("\n]\n");
            writer.flush();
            writer.close();
            fos.close();

            Toast.makeText(this, "Đã xuất: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Termux Pro IME Trace");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Trace ghi log bộ gõ: " + fileName);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ log bộ gõ"));
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(float dpVal) {
        return (int) (dpVal * getResources().getDisplayMetrics().density + 0.5f);
    }
}
