package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermuxSessionsListViewController extends ArrayAdapter<TermuxSession> implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    final TermuxActivity mActivity;

    // Palette of tag colors: Emerald, Sapphire, Amber, Coral, Lavender
    public static final int[] TAG_COLORS = {
        0xFF10B981, // Emerald
        0xFF38BDF8, // Sapphire
        0xFFF59E0B, // Amber
        0xFFEF4444, // Coral
        0xFFC084FC  // Lavender
    };

    public static final String[] TAG_COLOR_NAMES = {
        "Xanh ngọc (Emerald)",
        "Xanh lam (Sapphire)",
        "Hổ phách (Amber)",
        "San hô (Coral)",
        "Oải hương (Lavender)"
    };

    private static final Map<String, Integer> sSessionColorMap = new HashMap<>();

    public TermuxSessionsListViewController(TermuxActivity activity, List<TermuxSession> sessionList) {
        super(activity.getApplicationContext(), R.layout.item_terminal_sessions_list, sessionList);
        this.mActivity = activity;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View sessionRowView = convertView;
        if (sessionRowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            sessionRowView = inflater.inflate(R.layout.item_terminal_sessions_list, parent, false);
        }

        final TermuxSession termuxSession = getItem(position);
        if (termuxSession == null || termuxSession.getTerminalSession() == null) {
            return sessionRowView;
        }

        final TerminalSession sessionAtRow = termuxSession.getTerminalSession();
        TextView sessionTitleView = sessionRowView.findViewById(R.id.session_title);
        TextView sessionSubtitleView = sessionRowView.findViewById(R.id.session_subtitle);
        View colorTagView = sessionRowView.findViewById(R.id.session_color_tag);
        ImageView closeButton = sessionRowView.findViewById(R.id.session_close_button);

        // Highlight active session
        TerminalSession currentSession = mActivity.getCurrentSession();
        boolean isActive = (sessionAtRow == currentSession);
        sessionRowView.setBackgroundResource(isActive ? R.drawable.bg_glass_session_item_selected : R.drawable.bg_glass_session_item);

        // Session name & title
        String name = sessionAtRow.mSessionName;
        String sessionTitle = sessionAtRow.getTitle();
        String numberPart = "Phiên " + (position + 1);

        if (!TextUtils.isEmpty(name)) {
            sessionTitleView.setText(name + " (" + numberPart + ")");
        } else {
            sessionTitleView.setText(numberPart);
        }

        if (!TextUtils.isEmpty(sessionTitle)) {
            sessionSubtitleView.setVisibility(View.VISIBLE);
            sessionSubtitleView.setText(sessionTitle);
        } else {
            sessionSubtitleView.setVisibility(View.GONE);
        }

        // Color tag
        int tagColor = sSessionColorMap.getOrDefault(sessionAtRow.mHandle, TAG_COLORS[position % TAG_COLORS.length]);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(tagColor);
        dot.setSize(20, 20);
        colorTagView.setBackground(dot);

        // Strike through if stopped
        boolean sessionRunning = sessionAtRow.isRunning();
        if (sessionRunning) {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            sessionTitleView.setTextColor(Color.WHITE);
        } else {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            sessionTitleView.setTextColor(Color.RED);
        }

        // Close button action
        closeButton.setOnClickListener(v -> promptCloseSession(sessionAtRow));

        return sessionRowView;
    }

    private void promptCloseSession(final TerminalSession session) {
        new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_confirm_kill_process)
            .setMessage("Đóng phiên này sẽ dừng các tiến trình đang chạy trong phiên.")
            .setPositiveButton("Đóng phiên", (dialog, which) -> {
                session.finishIfRunning();
                mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                notifyDataSetChanged();
                Toast.makeText(mActivity, "Đã đóng phiên", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TermuxSession clickedSession = getItem(position);
        if (clickedSession != null) {
            mActivity.getTermuxTerminalSessionClient().setCurrentSession(clickedSession.getTerminalSession());
            mActivity.getDrawer().closeDrawers();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final TermuxSession selectedSession = getItem(position);
        if (selectedSession == null || selectedSession.getTerminalSession() == null) return false;
        final TerminalSession session = selectedSession.getTerminalSession();

        String[] options = {
            "Đổi tên phiên",
            "Đổi màu thẻ",
            "Đóng phiên"
        };

        new AlertDialog.Builder(mActivity)
            .setTitle("Tùy chọn phiên")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    mActivity.getTermuxTerminalSessionClient().renameSession(session);
                } else if (which == 1) {
                    showColorTagDialog(session);
                } else if (which == 2) {
                    promptCloseSession(session);
                }
            })
            .setNegativeButton("Hủy", null)
            .show();

        return true;
    }

    private void showColorTagDialog(final TerminalSession session) {
        new AlertDialog.Builder(mActivity)
            .setTitle(R.string.action_tag_color)
            .setItems(TAG_COLOR_NAMES, (dialog, which) -> {
                sSessionColorMap.put(session.mHandle, TAG_COLORS[which]);
                notifyDataSetChanged();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

}
