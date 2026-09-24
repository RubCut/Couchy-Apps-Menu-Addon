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
 * Semi-transparent black background matching the right-side overlay panel from the photo.
 * The left area (~33%) is empty dimmed space showing whatever is running underneath.
 * The right area (~67%) is the sleek semi-transparent dark apps drawer surface.
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
        float density = getResources().getDisplayMetrics().density;

        // 1. Left area: semi-transparent dark scrim (pure empty dismiss area)
        bounds.set(0, 0, splitX, height);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(bounds, paint);

        // 2. Right side panel: semi-transparent black surface with subtle dark depth
        rightBounds.set(splitX, 0, width, height);
        paint.setShader(new LinearGradient(
                splitX, 0, width, height,
                new int[]{
                        Color.argb(230, 11, 15, 21),
                        Color.argb(242, 6, 9, 13)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(rightBounds, paint);

        // 3. Subtle vertical border divider line at splitX
        paint.setShader(null);
        paint.setColor(Color.argb(32, 255, 255, 255));
        paint.setStrokeWidth(Math.max(1f, density * 0.75f));
        canvas.drawLine(splitX, 0, splitX, height, paint);

        // 4. Soft shadow cast from the right drawer onto the left area
        paint.setShader(new LinearGradient(
                splitX - (density * 16f), 0, splitX, 0,
                new int[]{
                        Color.argb(0, 0, 0, 0),
                        Color.argb(70, 0, 0, 0)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(splitX - (density * 16f), 0, splitX, height, paint);
    }
}
