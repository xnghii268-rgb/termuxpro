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

        sessionSubtitleView.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(sessionTitle)) {
            sessionSubtitleView.setText(sessionTitle);
        } else {
            boolean isRunning = sessionAtRow.isRunning();
            sessionSubtitleView.setText("PID " + sessionAtRow.getPid() + " • " + (isRunning ? "Đang chạy" : "Đã kết thúc"));
        }

        // Color tag
        int tagColor = sSessionColorMap.getOrDefault(sessionAtRow.mHandle, TAG_COLORS[position % TAG_COLORS.length]);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(tagColor);
        dot.setSize(22, 22);
        colorTagView.setBackground(dot);

        // Strike through if stopped
        boolean sessionRunning = sessionAtRow.isRunning();
        if (sessionRunning) {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            sessionTitleView.setTextColor(isActive ? Color.WHITE : Color.parseColor("#E2E8F0"));
            sessionSubtitleView.setTextColor(isActive ? Color.parseColor("#CBD5E1") : Color.parseColor("#94A3B8"));
        } else {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            sessionTitleView.setTextColor(Color.parseColor("#EF4444"));
            sessionSubtitleView.setTextColor(Color.parseColor("#F87171"));
        }

        // Close button action
        closeButton.setOnClickListener(v -> promptCloseSession(sessionAtRow));

        return sessionRowView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        updateDrawerHeaderCount();
    }

    public void updateDrawerHeaderCount() {
        if (mActivity == null) return;
        mActivity.runOnUiThread(() -> {
            TextView countText = mActivity.findViewById(R.id.drawer_session_count_text);
            if (countText != null) {
                int count = getCount();
                int running = 0;
                for (int i = 0; i < count; i++) {
                    TermuxSession ts = getItem(i);
                    if (ts != null && ts.getTerminalSession() != null && ts.getTerminalSession().isRunning()) {
                        running++;
                    }
                }
                countText.setText(count + " phiên đang mở • " + running + " đang chạy");
            }
        });
    }

    private void promptCloseSession(final TerminalSession session) {
        com.termux.app.ui.GlassSheet.showConfirmationSheet(
            mActivity,
            mActivity.getString(R.string.title_confirm_kill_process),
            "Đóng phiên này sẽ dừng các tiến trình đang chạy trong phiên.",
            "Đóng phiên",
            () -> {
                session.finishIfRunning();
                mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                notifyDataSetChanged();
                Toast.makeText(mActivity, "Đã đóng phiên", Toast.LENGTH_SHORT).show();
            },
            "Hủy",
            null
        );
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

        List<String> options = java.util.Arrays.asList(
            "Đổi tên phiên",
            "Đổi màu thẻ",
            "Đóng phiên"
        );

        com.termux.app.ui.GlassSheet.showListSheet(
            mActivity,
            "Tùy chọn phiên",
            options,
            -1,
            which -> {
                if (which == 0) {
                    mActivity.getTermuxTerminalSessionClient().renameSession(session);
                } else if (which == 1) {
                    showColorTagDialog(session);
                } else if (which == 2) {
                    promptCloseSession(session);
                }
            }
        );

        return true;
    }

    private void showColorTagDialog(final TerminalSession session) {
        com.termux.app.ui.GlassSheet.showListSheet(
            mActivity,
            mActivity.getString(R.string.action_tag_color),
            java.util.Arrays.asList(TAG_COLOR_NAMES),
            -1,
            which -> {
                sSessionColorMap.put(session.mHandle, TAG_COLORS[which]);
                notifyDataSetChanged();
            }
        );
    }

}
