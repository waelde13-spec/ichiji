package com.ichiji.kana;

/**
 * FSRS-5 spaced repetition, default parameters.
 * Pure Java: no Android dependencies, so it can be unit-tested on a desktop JVM.
 * Grades: 1 Again, 2 Hard, 3 Good, 4 Easy. Times are in whole days.
 */
public final class Fsrs {
    private Fsrs() {}

    static final double[] W = {
            0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192,
            1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621
    };
    static final double DECAY = -0.5;
    static final double FACTOR = 19.0 / 81.0;
    /** Target retention 0.9: with FSRS-5's curve the interval equals the stability. */
    public static final int MAX_INTERVAL = 365;

    public static double retrievability(double elapsedDays, double stability) {
        return Math.pow(1 + FACTOR * elapsedDays / stability, DECAY);
    }

    public static double initS(int g) { return W[g - 1]; }

    public static double initD(int g) { return clampD(W[4] - Math.exp(W[5] * (g - 1)) + 1); }

    static double clampD(double d) { return Math.min(10, Math.max(1, d)); }

    static double nextD(double d, int g) {
        double delta = -W[6] * (g - 3);
        double damped = d + delta * (10 - d) / 9;
        return clampD(W[7] * initD(4) + (1 - W[7]) * damped);
    }

    static double recallS(double d, double s, double r, int g) {
        double hard = g == 2 ? W[15] : 1;
        double easy = g == 4 ? W[16] : 1;
        return s * (Math.exp(W[8]) * (11 - d) * Math.pow(s, -W[9])
                * (Math.exp(W[10] * (1 - r)) - 1) * hard * easy + 1);
    }

    static double forgetS(double d, double s, double r) {
        double v = W[11] * Math.pow(d, -W[12]) * (Math.pow(s + 1, W[13]) - 1) * Math.exp(W[14] * (1 - r));
        return Math.min(v, s);
    }

    static double shortTermS(double s, int g) {
        return s * Math.exp(W[17] * (g - 3 + W[18]));
    }

    /** Returns {newDifficulty, newStability}. */
    public static double[] review(double d, double s, double elapsedDays, int g) {
        double ns;
        if (elapsedDays < 1) {
            ns = shortTermS(s, g);
        } else {
            double r = retrievability(elapsedDays, s);
            ns = g == 1 ? forgetS(d, s, r) : recallS(d, s, r, g);
        }
        ns = Math.max(0.1, ns);
        return new double[]{nextD(d, g), ns};
    }

    /** Days until the next review for a given stability (retention 0.9). */
    public static int interval(double s) {
        long i = Math.round(s);
        return (int) Math.max(1, Math.min(MAX_INTERVAL, i));
    }
}
