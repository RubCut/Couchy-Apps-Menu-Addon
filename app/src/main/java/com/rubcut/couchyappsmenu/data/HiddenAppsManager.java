package com.rubcut.couchyappsmenu.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages persisted set of hidden application component names.
 */
public final class HiddenAppsManager {
    private static final String PREF_NAME = "couchy_apps_prefs";
    private static final String KEY_HIDDEN = "hidden_components";

    private HiddenAppsManager() {
    }

    public static Set<String> getHidden(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = sp.getStringSet(KEY_HIDDEN, null);
        if (set == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(set);
    }

    public static boolean isHidden(Context context, String component) {
        return getHidden(context).contains(component);
    }

    public static void setHidden(Context context, String component, boolean hidden) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(getHidden(context));
        if (hidden) {
            set.add(component);
        } else {
            set.remove(component);
        }
        sp.edit().putStringSet(KEY_HIDDEN, set).apply();
    }

    public static void unhideAll(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_HIDDEN).apply();
    }
}
