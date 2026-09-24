package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** A lightweight, asset-free version of Couchy's calm midnight wallpaper. */
public final class CouchyBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final RectF bounds = new RectF();

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

        bounds.set(0, 0, width, height);
        paint.setShader(new LinearGradient(
                0, 0, width, height,
                new int[]{Color.rgb(15, 32, 39), Color.rgb(29, 61, 72), Color.rgb(44, 83, 100)},
                new float[]{0f, .55f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(bounds, paint);

        // A very soft cool glow gives the otherwise flat gradient depth without video/GPU cost.
        paint.setShader(new RadialGradient(
                width * .78f, height * .08f, Math.max(width, height) * .66f,
                new int[]{0x453A91A4, 0x003A91A4},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(bounds, paint);

        // Couchy keeps its wallpaper deliberately dim so artwork remains legible from a sofa.
        paint.setShader(null);
        paint.setColor(0x47000000);
        canvas.drawRect(bounds, paint);
    }
}
