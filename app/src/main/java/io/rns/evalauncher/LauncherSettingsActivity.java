package io.rns.evalauncher;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Category Grid View Launcher Settings with Sub-Menu Grids and Focus Scale Animations. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    static final String EXTRA_REORDER = "io.rns.evalauncher.extra.REORDER_FROM_SETTINGS";
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

    static final class SubGridItem {
        final int id;
        final String title;
        String value;
        final String hint;

        SubGridItem(int id, String title, String value, String hint) {
            this.id = id;
            this.title = title;
            this.value = value;
            this.hint = hint;
        }
    }

    private static final int ITEM_COLUMNS = 101;
    private static final int ITEM_ROWS = 102;
    private static final int ITEM_DOCK = 103;

    private static final int ITEM_CLOCK_FORMAT = 201;
    private static final int ITEM_ANIMATIONS = 202;
    private static final int ITEM_ADVANCED = 203;

    private static final int ITEM_DEFAULT_THEME = 301;
    private static final int ITEM_ADD_THEME = 302;
    private static final int ITEM_PICK_WALLPAPER = 303;
    private static final int ITEM_FETCH_REDDIT = 304;

    private static final int ITEM_CHECK_UPDATE = 401;
    private static final int ITEM_OPEN_START = 402;

    private static final int ITEM_DEFAULT_HOME = 501;
    private static final int ITEM_TV_SETTINGS = 502;

    private static final int ITEM_DEV_INFO = 601;
    private static final int ITEM_TELEGRAM = 602;
    private static final int ITEM_GITHUB = 603;
    private static final int ITEM_CREDITS = 604;

    private void openCategorySection(CategoryItem category) {
        if (category.id == CAT_REORDER) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings_grid, null);
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView subtitle = dialogView.findViewById(R.id.dialogSubtitle);
        RecyclerView gridView = dialogView.findViewById(R.id.subGridRecyclerView);

        title.setText(category.title);
        List<SubGridItem> items = new ArrayList<>();

        switch (category.id) {
            case CAT_HOMESCREEN:
                subtitle.setText("Press OK on a card to adjust setting value");
                items.add(new SubGridItem(ITEM_COLUMNS, getString(R.string.columns_label), "‹ " + columns + " ›", "Press OK to cycle (4..7)"));
                items.add(new SubGridItem(ITEM_ROWS, getString(R.string.rows_below_dock_label), "‹ " + rowsBelowDock + " ›", "Press OK to cycle (0..3)"));
                items.add(new SubGridItem(ITEM_DOCK, getString(R.string.dock_opacity_label), "‹ " + dockOpacity + "% ›", "Press OK to cycle opacity"));
                break;

            case CAT_CLOCK:
                subtitle.setText("Customize clock format and motion animations");
                items.add(new SubGridItem(ITEM_CLOCK_FORMAT, getString(R.string.dream_name), "‹ " + clockFormatLabel(clockFormat) + " ›", "Press OK to cycle format"));
                items.add(new SubGridItem(ITEM_ANIMATIONS, "Animations", animations ? getString(R.string.on) : getString(R.string.off), "Press OK to toggle"));
                items.add(new SubGridItem(ITEM_ADVANCED, getString(R.string.advanced_effects), advancedEffects ? getString(R.string.on) : getString(R.string.off), "Press OK to toggle"));
                break;

            case CAT_WALLPAPER:
                subtitle.setText("Themes and Wallpaper visual selection");
                items.add(new SubGridItem(ITEM_DEFAULT_THEME, "Default Theme", "Reset Choices", "Revert to built-in theme"));
                items.add(new SubGridItem(ITEM_ADD_THEME, "+ Add Theme Addon", "Custom Addons", "Add custom theme folders"));
                items.add(new SubGridItem(ITEM_PICK_WALLPAPER, "Choose Wallpaper", "Visual Grid Previews", "Select 16:9 wallpaper card"));
                items.add(new SubGridItem(ITEM_FETCH_REDDIT, "Fetch Wallpapers", "Reddit r/wallpapers", "Download HD wallpapers"));
                break;

            case CAT_UPDATES:
                subtitle.setText("Check for new launcher releases and auto-launch");
                String currentVersionName = "2.5";
                try {
                    currentVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (Exception ignored) {}
                items.add(new SubGridItem(ITEM_CHECK_UPDATE, "Check for Updates", "Check Latest Release", "Installed version: v" + currentVersionName));
                if (config.openOnStartChoice) {
                    items.add(new SubGridItem(ITEM_OPEN_START, getString(R.string.open_on_start_label), openOnStartLabel(), "App on boot"));
                }
                break;

            case CAT_SYSTEM:
                subtitle.setText("Access default home and TV system settings");
                items.add(new SubGridItem(ITEM_DEFAULT_HOME, getString(R.string.home_settings), "Default Launcher", "Choose TV home app"));
                items.add(new SubGridItem(ITEM_TV_SETTINGS, getString(R.string.tv_settings), "System Options", "Open native TV settings"));
                break;

            case CAT_ABOUT:
                subtitle.setText("Developer details, support links, and credits");
                items.add(new SubGridItem(ITEM_DEV_INFO, "Developer Info", "Rajat Shahare", "rns@cyberservices.com"));
                items.add(new SubGridItem(ITEM_TELEGRAM, "Telegram Channel", "t.me/EvaTVLauncher", "Join support channel"));
                items.add(new SubGridItem(ITEM_GITHUB, "GitHub Repository", "therajatshahare/eva-launcher", "Source code & releases"));
                items.add(new SubGridItem(ITEM_CREDITS, "Credits", "CleanYourTV by Gathin23", "Original base project"));
                break;
        }

        gridView.setLayoutManager(new GridLayoutManager(this, 2));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        SubGridAdapter adapter = new SubGridAdapter(this, items, (item, holder) -> {
            handleSubGridClick(item, holder, dialog);
        });

        gridView.setAdapter(adapter);
        dialog.show();

        if (dialog.getWindow() != null) {
            int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.85f);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.65f);
            dialog.getWindow().setLayout(Math.max(width, (int) (550 * getResources().getDisplayMetrics().density)), maxHeight);
        }
    }

    private void handleSubGridClick(SubGridItem item, SubGridViewHolder holder, AlertDialog dialog) {
        switch (item.id) {
            case ITEM_COLUMNS:
                columns = columns >= 7 ? 4 : columns + 1;
                choices.edit().putInt("columns", columns).apply();
                changed();
                item.value = "‹ " + columns + " ›";
                if (holder != null) holder.value.setText(item.value);
                break;

            case ITEM_ROWS:
                rowsBelowDock = rowsBelowDock >= 3 ? 0 : rowsBelowDock + 1;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, rowsBelowDock).apply();
                changed();
                item.value = "‹ " + rowsBelowDock + " ›";
                if (holder != null) holder.value.setText(item.value);
                break;

            case ITEM_DOCK:
                int idx = indexOfInt(DOCK_OPACITIES, dockOpacity);
                dockOpacity = DOCK_OPACITIES[(idx + 1) % DOCK_OPACITIES.length];
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, dockOpacity).apply();
                changed();
                item.value = "‹ " + dockOpacity + "% ›";
                if (holder != null) holder.value.setText(item.value);
                break;

            case ITEM_CLOCK_FORMAT:
                int cIdx = indexOfStr(CLOCK_FORMATS, clockFormat);
                clockFormat = CLOCK_FORMATS[(cIdx + 1) % CLOCK_FORMATS.length];
                choices.edit().putString(Config.CLOCK_FORMAT_KEY, clockFormat).apply();
                changed();
                item.value = "‹ " + clockFormatLabel(clockFormat) + " ›";
                if (holder != null) holder.value.setText(item.value);
                break;

            case ITEM_ANIMATIONS:
                animations = !animations;
                if (!animations) advancedEffects = false;
                choices.edit().putBoolean("animations", animations)
                        .putBoolean("advanced_effects", advancedEffects).apply();
                changed();
                item.value = animations ? getString(R.string.on) : getString(R.string.off);
                if (holder != null) holder.value.setText(item.value);
                break;

            case ITEM_ADVANCED:
                if (advancedEffects) {
                    setAdvanced(false);
                    item.value = getString(R.string.off);
                    if (holder != null) holder.value.setText(item.value);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.advanced_effects)
                            .setMessage(R.string.advanced_effects_warning)
                            .setPositiveButton(R.string.enable, (d, w) -> {
                                setAdvanced(true);
                                item.value = getString(R.string.on);
                                if (holder != null) holder.value.setText(item.value);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
                break;

            case ITEM_DEFAULT_THEME:
                choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                changed();
                Toast.makeText(this, "Default theme active", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                break;

            case ITEM_ADD_THEME:
                Toast.makeText(this, "Theme Addons: Place theme folders in Android/data/io.rns.evalauncher/files/", Toast.LENGTH_LONG).show();
                break;

            case ITEM_PICK_WALLPAPER:
                dialog.dismiss();
                showWallpaperPicker();
                break;

            case ITEM_FETCH_REDDIT:
                Toast.makeText(this, "Fetching wallpapers from Reddit...", Toast.LENGTH_SHORT).show();
                WallpaperFetcher.fetchFromReddit(this, (success, msg) -> {
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
                });
                break;

            case ITEM_CHECK_UPDATE:
                Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
                UpdateManager.checkForUpdates(this, true, msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
                break;

            case ITEM_OPEN_START:
                dialog.dismiss();
                showOpenOnStartPicker();
                break;

            case ITEM_DEFAULT_HOME:
                Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
                open(new Intent(Settings.ACTION_HOME_SETTINGS));
                break;

            case ITEM_TV_SETTINGS:
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS));
                break;

            case ITEM_DEV_INFO:
                Toast.makeText(this, "Developer: Rajat Shahare (rns@cyberservices.com)", Toast.LENGTH_LONG).show();
                break;

            case ITEM_TELEGRAM:
                openUrl("https://t.me/EvaTVLauncher");
                break;

            case ITEM_GITHUB:
                openUrl("https://github.com/therajatshahare/eva-launcher");
                break;

            case ITEM_CREDITS:
                openUrl("https://github.com/Gathin23/cleanyourtv");
                break;
        }
    }

    private String clockFormatLabel(String fmt) {
        if ("12".equals(fmt)) return getString(R.string.clock_format_12);
        if ("24".equals(fmt)) return getString(R.string.clock_format_24);
        return getString(R.string.clock_format_system);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
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

    private static final class SubGridAdapter extends RecyclerView.Adapter<SubGridViewHolder> {
        private final Activity activity;
        private final List<SubGridItem> items;
        private final OnSubGridClickListener listener;

        interface OnSubGridClickListener {
            void onClick(SubGridItem item, SubGridViewHolder holder);
        }

        SubGridAdapter(Activity activity, List<SubGridItem> items, OnSubGridClickListener listener) {
            this.activity = activity;
            this.items = items;
            this.listener = listener;
        }

        @Override
        public SubGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = activity.getLayoutInflater().inflate(R.layout.item_settings_grid_card, parent, false);
            return new SubGridViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SubGridViewHolder holder, int position) {
            SubGridItem item = items.get(position);
            holder.title.setText(item.title);
            holder.value.setText(item.value);
            holder.hint.setText(item.hint);

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

            holder.card.setOnClickListener(v -> listener.onClick(item, holder));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static final class SubGridViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final TextView title;
        final TextView value;
        final TextView hint;

        SubGridViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.subGridCard);
            title = itemView.findViewById(R.id.cardTitle);
            value = itemView.findViewById(R.id.cardValue);
            hint = itemView.findViewById(R.id.cardHint);
        }
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

        if (dialog.getWindow() != null) {
            int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.82f);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.65f);
            dialog.getWindow().setLayout(Math.max(width, (int) (550 * getResources().getDisplayMetrics().density)), maxHeight);
        }
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
