package com.ichiji.kana;

import android.content.Context;
import android.content.res.Configuration;

/** App colors. Paper by day, night by night; indigo is the one accent, vermilion only for seals. */
final class Palette {
    final boolean dark;
    final int bg, surface, ink, muted, line, accent, accentSoft, onAccent, seal, good, warn, bad;

    private Palette(boolean dark, int bg, int surface, int ink, int muted, int line, int accent,
                    int accentSoft, int onAccent, int seal, int good, int warn, int bad) {
        this.dark = dark; this.bg = bg; this.surface = surface; this.ink = ink; this.muted = muted;
        this.line = line; this.accent = accent; this.accentSoft = accentSoft; this.onAccent = onAccent;
        this.seal = seal; this.good = good; this.warn = warn; this.bad = bad;
    }

    static final Palette PAPER = new Palette(false,
            0xFFF3EEE4, 0xFFFBF8F2, 0xFF1C1D22, 0xFF726C62, 0xFFDDD5C6,
            0xFF2E3F7F, 0xFFDDE1EE, 0xFFFBF8F2, 0xFFC2402E, 0xFF3E7D52, 0xFFB7801F, 0xFFB8432F);

    static final Palette NIGHT = new Palette(true,
            0xFF111216, 0xFF1A1C21, 0xFFECE6DA, 0xFF9A958B, 0xFF2C2F37,
            0xFF93A6EA, 0xFF232A45, 0xFF111216, 0xFFE0634F, 0xFF6DBA86, 0xFFE0B154, 0xFFE07A63);

    static Palette forApp(Context c, int pref) {
        boolean night;
        if (pref == 1) night = false;
        else if (pref == 2) night = true;
        else night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        return night ? NIGHT : PAPER;
    }

    static int alpha(int color, float a) {
        int al = Math.round(((color >>> 24) & 0xFF) * a);
        return (al << 24) | (color & 0x00FFFFFF);
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t), g = Math.round(ag + (bg - ag) * t), bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
