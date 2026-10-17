package com.ichiji.kana;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Draws both home-screen widgets as bitmaps, so the three colourways and the
 * serif Japanese glyphs look the same on every launcher.
 */
final class WidgetArt {
    private WidgetArt() {}

    /** bg, ink, accent */
    static final int[][] THEMES = {
            {0xFF24305E, 0xFFF1EBDD, 0xFFE46A4F},   // 藍 Indigo
            {0xFFF3EEE4, 0xFF1C1D22, 0xFFC2402E},   // 紙 Paper
            {0xFF121316, 0xFFECE6DA, 0xFFE0634F}};  // 夜 Night

    private static Paint glyphPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(Ui.SERIF);
        p.setTextLocale(Locale.JAPAN);
        p.setTextAlign(Paint.Align.CENTER);
        return p;
    }

    private static Paint sansPaint(boolean medium) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(medium ? Ui.SANS_MEDIUM : Ui.SANS);
        return p;
    }

    private static void centerText(Canvas cv, String s, float cx, float cy, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        cv.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, p);
    }

    private static Bitmap canvasFor(int w, int h, int bg, float radius, Canvas[] out) {
        Bitmap b = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(bg);
        cv.drawRoundRect(new RectF(0, 0, w, h), radius, radius, fill);
        out[0] = cv;
        return b;
    }

    /** Small widget: today's character, its reading, a dot until today is done. */
    static Bitmap small(Context c, Store store, int w, int h) {
        int[] t = THEMES[Math.max(0, Math.min(2, store.widgetTheme))];
        float radius = Ui.dp(c, 22);
        Canvas[] holder = new Canvas[1];
        Bitmap bmp = canvasFor(w, h, t[0], radius, holder);
        Canvas cv = holder[0];
        float m = Math.min(w, h);
        float pad = m * 0.1f;

        Card card = store.featured();
        boolean done = store.doneToday();
        int due = store.dueReviews().size();

        Paint label = sansPaint(true);
        label.setColor(Palette.alpha(t[1], 0.6f));
        label.setTextSize(m * 0.085f);
        label.setLetterSpacing(0.08f);
        int streak = store.streak();
        cv.drawText(streak > 0 ? streak + (streak == 1 ? " DAY" : " DAYS") : "TODAY", pad, pad + label.getTextSize(), label);

        if (!done) {
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(t[2]);
            float r = m * 0.045f;
            cv.drawCircle(w - pad - r, pad + label.getTextSize() * 0.62f, r, dot);
        }

        Paint g = glyphPaint();
        g.setColor(t[1]);
        String ch = card == null ? "字" : card.ch;
        g.setTextSize(m * (ch.length() > 1 ? 0.34f : 0.46f));
        centerText(cv, ch, w / 2f, h * 0.5f, g);

        Paint read = sansPaint(true);
        read.setColor(t[1]);
        read.setTextAlign(Paint.Align.CENTER);
        String reading;
        if (card == null) reading = "open Ichiji";
        else if (store.isLearned(card) && due > 0 && done == false && allLearned(store)) reading = due + " to review";
        else reading = card.isKanji() ? card.meaning : card.rd;
        read.setTextSize(m * (reading.length() > 12 ? 0.08f : 0.11f));
        cv.drawText(ellipsize(reading, read, w - pad * 2), w / 2f, h - pad * 1.1f, read);
        return bmp;
    }

    private static boolean allLearned(Store store) {
        for (Card c : store.todayBatch()) if (!store.isLearned(c)) return false;
        return true;
    }

    private static String ellipsize(String s, Paint p, float max) {
        if (p.measureText(s) <= max) return s;
        while (s.length() > 1 && p.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Wide widget: the whole kana chart, filling in as you learn. Columns read right to left. */
    static Bitmap board(Context c, Store store, int w, int h) {
        int[] t = THEMES[Math.max(0, Math.min(2, store.widgetTheme))];
        float radius = Ui.dp(c, 22);
        Canvas[] holder = new Canvas[1];
        Bitmap bmp = canvasFor(w, h, t[0], radius, holder);
        Canvas cv = holder[0];
        boolean kata = store.boardIsKatakana();
        List<Card> kana = store.cardsOfKind(kata ? Card.KATA : Card.HIRA);
        Set<String> today = new HashSet<>();
        for (Card card : store.todayBatch()) today.add(card.id);

        float pad = Math.min(w, h) * 0.07f;
        Paint head = sansPaint(true);
        head.setColor(Palette.alpha(t[1], 0.65f));
        head.setTextSize(Math.max(Ui.dp(c, 10), h * 0.06f));
        head.setLetterSpacing(0.06f);
        int learned = store.learnedCount(kata ? Card.KATA : Card.HIRA);
        String left = (kata ? "カタカナ  " : "ひらがな  ") + learned + " / 46";
        Paint headJp = glyphPaint();
        headJp.setTextAlign(Paint.Align.LEFT);
        headJp.setColor(Palette.alpha(t[1], 0.75f));
        headJp.setTextSize(head.getTextSize());
        float headY = pad + head.getTextSize();
        cv.drawText(left, pad, headY, headJp);
        int streak = store.streak();
        String right = streak > 0 ? streak + (streak == 1 ? " day" : " days") : "";
        head.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(right, w - pad, headY, head);

        // 11 chart rows become columns, right to left; 5 vowels top to bottom.
        String[] rows = {"11111", "11111", "11111", "11111", "11111", "11111", "11111", "10101", "11111", "10001", "10000"};
        float top = headY + pad * 0.7f;
        float gridH = h - top - pad;
        float gridW = w - pad * 2;
        float cell = Math.min(gridW / 11f, gridH / 5f);
        float x0 = pad + (gridW - cell * 11) / 2f;
        float y0 = top + (gridH - cell * 5) / 2f;
        Paint g = glyphPaint();
        g.setTextSize(cell * 0.62f);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Math.max(2f, cell * 0.07f));
        ring.setColor(t[2]);
        RectF r = new RectF();
        int k = 0;
        for (int row = 0; row < rows.length; row++) {
            int col = 10 - row;
            for (int v = 0; v < 5; v++) {
                if (rows[row].charAt(v) != '1' || k >= kana.size()) continue;
                Card card = kana.get(k++);
                float cx = x0 + col * cell + cell / 2f, cy = y0 + v * cell + cell / 2f;
                CardState s = store.state(card);
                float inset = cell * 0.08f;
                r.set(cx - cell / 2 + inset, cy - cell / 2 + inset, cx + cell / 2 - inset, cy + cell / 2 - inset);
                if (s != null && s.stage() == CardState.SEALED) {
                    fill.setColor(t[1]);
                    cv.drawRoundRect(r, cell * 0.18f, cell * 0.18f, fill);
                    g.setColor(t[0]);
                } else if (s == null) {
                    g.setColor(Palette.alpha(t[1], 0.13f));
                } else if (!s.passed()) {
                    g.setColor(Palette.alpha(t[1], 0.5f));
                } else {
                    g.setColor(Palette.alpha(t[1], 0.92f));
                }
                centerText(cv, card.ch, cx, cy, g);
                if (today.contains(card.id)) cv.drawRoundRect(r, cell * 0.18f, cell * 0.18f, ring);
            }
        }
        return bmp;
    }
}
