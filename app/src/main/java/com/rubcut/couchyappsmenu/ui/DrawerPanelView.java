package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Slide-in right side drawer panel with strongly darkened semi-transparent black surface,
 * subtle left border line, and soft left drop shadow.
 */
public final class DrawerPanelView extends LinearLayout {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final RectF bounds = new RectF();

    public DrawerPanelView(Context context) {
        super(context);
        init();
    }

    public DrawerPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawerPanelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.argb(42, 255, 255, 255));
        borderPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 1.2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) {
            super.onDraw(canvas);
            return;
        }

        final float density = getResources().getDisplayMetrics().density;
        final float shadowWidth = density * 18f;

        // 1. Soft shadow cast outside the left edge onto the transparent background
        shadowPaint.setShader(new LinearGradient(
                -shadowWidth, 0, 0, 0,
                new int[]{
                        Color.argb(0, 0, 0, 0),
                        Color.argb(85, 0, 0, 0)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(-shadowWidth, 0, 0, height, shadowPaint);

        // 2. Strongly darkened semi-transparent black surface (~97% opacity)
        bounds.set(0, 0, width, height);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(new LinearGradient(
                0, 0, width, height,
                new int[]{
                        Color.argb(246, 8, 11, 16),
                        Color.argb(252, 4, 6, 9)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(bounds, fillPaint);

        // 3. Subtle vertical border line at x = 0
        canvas.drawLine(0, 0, 0, height, borderPaint);

        super.onDraw(canvas);
    }
}
