package com.termux.app.features;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.theme.TermuxProThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter rendering Inset-Grouped cards with iOS-27-grade polish.
 * Displays sections for "Đã ghim" and "Gần đây" / "Tất cả mẫu".
 */
public class CommandPaletteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    public interface OnItemActionListener {
        void onItemClick(CommandItem item);
        void onOptionsClick(CommandItem item, View anchorView);
    }

    public static class RowModel {
        public final int type;
        public final String headerTitle;
        public final CommandItem item;
        public final boolean isFirstInGroup;
        public final boolean isLastInGroup;

        public RowModel(String headerTitle) {
            this.type = VIEW_TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.item = null;
            this.isFirstInGroup = false;
            this.isLastInGroup = false;
        }

        public RowModel(CommandItem item, boolean isFirst, boolean isLast) {
            this.type = VIEW_TYPE_ITEM;
            this.headerTitle = null;
            this.item = item;
            this.isFirstInGroup = isFirst;
            this.isLastInGroup = isLast;
        }
    }

    private final Context mContext;
    private final OnItemActionListener mListener;
    private final List<RowModel> mRows = new ArrayList<>();
    private final boolean mIsDarkTheme;
    private final float mDensity;

    private final int mTextColor;
    private final int mSubTextColor;
    private final int mCardBgColor;
    private final int mSeparatorColor;
    private final int mAccentColor;

    public CommandPaletteAdapter(Context context, OnItemActionListener listener) {
        this.mContext = context;
        this.mListener = listener;
        this.mDensity = context.getResources().getDisplayMetrics().density;

        String themeId = TermuxProThemeManager.getCurrentThemeId(context);
        this.mIsDarkTheme = !themeId.equals("giay") && !themeId.equals("suong");

        this.mTextColor = mIsDarkTheme ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        this.mSubTextColor = mIsDarkTheme ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
        this.mCardBgColor = mIsDarkTheme ? Color.parseColor("#1E2430") : Color.parseColor("#FFFFFF");
        this.mSeparatorColor = mIsDarkTheme ? Color.parseColor("#1AFFFFFF") : Color.parseColor("#14000000");
        this.mAccentColor = Color.parseColor("#FF3C70");
    }

    public void updateData(List<CommandItem> pinnedItems, List<CommandItem> regularItems, boolean isTemplateTab) {
        mRows.clear();

        // 1. Pinned Section (only if pins exist)
        if (pinnedItems != null && !pinnedItems.isEmpty()) {
            mRows.add(new RowModel("ĐÃ GHIM"));
            for (int i = 0; i < pinnedItems.size(); i++) {
                boolean first = (i == 0);
                boolean last = (i == pinnedItems.size() - 1);
                mRows.add(new RowModel(pinnedItems.get(i), first, last));
            }
        }

        // 2. Regular Section
        if (regularItems != null && !regularItems.isEmpty()) {
            String sectionTitle = isTemplateTab ? "TẤT CẢ MẪU" : "GẦN ĐÂY";
            mRows.add(new RowModel(sectionTitle));
            for (int i = 0; i < regularItems.size(); i++) {
                boolean first = (i == 0);
                boolean last = (i == regularItems.size() - 1);
                mRows.add(new RowModel(regularItems.get(i), first, last));
            }
        }

        notifyDataSetChanged();
    }

    public CommandItem getItemAt(int position) {
        if (position >= 0 && position < mRows.size()) {
            return mRows.get(position).item;
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return mRows.get(position).type;
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            TextView headerView = new TextView(mContext);
            headerView.setTextSize(12);
            headerView.setTypeface(null, Typeface.BOLD);
            headerView.setTextColor(mSubTextColor);
            headerView.setLetterSpacing(0.06f);
            int leftPad = (int)(20 * mDensity);
            int topPad = (int)(16 * mDensity);
            int bottomPad = (int)(6 * mDensity);
            headerView.setPadding(leftPad, topPad, leftPad, bottomPad);
            headerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderViewHolder(headerView);
        } else {
            return new ItemViewHolder(createItemRowView());
        }
    }

    private View createItemRowView() {
        FrameLayout container = new FrameLayout(mContext);
        int marginH = (int)(16 * mDensity);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        containerParams.leftMargin = marginH;
        containerParams.rightMargin = marginH;
        container.setLayoutParams(containerParams);

        LinearLayout row = new LinearLayout(mContext);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = (int)(14 * mDensity);
        int padV = (int)(10 * mDensity);
        row.setPadding(padH, padV, (int)(8 * mDensity), padV);
        row.setMinimumHeight((int)(58 * mDensity));
        row.setId(R.id.row_container_id);

        // Leading Pin Icon
        ImageView pinIcon = new AppCompatImageView(mContext);
        pinIcon.setId(R.id.pin_icon_id);
        int pinSize = (int)(16 * mDensity);
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(pinSize, pinSize);
        pinParams.rightMargin = (int)(10 * mDensity);
        pinIcon.setLayoutParams(pinParams);
        pinIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_pin_filled));
        row.addView(pinIcon);

        // Text Column
        LinearLayout textCol = new LinearLayout(mContext);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textColParams);

        // Command (Monospace, near-white)
        TextView cmdView = new TextView(mContext);
        cmdView.setId(R.id.cmd_text_id);
        cmdView.setTypeface(Typeface.MONOSPACE);
        cmdView.setTextSize(13.5f);
        cmdView.setTextColor(mTextColor);
        cmdView.setMaxLines(2);
        cmdView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(cmdView);

        // Subtitle (Relative time or template title)
        TextView subView = new TextView(mContext);
        subView.setId(R.id.sub_text_id);
        subView.setTextSize(11.5f);
        subView.setTextColor(mSubTextColor);
        subView.setSingleLine(true);
        subView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = (int)(2 * mDensity);
        textCol.addView(subView, subParams);

        row.addView(textCol);

        // Trailing "…" button (>= 48dp touch target)
        FrameLayout moreBtn = new FrameLayout(mContext);
        moreBtn.setId(R.id.more_btn_id);
        int touchTarget = (int)(48 * mDensity);
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(touchTarget, touchTarget);
        moreBtn.setLayoutParams(moreParams);

        ImageView moreIcon = new AppCompatImageView(mContext);
        int iconSize = (int)(20 * mDensity);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
        moreIcon.setLayoutParams(iconParams);
        moreIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_more_dots));
        moreIcon.setColorFilter(mSubTextColor);
        moreBtn.addView(moreIcon);

        // Ripple on more button
        addRipple(moreBtn, 24 * mDensity);

        row.addView(moreBtn);

        container.addView(row);

        // Bottom hairline separator
        View separator = new View(mContext);
        separator.setId(R.id.separator_id);
        FrameLayout.LayoutParams sepParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int)(0.5f * mDensity)), Gravity.BOTTOM);
        sepParams.leftMargin = (int)(14 * mDensity);
        sepParams.rightMargin = (int)(14 * mDensity);
        separator.setLayoutParams(sepParams);
        separator.setBackgroundColor(mSeparatorColor);
        container.addView(separator);

        return container;
    }

    private void addRipple(View view, float radius) {
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(radius);
        mask.setColor(Color.WHITE);
        int rippleColor = mIsDarkTheme ? Color.parseColor("#33FFFFFF") : Color.parseColor("#1F000000");
        RippleDrawable ripple = new RippleDrawable(
            android.content.res.ColorStateList.valueOf(rippleColor), null, mask);
        view.setBackground(ripple);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowModel rowModel = mRows.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).titleView.setText(rowModel.headerTitle);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder ivh = (ItemViewHolder) holder;
            CommandItem item = rowModel.item;
            if (item == null) return;

            // Command
            ivh.cmdText.setText(item.getCommand());

            // Subtitle
            if (item.isTemplate()) {
                ivh.subText.setText(item.getTitle());
            } else {
                ivh.subText.setText(item.getFormattedRelativeTime());
            }

            // Pin Icon visibility
            ivh.pinIcon.setVisibility(item.isPinned() ? View.VISIBLE : View.GONE);

            // Separator visibility (hide on last item in group)
            ivh.separator.setVisibility(rowModel.isLastInGroup ? View.GONE : View.VISIBLE);

            // iOS Inset Grouped card background styling
            float r = 14 * mDensity;
            float[] radii;
            if (rowModel.isFirstInGroup && rowModel.isLastInGroup) {
                // Single item: round all 4 corners
                radii = new float[]{r, r, r, r, r, r, r, r};
            } else if (rowModel.isFirstInGroup) {
                // Top item: round top corners only
                radii = new float[]{r, r, r, r, 0, 0, 0, 0};
            } else if (rowModel.isLastInGroup) {
                // Bottom item: round bottom corners only
                radii = new float[]{0, 0, 0, 0, r, r, r, r};
            } else {
                // Middle item: no rounded corners
                radii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
            }

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadii(radii);
            cardBg.setColor(mCardBgColor);

            GradientDrawable mask = new GradientDrawable();
            mask.setCornerRadii(radii);
            mask.setColor(Color.WHITE);

            int rippleColor = mIsDarkTheme ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000");
            RippleDrawable rowRipple = new RippleDrawable(
                android.content.res.ColorStateList.valueOf(rippleColor), cardBg, mask);
            ivh.rowContainer.setBackground(rowRipple);

            // Tap row
            ivh.rowContainer.setOnClickListener(v -> {
                // Subtle scale animation
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(75).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(75).start();
                    if (mListener != null) mListener.onItemClick(item);
                }).start();
            });

            // Long-press
            ivh.rowContainer.setOnLongClickListener(v -> {
                if (mListener != null) {
                    mListener.onOptionsClick(item, v);
                    return true;
                }
                return false;
            });

            // More button
            ivh.moreBtn.setOnClickListener(v -> {
                if (mListener != null) mListener.onOptionsClick(item, v);
            });
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView titleView;
        HeaderViewHolder(View itemView) {
            super(itemView);
            this.titleView = (TextView) itemView;
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final View rowContainer;
        final ImageView pinIcon;
        final TextView cmdText;
        final TextView subText;
        final View moreBtn;
        final View separator;

        ItemViewHolder(View itemView) {
            super(itemView);
            this.rowContainer = itemView.findViewById(R.id.row_container_id);
            this.pinIcon = itemView.findViewById(R.id.pin_icon_id);
            this.cmdText = itemView.findViewById(R.id.cmd_text_id);
            this.subText = itemView.findViewById(R.id.sub_text_id);
            this.moreBtn = itemView.findViewById(R.id.more_btn_id);
            this.separator = itemView.findViewById(R.id.separator_id);
        }
    }
}
