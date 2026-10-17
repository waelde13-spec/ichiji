package com.ichiji.kana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A kana chart. In SELECT mode cells toggle (placement); in PROGRESS mode cells are
 * shaded by memory stage and tapping one opens it.
 */
@SuppressLint("ViewConstructor")
public class KanaGridView extends View {
    public static final int SELECT = 0, PROGRESS = 1;

    public interface Listener { void onCell(Card c); }

    /** Gojūon rows: which of the five vowel columns hold a character. */
    private static final String[] GOJUON_ROWS = {
            "11111", "11111", "11111", "11111", "11111", "11111", "11111",
            "10101", "11111", "10001", "10000"};
    public static final String[] GOJUON_LABELS = {"a", "k", "s", "t", "n", "h", "m", "y", "r", "w", "n"};

    private final Palette p;
    private final Store store;
    private final int mode;
    private final List<Card> cells = new ArrayList<>();
    private int cols = 5;
    private String[] rowLabels;
    private final Set<String> selected = new HashSet<>();
    private Set<String> highlight = new HashSet<>();
    private Listener listener;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float cell, labelW, downX, downY;

    public KanaGridView(Context c, Palette p, Store store, int mode) {
        super(c);
        this.p = p;
        this.store = store;
        this.mode = mode;
        glyph.setTypeface(Ui.SERIF);
        glyph.setTextLocale(Locale.JAPAN);
        glyph.setTextAlign(Paint.Align.CENTER);
        small.setTypeface(Ui.SANS_MEDIUM);
        small.setTextAlign(Paint.Align.CENTER);
        stroke.setStyle(Paint.Style.STROKE);
    }

    /** Lays the 46 basic kana out the way the chart is printed, with its gaps. */
    public KanaGridView gojuon(List<Card> kana) {
        cells.clear();
        int k = 0;
        for (String row : GOJUON_ROWS) {
            for (int i = 0; i < 5; i++) cells.add(row.charAt(i) == '1' && k < kana.size() ? kana.get(k++) : null);
        }
        cols = 5;
        rowLabels = GOJUON_LABELS;
        requestLayout();
        return this;
    }

    public KanaGridView flow(List<Card> list, int columns) {
        cells.clear();
        cells.addAll(list);
        cols = columns;
        rowLabels = null;
        requestLayout();
        return this;
    }

    public KanaGridView listener(Listener l) { listener = l; return this; }

    public KanaGridView highlight(Set<String> ids) { highlight = ids; invalidate(); return this; }

    public Set<String> selected() { return selected; }

    public void setAll(boolean on) {
        for (Card c : cells) if (c != null) { if (on) selected.add(c.id); else selected.remove(c.id); }
        invalidate();
        if (listener != null) listener.onCell(null);
    }

    public void setSelected(Set<String> ids) {
        selected.clear();
        selected.addAll(ids);
        invalidate();
    }

    private int rows() { return (cells.size() + cols - 1) / cols; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        labelW = rowLabels != null ? Ui.dp(getContext(), 26) : 0;
        cell = Math.min((w - labelW) / cols, Ui.dp(getContext(), 76));
        setMeasuredDimension(w, Math.round(cell * rows()));
    }

    private float left() {
        return labelW + (getWidth() - labelW - cell * cols) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float gap = Ui.dp(getContext(), 3);
        float radius = Ui.dp(getContext(), 10);
        float x0 = left();
        small.setTextSize(Ui.dp(getContext(), 11));
        small.setColor(p.muted);
        if (rowLabels != null) {
            for (int row = 0; row < rows() && row < rowLabels.length; row++) {
                canvas.drawText(rowLabels[row], labelW / 2f, row * cell + cell / 2f + small.getTextSize() / 3f, small);
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            Card c = cells.get(i);
            if (c == null) continue;
            int row = i / cols, col = i % cols;
            r.set(x0 + col * cell + gap, row * cell + gap, x0 + (col + 1) * cell - gap, (row + 1) * cell - gap);
            int bg, fg, border = 0;
            if (mode == SELECT) {
                boolean on = selected.contains(c.id);
                bg = on ? p.accent : p.surface;
                fg = on ? p.onAccent : p.ink;
                border = on ? 0 : p.line;
            } else {
                CardState s = store.state(c);
                if (s == null) {
                    bg = 0;
                    fg = Palette.alpha(p.muted, 0.45f);
                    border = Palette.alpha(p.line, 0.6f);
                } else {
                    switch (s.stage()) {
                        case CardState.SEED: bg = p.surface; fg = p.ink; border = p.line; break;
                        case CardState.SPROUT: bg = p.accentSoft; fg = p.ink; break;
                        case CardState.BLOOM: bg = Palette.mix(p.accentSoft, p.accent, 0.45f); fg = p.dark ? p.ink : p.onAccent; break;
                        default: bg = p.accent; fg = p.onAccent;
                    }
                }
            }
            if (bg != 0) {
                fill.setColor(bg);
                canvas.drawRoundRect(r, radius, radius, fill);
            }
            if (border != 0) {
                stroke.setColor(border);
                stroke.setStrokeWidth(Ui.dp(getContext(), 1));
                canvas.drawRoundRect(r, radius, radius, stroke);
            }
            if (highlight.contains(c.id)) {
                stroke.setColor(p.seal);
                stroke.setStrokeWidth(Ui.dp(getContext(), 2));
                canvas.drawRoundRect(r, radius, radius, stroke);
            }
            int len = c.ch.length();
            glyph.setTextSize(cell * (len > 1 ? 0.36f : 0.5f));
            glyph.setColor(fg);
            Paint.FontMetrics fm = glyph.getFontMetrics();
            float baseline = r.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(c.ch, r.centerX(), baseline, glyph);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
            return true;
        }
        if (e.getAction() != MotionEvent.ACTION_UP) return true;
        if (Math.hypot(e.getX() - downX, e.getY() - downY) > Ui.dp(getContext(), 12)) return true;
        int row = (int) (e.getY() / cell);
        float x0 = left();
        if (e.getX() < x0) {
            if (mode == SELECT && rowLabels != null) toggleRow(row);
            return true;
        }
        int col = (int) ((e.getX() - x0) / cell);
        if (col < 0 || col >= cols) return true;
        int i = row * cols + col;
        if (i < 0 || i >= cells.size() || cells.get(i) == null) return true;
        Card c = cells.get(i);
        performClick();
        if (mode == SELECT) {
            if (!selected.remove(c.id)) selected.add(c.id);
            invalidate();
        }
        if (listener != null) listener.onCell(c);
        return true;
    }

    private void toggleRow(int row) {
        boolean allOn = true;
        for (int i = row * cols; i < Math.min(cells.size(), (row + 1) * cols); i++) {
            Card c = cells.get(i);
            if (c != null && !selected.contains(c.id)) allOn = false;
        }
        for (int i = row * cols; i < Math.min(cells.size(), (row + 1) * cols); i++) {
            Card c = cells.get(i);
            if (c == null) continue;
            if (allOn) selected.remove(c.id); else selected.add(c.id);
        }
        invalidate();
        if (listener != null) listener.onCell(null);
    }

    @Override
    public boolean performClick() { return super.performClick(); }
}
