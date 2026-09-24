package com.rubcut.couchyappsmenu.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rubcut.couchyappsmenu.data.AppEntry;

/** One 16:9 Couchy-style application tile, entirely usable with a D-pad. */
public final class AppTileView extends LinearLayout {
    private static final int TEXT_NORMAL = Color.rgb(236, 241, 244);
    private static final int TEXT_FOCUSED = Color.WHITE;
    private final RoundedCardView card;
    private final ImageView banner;
    private final ImageView icon;
    private final TextView label;
    private final int labelTopMargin;

    public AppTileView(Context context) {
        this(context, null);
    }

    public AppTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setClipChildren(false);
        setClipToPadding(false);

        int iconSize = dp(57);
        labelTopMargin = dp(10);

        card = new RoundedCardView(context);
        addView(card, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(108)
        ));

        banner = new ImageView(context);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.setContentDescription(null);
        card.addView(banner, new RoundedCardView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setContentDescription(null);
        RoundedCardView.LayoutParams iconParams = new RoundedCardView.LayoutParams(iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER;
        card.addView(icon, iconParams);

        label = new TextView(context);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTextColor(TEXT_NORMAL);
        label.setTextSize(14);
        label.setIncludeFontPadding(false);
        label.setMaxLines(1);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = labelTopMargin;
        addView(label, labelParams);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                card.setFocusedVisual(hasFocus);
                label.setTextColor(hasFocus ? TEXT_FOCUSED : TEXT_NORMAL);
                label.setSelected(hasFocus);
            }
        });
    }

    public void bind(AppEntry entry) {
        setContentDescription(entry.label);
        label.setText(entry.label);
        card.setTileColor(tileColor(entry.packageName));

        if (entry.banner != null) {
            banner.setImageDrawable(entry.banner);
            banner.setVisibility(VISIBLE);
            icon.setImageDrawable(null);
            icon.setVisibility(GONE);
        } else {
            banner.setImageDrawable(null);
            banner.setVisibility(GONE);
            icon.setImageDrawable(entry.icon);
            icon.setVisibility(entry.icon == null ? GONE : VISIBLE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width > 0) {
            int artworkHeight = Math.round(width * 9f / 16f);
            LayoutParams cardParams = (LayoutParams) card.getLayoutParams();
            if (cardParams.height != artworkHeight) {
                cardParams.height = artworkHeight;
                card.setLayoutParams(cardParams);
            }
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

    /** Stable muted colors for banner-less apps, like Couchy's own fallback cards. */
    private static int tileColor(String packageName) {
        final int[] palette = {
                0xFF37474F, 0xFF4E342E, 0xFF1B5E20,
                0xFF0D47A1, 0xFF4A148C, 0xFF880E4F,
                0xFF3E2723, 0xFF263238, 0xFF33691E
        };
        return palette[(packageName.hashCode() & 0x7fffffff) % palette.length];
    }
}
