package com.ichiji.kana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.time.LocalDate;

/** Days studied, one square per day, weeks as columns, today at the bottom right. */
@SuppressLint("ViewConstructor")
public class HeatmapView extends View {
    private final Palette p;
    private final Store store;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int weeks = 18;
    private float cell;

    public HeatmapView(Context c, Palette p, Store store) {
        super(c);
        this.p = p;
        this.store = store;
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(p.seal);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        float target = Ui.dp(getContext(), 17);
        weeks = Math.max(8, Math.min(26, (int) (w / target)));
        cell = w / (float) weeks;
        setMeasuredDimension(w, Math.round(cell * 7));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long today = Store.today();
        int todayRow = LocalDate.ofEpochDay(today).getDayOfWeek().getValue() % 7; // Sunday = 0
        long start = today - (long) (weeks - 1) * 7 - todayRow;
        float gap = Math.max(1.5f, cell * 0.12f);
        float rad = cell * 0.22f;
        ring.setStrokeWidth(Ui.dp(getContext(), 1.5f));
        for (int w = 0; w < weeks; w++) {
            for (int d = 0; d < 7; d++) {
                long day = start + w * 7L + d;
                if (day > today) continue;
                int n = store.activityOn(day);
                int color;
                if (n == 0) color = Palette.alpha(p.ink, p.dark ? 0.08f : 0.06f);
                else if (n < 3) color = Palette.mix(p.accentSoft, p.accent, 0.3f);
                else if (n < 8) color = Palette.mix(p.accentSoft, p.accent, 0.65f);
                else color = p.accent;
                fill.setColor(color);
                r.set(w * cell + gap / 2, d * cell + gap / 2, (w + 1) * cell - gap / 2, (d + 1) * cell - gap / 2);
                canvas.drawRoundRect(r, rad, rad, fill);
                if (day == today) canvas.drawRoundRect(r, rad, rad, ring);
            }
        }
    }
}
