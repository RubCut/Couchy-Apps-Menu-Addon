package com.rubcut.couchyappsmenu.data;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

/** Immutable description of an activity that can be launched from the TV menu. */
public final class AppEntry {
    public final ComponentName component;
    public final String packageName;
    public final String label;
    public final Drawable banner;
    public final Drawable icon;

    public AppEntry(
            ComponentName component,
            String packageName,
            String label,
            Drawable banner,
            Drawable icon
    ) {
        this.component = component;
        this.packageName = packageName;
        this.label = label;
        this.banner = banner;
        this.icon = icon;
    }
}
