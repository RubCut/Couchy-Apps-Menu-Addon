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
 * Slide-in right side drawer panel styled in Couchy Launcher's signature midnight palette.
 * Darkened with a deep translucent navy/midnight gradient (~95% opacity),
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
        borderPaint.setColor(Color.argb(35, 138, 180, 248)); // Couchy accent border tint
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
        final float shadowWidth = density * 20f;

        // 1. Soft shadow cast outside the left edge onto the transparent background
        shadowPaint.setShader(new LinearGradient(
                -shadowWidth, 0, 0, 0,
                new int[]{
                        Color.argb(0, 0, 0, 0),
                        Color.argb(90, 0, 0, 0)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(-shadowWidth, 0, 0, height, shadowPaint);

        // 2. Couchy signature midnight gradient (~95% deep opacity)
        bounds.set(0, 0, width, height);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(new LinearGradient(
                0, 0, width, height,
                new int[]{
                        Color.argb(242, 13, 24, 35), // Couchy dark midnight top
                        Color.argb(248, 17, 33, 47)  // Couchy dark midnight-blue bottom
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
