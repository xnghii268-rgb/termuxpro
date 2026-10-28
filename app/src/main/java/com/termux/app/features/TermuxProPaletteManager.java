package com.termux.app.features;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.theme.TermuxProThemeManager;
import com.termux.terminal.TerminalSession;

import java.util.List;

/**
 * Rebuilt Command Palette (Bảng lệnh tương tác) with iOS-27-grade polish.
 * Features:
 * - Segmented control: "Lịch sử" (History) | "Mẫu lệnh" (Templates).
 * - Real-time search filter with search & clear icons.
 * - Section "Đã ghim" (Pins max 5 across history + templates).
 * - History hard cap (100) with eviction of oldest unpinned entry.
 * - Inset-grouped cards, hairline separators, 56-64dp rows.
 * - Touch & Gestures:
 *   - Tap row: inserts command into terminal without auto-run.
 *   - Swipe left: Xoá (with undo Snackbar).
 *   - Swipe right: Ghim / Bỏ ghim.
 *   - Long-press or trailing "…": Context menu (Ghim, Sao chép, Sửa, Xoá).
 * - Full fix for nested scroll / drag bug:
 *   - RecyclerView owns vertical scrolling; sheet drag only from handle/top bar or scrollTop == 0 downward.
 *   - Dragging up never dismisses.
 *   - Half state (~60%) and expanded state (~92%).
 *   - Soft input adjust resize for keyboard friendliness.
 * - 100% Vietnamese typography and natural translations.
 */
public class TermuxProPaletteManager {

