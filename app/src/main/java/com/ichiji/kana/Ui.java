package com.ichiji.kana;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Small builders so screens can be written as plain code, no XML layouts. */
final class Ui {
    private Ui() {}

    static final Typeface SERIF = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
    static final Typeface SERIF_BOLD = Typeface.create(Typeface.SERIF, Typeface.BOLD);
    static final Typeface SANS = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface SANS_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }

    static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(SANS);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    static TextView medium(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(SANS_MEDIUM);
        return t;
    }

    /** Japanese text in the serif face, with Japanese glyph shapes. */
    static TextView jp(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(SERIF);
        t.setTextLocale(Locale.JAPAN);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** Small spaced-out caps label. */
    static TextView label(Context c, CharSequence s, int color) {
        TextView t = text(c, s, 11.5f, color);
        t.setTypeface(SANS_MEDIUM);
        t.setAllCaps(true);
        t.setLetterSpacing(0.12f);
        return t;
    }

    static GradientDrawable rounded(int color, float radiusPx, int strokeColor, int strokePx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        if (strokePx > 0) g.setStroke(strokePx, strokeColor);
        return g;
    }

    static Drawable pressable(Drawable bg, int ripple, float radiusPx) {
        GradientDrawable mask = rounded(0xFFFFFFFF, radiusPx, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(ripple), bg, mask);
    }

    static final int PRIMARY = 0, SECONDARY = 1, GHOST = 2;

    static TextView button(Context c, Palette p, String label, int style, View.OnClickListener l) {
        TextView b = medium(c, label, 15.5f, p.ink);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(c, 50));
        b.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        float r = dp(c, 14);
        Drawable bg;
        if (style == PRIMARY) {
            b.setTextColor(p.onAccent);
            bg = rounded(p.accent, r, 0, 0);
        } else if (style == SECONDARY) {
            bg = rounded(p.surface, r, p.line, dp(c, 1));
        } else {
            b.setTextColor(p.accent);
            bg = rounded(0x00000000, r, 0, 0);
        }
        b.setBackground(pressable(bg, Palette.alpha(p.ink, 0.12f), r));
        b.setClickable(true);
        b.setFocusable(true);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    static void setEnabled(View v, boolean on) {
        v.setEnabled(on);
        v.setAlpha(on ? 1f : 0.4f);
    }

    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout card(Context c, Palette p) {
        LinearLayout l = col(c);
        int pad = dp(c, 20);
        l.setPadding(pad, pad, pad, pad);
        l.setBackground(rounded(p.surface, dp(c, 20), p.line, dp(c, 1)));
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    static LinearLayout.LayoutParams lp(int w, int h, int topMarginPx) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(w, h);
        l.topMargin = topMarginPx;
        return l;
    }

    static LinearLayout.LayoutParams weight(float w) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, WRAP, w);
        return l;
    }

    static LinearLayout.LayoutParams weight(float w, int sideMarginPx) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, WRAP, w);
        l.leftMargin = sideMarginPx;
        l.rightMargin = sideMarginPx;
        return l;
    }

    static View gap(Context c, int dpH) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpH)));
        return v;
    }

    static View divider(Context c, Palette p) {
        View v = new View(c);
        v.setBackgroundColor(p.line);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(c, 1)));
        l.topMargin = dp(c, 14);
        l.bottomMargin = dp(c, 14);
        v.setLayoutParams(l);
        return v;
    }

    /** A scrolling page with comfortable side margins. Returns the inner column to fill. */
    static LinearLayout page(Context c, FrameLayout host) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout inner = col(c);
        int side = dp(c, 20);
        inner.setPadding(side, dp(c, 12), side, dp(c, 28));
        sv.addView(inner, new FrameLayout.LayoutParams(MATCH, WRAP));
        host.addView(sv, new FrameLayout.LayoutParams(MATCH, MATCH));
        return inner;
    }

    /** Back arrow plus a title, for pushed screens. */
    static LinearLayout topBar(MainActivity a, String title, String right) {
        Context c = a;
        Palette p = a.pal;
        LinearLayout bar = row(c);
        bar.setPadding(0, dp(c, 4), 0, dp(c, 8));
        TextView back = medium(c, "‹  Back", 16, p.accent);
        back.setPadding(dp(c, 4), dp(c, 10), dp(c, 12), dp(c, 10));
        back.setBackground(pressable(rounded(0, dp(c, 10), 0, 0), Palette.alpha(p.ink, 0.1f), dp(c, 10)));
        back.setOnClickListener(v -> a.back());
        bar.addView(back);
        TextView t = medium(c, title, 15, p.muted);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        bar.addView(t, weight(1));
        TextView r = medium(c, right == null ? "" : right, 14, p.muted);
        r.setMinWidth(dp(c, 64));
        r.setGravity(Gravity.END);
        bar.addView(r);
        return bar;
    }

    /** A row of mutually exclusive choices. */
    static LinearLayout segmented(Context c, Palette p, String[] options, int selected, java.util.function.IntConsumer onPick) {
        LinearLayout row = row(c);
        row.setBackground(rounded(p.surface, dp(c, 14), p.line, dp(c, 1)));
        int pad = dp(c, 4);
        row.setPadding(pad, pad, pad, pad);
        TextView[] views = new TextView[options.length];
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            TextView t = medium(c, options[i], 14, p.ink);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(c, 6), dp(c, 10), dp(c, 6), dp(c, 10));
            views[i] = t;
            t.setOnClickListener(v -> {
                for (int j = 0; j < views.length; j++) styleSegment(c, p, views[j], j == idx);
                onPick.accept(idx);
            });
            styleSegment(c, p, t, i == selected);
            row.addView(t, weight(1));
        }
        return row;
    }

    private static void styleSegment(Context c, Palette p, TextView t, boolean on) {
        t.setTextColor(on ? p.onAccent : p.ink);
        t.setBackground(rounded(on ? p.accent : 0x00000000, dp(c, 10), 0, 0));
    }

    /** A pill-shaped tag. */
    static TextView chip(Context c, Palette p, CharSequence s, boolean strong) {
        TextView t = medium(c, s, 13, strong ? p.onAccent : p.ink);
        t.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        t.setBackground(rounded(strong ? p.accent : p.accentSoft, dp(c, 100), 0, 0));
        return t;
    }
}
