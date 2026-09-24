package com.rubcut.couchyappsmenu.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Finds every activity intended to be launched on a TV.  Leanback comes first so
 * an app that exposes both a TV and a phone entry point gets its TV entry.
 */
public final class AppCatalog {
    private AppCatalog() {
    }

    public static List<AppEntry> load(Context context) {
        PackageManager packageManager = context.getPackageManager();
        LinkedHashMap<ComponentName, ResolveInfo> activities = new LinkedHashMap<>();

        collect(packageManager, new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), activities);
        collect(packageManager, new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER), activities);

        ArrayList<AppEntry> apps = new ArrayList<>();
        for (ResolveInfo resolveInfo : activities.values()) {
            ActivityInfo info = resolveInfo.activityInfo;
            if (info == null || context.getPackageName().equals(info.packageName)) {
                continue;
            }

            String label = safeLabel(resolveInfo, packageManager, info.packageName);
            Drawable banner = loadBanner(info, packageManager);
            Drawable icon = banner == null ? safeIcon(resolveInfo, packageManager) : null;
            apps.add(new AppEntry(
                    new ComponentName(info.packageName, info.name),
                    info.packageName,
                    label,
                    banner,
                    icon
            ));
        }

        final Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry first, AppEntry second) {
                return collator.compare(first.label, second.label);
            }
        });
        return apps;
    }

    private static void collect(
            PackageManager packageManager,
            Intent intent,
            LinkedHashMap<ComponentName, ResolveInfo> target
    ) {
        List<ResolveInfo> found = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : found) {
            ActivityInfo info = resolveInfo.activityInfo;
            if (info == null || info.name == null || info.packageName == null) {
                continue;
            }
            ComponentName component = new ComponentName(info.packageName, info.name);
            if (!target.containsKey(component)) {
                target.put(component, resolveInfo);
            }
        }
    }

    private static String safeLabel(ResolveInfo info, PackageManager packageManager, String fallback) {
        try {
            CharSequence label = info.loadLabel(packageManager);
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (RuntimeException ignored) {
            // A broken third-party resource must not make the complete menu fail.
        }
        return fallback;
    }

    private static Drawable loadBanner(ActivityInfo info, PackageManager packageManager) {
        try {
            Drawable banner = info.loadBanner(packageManager);
            if (banner == null && info.applicationInfo != null) {
                banner = info.applicationInfo.loadBanner(packageManager);
            }
            return banner;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Drawable safeIcon(ResolveInfo info, PackageManager packageManager) {
        try {
            return info.loadIcon(packageManager);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
