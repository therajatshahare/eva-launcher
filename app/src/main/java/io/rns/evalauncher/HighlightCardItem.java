package io.rns.evalauncher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public final class HighlightCardItem {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String posterUri;
    public final int posterResId;
    public final Drawable appBadge;
    public final Intent intent;

    public HighlightCardItem(String id, String title, String subtitle, String posterUri, Drawable appBadge, Intent intent) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.posterUri = posterUri;
        this.posterResId = 0;
        this.appBadge = appBadge;
        this.intent = intent;
    }

    public HighlightCardItem(String id, String title, String subtitle, int posterResId, Intent intent) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.posterUri = null;
        this.posterResId = posterResId;
        this.appBadge = null;
        this.intent = intent;
    }
}
