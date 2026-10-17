package com.ichiji.kana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a character drawing itself stroke by stroke, then lets the learner trace it.
 * Each traced stroke is compared with the reference and marked clean, close, drifted or reversed.
 */
@SuppressLint("ViewConstructor")
public class StrokeView extends View {
    public static final int CLEAN = 0, CLOSE = 1, DRIFT = 2, REVERSED = 3;
    private static final int SAMPLES = 32;
    private static final long STROKE_MS = 620, GAP_MS = 140;

    public interface TraceListener {
        void onStroke(int index, int total, int verdict);
        void onDone(int clean, int total);
    }

    private final Palette p;
    private final List<Path> strokes;
    private final List<float[]> ref = new ArrayList<>();
    private boolean tracing;
    private boolean animating;
    private long animStart;

    private int current;
    private final List<Path> drawn = new ArrayList<>();
    private final List<Integer> verdicts = new ArrayList<>();
    private Path live;
    private final List<Float> livePts = new ArrayList<>();
    private TraceListener listener;

    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghost = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint user = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path seg = new Path();
    private final RectF box = new RectF();
    private float scale = 1, ox, oy;

    public StrokeView(Context c, Palette p, List<Path> strokes) {
        super(c);
        this.p = p;
        this.strokes = strokes;
        for (Path s : strokes) ref.add(Strokes.sample(s, SAMPLES));
        for (Paint pt : new Paint[]{ink, ghost, user}) {
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeCap(Paint.Cap.ROUND);
            pt.setStrokeJoin(Paint.Join.ROUND);
        }
        ink.setColor(p.ink);
        ink.setStrokeWidth(4.2f);
        ghost.setColor(Palette.alpha(p.ink, p.dark ? 0.16f : 0.11f));
        ghost.setStrokeWidth(5.2f);
        user.setStrokeWidth(4.6f);
        guide.setStyle(Paint.Style.STROKE);
        guide.setColor(Palette.alpha(p.muted, 0.35f));
        guide.setStrokeWidth(0.5f);
        guide.setPathEffect(new DashPathEffect(new float[]{2.5f, 2.5f}, 0));
        frame.setColor(p.surface);
        dot.setColor(p.seal);
        num.setColor(p.muted);
        num.setTypeface(Ui.SANS_MEDIUM);
        num.setTextSize(6.2f);
        num.setTextAlign(Paint.Align.CENTER);
        setContentDescription("Stroke order");
    }

    public int strokeCount() { return strokes.size(); }

    public void setListener(TraceListener l) { listener = l; }

    public void replay() {
        tracing = false;
        animating = true;
        animStart = SystemClock.uptimeMillis();
        invalidate();
    }

    public void startTracing() {
        tracing = true;
        animating = false;
        clearTrace();
    }

    public boolean isTracing() { return tracing; }

