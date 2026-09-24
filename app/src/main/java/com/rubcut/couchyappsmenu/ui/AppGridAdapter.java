package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.rubcut.couchyappsmenu.data.AppEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reuses tiles to keep scrolling fluid on entry-level TV sticks. */
public final class AppGridAdapter extends BaseAdapter {
    public interface Listener {
        void onAppClicked(AppEntry app);
        boolean onAppLongClicked(AppEntry app);
    }

    private final Context context;
    private final Listener listener;
    private List<AppEntry> apps = Collections.emptyList();

    public AppGridAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submit(List<AppEntry> newApps) {
        apps = Collections.unmodifiableList(new ArrayList<>(newApps));
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public AppEntry getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return apps.get(position).component.flattenToShortString().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AppTileView tile;
        if (convertView instanceof AppTileView) {
            tile = (AppTileView) convertView;
        } else {
            tile = new AppTileView(context);
        }

        final AppEntry app = getItem(position);
        tile.bind(app);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onAppClicked(app);
            }
        });
        tile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return listener.onAppLongClicked(app);
            }
        });
        return tile;
    }
}
