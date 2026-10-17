package com.ichiji.kana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.List;

/** One small frame per stroke, each showing the character built up to that stroke. */
@SuppressLint("ViewConstructor")
public class StrokeFramesView extends View {
    private final Palette p;
    private final List<Path> strokes;
    private final Paint done = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint now = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float frame;
    private int cols;

    public StrokeFramesView(Context c, Palette p, List<Path> strokes) {
        super(c);
        this.p = p;
        this.strokes = strokes;
        for (Paint pt : new Paint[]{done, now}) {
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeCap(Paint.Cap.ROUND);
            pt.setStrokeJoin(Paint.Join.ROUND);
            pt.setStrokeWidth(5.5f);
        }
        done.setColor(Palette.alpha(p.ink, 0.45f));
        now.setColor(p.seal);
        bg.setColor(p.surface);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(p.line);
        num.setColor(p.muted);
        num.setTypeface(Ui.SANS_MEDIUM);
        num.setTextSize(13f);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        float target = Ui.dp(getContext(), 62);
        cols = Math.max(1, (int) (w / target));
        frame = w / (float) cols;
        int rows = (strokes.size() + cols - 1) / cols;
        setMeasuredDimension(w, Math.round(rows * frame));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float gap = Ui.dp(getContext(), 3);
        float rad = Ui.dp(getContext(), 10);
        border.setStrokeWidth(Math.max(1, Ui.dp(getContext(), 1)));
        num.setTextSize(Ui.dp(getContext(), 9.5f));
        for (int i = 0; i < strokes.size(); i++) {
            float x = (i % cols) * frame, y = (i / cols) * frame;
            r.set(x + gap, y + gap, x + frame - gap, y + frame - gap);
            canvas.drawRoundRect(r, rad, rad, bg);
            canvas.drawRoundRect(r, rad, rad, border);
            canvas.drawText(String.valueOf(i + 1), r.left + Ui.dp(getContext(), 5), r.top + Ui.dp(getContext(), 12), num);
            canvas.save();
            float inner = r.width() * 0.84f;
            canvas.translate(r.centerX() - inner / 2, r.centerY() - inner / 2);
            float s = inner / Strokes.BOX;
            canvas.scale(s, s);
            for (int j = 0; j < i; j++) canvas.drawPath(strokes.get(j), done);
            canvas.drawPath(strokes.get(i), now);
            canvas.restore();
        }
    }
}
