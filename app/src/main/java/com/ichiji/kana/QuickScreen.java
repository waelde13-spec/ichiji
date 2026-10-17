package com.ichiji.kana;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Quick Practice: ten fast questions from any deck, for a spare minute. Doesn't touch the schedule. */
final class QuickScreen extends Screen {
    private static final String[][] DECKS = {
            {"あ", "Hiragana", Card.HIRA}, {"ア", "Katakana", Card.KATA},
            {"っ", "Sound rules", Card.RULE}, {"日", "Kanji", Card.KANJI}, {"全", "Everything you've learned", null}};
    private static final int LENGTH = 10;

    private final Random rnd = new Random();
    private int deck = -1;
    private List<Card> questions;
    private List<String> choices;
    private int qi, picked = -1, score;
    private boolean usedFullDeck;

    QuickScreen(MainActivity a) { super(a); }

    @Override boolean isTab() { return deck < 0; }
    @Override int tabIndex() { return 1; }
    @Override boolean rebuildOnResume() { return deck < 0; }

    @Override
    boolean handleBack() {
        if (deck < 0) return false;
        deck = -1;
        a.render();
        return true;
    }

    @Override
    void build(FrameLayout host) {
        if (deck < 0) buildMenu(host);
        else buildQuestion(host);
    }

    private List<Card> deckCards(int d, boolean learnedOnly) {
        List<Card> out = new ArrayList<>();
        String kind = DECKS[d][2];
        for (Card c : store.cards) {
            if (kind != null && !kind.equals(c.kind)) continue;
            if (learnedOnly && !store.isLearned(c)) continue;
            out.add(c);
        }
        return out;
    }

