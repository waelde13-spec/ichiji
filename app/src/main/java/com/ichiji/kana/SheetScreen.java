package com.ichiji.kana;

import android.content.Context;
import android.graphics.Path;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** The practice sheet for one character: reading, example, story, stroke order and tracing. */
final class SheetScreen extends Screen {
    private final Card card;
    private final boolean learnMode;
    private boolean tracing;
    private String feedback = "";

    SheetScreen(MainActivity a, Card card, boolean learnMode) {
        super(a);
        this.card = card;
        this.learnMode = learnMode;
    }

    /** Open straight into tracing. */
    SheetScreen tracing() {
        tracing = true;
        return this;
    }

    @Override boolean rebuildOnResume() { return false; }

    @Override
    void build(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        page.addView(Ui.topBar(a, card.kindLabel(), null));

        // Character and reading
        LinearLayout head = Ui.row(c);
        TextView big = Ui.jp(c, card.ch, card.ch.length() > 1 ? 64 : 92, p.ink);
        big.setPadding(0, 0, Ui.dp(c, 18), 0);
        big.setOnClickListener(v -> a.speech.say(card.speakText()));
        head.addView(big);
        LinearLayout side = Ui.col(c);
        side.addView(Ui.label(c, card.isKanji() ? "Meaning" : "Reading", p.muted));
        side.addView(Ui.medium(c, card.isKanji() ? card.meaning : card.rd, 26, p.ink));
        if (card.isKanji()) side.addView(Ui.jp(c, card.kana + "  ·  " + card.rd, 17, p.muted));
        if (Card.KATA.equals(card.kind) && !card.pair.isEmpty()) {
            side.addView(Ui.jp(c, "Hiragana twin: " + card.pair, 14, p.muted));
        }
        TextView listen = Ui.medium(c, "▶  Listen", 14, p.accent);
        listen.setPadding(0, Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 4));
        listen.setOnClickListener(v -> a.speech.say(card.speakText()));
        side.addView(listen);
        head.addView(side, Ui.weight(1));
        page.addView(head);

        // Note / story
        if (!card.note.isEmpty()) {
            page.addView(Ui.gap(c, 14));
            TextView note = Ui.jp(c, card.note, 17, p.ink);
            note.setLineSpacing(0, 1.3f);
            page.addView(note);
        }

        // Parts (kanji)
        if (!card.parts.isEmpty()) {
            page.addView(Ui.gap(c, 14));
            page.addView(Ui.label(c, "Built from", p.muted));
            LinearLayout chips = Ui.row(c);
            chips.setPadding(0, Ui.dp(c, 8), 0, 0);
            for (String[] part : card.parts) {
                TextView chip = Ui.chip(c, p, part[0] + "  " + part[1], false);
                chip.setTextLocale(java.util.Locale.JAPAN);
                LinearLayout.LayoutParams lp = Ui.lp(Ui.WRAP, Ui.WRAP);
                lp.rightMargin = Ui.dp(c, 8);
                chips.addView(chip, lp);
            }
            page.addView(chips);
        }

        // Example word
        if (!card.word.isEmpty()) {
            page.addView(Ui.gap(c, 16));
            LinearLayout ex = Ui.card(c, p);
            ex.addView(Ui.label(c, "Example", p.muted));
            TextView w = Ui.jp(c, card.word, 28, p.ink);
            w.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 4));
            ex.addView(w);
            ex.addView(Ui.text(c, card.wordRomaji + "  ·  " + card.wordMeaning, 15, p.muted));
            ex.setOnClickListener(v -> a.speech.say(card.word));
            page.addView(ex);
        }

        // Stroke order and tracing
        List<Path> strokes = card.hasStrokes() ? Strokes.get(c, card.strokeChar) : null;
        if (strokes != null && !strokes.isEmpty()) {
            page.addView(Ui.gap(c, 20));
            String what = card.strokeChar.equals(card.ch) ? "" : "  ·  " + card.strokeChar;
            LinearLayout lrow = Ui.row(c);
            lrow.addView(Ui.label(c, (tracing ? "Trace it" : "Stroke order") + what, p.muted), Ui.weight(1));
            lrow.addView(Ui.label(c, strokes.size() + (strokes.size() == 1 ? " stroke" : " strokes"), p.muted));
            page.addView(lrow);
            page.addView(Ui.gap(c, 10));

            StrokeView sv = new StrokeView(c, p, strokes);
            page.addView(sv, Ui.lp(Ui.MATCH, Ui.WRAP));
            TextView fb = Ui.text(c, feedback.isEmpty()
                    ? (tracing ? "Start at the red dot. Follow the order shown." : "Watch it draw, then trace it yourself.") : feedback, 14.5f, p.muted);
            fb.setGravity(Gravity.CENTER);
            fb.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
            page.addView(fb, Ui.lp(Ui.MATCH, Ui.WRAP));

            LinearLayout buttons = Ui.row(c);
            int m = Ui.dp(c, 4);
            if (tracing) {
                sv.startTracing();
                sv.setListener(new StrokeView.TraceListener() {
                    @Override
                    public void onStroke(int index, int total, int verdict) {
                        String[] words = {"clean", "close, a little off", "drifted: the blue line shows the way",
                                "backwards: start from the red dot"};
                        fb.setText("Stroke " + (index + 1) + " of " + total + " · " + words[verdict]);
                        fb.setTextColor(verdict == StrokeView.CLEAN ? p.good : verdict == StrokeView.CLOSE ? p.warn : p.bad);
                    }

                    @Override
                    public void onDone(int clean, int total) {
                        store.bump();
                        store.save();
                        fb.setText(clean == total ? "All " + total + " strokes clean. 見事。"
                                : clean + " of " + total + " clean. Clear it and go again?");
                        fb.setTextColor(clean == total ? p.good : p.muted);
                    }
                });
                buttons.addView(Ui.button(c, p, "Clear", Ui.SECONDARY, v -> {
                    sv.clearTrace();
                    fb.setText("Start at the red dot.");
                    fb.setTextColor(p.muted);
                }), Ui.weight(1, m));
                buttons.addView(Ui.button(c, p, "Show me", Ui.SECONDARY, v -> {
                    tracing = false;
                    feedback = "";
                    a.render();
                }), Ui.weight(1, m));
            } else {
                sv.replay();
                buttons.addView(Ui.button(c, p, "Replay", Ui.SECONDARY, v -> sv.replay()), Ui.weight(1, m));
                buttons.addView(Ui.button(c, p, "Trace it", Ui.PRIMARY, v -> {
                    tracing = true;
                    feedback = "";
                    a.render();
                }), Ui.weight(1, m));
            }
            page.addView(buttons);

            if (!tracing && strokes.size() > 1) {
                page.addView(Ui.gap(c, 18));
                page.addView(new StrokeFramesView(c, p, strokes), Ui.lp(Ui.MATCH, Ui.WRAP));
            }
        }

        // Learn
        if (learnMode && !store.isLearned(card)) {
            page.addView(Ui.gap(c, 24));
            page.addView(Ui.button(c, p, "I've got it", Ui.PRIMARY, v -> {
                store.learn(card);
                Ui.toast(c, card.ch + " added. It comes back tomorrow.");
                a.back();
            }));
            TextView hint = Ui.text(c, "It joins your reviews and comes back, spaced out, until it sticks.", 13, p.muted);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, Ui.dp(c, 8), 0, 0);
            page.addView(hint, Ui.lp(Ui.MATCH, Ui.WRAP));
        }
    }
}
