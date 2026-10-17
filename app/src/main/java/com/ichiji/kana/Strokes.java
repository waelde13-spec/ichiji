package com.ichiji.kana;

import android.content.Context;
import android.graphics.Path;
import android.graphics.PathMeasure;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stroke-order data from KanjiVG (CC BY-SA 3.0, Ulrich Apel), in a 109 x 109 box.
 * Paths are parsed from SVG path syntax on first use and cached.
 */
public final class Strokes {
    public static final float BOX = 109f;
    private static JSONObject raw;
    private static final Map<String, List<Path>> cache = new HashMap<>();

    private Strokes() {}

    public static synchronized List<Path> get(Context c, String ch) {
        if (ch == null || ch.isEmpty()) return null;
        List<Path> hit = cache.get(ch);
        if (hit != null) return hit;
        try {
            if (raw == null) raw = new JSONObject(Store.readAsset(c, "strokes.json"));
            JSONArray arr = raw.optJSONArray(ch);
            if (arr == null) return null;
            List<Path> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) out.add(parse(arr.getString(i)));
            out = Collections.unmodifiableList(out);
            cache.put(ch, out);
            return out;
        } catch (JSONException e) {
            return null;
        }
    }

    /** Evenly spaced points along a path: {x0, y0, x1, y1, ...}. */
    public static float[] sample(Path p, int n) {
        PathMeasure pm = new PathMeasure(p, false);
        float len = pm.getLength();
        float[] out = new float[n * 2];
        float[] pos = new float[2];
        for (int i = 0; i < n; i++) {
            pm.getPosTan(len * i / (n - 1), pos, null);
            out[i * 2] = pos[0];
            out[i * 2 + 1] = pos[1];
        }
        return out;
    }

    /** Re-spaces a free-hand polyline into n evenly spaced points. */
    public static float[] resample(List<Float> xy, int n) {
        int m = xy.size() / 2;
        float[] out = new float[n * 2];
        if (m == 0) return out;
        float[] cum = new float[m];
        for (int i = 1; i < m; i++) {
            float dx = xy.get(i * 2) - xy.get(i * 2 - 2), dy = xy.get(i * 2 + 1) - xy.get(i * 2 - 1);
            cum[i] = cum[i - 1] + (float) Math.hypot(dx, dy);
        }
        float total = cum[m - 1];
        int j = 0;
        for (int i = 0; i < n; i++) {
            float target = total * i / (n - 1);
            while (j < m - 2 && cum[j + 1] < target) j++;
            if (m == 1 || total == 0) {
                out[i * 2] = xy.get(0);
                out[i * 2 + 1] = xy.get(1);
                continue;
            }
            float seg = cum[j + 1] - cum[j];
            float t = seg == 0 ? 0 : (target - cum[j]) / seg;
            t = Math.max(0, Math.min(1, t));
            out[i * 2] = xy.get(j * 2) + (xy.get(j * 2 + 2) - xy.get(j * 2)) * t;
            out[i * 2 + 1] = xy.get(j * 2 + 1) + (xy.get(j * 2 + 3) - xy.get(j * 2 + 1)) * t;
        }
        return out;
    }

    // ------------------------------------------------------------ minimal SVG path parser

    static Path parse(String d) {
        Path p = new Path();
        Tok t = new Tok(d);
        float x = 0, y = 0, cx2 = 0, cy2 = 0, sx = 0, sy = 0;
        char cmd = 'M';
        char prev = 0;
        while (t.more()) {
            if (t.isCmd()) cmd = t.cmd();
            switch (cmd) {
                case 'M': case 'm': {
                    float nx = t.num(), ny = t.num();
                    if (cmd == 'm') { nx += x; ny += y; }
                    p.moveTo(nx, ny);
                    x = sx = nx; y = sy = ny;
                    cmd = cmd == 'm' ? 'l' : 'L';
                    break;
                }
                case 'L': case 'l': {
                    float nx = t.num(), ny = t.num();
                    if (cmd == 'l') { nx += x; ny += y; }
                    p.lineTo(nx, ny);
                    x = nx; y = ny;
                    break;
                }
                case 'H': case 'h': {
                    float nx = t.num();
                    if (cmd == 'h') nx += x;
                    p.lineTo(nx, y);
                    x = nx;
                    break;
                }
                case 'V': case 'v': {
                    float ny = t.num();
                    if (cmd == 'v') ny += y;
                    p.lineTo(x, ny);
                    y = ny;
                    break;
                }
                case 'C': case 'c': {
                    float x1 = t.num(), y1 = t.num(), x2 = t.num(), y2 = t.num(), nx = t.num(), ny = t.num();
                    if (cmd == 'c') { x1 += x; y1 += y; x2 += x; y2 += y; nx += x; ny += y; }
                    p.cubicTo(x1, y1, x2, y2, nx, ny);
                    cx2 = x2; cy2 = y2; x = nx; y = ny;
                    break;
                }
                case 'S': case 's': {
                    float x1 = x, y1 = y;
                    if (prev == 'C' || prev == 'c' || prev == 'S' || prev == 's') { x1 = 2 * x - cx2; y1 = 2 * y - cy2; }
                    float x2 = t.num(), y2 = t.num(), nx = t.num(), ny = t.num();
                    if (cmd == 's') { x2 += x; y2 += y; nx += x; ny += y; }
                    p.cubicTo(x1, y1, x2, y2, nx, ny);
                    cx2 = x2; cy2 = y2; x = nx; y = ny;
                    break;
                }
                case 'Z': case 'z':
                    p.close();
                    x = sx; y = sy;
                    break;
                default:
                    t.num(); // unknown command: skip a number to avoid looping forever
            }
            prev = cmd;
        }
        return p;
    }

    private static final class Tok {
        final String s;
        int i;

        Tok(String s) { this.s = s; }

        void skip() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == ',' || c == '\n' || c == '\t' || c == '\r') i++;
                else break;
            }
        }

        boolean more() { skip(); return i < s.length(); }

        boolean isCmd() {
            skip();
            if (i >= s.length()) return false;
            char c = s.charAt(i);
            return Character.isLetter(c) && c != 'e' && c != 'E';
        }

        char cmd() { return s.charAt(i++); }

        float num() {
            skip();
            int start = i;
            if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
            boolean dot = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) i++;
                else if (c == '.' && !dot) { dot = true; i++; }
                else break;
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++;
                if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            if (start == i) { i++; return 0; }
            try {
                return Float.parseFloat(s.substring(start, i));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
