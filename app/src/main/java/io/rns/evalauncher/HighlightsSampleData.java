package io.rns.evalauncher;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public final class HighlightsSampleData {
    public static List<HighlightCardItem> getSampleItems(Context context) {
        List<HighlightCardItem> items = new ArrayList<>();
        items.add(new HighlightCardItem("1", "The Great Indian Kapil Show", "Watch Next · Netflix", R.drawable.banner, null));
        items.add(new HighlightCardItem("2", "Panchayat", "Watch Next · Prime Video", R.drawable.banner, null));
        items.add(new HighlightCardItem("3", "Dhadak 2", "Watch Next · JioHotstar", R.drawable.banner, null));
        return items;
    }
}
