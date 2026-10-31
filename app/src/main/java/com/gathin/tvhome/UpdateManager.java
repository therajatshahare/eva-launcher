package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
<<<<<<< HEAD
import android.os.Build;
=======
>>>>>>> ef71f96 (Release v1.1: Fix catalog sorting NPE and Javadoc formatting)
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
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
<<<<<<< HEAD
                String currentVersionName = "3.2";
=======
                String currentVersionName = "1.1";
>>>>>>> ef71f96 (Release v1.1: Fix catalog sorting NPE and Javadoc formatting)
                try {
                    PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                    currentVersionName = pInfo.versionName;
                } catch (Exception ignored) {}

                URL url = new URL("https://api.github.com/repos/" + targetRepo + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Android:com.gathin.tvhome");
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
                HttpURLConnection conn = (HttpURLConnection) new URL(apkUrl).openConnection();
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(apkFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();

                activity.runOnUiThread(() -> {
                    callback.onStatus("Installing...");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
<<<<<<< HEAD
                    Uri apkUri = Build.VERSION.SDK_INT >= 24 ? Uri.parse("content://" + activity.getPackageName() + ".updateprovider/update") : Uri.fromFile(apkFile);
=======
                    Uri apkUri = Uri.parse("content://" + activity.getPackageName() + ".updateprovider/update");
>>>>>>> ef71f96 (Release v1.1: Fix catalog sorting NPE and Javadoc formatting)
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onStatus("Download failed"));
            }
        }).start();
    }
}
