package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateManager {
    private static final String TAG = "TvHome.UpdateManager";

    public interface StatusCallback {
        void onStatus(String message);
    }

    public static void checkForUpdates(Activity activity, boolean manualCheck, StatusCallback callback) {
        String repo;
        try {
            repo = activity.getString(R.string.github_update_repo);
        } catch (Exception e) {
            repo = "therajatshahare/eva-launcher";
        }
        if (repo == null || repo.isEmpty()) {
            repo = "therajatshahare/eva-launcher";
        }

        final String targetRepo = repo;

        new Thread(() -> {
            try {
                String currentVersionName = "1.4";
                try {
                    PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                    currentVersionName = pInfo.versionName;
                } catch (Exception ignored) {}

                URL url = new URL("https://api.github.com/repos/" + targetRepo + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Android:com.gathin.tvhome:v1.4");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() != 200) {
                    if (manualCheck) activity.runOnUiThread(() -> callback.onStatus("No updates found."));
                    return;
                }

                InputStream in = conn.getInputStream();
                byte[] buf = new byte[32 * 1024];
                int n = 0, r;
                while ((r = in.read(buf, n, buf.length - n)) > 0) n += r;
                String jsonStr = new String(buf, 0, n, StandardCharsets.UTF_8);
                in.close();

                JSONObject release = new JSONObject(jsonStr);
                String latestTag = release.optString("tag_name", "").replace("v", "").trim();
                JSONArray assets = release.optJSONArray("assets");

                if (latestTag.isEmpty() || assets == null || assets.length() == 0) {
                    if (manualCheck) activity.runOnUiThread(() -> callback.onStatus("No APK asset found."));
                    return;
                }

                String apkUrl = "";
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    if (asset.optString("name", "").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "");
                        break;
                    }
                }

                if (apkUrl.isEmpty()) {
                    if (manualCheck) activity.runOnUiThread(() -> callback.onStatus("No .apk attached."));
                    return;
                }

                String finalApkUrl = apkUrl;
                String finalTag = latestTag;
                String finalCurrentVer = currentVersionName;

                activity.runOnUiThread(() -> {
                    new AlertDialog.Builder(activity)
                            .setTitle("Launcher Update")
                            .setMessage("Version " + finalTag + " is available! (Current: " + finalCurrentVer + ")\n\nDownload and install now?")
                            .setPositiveButton("Update Now", (d, w) -> downloadAndInstall(activity, finalApkUrl, callback))
                            .setNegativeButton("Later", null)
                            .show();
                });
            } catch (Exception e) {
                if (manualCheck) activity.runOnUiThread(() -> callback.onStatus("Error: " + e.getMessage()));
            }
        }).start();
    }

    private static void downloadAndInstall(Activity activity, String apkUrl, StatusCallback callback) {
        activity.runOnUiThread(() -> callback.onStatus("Downloading update..."));
        new Thread(() -> {
            try {
                File cacheDir = new File(activity.getCacheDir(), "updates");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File apkFile = new File(cacheDir, "launcher-update.apk");

                InputStream in = openStreamWithRedirects(apkUrl);
                FileOutputStream out = new FileOutputStream(apkFile);
                byte[] buf = new byte[16 * 1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();

                if (!apkFile.exists() || apkFile.length() < 100 * 1024) {
                    activity.runOnUiThread(() -> callback.onStatus("Download invalid/corrupt."));
                    return;
                }

                activity.runOnUiThread(() -> {
                    try {
                        callback.onStatus("Installing...");
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updateprovider", apkFile);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Install failed", e);
                        callback.onStatus("Install error: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                activity.runOnUiThread(() -> callback.onStatus("Download failed: " + e.getMessage()));
            }
        }).start();
    }

    private static InputStream openStreamWithRedirects(String initialUrl) throws IOException {
        String currentUrl = initialUrl;
        for (int redirects = 0; redirects < 5; redirects++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    currentUrl = location;
                    continue;
                }
            }
            if (status == HttpURLConnection.HTTP_OK) {
                return conn.getInputStream();
            } else {
                throw new IOException("HTTP " + status);
            }
        }
        throw new IOException("Too many redirects");
    }
}
