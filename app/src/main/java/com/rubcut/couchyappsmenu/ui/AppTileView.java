package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rubcut.couchyappsmenu.data.AppEntry;

/**
 * 16:9 Android TV application tile matching Couchy Launcher's visual style.
 * 20dp corner radius, banner crop, and Couchy accent focus ring.
 */
public final class AppTileView extends FrameLayout {
    private final RoundedCardView card;
    private final ImageView banner;
    private final LinearLayout fallbackLayout;
    private final ImageView icon;
    private final TextView label;

    public AppTileView(Context context) {
        this(context, null);
    }

    public AppTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(false);
        setClickable(true);
        setClipChildren(false);
        setClipToPadding(false);

        int iconSize = dp(42);

        card = new RoundedCardView(context);
        addView(card, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        banner = new ImageView(context);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.setContentDescription(null);
        card.addView(banner, new RoundedCardView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        fallbackLayout = new LinearLayout(context);
        fallbackLayout.setOrientation(LinearLayout.VERTICAL);
        fallbackLayout.setGravity(Gravity.CENTER);
        fallbackLayout.setPadding(dp(8), dp(6), dp(8), dp(6));
        card.addView(fallbackLayout, new RoundedCardView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setContentDescription(null);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        fallbackLayout.addView(icon, iconParams);

        label = new TextView(context);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setMaxLines(1);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(6);
        fallbackLayout.addView(label, labelParams);

        // ONLY real View focus drives visual focus.
        // This ensures the focus ring follows D-pad focus and NEVER duplicates.
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                card.setFocusedVisual(hasFocus);
            }
        });
    }

    public void bind(AppEntry entry) {
        setContentDescription(entry.label);
        label.setText(entry.label);

        if (entry.banner != null) {
            banner.setImageDrawable(entry.banner);
            banner.setVisibility(VISIBLE);
            fallbackLayout.setVisibility(GONE);
            card.setTileColor(Color.TRANSPARENT);
        } else {
            banner.setImageDrawable(null);
            banner.setVisibility(GONE);
            card.setTileColor(tileColor(entry.packageName));
            icon.setImageDrawable(entry.icon);
            fallbackLayout.setVisibility(VISIBLE);
        }
        card.setFocusedVisual(hasFocus());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width > 0) {
            int artworkHeight = Math.round(width * 9f / 16f);
            int exactHeightSpec = MeasureSpec.makeMeasureSpec(artworkHeight, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, exactHeightSpec);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDetachedFromWindow() {
        card.animate().cancel();
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Couchy calm muted palette for bannerless apps */
    private static int tileColor(String packageName) {
        final int[] palette = {
                0xFF1E2A38, // Slate midnight
                0xFF163042, // Navy
                0xFF14383E, // Deep teal
                0xFF2A2234, // Plum
                0xFF28241C, // Bronze
                0xFF1C2832, // Dark steel
                0xFF1F3546  // Deep blue
        };
        return palette[(packageName.hashCode() & 0x7fffffff) % palette.length];
    }
}
