package com.ichiji.kana;

import org.json.JSONException;
import org.json.JSONObject;

/** Where one card stands in the learner's memory. Days are epoch days (LocalDate.toEpochDay). */
public final class CardState {
    public static final int SEED = 0, SPROUT = 1, BLOOM = 2, SEALED = 3;
    public static final String[] STAGE_NAMES = {"Seed", "Sprout", "Bloom", "Sealed"};
    public static final String[] STAGE_KANJI = {"種", "芽", "花", "印"};

    public double s, d;
    public long due, last, learned;
    public int reps, lapses, grade;
    public boolean known;

    static CardState learnedOn(long today) {
        CardState c = new CardState();
        c.s = Fsrs.initS(3);
        c.d = Fsrs.initD(3);
        c.learned = today;
        c.last = today;
        c.due = today + 1;
        c.grade = 3;
        return c;
    }

    static CardState knownOn(long today, long due) {
        CardState c = new CardState();
        c.s = 20;
        c.d = Fsrs.initD(3);
        c.learned = today;
        c.last = today;
        c.due = due;
        c.grade = 3;
        c.known = true;
        return c;
    }

    void review(long today, int g) {
        double[] r = Fsrs.review(d, s, Math.max(0, today - last), g);
        d = r[0];
        s = r[1];
        last = today;
        reps++;
        grade = g;
        if (g == 1) {
            lapses++;
            due = today + 1;
        } else {
            due = today + Fsrs.interval(s);
        }
    }

    /** Days until due if graded g today, without changing anything. */
    int previewDays(long today, int g) {
        if (g == 1) return 1;
        double[] r = Fsrs.review(d, s, Math.max(0, today - last), g);
        return Fsrs.interval(r[1]);
    }

    public boolean passed() { return known || (reps >= 1 && grade >= 2); }

    public int stage() {
        if (s < 5) return SEED;
        if (s < 15) return SPROUT;
        if (s < 60) return BLOOM;
        return SEALED;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("s", s); o.put("d", d); o.put("due", due); o.put("last", last); o.put("ln", learned);
        o.put("r", reps); o.put("l", lapses); o.put("g", grade);
        if (known) o.put("k", true);
        return o;
    }

    static CardState fromJson(JSONObject o) {
        CardState c = new CardState();
        c.s = o.optDouble("s", 3);
        c.d = o.optDouble("d", 5);
        c.due = o.optLong("due");
        c.last = o.optLong("last");
        c.learned = o.optLong("ln");
        c.reps = o.optInt("r");
        c.lapses = o.optInt("l");
        c.grade = o.optInt("g", 3);
        c.known = o.optBoolean("k");
        return c;
    }
}
