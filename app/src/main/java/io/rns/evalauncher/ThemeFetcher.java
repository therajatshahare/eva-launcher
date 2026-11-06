package io.rns.evalauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and extracts online theme store zips from GitHub into Config.dir.
 */
public final class ThemeFetcher {
    private static final String TAG = "TvHomeThemeFetcher";
    public static final String STORE_URL = "https://raw.githubusercontent.com/therajatshahare/eva-launcher/main/themes/index.json";

    public static final class ThemeItem {
        public final String id;
        public final String name;
        public final String author;
        public final String description;
        public final String previewUrl;
        public final String downloadUrl;

        public ThemeItem(String id, String name, String author, String description, String previewUrl, String downloadUrl) {
            this.id = id;
            this.name = name;
            this.author = author;
            this.description = description;
            this.previewUrl = previewUrl;
            this.downloadUrl = downloadUrl;
        }
    }

    public interface OnThemesFetchedListener {
        void onResult(boolean success, List<ThemeItem> themes, String message);
    }

    public interface OnThemeAppliedListener {
        void onResult(boolean success, String message);
    }

    public static void fetchThemes(Context context, OnThemesFetchedListener listener) {
        new Thread(() -> {
            try {
                URL url = new URL(STORE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    if (listener != null) listener.onResult(false, null, "Server response: " + conn.getResponseCode());
                    return;
                }

                InputStream in = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                in.close();
                conn.disconnect();

                String jsonText = out.toString(StandardCharsets.UTF_8.name());
                if (jsonText.startsWith("\uFEFF")) jsonText = jsonText.substring(1);

                JSONArray array = new JSONArray(jsonText);
                List<ThemeItem> items = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    items.add(new ThemeItem(
                            obj.optString("id", ""),
                            obj.optString("name", "Theme"),
                            obj.optString("author", "Community"),
                            obj.optString("description", ""),
                            obj.optString("preview_url", ""),
                            obj.optString("download_url", "")
                    ));
                }

                if (listener != null) listener.onResult(true, items, "Loaded " + items.size() + " themes");
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch themes: " + e.getMessage(), e);
                if (listener != null) listener.onResult(false, null, "Could not load themes online");
            }
        }).start();
    }

    public static void downloadAndApplyTheme(Context context, ThemeItem item, OnThemeAppliedListener listener) {
        new Thread(() -> {
            try {
                File targetDir = Config.dir(context);
                if (targetDir == null) {
                    if (listener != null) listener.onResult(false, "Storage unavailable");
                    return;
                }

                URL url = new URL(item.downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    if (listener != null) listener.onResult(false, "Download error: " + conn.getResponseCode());
                    return;
                }

                InputStream in = new BufferedInputStream(conn.getInputStream());
                ZipInputStream zipIn = new ZipInputStream(in);
                ZipEntry entry;

                while ((entry = zipIn.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.contains("..")) continue;

                    if (name.contains("/")) {
                        name = name.substring(name.lastIndexOf('/') + 1);
                    }
                    if (name.isEmpty() || entry.isDirectory()) continue;

                    File destFile;
                    if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".webp")) {
                        if (name.equalsIgnoreCase("wallpaper.jpg") || name.equalsIgnoreCase("wallpaper.png") || name.equalsIgnoreCase("wallpaper.webp")) {
                            destFile = new File(targetDir, Config.WALLPAPER_NAME);
                        } else {
                            File wallpapersSubDir = new File(targetDir, "wallpapers");
                            if (!wallpapersSubDir.isDirectory()) wallpapersSubDir.mkdirs();
                            destFile = new File(wallpapersSubDir, name);
                        }
                    } else if (name.equalsIgnoreCase("config.json")) {
                        destFile = new File(targetDir, Config.FILE_NAME);
                    } else {
                        continue;
                    }

                    FileOutputStream fOut = new FileOutputStream(destFile);
                    byte[] buffer = new byte[2048];
                    int count;
                    while ((count = zipIn.read(buffer)) != -1) {
                        fOut.write(buffer, 0, count);
                    }
                    fOut.close();
                    zipIn.closeEntry();
                }

                zipIn.close();
                conn.disconnect();

                File internalCopy = new File(context.getFilesDir(), Config.FILE_NAME);
                if (internalCopy.exists()) internalCopy.delete();

                if (listener != null) listener.onResult(true, "Applied " + item.name);
            } catch (Exception e) {
                Log.e(TAG, "Failed to download theme: " + e.getMessage(), e);
                if (listener != null) listener.onResult(false, "Failed to apply theme");
            }
        }).start();
    }

    public static Bitmap loadBitmapFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.connect();
            InputStream in = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            conn.disconnect();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