    private void buildMenu(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        page.addView(Ui.label(c, "Quick practice", p.muted));
        TextView title = Ui.jp(c, "練習", 34, p.ink);
        title.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 4));
        page.addView(title);
        page.addView(Ui.text(c, "Ten quick questions. It won't change your review schedule.", 15, p.muted));
        page.addView(Ui.gap(c, 16));

        for (int i = 0; i < DECKS.length; i++) {
            final int d = i;
            int learned = deckCards(d, true).size();
            int total = deckCards(d, false).size();
            LinearLayout row = Ui.row(c);
            row.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
            row.setBackground(Ui.pressable(Ui.rounded(p.surface, Ui.dp(c, 18), p.line, Ui.dp(c, 1)),
                    Palette.alpha(p.ink, 0.08f), Ui.dp(c, 18)));
            TextView glyph = Ui.jp(c, DECKS[i][0], 34, p.accent);
            glyph.setMinWidth(Ui.dp(c, 56));
            row.addView(glyph);
            LinearLayout t = Ui.col(c);
            t.addView(Ui.medium(c, DECKS[i][1], 17, p.ink));
            t.addView(Ui.text(c, DECKS[i][2] == null ? learned + " characters" : learned + " of " + total + " learned", 13.5f, p.muted));
            row.addView(t, Ui.weight(1));
            row.addView(Ui.medium(c, "›", 22, p.muted));
            row.setOnClickListener(v -> start(d));
            page.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP, i == 0 ? 0 : Ui.dp(c, 10)));
        }

        page.addView(Ui.gap(c, 22));
        page.addView(Ui.label(c, "Write it", p.muted));
        page.addView(Ui.gap(c, 8));
        page.addView(Ui.button(c, p, "Trace a random character", Ui.SECONDARY, v -> {
            List<Card> pool = new ArrayList<>();
            for (Card card : store.cards) if (store.isLearned(card) && card.hasStrokes()) pool.add(card);
            if (pool.isEmpty()) for (Card card : store.todayBatch()) if (card.hasStrokes()) pool.add(card);
            if (pool.isEmpty()) {
                Ui.toast(c, "Learn your first character on the Today tab.");
                return;
            }
            a.push(new SheetScreen(a, pool.get(rnd.nextInt(pool.size())), false).tracing());
        }));
    }

    private void start(int d) {
        List<Card> pool = deckCards(d, true);
        usedFullDeck = pool.size() < 4;
        if (usedFullDeck) {
            if (d == DECKS.length - 1) {
                Ui.toast(a, "Learn a few characters first.");
                return;
            }
            pool = deckCards(d, false);
        }
        List<Card> qs = new ArrayList<>();
        while (qs.size() < LENGTH) {
            List<Card> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled, rnd);
            for (Card card : shuffled) {
                if (qs.size() >= LENGTH) break;
                if (!qs.isEmpty() && qs.get(qs.size() - 1) == card && pool.size() > 1) continue;
                qs.add(card);
            }
        }
        deck = d;
        questions = qs;
        qi = 0;
        score = 0;
        nextChoices();
        a.render();
    }

    private void nextChoices() {
        picked = -1;
        if (qi >= questions.size()) return;
        Card q = questions.get(qi);
        List<Card> sameKind = new ArrayList<>();
        for (Card c : store.cards) if (c.kind.equals(q.kind)) sameKind.add(c);
        Collections.shuffle(sameKind, rnd);
        Set<String> opts = new LinkedHashSet<>();
        opts.add(q.answer());
        for (Card c : sameKind) {
            if (opts.size() >= 4) break;
            opts.add(c.answer());
        }
        choices = new ArrayList<>(opts);
        Collections.shuffle(choices, rnd);
    }

    private void buildQuestion(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        boolean done = qi >= questions.size();
        LinearLayout bar = Ui.topBar(a, DECKS[deck][1], done ? "" : (qi + 1) + " / " + questions.size());
        page.addView(bar);

        if (done) {
            LinearLayout box = Ui.card(c, p);
            TextView big = Ui.medium(c, score + " / " + questions.size(), 44, p.ink);
            big.setGravity(Gravity.CENTER);
            box.addView(big, Ui.lp(Ui.MATCH, Ui.WRAP));
            String msg = score == questions.size() ? "Every one. 完璧。" : score >= questions.size() * 0.7 ? "Solid." : "Worth another round.";
            TextView m = Ui.jp(c, msg, 16, p.muted);
            m.setGravity(Gravity.CENTER);
            m.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 18));
            box.addView(m, Ui.lp(Ui.MATCH, Ui.WRAP));
            box.addView(Ui.button(c, p, "Another round", Ui.PRIMARY, v -> start(deck)));
            box.addView(Ui.gap(c, 8));
            box.addView(Ui.button(c, p, "Choose a deck", Ui.SECONDARY, v -> { deck = -1; a.render(); }));
            page.addView(box);
            return;
        }

        if (usedFullDeck && qi == 0 && picked < 0) {
            TextView note = Ui.text(c, "You haven't learned many from this deck yet, so this round uses all of it.", 13.5f, p.muted);
            note.setPadding(0, 0, 0, Ui.dp(c, 10));
            page.addView(note);
        }

        Card q = questions.get(qi);
        LinearLayout box = Ui.card(c, p);
        TextView big = Ui.jp(c, q.ch, q.ch.length() > 1 ? 96 : 128, p.ink);
        big.setGravity(Gravity.CENTER);
        big.setPadding(0, Ui.dp(c, 16), 0, Ui.dp(c, 16));
        big.setOnClickListener(v -> a.speech.say(q.speakText()));
        box.addView(big, Ui.lp(Ui.MATCH, Ui.WRAP));
        page.addView(box);
        page.addView(Ui.gap(c, 14));

        String right = q.answer();
        for (int i = 0; i < choices.size(); i++) {
            final int idx = i;
            String opt = choices.get(i);
            TextView b = Ui.button(c, p, opt, Ui.SECONDARY, picked >= 0 ? null : (View.OnClickListener) v -> {
                picked = idx;
                if (choices.get(idx).equals(right)) score++;
                a.speech.say(q.speakText());
                a.render();
            });
            if (picked >= 0) {
                boolean isRight = opt.equals(right);
                if (isRight) {
                    b.setBackground(Ui.rounded(Palette.alpha(p.good, 0.18f), Ui.dp(c, 14), p.good, Ui.dp(c, 2)));
                } else if (idx == picked) {
                    b.setBackground(Ui.rounded(Palette.alpha(p.bad, 0.15f), Ui.dp(c, 14), p.bad, Ui.dp(c, 2)));
                }
            }
            page.addView(b, Ui.lp(Ui.MATCH, Ui.WRAP, i == 0 ? 0 : Ui.dp(c, 8)));
        }

        if (picked >= 0) {
            page.addView(Ui.gap(c, 14));
            TextView info = Ui.jp(c, q.word + " · " + q.wordRomaji + " · " + q.wordMeaning, 14.5f, p.muted);
            info.setGravity(Gravity.CENTER);
            page.addView(info, Ui.lp(Ui.MATCH, Ui.WRAP));
            page.addView(Ui.gap(c, 12));
            page.addView(Ui.button(c, p, qi == questions.size() - 1 ? "See score" : "Next", Ui.PRIMARY, v -> {
                qi++;
                if (qi >= questions.size()) {
                    store.bump();
                    store.commit();
                }
                nextChoices();
                a.render();
            }));
        }
    }
}
