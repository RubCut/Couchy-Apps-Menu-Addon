package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A rounded card with Couchy Launcher style 20dp corner radius,
 * smooth focus scaling (1.06x), and Couchy accent focus border (#8AB4F8).
 */
public final class RoundedCardView extends FrameLayout {
    private static final int ACCENT_FOCUS_COLOR = Color.rgb(138, 180, 248);

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Path roundedPath = new Path();
    private final RectF cardBounds = new RectF();
    private final float radius;
    private int tileColor = Color.rgb(28, 38, 48);
    private boolean focusedVisual;

    public RoundedCardView(Context context) {
        this(context, null);
    }

    public RoundedCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        radius = density(context) * 20f;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(density(context) * 2.5f);
        borderPaint.setColor(ACCENT_FOCUS_COLOR); // Couchy accent focus border (#8AB4F8)
        setWillNotDraw(false);
        setClipToPadding(false);
        setClipChildren(false);
        setFocusable(false);
    }

    public void setTileColor(int color) {
        tileColor = color;
        invalidate();
    }

    public void setFocusedVisual(boolean focused) {
        if (focusedVisual == focused) {
            return;
        }
        focusedVisual = focused;
        animate()
                .scaleX(focused ? 1.06f : 1f)
                .scaleY(focused ? 1.06f : 1f)
                .translationZ(focused ? density(getContext()) * 8f : 0f)
                .setDuration(focused ? 120 : 90)
                .start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updatePath();
        fillPaint.setColor(tileColor);
        canvas.drawRoundRect(cardBounds, radius, radius, fillPaint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        updatePath();
        int checkpoint = canvas.save();
        canvas.clipPath(roundedPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(checkpoint);

        if (focusedVisual) {
            float inset = borderPaint.getStrokeWidth() / 2f;
            canvas.drawRoundRect(
                    inset,
                    inset,
                    getWidth() - inset,
                    getHeight() - inset,
                    radius,
                    radius,
                    borderPaint
            );
        }
    }

    private void updatePath() {
        cardBounds.set(0, 0, getWidth(), getHeight());
        roundedPath.reset();
        roundedPath.addRoundRect(cardBounds, radius, radius, Path.Direction.CW);
    }

    private static float density(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }
}
