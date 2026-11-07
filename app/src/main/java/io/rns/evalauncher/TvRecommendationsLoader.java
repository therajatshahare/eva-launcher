package io.rns.evalauncher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class TvRecommendationsLoader {

    private static final Uri WATCH_NEXT_URI = Uri.parse("content://android.media.tv/watch_next_program");
    private static final Uri PREVIEW_PROGRAMS_URI = Uri.parse("content://android.media.tv/preview_program");

    private static final String[] PROGRAM_PROJECTION = new String[] {
            "_id",
            "package_name",
            "title",
            "episode_title",
            "poster_art_uri",
            "intent_uri"
    };

    public static List<HighlightCardItem> loadLiveRecommendations(Context context) {
        List<HighlightCardItem> cards = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        try {
            cards.addAll(queryPrograms(context, resolver, WATCH_NEXT_URI, "Watch Next"));
        } catch (Exception ignored) {}

        if (cards.size() < 6) {
            try {
                cards.addAll(queryPrograms(context, resolver, PREVIEW_PROGRAMS_URI, "Recommended"));
            } catch (Exception ignored) {}
        }

        if (cards.isEmpty()) {
            cards.addAll(HighlightsSampleData.getSampleItems(context));
        }

        return cards;
    }

    private static List<HighlightCardItem> queryPrograms(
            Context context,
            ContentResolver resolver,
            Uri queryUri,
            String defaultSubtitle) {

        List<HighlightCardItem> results = new ArrayList<>();

        try (Cursor cursor = resolver.query(
                queryUri,
                PROGRAM_PROJECTION,
                null,
                null,
                "_id DESC LIMIT 10")) {

            if (cursor == null || !cursor.moveToFirst()) {
                return results;
            }

            int idIdx = cursor.getColumnIndex("_id");
            int pkgIdx = cursor.getColumnIndex("package_name");
            int titleIdx = cursor.getColumnIndex("title");
            int episodeTitleIdx = cursor.getColumnIndex("episode_title");
            int posterIdx = cursor.getColumnIndex("poster_art_uri");
            int intentUriIdx = cursor.getColumnIndex("intent_uri");

            do {
                long progId = idIdx != -1 ? cursor.getLong(idIdx) : -1;
                String packageName = pkgIdx != -1 ? cursor.getString(pkgIdx) : "";
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String episodeTitle = episodeTitleIdx != -1 ? cursor.getString(episodeTitleIdx) : null;
                String posterUriStr = posterIdx != -1 ? cursor.getString(posterIdx) : null;
                String intentUriStr = intentUriIdx != -1 ? cursor.getString(intentUriIdx) : null;

                if (title == null || title.trim().isEmpty()) {
                    continue;
                }

                Intent launchIntent = null;
                if (intentUriStr != null && !intentUriStr.isEmpty()) {
                    try {
                        launchIntent = Intent.parseUri(intentUriStr, Intent.URI_INTENT_SCHEME);
                    } catch (Exception ignored) {}
                }
                if (launchIntent == null && packageName != null && !packageName.isEmpty()) {
                    launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                }

                Drawable appBadge = null;
                if (packageName != null && !packageName.isEmpty()) {
                    try {
                        appBadge = context.getPackageManager().getApplicationIcon(packageName);
                    } catch (Exception ignored) {}
                }

                String subtitle = (episodeTitle != null && !episodeTitle.isEmpty())
                        ? episodeTitle
                        : defaultSubtitle;

                results.add(new HighlightCardItem(
                        String.valueOf(progId),
                        title,
                        subtitle,
                        posterUriStr,
                        appBadge,
                        launchIntent
                ));
            } while (cursor.moveToNext());

        } catch (Exception ignored) {}

        return results;
    }
}
