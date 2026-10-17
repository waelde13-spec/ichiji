package com.ichiji.kana;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One batch of reviews: recall with the answer hidden, then say how it went. */
final class ReviewScreen extends Screen {
    private final List<Card> queue;
    private final Set<String> requeued = new HashSet<>();
    private int idx;
    private boolean revealed;
    private int good, missed;

    ReviewScreen(MainActivity a) {
        super(a);
        queue = new ArrayList<>(a.store.dueReviews());
    }

    @Override boolean rebuildOnResume() { return false; }

    static String days(int d) {
        if (d <= 1) return "tomorrow";
        if (d < 14) return d + " days";
        if (d < 60) return Math.round(d / 7f) + " weeks";
        if (d < 365) return Math.round(d / 30f) + " months";
        return "a year";
    }

    @Override
    void build(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        boolean finished = idx >= queue.size();
        page.addView(Ui.topBar(a, "Reviews", finished ? "" : (idx + 1) + " / " + queue.size()));

        if (finished) {
            LinearLayout done = Ui.card(c, p);
            done.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView seal = Ui.jp(c, "済", 72, p.seal);
            seal.setGravity(Gravity.CENTER);
            done.addView(seal, Ui.lp(Ui.MATCH, Ui.WRAP));
            TextView t = Ui.medium(c, queue.isEmpty() ? "Nothing to review" : "Batch done", 22, p.ink);
            t.setGravity(Gravity.CENTER);
            done.addView(t, Ui.lp(Ui.MATCH, Ui.WRAP));
            TextView sub = Ui.text(c, queue.isEmpty() ? "Come back tomorrow."
                    : good + " remembered, " + missed + " to see again soon. The schedule has been updated.", 15, p.muted);
            sub.setGravity(Gravity.CENTER);
            sub.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 18));
            done.addView(sub, Ui.lp(Ui.MATCH, Ui.WRAP));
            done.addView(Ui.button(c, p, "Back to today", Ui.PRIMARY, v -> a.back()), Ui.lp(Ui.MATCH, Ui.WRAP));
            page.addView(done);
            return;
        }

        Card card = queue.get(idx);
        LinearLayout box = Ui.card(c, p);
        box.addView(Ui.label(c, card.kindLabel(), p.muted));
        TextView big = Ui.jp(c, card.ch, card.ch.length() > 1 ? 104 : 140, p.ink);
        big.setGravity(Gravity.CENTER);
        big.setPadding(0, Ui.dp(c, 24), 0, Ui.dp(c, 24));
        box.addView(big, Ui.lp(Ui.MATCH, Ui.WRAP));

        if (revealed) {
            big.setOnClickListener(v -> a.speech.say(card.speakText()));
            TextView ans = Ui.medium(c, card.answer(), 28, p.accent);
            ans.setGravity(Gravity.CENTER);
            box.addView(ans, Ui.lp(Ui.MATCH, Ui.WRAP));
            String sub = card.isKanji() ? card.kana + " · " + card.rd + "\n" + card.word + " · " + card.wordMeaning
                    : card.word + " · " + card.wordRomaji + " · " + card.wordMeaning;
            TextView s = Ui.jp(c, sub, 15, p.muted);
            s.setGravity(Gravity.CENTER);
            s.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 4));
            box.addView(s, Ui.lp(Ui.MATCH, Ui.WRAP));
        } else {
            TextView q = Ui.text(c, card.isKanji() ? "What does it mean, and how is it read?" : "How is it read?", 15, p.muted);
            q.setGravity(Gravity.CENTER);
            box.addView(q, Ui.lp(Ui.MATCH, Ui.WRAP));
        }
        page.addView(box);
        page.addView(Ui.gap(c, 16));

        if (!revealed) {
            page.addView(Ui.button(c, p, "Show answer", Ui.PRIMARY, v -> {
                revealed = true;
                a.speech.say(card.speakText());
                a.render();
            }));
            return;
        }

        String[] names = {"Again", "Hard", "Good", "Easy"};
        int m = Ui.dp(c, 4);
        LinearLayout r1 = Ui.row(c), r2 = Ui.row(c);
        for (int g = 1; g <= 4; g++) {
            final int grade = g;
            int d = store.previewDays(card, g);
            TextView b = Ui.button(c, p, names[g - 1] + "\n" + days(d), g == 3 ? Ui.PRIMARY : Ui.SECONDARY, v -> grade(card, grade));
            b.setLineSpacing(0, 1.1f);
            (g <= 2 ? r1 : r2).addView(b, Ui.weight(1, m));
        }
        page.addView(r1);
        page.addView(Ui.gap(c, 8));
        page.addView(r2);
        TextView hint = Ui.text(c, "Be honest: what you miss comes back sooner.", 13, p.muted);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(c, 12), 0, 0);
        page.addView(hint, Ui.lp(Ui.MATCH, Ui.WRAP));
    }

    private void grade(Card card, int g) {
        store.grade(card, g);
        if (g == 1) {
            missed++;
            if (requeued.add(card.id)) queue.add(card);
        } else {
            good++;
        }
        idx++;
        revealed = false;
        a.render();
    }
}
