package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Android TV style semi-transparent black background.
 * Splits the screen between the dimmed home scrim on the left (~33%)
 * and the overlay apps panel on the right (~67%).
 */
public final class CouchyBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final RectF bounds = new RectF();
    private final RectF rightBounds = new RectF();

    public CouchyBackgroundView(Context context) {
        super(context);
    }

    public CouchyBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int width = getWidth();
        final int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float splitX = width * 0.33f;

        // 1. Semi-transparent black scrim for the left area
        bounds.set(0, 0, splitX, height);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(204, 6, 9, 13)); // ~80% black scrim
        canvas.drawRect(bounds, paint);

        // 2. Right panel: semi-transparent black surface with subtle dark depth
        rightBounds.set(splitX, 0, width, height);
        paint.setShader(new LinearGradient(
                splitX, 0, width, height,
                new int[]{
                        Color.argb(230, 11, 15, 21),
                        Color.argb(240, 7, 10, 15)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(rightBounds, paint);

        // 3. Subtle vertical divider line at splitX
        paint.setShader(null);
        paint.setColor(Color.argb(32, 255, 255, 255));
        paint.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density * 0.75f));
        canvas.drawLine(splitX, 0, splitX, height, paint);

        // 4. Soft shadow cast from the split to the right panel
        paint.setShader(new LinearGradient(
                splitX, 0, splitX + (getResources().getDisplayMetrics().density * 18f), 0,
                new int[]{
                        Color.argb(60, 0, 0, 0),
                        Color.argb(0, 0, 0, 0)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(splitX, 0, splitX + (getResources().getDisplayMetrics().density * 18f), height, paint);
    }
}
