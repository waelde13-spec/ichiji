package com.ichiji.kana;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** The home tab: today's new character(s), the review batch, and how far the course has come. */
final class TodayScreen extends Screen {
    TodayScreen(MainActivity a) { super(a); }

    @Override boolean isTab() { return true; }
    @Override int tabIndex() { return 0; }

    @Override
    void build(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);

        // Header
        LinearLayout head = Ui.row(c);
        LinearLayout titles = Ui.col(c);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM", Locale.getDefault()));
        titles.addView(Ui.label(c, date, p.muted));
        TextView title = Ui.jp(c, "今日", 34, p.ink);
        title.setPadding(0, Ui.dp(c, 6), 0, 0);
        titles.addView(title);
        head.addView(titles, Ui.weight(1));
        int streak = store.streak();
        TextView pill = Ui.medium(c, streak + (streak == 1 ? " day" : " days"), 13.5f, p.seal);
        pill.setPadding(Ui.dp(c, 12), Ui.dp(c, 6), Ui.dp(c, 12), Ui.dp(c, 6));
        pill.setBackground(Ui.rounded(Palette.alpha(p.seal, 0.12f), Ui.dp(c, 100), 0, 0));
        head.addView(pill);
        page.addView(head);
        page.addView(Ui.gap(c, 18));

        // Today's new characters
        List<Card> batch = store.todayBatch();
        boolean allLearned = true;
        for (Card card : batch) {
            page.addView(characterCard(card), Ui.lp(Ui.MATCH, Ui.WRAP, Ui.dp(c, 0)));
            page.addView(Ui.gap(c, 12));
            if (!store.isLearned(card)) allLearned = false;
        }
        if (batch.isEmpty()) {
            LinearLayout empty = Ui.card(c, p);
            if (store.courseFinished()) {
                empty.addView(Ui.jp(c, "完", 64, p.ink));
                empty.addView(Ui.text(c, "You've learned every character in this version of the course. Reviews keep it all fresh.", 15, p.muted));
            } else if (store.waitingForKanji()) {
                empty.addView(Ui.label(c, "Next: kanji", p.muted));
                empty.addView(Ui.gap(c, 6));
                empty.addView(Ui.jp(c, "日", 64, p.ink));
                empty.addView(Ui.gap(c, 6));
                empty.addView(Ui.text(c, "Kanji opens once " + Store.KANJI_UNLOCK + " of your " + Store.KANA_TOTAL
                        + " kana have passed a review. You're at " + store.kanaPassed() + ". Today is for reviews.", 15, p.muted));
            } else {
                empty.addView(Ui.text(c, "Nothing new today.", 15, p.muted));
            }
            page.addView(empty);
            page.addView(Ui.gap(c, 12));
        } else if (allLearned && !store.waitingForKanji() && !store.courseFinished()) {
            page.addView(Ui.button(c, p, "Learn one more today", Ui.GHOST, v -> {
                Card extra = store.addExtra();
                if (extra != null) a.render();
            }));
            page.addView(Ui.gap(c, 6));
        }

        // Reviews
        List<Card> due = store.dueReviews();
        LinearLayout rev = Ui.card(c, p);
        LinearLayout revRow = Ui.row(c);
        LinearLayout revText = Ui.col(c);
        revText.addView(Ui.label(c, "Reviews", p.muted));
        if (due.isEmpty()) {
            int tomorrow = store.dueTomorrow();
            revText.addView(Ui.medium(c, "All clear", 20, p.ink));
            revText.addView(Ui.text(c, tomorrow > 0 ? tomorrow + " come back tomorrow." : "Nothing scheduled for tomorrow yet.", 14, p.muted));
        } else {
            revText.addView(Ui.medium(c, due.size() + " waiting", 20, p.ink));
            revText.addView(Ui.text(c, "Recall each one with the answer hidden.", 14, p.muted));
        }
        revRow.addView(revText, Ui.weight(1));
        TextView mark = Ui.jp(c, "復", 40, due.isEmpty() ? Palette.alpha(p.muted, 0.5f) : p.accent);
        revRow.addView(mark);
        rev.addView(revRow);
        if (!due.isEmpty()) {
            rev.addView(Ui.gap(c, 14));
            rev.addView(Ui.button(c, p, "Start reviews", Ui.PRIMARY, v -> a.push(new ReviewScreen(a))));
        }
        page.addView(rev);
        page.addView(Ui.gap(c, 12));