    public static void showCommandPalette(final TermuxActivity activity) {
        final CommandStore store = CommandStore.getInstance(activity);
        final float density = activity.getResources().getDisplayMetrics().density;
        final int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        final String currentThemeId = TermuxProThemeManager.getCurrentThemeId(activity);
        final boolean isDark = !currentThemeId.equals("giay") && !currentThemeId.equals("suong");

        final int textColor = isDark ? Color.parseColor("#F2EDE4") : Color.parseColor("#1C1917");
        final int subTextColor = isDark ? Color.parseColor("#9E988F") : Color.parseColor("#64748B");
        final int cardBgColor = isDark ? Color.parseColor("#1A1D24") : Color.parseColor("#FFFFFF");
        final int sheetBgColor = isDark ? Color.parseColor("#E61A1D24") : Color.parseColor("#F5FBF9F5");
        final int borderColor = isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#14000000");
        final int accentColor = Color.parseColor("#3DDC97");

        final BottomSheetDialog sheet = new BottomSheetDialog(activity);

        // State variables
        final boolean[] isTemplateTab = new boolean[]{false};
        final String[] currentQuery = new String[]{""};

        // Root container
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Drag Handle
        View handle = new View(activity);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setShape(GradientDrawable.RECTANGLE);
        handleBg.setCornerRadius(2.5f * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams((int)(36 * density), (int)(5 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = (int)(10 * density);
        handleParams.bottomMargin = (int)(12 * density);
        handle.setLayoutParams(handleParams);
        handle.setBackground(handleBg);
        root.addView(handle);

        // 2. Header Row: Large Title "Lệnh" + Action Button ("Thêm mẫu" or "Xoá lịch sử")
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding((int)(20 * density), 0, (int)(18 * density), (int)(10 * density));

        TextView titleView = new TextView(activity);
        titleView.setText("Lệnh");
        titleView.setTextSize(24);
        titleView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titleView.setTextColor(textColor);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);

        // Action Button in header
        TextView actionBtn = new TextView(activity);
        actionBtn.setTextSize(13.5f);
        actionBtn.setTypeface(null, Typeface.BOLD);
        actionBtn.setPadding((int)(14 * density), (int)(7 * density), (int)(14 * density), (int)(7 * density));
        headerRow.addView(actionBtn);

        FrameLayout closeBtn = com.termux.app.ui.GlassComponents.createRoundGlassButton(activity, R.drawable.ic_close, v -> sheet.dismiss());
        headerRow.addView(closeBtn);

        root.addView(headerRow);

        // 3. Segmented Control ("Lịch sử" | "Mẫu lệnh")
        LinearLayout segmentedContainer = new LinearLayout(activity);
        segmentedContainer.setOrientation(LinearLayout.HORIZONTAL);
        segmentedContainer.setGravity(Gravity.CENTER);
        int segMarginH = (int)(20 * density);
        LinearLayout.LayoutParams segContainerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (int)(40 * density));
        segContainerParams.leftMargin = segMarginH;
        segContainerParams.rightMargin = segMarginH;
        segContainerParams.bottomMargin = (int)(12 * density);
        segmentedContainer.setLayoutParams(segContainerParams);

        GradientDrawable segBg = new GradientDrawable();
        segBg.setCornerRadius(20 * density);
        segBg.setColor(isDark ? Color.parseColor("#14FFFFFF") : Color.parseColor("#0D000000"));
        segmentedContainer.setBackground(segBg);
        segmentedContainer.setPadding((int)(3 * density), (int)(3 * density), (int)(3 * density), (int)(3 * density));

        final TextView tabHistory = new TextView(activity);
        tabHistory.setText("Lịch sử");
        tabHistory.setTextSize(13.5f);
        tabHistory.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tab1Params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        tabHistory.setLayoutParams(tab1Params);

        final TextView tabTemplates = new TextView(activity);
        tabTemplates.setText("Mẫu lệnh");
        tabTemplates.setTextSize(13.5f);
        tabTemplates.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tab2Params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        tabTemplates.setLayoutParams(tab2Params);

        segmentedContainer.addView(tabHistory);
        segmentedContainer.addView(tabTemplates);
        root.addView(segmentedContainer);

        // 4. Search Bar (Search field with magnifier icon and clear button)
        LinearLayout searchBox = new LinearLayout(activity);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchBoxParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (int)(42 * density));
        searchBoxParams.leftMargin = segMarginH;
        searchBoxParams.rightMargin = segMarginH;
        searchBoxParams.bottomMargin = (int)(8 * density);
        searchBox.setLayoutParams(searchBoxParams);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setCornerRadius(13 * density);
        searchBg.setColor(isDark ? Color.parseColor("#1A1F2B") : Color.parseColor("#FFFFFF"));
        searchBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000"));
        searchBox.setBackground(searchBg);
        searchBox.setPadding((int)(12 * density), 0, (int)(10 * density), 0);

        ImageView searchIcon = new AppCompatImageView(activity);
        int sIconSize = (int)(18 * density);
        LinearLayout.LayoutParams sIconParams = new LinearLayout.LayoutParams(sIconSize, sIconSize);
        sIconParams.rightMargin = (int)(8 * density);
        searchIcon.setLayoutParams(sIconParams);
        searchIcon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_search));
        searchIcon.setColorFilter(subTextColor);
        searchBox.addView(searchIcon);

        final EditText searchInput = new EditText(activity);
        searchInput.setHint("Tìm lệnh…");
        searchInput.setHintTextColor(subTextColor);
        searchInput.setTextColor(textColor);
        searchInput.setTextSize(14);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams sInputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        searchInput.setLayoutParams(sInputParams);
        searchBox.addView(searchInput);

        final ImageView clearIcon = new AppCompatImageView(activity);
        int cIconSize = (int)(18 * density);
        LinearLayout.LayoutParams cIconParams = new LinearLayout.LayoutParams(cIconSize, cIconSize);
        clearIcon.setLayoutParams(cIconParams);
        clearIcon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_close));
        clearIcon.setColorFilter(subTextColor);
        clearIcon.setVisibility(View.GONE);
        clearIcon.setOnClickListener(v -> searchInput.setText(""));
        searchBox.addView(clearIcon);

        root.addView(searchBox);

        // 5. Content Frame (RecyclerView + Empty State)
        FrameLayout contentFrame = new FrameLayout(activity);
        LinearLayout.LayoutParams contentFrameParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        contentFrame.setLayoutParams(contentFrameParams);

        // RecyclerView with Nested Scrolling enabled
        final RecyclerView recyclerView = new RecyclerView(activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        recyclerView.setPadding(0, 0, 0, (int)(24 * density));
        recyclerView.setClipToPadding(false);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        contentFrame.addView(recyclerView);

        // Empty State View
        final LinearLayout emptyLayout = new LinearLayout(activity);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setGravity(Gravity.CENTER);
        emptyLayout.setVisibility(View.GONE);
        emptyLayout.setPadding((int)(32 * density), (int)(40 * density), (int)(32 * density), (int)(40 * density));
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        emptyLayout.setLayoutParams(emptyParams);

        final ImageView emptyIcon = new AppCompatImageView(activity);
        LinearLayout.LayoutParams emptyIconParams = new LinearLayout.LayoutParams((int)(44 * density), (int)(44 * density));
        emptyIconParams.bottomMargin = (int)(12 * density);
        emptyIcon.setLayoutParams(emptyIconParams);
        emptyIcon.setColorFilter(subTextColor);
        emptyLayout.addView(emptyIcon);

        final TextView emptyText = new TextView(activity);
        emptyText.setTextSize(14);
        emptyText.setTextColor(subTextColor);
        emptyText.setGravity(Gravity.CENTER);
        emptyLayout.addView(emptyText);

        contentFrame.addView(emptyLayout);
        root.addView(contentFrame);

        // Setup Adapter
        final CommandPaletteAdapter.OnItemActionListener actionListener = new CommandPaletteAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(CommandItem item) {
                TerminalSession session = activity.getCurrentSession();
                if (session != null && session.isRunning()) {
                    session.write(item.getCommand());
                }
                // Record to history if running a command
                store.addHistoryCommand(item.getCommand(), activity);
                sheet.dismiss();
            }

            @Override
            public void onOptionsClick(CommandItem item, View anchorView) {
                showItemContextMenu(activity, store, item, () -> refreshPaletteData(
                    store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity));
            }
        };

        final CommandPaletteAdapter adapter = new CommandPaletteAdapter(activity, actionListener);
        recyclerView.setAdapter(adapter);

        // Swipe Gestures: Swipe Left = Delete, Swipe Right = Pin/Unpin
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == CommandPaletteAdapter.VIEW_TYPE_HEADER) return 0;
                return super.getSwipeDirs(rv, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                CommandItem item = adapter.getItemAt(pos);
                if (item == null) return;

                if (direction == ItemTouchHelper.LEFT) {
                    // Delete item
                    store.deleteItem(item, activity);
                    viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    refreshPaletteData(store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity);

                    Snackbar.make(root, "Đã xoá lệnh", Snackbar.LENGTH_LONG)
                        .setAction("Hoàn tác", v -> {
                            store.restoreItem(item, pos, activity);
                            refreshPaletteData(store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity);
                        }).show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Toggle Pin
                    boolean success = store.togglePin(item, activity);
                    if (!success) {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        Toast.makeText(activity, "Tối đa 5 lệnh ghim", Toast.LENGTH_SHORT).show();
                    } else {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    refreshPaletteData(store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);

        // Update Tab visual state
        Runnable updateTabsVisual = () -> {
            boolean isTemplate = isTemplateTab[0];

            GradientDrawable activeBg = new GradientDrawable();
            activeBg.setCornerRadius(17 * density);
            activeBg.setColor(Color.parseColor("#1A3DDC97"));
            activeBg.setStroke((int)(1.2f * density), Color.parseColor("#3DDC97"));

            if (!isTemplate) {
                tabHistory.setBackground(activeBg);
                tabHistory.setTextColor(Color.parseColor("#3DDC97"));
                tabHistory.setTypeface(null, Typeface.BOLD);

                tabTemplates.setBackground(null);
                tabTemplates.setTextColor(subTextColor);
                tabTemplates.setTypeface(null, Typeface.NORMAL);

                // Action button: "Xoá lịch sử"
                actionBtn.setText("Xoá lịch sử");
                actionBtn.setTextColor(subTextColor);
                actionBtn.setBackground(null);
                actionBtn.setOnClickListener(v -> com.termux.app.ui.GlassComponents.showGlassConfirmDialog(
                    activity,
                    "Xoá lịch sử lệnh?",
                    "Toàn bộ lịch sử lệnh gần đây sẽ bị xoá. Các lệnh đã ghim sẽ được giữ lại nguyên vẹn.",
                    "Xoá lịch sử",
                    () -> {
                        store.clearHistoryKeepPinned(activity);
                        com.termux.app.ui.GlassComponents.showGlassToast(activity, root, "Đã xoá lịch sử lệnh");
                        refreshPaletteData(store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity);
                    },
                    "Hủy",
                    null
                ));
            } else {
                tabTemplates.setBackground(activeBg);
                tabTemplates.setTextColor(Color.parseColor("#3DDC97"));
                tabTemplates.setTypeface(null, Typeface.BOLD);

                tabHistory.setBackground(null);
                tabHistory.setTextColor(subTextColor);
                tabHistory.setTypeface(null, Typeface.NORMAL);

                // Action button: "Thêm mẫu" (Mint Pill with Obsidian Text)
                actionBtn.setText("+ Thêm mẫu");
                actionBtn.setTextColor(Color.parseColor("#0E0F11"));
                GradientDrawable addBg = new GradientDrawable();
                addBg.setCornerRadius(14 * density);
                addBg.setColor(Color.parseColor("#3DDC97"));
                actionBtn.setBackground(addBg);
                actionBtn.setOnClickListener(v -> showAddOrEditTemplateDialog(activity, store, null, () -> refreshPaletteData(
                    store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity)));
            }

            refreshPaletteData(store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity);
        };

        tabHistory.setOnClickListener(v -> {
            if (isTemplateTab[0]) {
                isTemplateTab[0] = false;
                updateTabsVisual.run();
            }
        });

        tabTemplates.setOnClickListener(v -> {
            if (!isTemplateTab[0]) {
                isTemplateTab[0] = true;
                updateTabsVisual.run();
            }
        });

        // Search TextWatcher
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery[0] = s.toString();
                clearIcon.setVisibility(currentQuery[0].isEmpty() ? View.GONE : View.VISIBLE);
                refreshPaletteData(store, isTemplateTab[0], currentQuery[0], recyclerView, emptyLayout, emptyIcon, emptyText, activity);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        updateTabsVisual.run();

        // 6. Sheet Configuration & Behavior
        sheet.setContentView(root);

        sheet.setOnShowListener(d -> {
            FrameLayout bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setFitToContents(false);
                behavior.setHalfExpandedRatio(0.60f); // Half state at ~60%
                behavior.setExpandedOffset((int)(screenHeight * 0.08f)); // Expanded at ~92%
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(true);

                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

                // Surface decoration: 28dp top radius
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadii(new float[]{28 * density, 28 * density, 28 * density, 28 * density, 0, 0, 0, 0});
                bg.setColor(sheetBgColor);
                bg.setStroke((int)(1 * density), borderColor);
                bottomSheet.setBackground(bg);

                // API 31+ real window blur-behind
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Window window = sheet.getWindow();
                    if (window != null) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                        window.getAttributes().setBlurBehindRadius(25);
                    }
                }
            }
        });

        // Soft input adjust resize
        Window window = sheet.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        sheet.show();
    }

    private static void refreshPaletteData(CommandStore store, boolean isTemplateTab, String query,
                                           RecyclerView recyclerView, LinearLayout emptyLayout,
                                           ImageView emptyIcon, TextView emptyText, Context context) {
        List<CommandItem> pinned = store.getPinnedItems(isTemplateTab);
        List<CommandItem> unpinned = store.getUnpinnedItems(isTemplateTab);

        if (!TextUtils.isEmpty(query)) {
            pinned = CommandStore.filter(pinned, query);
            unpinned = CommandStore.filter(unpinned, query);
        }

        CommandPaletteAdapter adapter = (CommandPaletteAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateData(pinned, unpinned, isTemplateTab);
        }

        boolean isEmpty = pinned.isEmpty() && unpinned.isEmpty();
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(query)) {
                emptyIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_search));
                emptyText.setText("Không tìm thấy lệnh phù hợp");
            } else if (isTemplateTab) {
                emptyIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_empty_templates));
                emptyText.setText("Chưa có mẫu lệnh nào");
            } else {
                emptyIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_empty_history));
                emptyText.setText("Chưa có lịch sử lệnh nào");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Context Menu bottom sheet triggered on long-press or trailing "…" click.
     */
    private static void showItemContextMenu(Context context, CommandStore store, CommandItem item, Runnable onDataChanged) {
        BottomSheetDialog menuDialog = new BottomSheetDialog(context);
        float density = context.getResources().getDisplayMetrics().density;
        String themeId = TermuxProThemeManager.getCurrentThemeId(context);
        boolean isDark = !themeId.equals("giay") && !themeId.equals("suong");

        int textColor = isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        int subTextColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
        int sheetBg = isDark ? Color.parseColor("#181D26") : Color.parseColor("#FFFFFF");

        LinearLayout menuLayout = new LinearLayout(context);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding((int)(18 * density), (int)(12 * density), (int)(18 * density), (int)(24 * density));

        // Drag handle
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = (int)(16 * density);
        handle.setLayoutParams(hp);
        handle.setBackground(handleBg);
        menuLayout.addView(handle);

        // Command Preview
        TextView preview = new TextView(context);
        preview.setText(item.getCommand());
        preview.setTypeface(Typeface.MONOSPACE);
        preview.setTextSize(13);
        preview.setMaxLines(2);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        preview.setTextColor(subTextColor);
        preview.setPadding((int)(8 * density), 0, (int)(8 * density), (int)(16 * density));
        menuLayout.addView(preview);

        // 1. Ghim / Bỏ ghim
        String pinTitle = item.isPinned() ? "Bỏ ghim" : "Ghim lên đầu";
        int pinDrawable = item.isPinned() ? R.drawable.ic_pin : R.drawable.ic_pin_filled;
        menuLayout.addView(createMenuRow(context, pinTitle, pinDrawable, textColor, density, v -> {
            menuDialog.dismiss();
            boolean success = store.togglePin(item, context);
            if (!success) {
                Toast.makeText(context, "Tối đa 5 lệnh ghim", Toast.LENGTH_SHORT).show();
            }
            if (onDataChanged != null) onDataChanged.run();
        }));

        // 2. Sao chép lệnh
        menuLayout.addView(createMenuRow(context, "Sao chép lệnh", R.drawable.ic_copy, textColor, density, v -> {
            menuDialog.dismiss();
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("command", item.getCommand()));
                Toast.makeText(context, "Đã sao chép lệnh", Toast.LENGTH_SHORT).show();
            }
        }));

        // 3. Sửa mẫu lệnh (only if template)
        if (item.isTemplate()) {
            menuLayout.addView(createMenuRow(context, "Chỉnh sửa mẫu lệnh", R.drawable.ic_edit, textColor, density, v -> {
                menuDialog.dismiss();
                showAddOrEditTemplateDialog(context, store, item, onDataChanged);
            }));
        }

        // 4. Xoá
        menuLayout.addView(createMenuRow(context, "Xoá", R.drawable.ic_delete, Color.parseColor("#EF4444"), density, v -> {
            menuDialog.dismiss();
            store.deleteItem(item, context);
            Toast.makeText(context, "Đã xoá", Toast.LENGTH_SHORT).show();
            if (onDataChanged != null) onDataChanged.run();
        }));

        menuDialog.setContentView(menuLayout);
        menuDialog.setOnShowListener(d -> {
            FrameLayout bs = menuDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadii(new float[]{24 * density, 24 * density, 24 * density, 24 * density, 0, 0, 0, 0});
                bg.setColor(sheetBg);
                bs.setBackground(bg);
            }
        });
        menuDialog.show();
    }

    private static View createMenuRow(Context context, String title, int iconRes, int textColor, float density, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding((int)(12 * density), (int)(14 * density), (int)(12 * density), (int)(14 * density));
        row.setMinimumHeight((int)(48 * density));

        ImageView icon = new AppCompatImageView(context);
        int iconSize = (int)(20 * density);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(iconSize, iconSize);
        ip.rightMargin = (int)(14 * density);
        icon.setLayoutParams(ip);
        icon.setImageDrawable(ContextCompat.getDrawable(context, iconRes));
        icon.setColorFilter(textColor);
        row.addView(icon);

        TextView text = new TextView(context);
        text.setText(title);
        text.setTextSize(14.5f);
        text.setTextColor(textColor);
        row.addView(text);

        // Ripple
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(10 * density);
        mask.setColor(Color.WHITE);
        RippleDrawable ripple = new RippleDrawable(android.content.res.ColorStateList.valueOf(Color.parseColor("#26888888")), null, mask);
        row.setBackground(ripple);
        row.setOnClickListener(onClick);

        return row;
    }

    /**
     * Dialog form sheet for Adding or Editing a Template.
     */
    public static void showAddOrEditTemplateDialog(Context context, CommandStore store, CommandItem existingItem, Runnable onSaved) {
        BottomSheetDialog formSheet = new BottomSheetDialog(context);
        float density = context.getResources().getDisplayMetrics().density;
        String themeId = TermuxProThemeManager.getCurrentThemeId(context);
        boolean isDark = !themeId.equals("giay") && !themeId.equals("suong");

        int textColor = isDark ? Color.parseColor("#F2EDE4") : Color.parseColor("#1C1917");
        int subTextColor = isDark ? Color.parseColor("#9E988F") : Color.parseColor("#64748B");
        int sheetBg = isDark ? Color.parseColor("#E61A1D24") : Color.parseColor("#F5FBF9F5");
        int inputBgColor = isDark ? Color.parseColor("#22000000") : Color.parseColor("#14000000");
        int accentColor = Color.parseColor("#3DDC97");

        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding((int)(20 * density), (int)(12 * density), (int)(20 * density), (int)(24 * density));

        // Drag handle
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setCornerRadius(2 * density);
        handleBg.setColor(isDark ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#33000000"));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams((int)(36 * density), (int)(4 * density));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = (int)(16 * density);
        handle.setLayoutParams(hp);
        handle.setBackground(handleBg);
        form.addView(handle);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(existingItem == null ? "Thêm mẫu lệnh" : "Chỉnh sửa mẫu lệnh");
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, (int)(16 * density));
        form.addView(titleView);

        // Name input
        TextView nameLabel = new TextView(context);
        nameLabel.setText("TÊN MẪU LỆNH");
        nameLabel.setTextSize(11.5f);
        nameLabel.setTypeface(null, Typeface.BOLD);
        nameLabel.setTextColor(subTextColor);
        nameLabel.setLetterSpacing(0.06f);
        nameLabel.setPadding(0, 0, 0, (int)(6 * density));
        form.addView(nameLabel);

        final EditText nameInput = new EditText(context);
        nameInput.setHint("Ví dụ: Cập nhật hệ thống");
        nameInput.setHintTextColor(subTextColor);
        nameInput.setTextColor(textColor);
        nameInput.setTextSize(14);
        nameInput.setSingleLine(true);
        if (existingItem != null) nameInput.setText(existingItem.getTitle());
        GradientDrawable nameBg = new GradientDrawable();
        nameBg.setCornerRadius(12 * density);
        nameBg.setColor(inputBgColor);
        nameBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000"));
        nameInput.setBackground(nameBg);
        nameInput.setPadding((int)(14 * density), (int)(11 * density), (int)(14 * density), (int)(11 * density));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.bottomMargin = (int)(16 * density);
        form.addView(nameInput, nameParams);

        // Command input (Multi-line monospace)
        TextView cmdLabel = new TextView(context);
        cmdLabel.setText("CÂU LỆNH SHELL");
        cmdLabel.setTextSize(11.5f);
        cmdLabel.setTypeface(null, Typeface.BOLD);
        cmdLabel.setTextColor(subTextColor);
        cmdLabel.setLetterSpacing(0.06f);
        cmdLabel.setPadding(0, 0, 0, (int)(6 * density));
        form.addView(cmdLabel);

        final EditText cmdInput = new EditText(context);
        cmdInput.setHint("Ví dụ: pkg update && pkg upgrade");
        cmdInput.setHintTextColor(subTextColor);
        cmdInput.setTextColor(textColor);
        cmdInput.setTextSize(13.5f);
        cmdInput.setTypeface(Typeface.MONOSPACE);
        cmdInput.setMinLines(3);
        cmdInput.setMaxLines(6);
        cmdInput.setGravity(Gravity.TOP | Gravity.START);
        if (existingItem != null) cmdInput.setText(existingItem.getCommand());
        GradientDrawable cmdBg = new GradientDrawable();
        cmdBg.setCornerRadius(12 * density);
        cmdBg.setColor(inputBgColor);
        cmdBg.setStroke((int)(1 * density), isDark ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000"));
        cmdInput.setBackground(cmdBg);
        cmdInput.setPadding((int)(14 * density), (int)(11 * density), (int)(14 * density), (int)(11 * density));
        LinearLayout.LayoutParams cmdParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cmdParams.bottomMargin = (int)(22 * density);
        form.addView(cmdInput, cmdParams);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        TextView cancelBtn = new TextView(context);
        cancelBtn.setText("Hủy");
        cancelBtn.setTextSize(14);
        cancelBtn.setTextColor(subTextColor);
        cancelBtn.setPadding((int)(16 * density), (int)(10 * density), (int)(16 * density), (int)(10 * density));
        cancelBtn.setOnClickListener(v -> formSheet.dismiss());
        LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbp.rightMargin = (int)(10 * density);
        btnRow.addView(cancelBtn, cbp);

        TextView saveBtn = new TextView(context);
        saveBtn.setText("Lưu");
        saveBtn.setTextSize(14);
        saveBtn.setTypeface(null, Typeface.BOLD);
        saveBtn.setTextColor(Color.parseColor("#0E0F11"));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setCornerRadius(14 * density);
        sbg.setColor(Color.parseColor("#3DDC97"));
        saveBtn.setBackground(sbg);
        saveBtn.setPadding((int)(24 * density), (int)(10 * density), (int)(24 * density), (int)(10 * density));

        saveBtn.setOnClickListener(v -> {
            String title = nameInput.getText().toString().trim();
            String cmd = cmdInput.getText().toString().trim();
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(cmd)) {
                Toast.makeText(context, "Vui lòng nhập đầy đủ tên và câu lệnh", Toast.LENGTH_SHORT).show();
                return;
            }

            if (existingItem != null) {
                store.updateTemplate(existingItem.getId(), title, cmd, context);
                Toast.makeText(context, "Đã cập nhật mẫu lệnh", Toast.LENGTH_SHORT).show();
            } else {
                store.addTemplate(title, cmd, context);
                Toast.makeText(context, "Đã thêm mẫu lệnh mới", Toast.LENGTH_SHORT).show();
            }

            formSheet.dismiss();
            if (onSaved != null) onSaved.run();
        });
        btnRow.addView(saveBtn);
        form.addView(btnRow);

        formSheet.setContentView(form);
        formSheet.setOnShowListener(d -> {
            FrameLayout bs = formSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadii(new float[]{24 * density, 24 * density, 24 * density, 24 * density, 0, 0, 0, 0});
                bg.setColor(sheetBg);
                bs.setBackground(bg);
            }
        });
        formSheet.show();
    }

    /**
     * Confirmation dialog before clearing history (keeps pinned items).
     */
    private static void showClearHistoryDialog(Context context, CommandStore store, Runnable onCleared) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        float density = context.getResources().getDisplayMetrics().density;
        String themeId = TermuxProThemeManager.getCurrentThemeId(context);
        boolean isDark = !themeId.equals("giay") && !themeId.equals("suong");

        int textColor = isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        int subTextColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
        int sheetBg = isDark ? Color.parseColor("#181D26") : Color.parseColor("#FFFFFF");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int)(20 * density), (int)(16 * density), (int)(20 * density), (int)(24 * density));

        TextView title = new TextView(context);
        title.setText("Xoá lịch sử lệnh?");
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        title.setPadding(0, 0, 0, (int)(8 * density));
        layout.addView(title);

        TextView message = new TextView(context);
        message.setText("Toàn bộ lịch sử lệnh gần đây sẽ bị xoá. Các lệnh đã ghim sẽ được giữ lại nguyên vẹn.");
        message.setTextSize(13.5f);
        message.setTextColor(subTextColor);
        message.setPadding(0, 0, 0, (int)(20 * density));
        layout.addView(message);

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        TextView cancelBtn = new TextView(context);
        cancelBtn.setText("Hủy");
        cancelBtn.setTextSize(14);
        cancelBtn.setTextColor(subTextColor);
        cancelBtn.setPadding((int)(16 * density), (int)(10 * density), (int)(16 * density), (int)(10 * density));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbp.rightMargin = (int)(10 * density);
        btnRow.addView(cancelBtn, cbp);

        TextView clearBtn = new TextView(context);
        clearBtn.setText("Xoá lịch sử");
        clearBtn.setTextSize(14);
        clearBtn.setTypeface(null, Typeface.BOLD);
        clearBtn.setTextColor(Color.WHITE);
        GradientDrawable delBg = new GradientDrawable();
        delBg.setCornerRadius(14 * density);
        delBg.setColor(Color.parseColor("#EF4444"));
        clearBtn.setBackground(delBg);
        clearBtn.setPadding((int)(20 * density), (int)(10 * density), (int)(20 * density), (int)(10 * density));
        clearBtn.setOnClickListener(v -> {
            store.clearHistoryKeepPinned(context);
            dialog.dismiss();
            Toast.makeText(context, "Đã xoá lịch sử lệnh", Toast.LENGTH_SHORT).show();
            if (onCleared != null) onCleared.run();
        });
        btnRow.addView(clearBtn);

        layout.addView(btnRow);

        dialog.setContentView(layout);
        dialog.setOnShowListener(d -> {
            FrameLayout bs = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadii(new float[]{24 * density, 24 * density, 24 * density, 24 * density, 0, 0, 0, 0});
                bg.setColor(sheetBg);
                bs.setBackground(bg);
            }
        });
        dialog.show();
    }
}
