package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private static final String[] CLOCK_FORMATS = {"system", "12", "24"};
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private String clockFormat;
    private boolean animations;
    private boolean advancedEffects;
    private Config config;

    static final class CategoryItem {
        final int id;
        final String title;
        final int iconRes;

        CategoryItem(int id, String title, int iconRes) {
            this.id = id;
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    private static final int CAT_HOMESCREEN = 1;
    private static final int CAT_CLOCK = 2;
    private static final int CAT_WALLPAPER = 3;
    private static final int CAT_UPDATES = 4;
    private static final int CAT_SYSTEM = 5;
    private static final int CAT_ABOUT = 6;
    private static final int CAT_REORDER = 7;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);

        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        config = Config.load(this);

        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        clockFormat = config.clockFormat;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        List<CategoryItem> categories = new ArrayList<>();
        categories.add(new CategoryItem(CAT_HOMESCREEN, "Homescreen Layout", R.drawable.ic_apps));
        categories.add(new CategoryItem(CAT_CLOCK, "Clock & Motion", R.drawable.ic_home_settings));
        categories.add(new CategoryItem(CAT_WALLPAPER, "Wallpaper & Themes", R.drawable.ic_picture));
        categories.add(new CategoryItem(CAT_UPDATES, "Updates & Startup", R.drawable.ic_settings));
        categories.add(new CategoryItem(CAT_SYSTEM, "System Settings", R.drawable.ic_wifi));
        categories.add(new CategoryItem(CAT_ABOUT, "About & Credits", R.drawable.ic_home_settings_pill));
        if (config.launcherSettingsButton) {
            categories.add(new CategoryItem(CAT_REORDER, getString(R.string.reorder), R.drawable.ic_swap_vert));
        }

        RecyclerView grid = findViewById(R.id.categoryGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 3));
        grid.setAdapter(new CategoryAdapter(this, categories, this::openCategorySection));
    }

    private void openCategorySection(CategoryItem category) {
        if (category.id == CAT_REORDER) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
            return;
        }

        View sectionView = getLayoutInflater().inflate(R.layout.dialog_settings_section, null);
        TextView title = sectionView.findViewById(R.id.sectionTitle);
        TextView subtitle = sectionView.findViewById(R.id.sectionSubtitle);
        LinearLayout container = sectionView.findViewById(R.id.sectionOptionsContainer);

        title.setText(category.title);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(sectionView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        switch (category.id) {
            case CAT_HOMESCREEN:
                subtitle.setText("Customize grid columns, dock rows, and opacity");
                buildHomescreenSection(container);
                break;
            case CAT_CLOCK:
                subtitle.setText("Customize clock format and motion animations");
                buildClockSection(container);
                break;
            case CAT_WALLPAPER:
                subtitle.setText("Themes and Wallpaper visual selection");
                buildWallpaperSection(container, dialog);
                break;
            case CAT_UPDATES:
                subtitle.setText("Check for new launcher releases and auto-launch");
                buildUpdatesSection(container, dialog);
                break;
            case CAT_SYSTEM:
                subtitle.setText("Access default home and TV system settings");
                buildSystemSection(container);
                break;
            case CAT_ABOUT:
                subtitle.setText("Developer details, support links, and credits");
                buildAboutSection(container);
                break;
        }

        dialog.show();
    }

    private void buildHomescreenSection(LinearLayout container) {
        // Apps per row
        addLabel(container, getString(R.string.columns_label));
        LinearLayout colRow = createHorizontalRow();
        TextView[] colBtns = new TextView[4];
        for (int i = 0; i < 4; i++) {
            final int val = i + 4;
            colBtns[i] = createChoiceButton(String.valueOf(val), columns == val);
            colBtns[i].setOnClickListener(v -> {
                columns = val;
                choices.edit().putInt("columns", val).apply();
                changed();
                updateRowSelection(colBtns, columns - 4);
            });
            colRow.addView(colBtns[i]);
        }
        container.addView(colRow);

        // Rows below dock
        addLabel(container, getString(R.string.rows_below_dock_label));
        LinearLayout rowRow = createHorizontalRow();
        TextView[] rowBtns = new TextView[4];
        for (int i = 0; i < 4; i++) {
            final int val = i;
            rowBtns[i] = createChoiceButton(String.valueOf(val), rowsBelowDock == val);
            rowBtns[i].setOnClickListener(v -> {
                rowsBelowDock = val;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, val).apply();
                changed();
                updateRowSelection(rowBtns, rowsBelowDock);
            });
            rowRow.addView(rowBtns[i]);
        }
        container.addView(rowRow);

        // Dock opacity
        addLabel(container, getString(R.string.dock_opacity_label));
        LinearLayout dockRow = createHorizontalRow();
        TextView[] dockBtns = new TextView[4];
        for (int i = 0; i < 4; i++) {
            final int val = DOCK_OPACITIES[i];
            dockBtns[i] = createChoiceButton(val + "%", dockOpacity == val);
            dockBtns[i].setOnClickListener(v -> {
                dockOpacity = val;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, val).apply();
                changed();
                updateRowSelection(dockBtns, indexOfInt(DOCK_OPACITIES, dockOpacity));
            });
            dockRow.addView(dockBtns[i]);
        }
        container.addView(dockRow);
    }

    private void buildClockSection(LinearLayout container) {
        addLabel(container, getString(R.string.dream_name));
        LinearLayout clockRow = createHorizontalRow();
        TextView[] clockBtns = new TextView[3];
        String[] labels = {getString(R.string.clock_format_system), getString(R.string.clock_format_12), getString(R.string.clock_format_24)};
        for (int i = 0; i < 3; i++) {
            final String val = CLOCK_FORMATS[i];
            clockBtns[i] = createChoiceButton(labels[i], val.equals(clockFormat));
            clockBtns[i].setOnClickListener(v -> {
                clockFormat = val;
                choices.edit().putString(Config.CLOCK_FORMAT_KEY, val).apply();
                changed();
                updateRowSelection(clockBtns, indexOfStr(CLOCK_FORMATS, clockFormat));
            });
            clockRow.addView(clockBtns[i]);
        }
        container.addView(clockRow);

        // Animations Toggle
        TextView animBtn = createFullRowButton(getString(R.string.animations_state, getString(animations ? R.string.on : R.string.off)));
        animBtn.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            animBtn.setText(getString(R.string.animations_state, getString(animations ? R.string.on : R.string.off)));
        });
        container.addView(animBtn);

        // Advanced Effects Toggle
        TextView advBtn = createFullRowButton(getString(R.string.advanced_effects_state, getString(advancedEffects ? R.string.on : R.string.off)));
        advBtn.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
                advBtn.setText(getString(R.string.advanced_effects_state, getString(R.string.off)));
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (d, w) -> {
                            setAdvanced(true);
                            advBtn.setText(getString(R.string.advanced_effects_state, getString(R.string.on)));
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        container.addView(advBtn);
    }

    private void buildWallpaperSection(LinearLayout container, AlertDialog dialog) {
        // SECTION 1: THEMES
        addSectionHeader(container, "1. THEMES");

        TextView defaultThemeBtn = createFullRowButton("Default Theme");
        defaultThemeBtn.setOnClickListener(v -> {
            choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
            changed();
            Toast.makeText(this, "Default theme active", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        container.addView(defaultThemeBtn);

        TextView addAddonBtn = createFullRowButton("+ Add Theme Addon");
        addAddonBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Theme Addons: Place custom theme folders in Android/data/com.gathin.tvhome/files/", Toast.LENGTH_LONG).show();
        });
        container.addView(addAddonBtn);

        // SECTION 2: WALLPAPERS
        addSectionHeader(container, "2. WALLPAPERS");

        TextView pickBtn = createFullRowButton("Choose Wallpaper (Visual Grid Previews)");
        pickBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showWallpaperPicker();
        });
        container.addView(pickBtn);

        TextView redditBtn = createFullRowButton("Fetch Wallpapers from Reddit r/wallpapers");
        redditBtn.setOnClickListener(v -> {
            redditBtn.setText("Fetching wallpapers from Reddit...");
            WallpaperFetcher.fetchFromReddit(this, (success, msg) -> {
                runOnUiThread(() -> {
                    redditBtn.setText("Fetch Wallpapers from Reddit r/wallpapers");
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
            });
        });
        container.addView(redditBtn);
    }

    private void buildUpdatesSection(LinearLayout container, AlertDialog dialog) {
        TextView updateBtn = createFullRowButton("Check for Launcher Updates");
        updateBtn.setOnClickListener(v -> {
            updateBtn.setText("Checking for updates...");
            UpdateManager.checkForUpdates(this, true, msg -> {
                updateBtn.setText("Check for Launcher Updates");
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
        });
        container.addView(updateBtn);

        if (config.openOnStartChoice) {
            TextView openStartBtn = createFullRowButton(getString(R.string.open_on_start_state, openOnStartLabel()));
            openStartBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showOpenOnStartPicker();
            });
            container.addView(openStartBtn);
        }
    }

    private void buildSystemSection(LinearLayout container) {
        TextView defaultHomeBtn = createFullRowButton(getString(R.string.home_settings));
        defaultHomeBtn.setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        container.addView(defaultHomeBtn);

        TextView tvSettingsBtn = createFullRowButton(getString(R.string.tv_settings));
        tvSettingsBtn.setOnClickListener(v -> open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        container.addView(tvSettingsBtn);
    }

    private void buildAboutSection(LinearLayout container) {
        addLabel(container, "EVA TV Launcher v1.8");

        TextView devBtn = createFullRowButton("Developer: Rajat Shahare");
        devBtn.setOnClickListener(v -> Toast.makeText(this, "Rajat Shahare", Toast.LENGTH_SHORT).show());
        container.addView(devBtn);

        TextView emailBtn = createFullRowButton("Contact Email: rns@cyberservices.com");
        emailBtn.setOnClickListener(v -> Toast.makeText(this, "Email: rns@cyberservices.com", Toast.LENGTH_LONG).show());
        container.addView(emailBtn);

        TextView telegramBtn = createFullRowButton("Telegram Channel: t.me/EvaTVLauncher");
        telegramBtn.setOnClickListener(v -> openUrl("https://t.me/EvaTVLauncher"));
        container.addView(telegramBtn);

        TextView githubBtn = createFullRowButton("GitHub Repository: therajatshahare/eva-launcher");
        githubBtn.setOnClickListener(v -> openUrl("https://github.com/therajatshahare/eva-launcher"));
        container.addView(githubBtn);

        addLabel(container, "Credits & Acknowledgments");

        TextView creditBtn = createFullRowButton("Based on CleanYourTV by Gathin23");
        creditBtn.setOnClickListener(v -> openUrl("https://github.com/Gathin23/cleanyourtv"));
        container.addView(creditBtn);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        }
    }

    private void addSectionHeader(LinearLayout container, String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(0xFF4ADE80);
        header.setTextSize(13);
        header.setLetterSpacing(0.05f);
        header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        header.setLayoutParams(lp);
        container.addView(header);
    }

    private void addLabel(LinearLayout container, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        label.setLayoutParams(lp);
        container.addView(label);
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        row.setLayoutParams(lp);
        return row;
    }

    private TextView createChoiceButton(String text, boolean selected) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setBackgroundResource(R.drawable.settings_choice_bg);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(16);
        btn.setSelected(selected);
        btn.setFocusable(true);
        btn.setClickable(true);

        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (48 * density), 1.0f);
        lp.setMargins((int) (4 * density), (int) (2 * density), (int) (4 * density), (int) (2 * density));
        btn.setLayoutParams(lp);

        applyFocusAnimation(btn);
        return btn;
    }

    private TextView createFullRowButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setGravity(android.view.Gravity.CENTER_VERTICAL);
        btn.setBackgroundResource(R.drawable.settings_choice_bg);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(16);
        btn.setFocusable(true);
        btn.setClickable(true);

        float density = getResources().getDisplayMetrics().density;
        btn.setPadding((int) (18 * density), 0, (int) (18 * density), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * density));
        lp.topMargin = (int) (10 * density);
        btn.setLayoutParams(lp);

        applyFocusAnimation(btn);
        return btn;
    }

    private void updateRowSelection(TextView[] btns, int selectedIdx) {
        for (int i = 0; i < btns.length; i++) {
            btns[i].setSelected(i == selectedIdx);
        }
    }

    private static int indexOfInt(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }

    private static int indexOfStr(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return 0;
    }

    private static final class CategoryAdapter extends RecyclerView.Adapter<CategoryViewHolder> {
        private final Activity activity;
        private final List<CategoryItem> items;
        private final OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onClick(CategoryItem category);
        }

        CategoryAdapter(Activity activity, List<CategoryItem> items, OnCategoryClickListener listener) {
            this.activity = activity;
            this.items = items;
            this.listener = listener;
        }

        @Override
        public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = activity.getLayoutInflater().inflate(R.layout.item_settings_category_card, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CategoryViewHolder holder, int position) {
            CategoryItem item = items.get(position);
            holder.title.setText(item.title);
            holder.icon.setImageResource(item.iconRes);

            final float density = activity.getResources().getDisplayMetrics().density;
            holder.card.setOnFocusChangeListener((v, focused) -> {
                float scale = focused ? 1.05f : 1.00f;
                float z = focused ? 8f * density : 0f;
                v.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .translationZ(z)
                        .setDuration(focused ? 180 : 140)
                        .setInterpolator(focused ? new DecelerateInterpolator(1.5f) : new AccelerateInterpolator())
                        .start();
                if (focused) v.bringToFront();
            });

            holder.card.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static final class CategoryViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView icon;
        final TextView title;

        CategoryViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.categoryCard);
            icon = itemView.findViewById(R.id.categoryIcon);
            title = itemView.findViewById(R.id.categoryTitle);
        }
    }

    private void applyFocusAnimation(View view) {
        if (view == null) return;
        final float density = getResources().getDisplayMetrics().density;
        view.setOnFocusChangeListener((v, focused) -> {
            float scale = focused ? 1.04f : 1.00f;
            float translationZ = focused ? 8f * density : 0f;
            v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .translationZ(translationZ)
                    .setDuration(focused ? 180 : 140)
                    .setInterpolator(focused ? new DecelerateInterpolator(1.5f) : new AccelerateInterpolator())
                    .start();
            if (focused) {
                v.bringToFront();
            }
        });
    }

    static final class WallpaperItem {
        final String key;
        final String label;
        final File file;
        final boolean isThemeDefault;
        final boolean isNone;

        WallpaperItem(String key, String label, File file, boolean isThemeDefault, boolean isNone) {
            this.key = key;
            this.label = label;
            this.file = file;
            this.isThemeDefault = isThemeDefault;
            this.isNone = isNone;
        }
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile() && !images.contains(single)) {
            images.add(0, single);
        }

        List<WallpaperItem> items = new ArrayList<>();
        items.add(new WallpaperItem(null, getString(R.string.wallpaper_theme_default), single, true, false));

        for (File img : images) {
            String name = img.getName();
            String displayLabel = name;
            if (name.startsWith("reddit_")) {
                int idx = name.lastIndexOf('_');
                String num = idx >= 0 ? name.substring(idx + 1).replace(".jpg", "") : "";
                displayLabel = "Reddit Wallpaper #" + num;
            }
            items.add(new WallpaperItem(name, displayLabel, img, false, false));
        }

        items.add(new WallpaperItem("none", getString(R.string.wallpaper_none), null, false, true));

        String currentChoice = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wallpaper_picker, null);
        RecyclerView grid = dialogView.findViewById(R.id.wallpaperGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 3));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        grid.setAdapter(new WallpaperGridAdapter(this, items, currentChoice, item -> {
            dialog.dismiss();
            if (item.isThemeDefault) {
                choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
            } else {
                choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, item.key).apply();
            }
            changed();
            Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
        }));

        dialog.show();
    }

    private static final class WallpaperGridAdapter extends RecyclerView.Adapter<WallpaperViewHolder> {
        private final Activity activity;
        private final List<WallpaperItem> items;
        private final String currentChoice;
        private final OnWallpaperSelectedListener listener;

        interface OnWallpaperSelectedListener {
            void onSelected(WallpaperItem item);
        }

        WallpaperGridAdapter(Activity activity, List<WallpaperItem> items, String currentChoice, OnWallpaperSelectedListener listener) {
            this.activity = activity;
            this.items = items;
            this.currentChoice = currentChoice;
            this.listener = listener;
        }

        @Override
        public WallpaperViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = activity.getLayoutInflater().inflate(R.layout.item_wallpaper_card, parent, false);
            return new WallpaperViewHolder(view);
        }

        @Override
        public void onBindViewHolder(WallpaperViewHolder holder, int position) {
            WallpaperItem item = items.get(position);
            holder.name.setText(item.label);

            boolean isCurrent = (item.isThemeDefault && currentChoice == null)
                    || (!item.isThemeDefault && item.key != null && item.key.equals(currentChoice));
            holder.selectedBadge.setVisibility(isCurrent ? View.VISIBLE : View.GONE);

            if (item.file != null && item.file.isFile()) {
                new Thread(() -> {
                    Bitmap bmp = loadThumbnail(item.file, 240, 135);
                    if (bmp != null) {
                        activity.runOnUiThread(() -> holder.preview.setImageBitmap(bmp));
                    }
                }).start();
            } else if (item.isNone) {
                holder.preview.setImageResource(android.R.color.transparent);
            } else {
                holder.preview.setImageResource(R.drawable.banner);
            }

            final float density = activity.getResources().getDisplayMetrics().density;
            holder.card.setOnFocusChangeListener((v, focused) -> {
                float scale = focused ? 1.06f : 1.00f;
                float z = focused ? 10f * density : 0f;
                v.animate().scaleX(scale).scaleY(scale).translationZ(z)
                        .setDuration(focused ? 180 : 140)
                        .setInterpolator(focused ? new DecelerateInterpolator(1.5f) : new AccelerateInterpolator())
                        .start();
                if (focused) v.bringToFront();
            });

            holder.card.setOnClickListener(v -> listener.onSelected(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static final class WallpaperViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView preview;
        final TextView name;
        final TextView selectedBadge;

        WallpaperViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.wallpaperCard);
            preview = itemView.findViewById(R.id.wallpaperPreview);
            name = itemView.findViewById(R.id.wallpaperName);
            selectedBadge = itemView.findViewById(R.id.selectedBadge);
        }
    }

    static Bitmap loadThumbnail(File file, int reqWidth, int reqHeight) {
        if (file == null || !file.isFile()) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception e) {
            return null;
        }
    }

    static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