        // Course progress
        LinearLayout course = Ui.card(c, p);
        course.addView(Ui.label(c, "The course", p.muted));
        course.addView(Ui.gap(c, 10));
        int kana = store.learnedCount(Card.HIRA) + store.learnedCount(Card.KATA) + store.learnedCount(Card.RULE);
        course.addView(progressRow(c, "Kana", kana, Store.KANA_TOTAL));
        course.addView(Ui.gap(c, 10));
        int kanjiTotal = store.cardsOfKind(Card.KANJI).size();
        if (store.kanjiUnlocked() || store.learnedCount(Card.KANJI) > 0) {
            course.addView(progressRow(c, "Kanji", store.learnedCount(Card.KANJI), kanjiTotal));
        } else {
            course.addView(progressRow(c, "Kana passed", store.kanaPassed(), Store.KANJI_UNLOCK));
            TextView hint = Ui.text(c, "Kanji opens at " + Store.KANJI_UNLOCK + " passed.", 13, p.muted);
            hint.setPadding(0, Ui.dp(c, 6), 0, 0);
            course.addView(hint);
        }
        page.addView(course);
    }

    private View characterCard(Card card) {
        Context c = a;
        boolean learned = store.isLearned(card);
        LinearLayout box = Ui.card(c, p);
        LinearLayout top = Ui.row(c);
        top.addView(Ui.label(c, card.kindLabel(), p.muted), Ui.weight(1));
        top.addView(Ui.chip(c, p, learned ? "Learned" : "New", !learned));
        box.addView(top);

        TextView big = Ui.jp(c, card.ch, card.ch.length() > 1 ? 96 : 128, p.ink);
        big.setGravity(Gravity.CENTER);
        big.setPadding(0, Ui.dp(c, 18), 0, Ui.dp(c, 10));
        big.setOnClickListener(v -> a.speech.say(card.speakText()));
        box.addView(big, Ui.lp(Ui.MATCH, Ui.WRAP));

        TextView reading = Ui.medium(c, card.isKanji() ? card.meaning : card.rd, 24, p.ink);
        reading.setGravity(Gravity.CENTER);
        box.addView(reading, Ui.lp(Ui.MATCH, Ui.WRAP));
        String sub = card.isKanji() ? card.kana + " · " + card.rd : card.word + " · " + card.wordRomaji + " · " + card.wordMeaning;
        TextView subline = Ui.jp(c, sub, 15, p.muted);
        subline.setGravity(Gravity.CENTER);
        subline.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 16));
        box.addView(subline, Ui.lp(Ui.MATCH, Ui.WRAP));

        box.addView(Ui.button(c, p, learned ? "Practice it again" : "Learn it", learned ? Ui.SECONDARY : Ui.PRIMARY,
                v -> a.push(new SheetScreen(a, card, !learned))));
        return box;
    }

    private View progressRow(Context c, String label, int n, int total) {
        LinearLayout col = Ui.col(c);
        LinearLayout row = Ui.row(c);
        row.addView(Ui.text(c, label, 14, p.ink), Ui.weight(1));
        row.addView(Ui.medium(c, n + " / " + total, 14, p.muted));
        col.addView(row);
        FrameLayout track = new FrameLayout(c);
        track.setBackground(Ui.rounded(Palette.alpha(p.ink, 0.07f), Ui.dp(c, 4), 0, 0));
        View fill = new View(c);
        fill.setBackground(Ui.rounded(p.accent, Ui.dp(c, 4), 0, 0));
        float frac = total == 0 ? 0 : Math.min(1f, n / (float) total);
        track.addView(fill, new FrameLayout.LayoutParams(0, Ui.MATCH));
        track.post(() -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = Math.round(track.getWidth() * frac);
            fill.setLayoutParams(lp);
        });
        col.addView(track, Ui.lp(Ui.MATCH, Ui.dp(c, 6), Ui.dp(c, 8)));
        return col;
    }
}
