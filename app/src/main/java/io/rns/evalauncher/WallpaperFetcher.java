package io.rns.evalauncher;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WallpaperFetcher {
    private static final String TAG = "TvHome.WallpaperFetcher";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    public interface Callback {
        void onComplete(boolean success, String message);
    }

    public static void fetchFromReddit(Context context, Callback callback) {
        new Thread(() -> {
            try {
                List<String> imageUrls = fetchImageUrlsFromRss("https://www.reddit.com/r/wallpapers/.rss");
                if (imageUrls.isEmpty()) {
                    imageUrls = fetchImageUrlsFromJson("https://www.reddit.com/r/wallpapers/hot.json?limit=25");
                }

                if (imageUrls.isEmpty()) {
                    callback.onComplete(false, "No wallpapers found on Reddit.");
                    return;
                }

                File dir = Config.dir(context);
                File wallpapersDir = new File(dir, "wallpapers");
                if (!wallpapersDir.exists()) wallpapersDir.mkdirs();

                int downloaded = 0;
                for (int i = 0; i < imageUrls.size(); i++) {
                    String imgUrl = imageUrls.get(i);
                    File outFile = new File(wallpapersDir, "reddit_" + System.currentTimeMillis() + "_" + i + ".jpg");
                    if (downloadImage(imgUrl, outFile)) {
                        downloaded++;
                        if (downloaded >= 5) break;
                    }
                }

                if (downloaded > 0) {
                    callback.onComplete(true, "Downloaded " + downloaded + " Reddit wallpapers!");
                } else {
                    callback.onComplete(false, "Failed to save Reddit wallpapers.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch failed", e);
                callback.onComplete(false, "Error: " + e.getMessage());
            }
        }).start();
    }

    private static List<String> fetchImageUrlsFromRss(String rssUrl) {
        List<String> urls = new ArrayList<>();
        try {
            String xmlContent = fetchStringWithRedirects(rssUrl);
            if (xmlContent == null || xmlContent.isEmpty()) return urls;

            // Extract i.redd.it or preview.redd.it direct image links from RSS Atom content
            Pattern pattern = Pattern.compile("https://(i\\.redd\\.it|preview\\.redd\\.it)/[a-zA-Z0-9_.]+\\.(jpg|png|jpeg)");
            Matcher matcher = pattern.matcher(xmlContent);
            while (matcher.find()) {
                String imgUrl = matcher.group();
                if (!urls.contains(imgUrl)) {
                    urls.add(imgUrl);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "RSS fetch failed, trying JSON fallback", e);
        }
        return urls;
    }

    private static List<String> fetchImageUrlsFromJson(String jsonUrl) {
        List<String> urls = new ArrayList<>();
        try {
            String jsonStr = fetchStringWithRedirects(jsonUrl);
            if (jsonStr == null || !jsonStr.trim().startsWith("{")) return urls;

            JSONObject root = new JSONObject(jsonStr);
            JSONArray children = root.getJSONObject("data").getJSONArray("children");
            for (int i = 0; i < children.length(); i++) {
                JSONObject post = children.getJSONObject(i).getJSONObject("data");
                String imgUrl = post.optString("url_overridden_by_dest", post.optString("url", ""));
                if (imgUrl.endsWith(".jpg") || imgUrl.endsWith(".png") || imgUrl.endsWith(".jpeg")) {
                    if (!urls.contains(imgUrl)) {
                        urls.add(imgUrl);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "JSON fetch failed", e);
        }
        return urls;
    }

    private static String fetchStringWithRedirects(String initialUrl) throws IOException {
        String currentUrl = initialUrl;
        for (int redirects = 0; redirects < 5; redirects++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    currentUrl = location;
                    continue;
                }
            }
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream in = conn.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                return out.toString("UTF-8");
            } else {
                throw new IOException("HTTP " + status);
            }
        }
        throw new IOException("Too many redirects");
    }

    private static boolean downloadImage(String urlStr, File outFile) {
        try {
            String currentUrl = urlStr;
            for (int redirects = 0; redirects < 5; redirects++) {
                URL url = new URL(currentUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    String location = conn.getHeaderField("Location");
                    if (location != null && !location.isEmpty()) {
                        currentUrl = location;
                        continue;
                    }
                }
                if (status == HttpURLConnection.HTTP_OK) {
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(outFile);
                    byte[] buf = new byte[16 * 1024];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    in.close();
                    return outFile.isFile() && outFile.length() > 10 * 1024;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Image download error: " + urlStr, e);
        }
        return false;
    }
}
