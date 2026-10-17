package com.ichiji.kana;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Before day one: what you already read, your pace, and when to be reminded. */
final class OnboardingScreen extends Screen {
    private int step;
    private int script; // 0 hiragana, 1 katakana
    private final Set<String> known = new HashSet<>();
    private int pace = 1;
    private int hour = 8;

    OnboardingScreen(MainActivity a) {
        super(a);
        hour = a.store.reminderHour;
    }

    @Override boolean rebuildOnResume() { return false; }

    @Override
    boolean handleBack() {
        if (step == 0) return false;
        step--;
        a.render();
        return true;
    }

    @Override
    void build(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        LinearLayout dots = Ui.row(c);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 18));
        for (int i = 0; i < 4; i++) {
            View d = new View(c);
            d.setBackground(Ui.rounded(i <= step ? p.accent : p.line, Ui.dp(c, 4), 0, 0));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, i == step ? 22 : 8), Ui.dp(c, 8));
            lp.leftMargin = lp.rightMargin = Ui.dp(c, 3);
            dots.addView(d, lp);
        }
        page.addView(dots, Ui.lp(Ui.MATCH, Ui.WRAP));
        switch (step) {
            case 0: welcome(page); break;
            case 1: chart(page); break;
            case 2: pace(page); break;
            default: reminder(page);
        }
    }

    private void next() {
        step++;
        a.render();
    }

    private void welcome(LinearLayout page) {
        Context c = a;
        TextView mark = Ui.jp(c, "一字", 96, p.accent);
        mark.setGravity(Gravity.CENTER);
        mark.setPadding(0, Ui.dp(c, 30), 0, Ui.dp(c, 6));
        page.addView(mark, Ui.lp(Ui.MATCH, Ui.WRAP));
        TextView sub = Ui.label(c, "Ichiji · one character", p.muted);
        sub.setGravity(Gravity.CENTER);
        page.addView(sub, Ui.lp(Ui.MATCH, Ui.WRAP));
        page.addView(Ui.gap(c, 34));
        page.addView(Ui.jp(c, "Learn to read Japanese, one character a day.", 26, p.ink));
        page.addView(Ui.gap(c, 14));
        page.addView(Ui.text(c, "Hiragana first, then katakana, then the sound rules that turn them into words, then kanji built "
                + "from parts you already know.\n\nToday's character lives on your home screen and lock screen. Open the app "
                + "to trace it, review, and watch the days add up. Two minutes is enough.", 16, p.muted));
        page.addView(Ui.gap(c, 36));
        page.addView(Ui.button(c, p, "Begin", Ui.PRIMARY, v -> next()));
    }

    private void chart(LinearLayout page) {
        Context c = a;
        page.addView(Ui.jp(c, "What can you already read?", 26, p.ink));
        page.addView(Ui.gap(c, 8));
        page.addView(Ui.text(c, "Tap the ones you know. Tap a row letter for the whole row. They start as known and get a light check in two weeks.", 14.5f, p.muted));
        page.addView(Ui.gap(c, 16));
        page.addView(Ui.segmented(c, p, new String[]{"ひらがな Hiragana", "カタカナ Katakana"}, script, i -> {
            script = i;
            a.render();
        }));
        page.addView(Ui.gap(c, 14));

        List<Card> deck = store.cardsOfKind(script == 0 ? Card.HIRA : Card.KATA);
        Set<String> deckIds = new HashSet<>();
        for (Card card : deck) deckIds.add(card.id);
        TextView count = Ui.text(c, "", 14, p.muted);
        KanaGridView grid = new KanaGridView(c, p, store, KanaGridView.SELECT).gojuon(deck);
        grid.setSelected(known);
        Runnable sync = () -> {
            known.removeAll(deckIds);
            for (String id : grid.selected()) if (deckIds.contains(id)) known.add(id);
            count.setText(known.size() == 0 ? "Nothing selected: you'll start from あ." : known.size() + " selected in total");
        };
        grid.listener(card -> sync.run());
        sync.run();
        page.addView(grid, Ui.lp(Ui.MATCH, Ui.WRAP));
        page.addView(Ui.gap(c, 10));

        LinearLayout tools = Ui.row(c);
        tools.addView(count, Ui.weight(1));
        TextView all = Ui.medium(c, "All", 14, p.accent);
        all.setPadding(Ui.dp(c, 12), Ui.dp(c, 8), Ui.dp(c, 12), Ui.dp(c, 8));
        all.setOnClickListener(v -> grid.setAll(true));
        TextView none = Ui.medium(c, "None", 14, p.accent);
        none.setPadding(Ui.dp(c, 12), Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8));
        none.setOnClickListener(v -> grid.setAll(false));
        tools.addView(all);
        tools.addView(none);
        page.addView(tools);
        page.addView(Ui.gap(c, 20));
        page.addView(Ui.button(c, p, "Next", Ui.PRIMARY, v -> next()));
    }

    private void pace(LinearLayout page) {
        Context c = a;
        page.addView(Ui.jp(c, "How many a day?", 26, p.ink));
        page.addView(Ui.gap(c, 8));
        page.addView(Ui.text(c, "You can change this any time. Miss a day and nothing piles up: the next character just waits.", 14.5f, p.muted));
        page.addView(Ui.gap(c, 18));
        String[][] opts = {{"1", "Light", "One new character a day. The kana take about three and a half months."},
                {"3", "Steady", "Three a day. Kana in about five weeks."},
                {"5", "Eager", "Five a day. Kana in about three weeks, with more to review."}};
        for (String[] o : opts) {
            int v = Integer.parseInt(o[0]);
            boolean on = pace == v;
            LinearLayout box = Ui.row(c);
            box.setPadding(Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16));
            box.setBackground(Ui.rounded(on ? p.accentSoft : p.surface, Ui.dp(c, 18), on ? p.accent : p.line, Ui.dp(c, on ? 2 : 1)));
            TextView num = Ui.jp(c, o[0], 36, on ? p.accent : p.ink);
            num.setMinWidth(Ui.dp(c, 48));
            box.addView(num);
            LinearLayout t = Ui.col(c);
            t.addView(Ui.medium(c, o[1], 17, p.ink));
            t.addView(Ui.text(c, o[2], 13.5f, p.muted));
            box.addView(t, Ui.weight(1));
            box.setOnClickListener(x -> {
                pace = v;
                a.render();
            });
            page.addView(box, Ui.lp(Ui.MATCH, Ui.WRAP, Ui.dp(c, 10)));
        }
        page.addView(Ui.gap(c, 24));
        page.addView(Ui.button(c, p, "Next", Ui.PRIMARY, v -> next()));
    }

    private void reminder(LinearLayout page) {
        Context c = a;
        page.addView(Ui.jp(c, "When should reviews arrive?", 26, p.ink));
        page.addView(Ui.gap(c, 8));
        page.addView(Ui.text(c, "Once a day, one quiet reminder, only if you haven't studied yet. Today's character also sits on your lock screen as a silent card.", 14.5f, p.muted));
        page.addView(Ui.gap(c, 22));
        LinearLayout stepper = Ui.row(c);
        stepper.setBackground(Ui.rounded(p.surface, Ui.dp(c, 18), p.line, Ui.dp(c, 1)));
        TextView minus = Ui.medium(c, "−", 26, p.accent);
        TextView plus = Ui.medium(c, "+", 26, p.accent);
        TextView val = Ui.jp(c, SettingsScreen.hour(hour), 30, p.ink);
        val.setGravity(Gravity.CENTER);
        for (TextView b : new TextView[]{minus, plus}) {
            b.setGravity(Gravity.CENTER);
            b.setPadding(Ui.dp(c, 26), Ui.dp(c, 18), Ui.dp(c, 26), Ui.dp(c, 18));
        }
        minus.setOnClickListener(v -> { hour = (hour + 23) % 24; val.setText(SettingsScreen.hour(hour)); });
        plus.setOnClickListener(v -> { hour = (hour + 1) % 24; val.setText(SettingsScreen.hour(hour)); });
        stepper.addView(minus);
        stepper.addView(val, Ui.weight(1));
        stepper.addView(plus);
        page.addView(stepper);
        page.addView(Ui.gap(c, 28));
        page.addView(Ui.button(c, p, "Start learning", Ui.PRIMARY, v -> finish()));
        page.addView(Ui.gap(c, 8));
        page.addView(Ui.button(c, p, "No reminders", Ui.GHOST, v -> {
            store.reminders = false;
            finish();
        }));
    }

    private void finish() {
        store.pace = pace;
        store.reminderHour = hour;
        store.finishOnboarding(known);
        if (store.reminders || store.lockCard) a.askNotifyPermission();
        Alarms.scheduleAll(a);
        a.showTab(0);
    }
}
