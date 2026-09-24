package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * Lightweight background view keeping the non-drawer space fully transparent.
 */
public final class CouchyBackgroundView extends View {
    public CouchyBackgroundView(Context context) {
        super(context);
    }

    public CouchyBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Kept 100% transparent: only the sliding right drawer is darkened.
        super.onDraw(canvas);
    }
}
