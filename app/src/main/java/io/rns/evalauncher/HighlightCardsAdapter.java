package io.rns.evalauncher;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public final class HighlightCardsAdapter extends RecyclerView.Adapter<HighlightCardsAdapter.ViewHolder> {
    private final Activity activity;
    private final List<HighlightCardItem> items;

    public HighlightCardsAdapter(Activity activity, List<HighlightCardItem> items) {
        this.activity = activity;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.item_highlight_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HighlightCardItem item = items.get(position);
        holder.card.setClipToOutline(true);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);

        if (item.posterResId != 0) {
            holder.poster.setImageResource(item.posterResId);
        } else if (item.posterUri != null && !item.posterUri.isEmpty()) {
            new Thread(() -> {
                Bitmap bmp = loadBitmapFromUri(item.posterUri);
                if (bmp != null) {
                    activity.runOnUiThread(() -> holder.poster.setImageBitmap(bmp));
                }
            }).start();
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

        holder.card.setOnClickListener(v -> {
            if (item.intent != null) {
                try {
                    activity.startActivity(item.intent);
                } catch (Exception e) {
                    Toast.makeText(activity, "Can't open " + item.title, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private Bitmap loadBitmapFromUri(String uriStr) {
        try {
            URL url = new URL(uriStr);
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

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView poster;
        final TextView title;
        final TextView subtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.highlightCard);
            poster = itemView.findViewById(R.id.cardPoster);
            title = itemView.findViewById(R.id.cardTitle);
            subtitle = itemView.findViewById(R.id.cardSubtitle);
        }
    }
}