    public void clearTrace() {
        current = 0;
        drawn.clear();
        verdicts.clear();
        live = null;
        livePts.clear();
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int size = Math.min(w, Ui.dp(getContext(), 360));
        setMeasuredDimension(w, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float size = Math.min(w, h);
        scale = size / Strokes.BOX;
        ox = (w - size) / 2f;
        oy = (h - size) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = Ui.dp(getContext(), 20);
        box.set(ox, oy, ox + Strokes.BOX * scale, oy + Strokes.BOX * scale);
        canvas.drawRoundRect(box, r, r, frame);
        canvas.save();
        canvas.translate(ox, oy);
        canvas.scale(scale, scale);
        float b = Strokes.BOX;
        canvas.drawLine(b / 2, 4, b / 2, b - 4, guide);
        canvas.drawLine(4, b / 2, b - 4, b / 2, guide);
        for (Path s : strokes) canvas.drawPath(s, ghost);
        if (tracing) drawTrace(canvas); else drawAnimation(canvas);
        canvas.restore();
    }

    private void drawAnimation(Canvas canvas) {
        int n = strokes.size();
        if (!animating) {
            for (int i = 0; i < n; i++) {
                canvas.drawPath(strokes.get(i), ink);
                drawNumber(canvas, i);
            }
            return;
        }
        long t = SystemClock.uptimeMillis() - animStart;
        int idx = (int) (t / (STROKE_MS + GAP_MS));
        float frac = Math.min(1f, (t % (STROKE_MS + GAP_MS)) / (float) STROKE_MS);
        for (int i = 0; i < n && i < idx; i++) {
            canvas.drawPath(strokes.get(i), ink);
            drawNumber(canvas, i);
        }
        if (idx < n) {
            PathMeasure pm = new PathMeasure(strokes.get(idx), false);
            seg.reset();
            float eased = (float) (1 - Math.pow(1 - frac, 2));
            pm.getSegment(0, pm.getLength() * eased, seg, true);
            canvas.drawPath(seg, ink);
            float[] pts = ref.get(idx);
            canvas.drawCircle(pts[0], pts[1], 2.2f, dot);
            postInvalidateOnAnimation();
        } else {
            animating = false;
        }
    }

    private void drawNumber(Canvas canvas, int i) {
        float[] pts = ref.get(i);
        float dx = pts[2] - pts[0], dy = pts[3] - pts[1];
        float len = (float) Math.max(0.001, Math.hypot(dx, dy));
        float nx = pts[0] - dx / len * 5f - dy / len * 3f;
        float ny = pts[1] - dy / len * 5f + dx / len * 3f;
        canvas.drawText(String.valueOf(i + 1), nx, ny + 2.2f, num);
    }

    private void drawTrace(Canvas canvas) {
        for (int i = 0; i < drawn.size(); i++) {
            int v = verdicts.get(i);
            if (v != CLEAN) {
                ink.setColor(Palette.alpha(p.accent, 0.55f));
                canvas.drawPath(strokes.get(i), ink);
                ink.setColor(p.ink);
            }
            user.setColor(v == CLEAN ? p.ink : v == CLOSE ? p.warn : p.bad);
            canvas.drawPath(drawn.get(i), user);
        }
        if (current < strokes.size()) {
            float[] pts = ref.get(current);
            dot.setAlpha(200);
            canvas.drawCircle(pts[0], pts[1], 2.4f, dot);
            dot.setAlpha(255);
        }
        if (live != null) {
            user.setColor(p.ink);
            canvas.drawPath(live, user);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!tracing || current >= strokes.size()) return false;
        float x = (e.getX() - ox) / scale, y = (e.getY() - oy) / scale;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                live = new Path();
                live.moveTo(x, y);
                livePts.clear();
                livePts.add(x);
                livePts.add(y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (live == null) return true;
                for (int h = 0; h < e.getHistorySize(); h++) {
                    float hx = (e.getHistoricalX(h) - ox) / scale, hy = (e.getHistoricalY(h) - oy) / scale;
                    live.lineTo(hx, hy);
                    livePts.add(hx);
                    livePts.add(hy);
                }
                live.lineTo(x, y);
                livePts.add(x);
                livePts.add(y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (live == null) return true;
                livePts.add(x);
                livePts.add(y);
                finishStroke();
                return true;
            case MotionEvent.ACTION_CANCEL:
                live = null;
                invalidate();
                return true;
        }
        return true;
    }

    private void finishStroke() {
        float[] u = Strokes.resample(livePts, SAMPLES);
        float[] rf = ref.get(current);
        float fwd = 0, rev = 0;
        for (int i = 0; i < SAMPLES; i++) {
            fwd += Math.hypot(u[i * 2] - rf[i * 2], u[i * 2 + 1] - rf[i * 2 + 1]);
            int j = SAMPLES - 1 - i;
            rev += Math.hypot(u[i * 2] - rf[j * 2], u[i * 2 + 1] - rf[j * 2 + 1]);
        }
        fwd /= SAMPLES;
        rev /= SAMPLES;
        int verdict;
        if (fwd <= 9f) verdict = CLEAN;
        else if (fwd <= 16f) verdict = CLOSE;
        else verdict = DRIFT;
        if (verdict != CLEAN && rev < fwd * 0.6f && rev <= 14f) verdict = REVERSED;
        drawn.add(live);
        verdicts.add(verdict);
        live = null;
        current++;
        invalidate();
        if (listener != null) {
            listener.onStroke(current - 1, strokes.size(), verdict);
            if (current == strokes.size()) {
                int clean = 0;
                for (int v : verdicts) if (v == CLEAN) clean++;
                listener.onDone(clean, strokes.size());
            }
        }
    }
}
