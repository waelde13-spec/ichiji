package com.ichiji.kana;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/** Streak, stamps, days studied, and every character shaded by how well it's held. */
final class ProgressScreen extends Screen {
    ProgressScreen(MainActivity a) { super(a); }

    @Override boolean isTab() { return true; }
    @Override int tabIndex() { return 2; }

    @Override
    void build(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        page.addView(Ui.label(c, "Progress", p.muted));
        TextView title = Ui.jp(c, "記録", 34, p.ink);
        title.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 16));
        page.addView(title);

        // Streak + stamps
        LinearLayout streakCard = Ui.card(c, p);
        LinearLayout top = Ui.row(c);
        int streak = store.streak();
        TextView n = Ui.jp(c, String.valueOf(streak), 56, p.ink);
        n.setPadding(0, 0, Ui.dp(c, 14), 0);
        top.addView(n);
        LinearLayout t = Ui.col(c);
        t.addView(Ui.medium(c, streak == 1 ? "day in a row" : "days in a row", 17, p.ink));
        t.addView(Ui.text(c, "Best " + store.bestStreak() + " · " + store.daysStudied() + " days studied", 13.5f, p.muted));
        top.addView(t, Ui.weight(1));
        streakCard.addView(top);
        streakCard.addView(Ui.gap(c, 16));
        LinearLayout stamps = Ui.row(c);
        int best = Math.max(store.bestStreak(), streak);
        String[][] marks = {{"七日", "7 days", "7"}, {"三十日", "30 days", "30"}, {"百日", "100 days", "100"}};
        for (String[] m : marks) {
            boolean earned = best >= Integer.parseInt(m[2]);
            LinearLayout s = Ui.col(c);
            s.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView seal = Ui.jp(c, m[0], m[0].length() > 2 ? 15 : 18, earned ? 0xFFFFFFFF : Palette.alpha(p.muted, 0.6f));
            seal.setGravity(Gravity.CENTER);
            int size = Ui.dp(c, 64);
            seal.setBackground(earned ? Ui.rounded(p.seal, Ui.dp(c, 14), 0, 0)
                    : Ui.rounded(0, Ui.dp(c, 14), Palette.alpha(p.muted, 0.4f), Ui.dp(c, 1.5f)));
            if (earned) seal.setRotation(-4f);
            s.addView(seal, new LinearLayout.LayoutParams(size, size));
            TextView cap = Ui.text(c, m[1], 12.5f, earned ? p.seal : p.muted);
            cap.setGravity(Gravity.CENTER);
            cap.setPadding(0, Ui.dp(c, 6), 0, 0);
            s.addView(cap);
            stamps.addView(s, Ui.weight(1));
        }
        streakCard.addView(stamps);
        page.addView(streakCard);
        page.addView(Ui.gap(c, 12));

        // Heatmap
        LinearLayout heat = Ui.card(c, p);
        heat.addView(Ui.label(c, "Days studied", p.muted));
        heat.addView(Ui.gap(c, 12));
        heat.addView(new HeatmapView(c, p, store), Ui.lp(Ui.MATCH, Ui.WRAP));
        page.addView(heat);
        page.addView(Ui.gap(c, 12));

        // Stages
        LinearLayout stages = Ui.card(c, p);
        stages.addView(Ui.label(c, "New moon to full moon", p.muted));
        stages.addView(Ui.gap(c, 10));
        LinearLayout srow = Ui.row(c);
        int[] counts = store.stageCounts();
        for (int i = 0; i < 4; i++) {
            LinearLayout col = Ui.col(c);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView k = Ui.jp(c, CardState.STAGE_KANJI[i], 22, i == 3 ? p.accent : p.ink);
            col.addView(k);
            col.addView(Ui.medium(c, String.valueOf(counts[i]), 20, p.ink));
            col.addView(Ui.text(c, CardState.STAGE_NAMES[i], 12.5f, p.muted));
            srow.addView(col, Ui.weight(1));
        }
        stages.addView(srow);
        TextView expl = Ui.text(c, "Every review that sticks pushes a character further out. Full moon (望) means it holds for two months or more.", 13, p.muted);
        expl.setPadding(0, Ui.dp(c, 12), 0, 0);
        stages.addView(expl);
        page.addView(stages);

        // Charts
        Set<String> today = new HashSet<>();
        for (Card card : store.todayBatch()) today.add(card.id);
        KanaGridView.Listener open = card -> {
            if (card == null) return;
            if (store.isLearned(card) || today.contains(card.id)) a.push(new SheetScreen(a, card, !store.isLearned(card)));
            else Ui.toast(c, card.ch + " hasn't come up yet.");
        };
        page.addView(section(c, "Hiragana", store.learnedCount(Card.HIRA), 46,
                new KanaGridView(c, p, store, KanaGridView.PROGRESS).gojuon(store.cardsOfKind(Card.HIRA)).highlight(today).listener(open)));
        page.addView(section(c, "Katakana", store.learnedCount(Card.KATA), 46,
                new KanaGridView(c, p, store, KanaGridView.PROGRESS).gojuon(store.cardsOfKind(Card.KATA)).highlight(today).listener(open)));
        page.addView(section(c, "Sound rules", store.learnedCount(Card.RULE), 14,
                new KanaGridView(c, p, store, KanaGridView.PROGRESS).flow(store.cardsOfKind(Card.RULE), 5).highlight(today).listener(open)));
        page.addView(section(c, "Kanji · levels 1 to 3", store.learnedCount(Card.KANJI), store.cardsOfKind(Card.KANJI).size(),
                new KanaGridView(c, p, store, KanaGridView.PROGRESS).flow(store.cardsOfKind(Card.KANJI), 6).highlight(today).listener(open)));
    }

    private View section(Context c, String name, int n, int total, View grid) {
        LinearLayout col = Ui.col(c);
        col.setPadding(0, Ui.dp(c, 24), 0, 0);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.medium(c, name, 18, p.ink), Ui.weight(1));
        head.addView(Ui.text(c, n + " / " + total, 14, p.muted));
        col.addView(head);
        col.addView(Ui.gap(c, 10));
        col.addView(grid, Ui.lp(Ui.MATCH, Ui.WRAP));
        return col;
    }
}
