package com.ichiji.kana;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Pace, reminders, appearance, widgets, voice, and the reset switch. */
final class SettingsScreen extends Screen {
    SettingsScreen(MainActivity a) { super(a); }

    @Override boolean isTab() { return true; }
    @Override int tabIndex() { return 3; }

    static String hour(int h) {
        return LocalTime.of(h, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    @Override
    void build(FrameLayout host) {
        Context c = a;
        LinearLayout page = Ui.page(c, host);
        page.addView(Ui.label(c, "Settings", p.muted));
        TextView title = Ui.jp(c, "設定", 34, p.ink);
        title.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 8));
        page.addView(title);

        // Pace
        section(page, "Pace", "New characters a day. Missed days never pile up.");
        int paceIdx = store.pace >= 5 ? 2 : store.pace >= 3 ? 1 : 0;
        page.addView(Ui.segmented(c, p, new String[]{"Light · 1", "Steady · 3", "Eager · 5"}, paceIdx, i -> {
            store.pace = i == 0 ? 1 : i == 1 ? 3 : 5;
            store.save();
        }));

        // Reminder
        section(page, "Daily reminder", "One nudge a day, only if today isn't done yet.");
        page.addView(Ui.segmented(c, p, new String[]{"On", "Off"}, store.reminders ? 0 : 1, i -> {
            store.reminders = i == 0;
            store.save();
            if (store.reminders) a.askNotifyPermission();
            Alarms.scheduleAll(a);
        }));
        page.addView(Ui.gap(c, 8));
        LinearLayout stepper = Ui.row(c);
        stepper.setBackground(Ui.rounded(p.surface, Ui.dp(c, 14), p.line, Ui.dp(c, 1)));
        TextView minus = Ui.medium(c, "−", 22, p.accent);
        TextView plus = Ui.medium(c, "+", 22, p.accent);
        TextView val = Ui.medium(c, hour(store.reminderHour), 17, p.ink);
        val.setGravity(Gravity.CENTER);
        for (TextView b : new TextView[]{minus, plus}) {
            b.setGravity(Gravity.CENTER);
            b.setPadding(Ui.dp(c, 22), Ui.dp(c, 10), Ui.dp(c, 22), Ui.dp(c, 10));
        }
        minus.setOnClickListener(v -> {
            store.reminderHour = (store.reminderHour + 23) % 24;
            val.setText(hour(store.reminderHour));
            store.save();
            Alarms.scheduleAll(a);
        });
        plus.setOnClickListener(v -> {
            store.reminderHour = (store.reminderHour + 1) % 24;
            val.setText(hour(store.reminderHour));
            store.save();
            Alarms.scheduleAll(a);
        });
        stepper.addView(minus);
        stepper.addView(val, Ui.weight(1));
        stepper.addView(plus);
        page.addView(stepper);

        // Lock screen
        section(page, "Lock screen card", "A quiet notification with today's character, so it's on your lock screen without opening anything.");
        page.addView(Ui.segmented(c, p, new String[]{"On", "Off"}, store.lockCard ? 0 : 1, i -> {
            store.lockCard = i == 0;
            store.save();
            if (store.lockCard) a.askNotifyPermission();
            Notifier.refreshLockCard(a);
        }));
        if (!a.canNotify()) {
            TextView warn = Ui.text(c, "Notifications are off for Ichiji, so reminders and the lock screen card can't show.", 13, p.bad);
            warn.setPadding(0, Ui.dp(c, 8), 0, 0);
            page.addView(warn);
            page.addView(Ui.button(c, p, "Allow notifications", Ui.GHOST, v -> a.askNotifyPermission()));
        }

        // Appearance
        section(page, "Appearance", null);
        page.addView(Ui.segmented(c, p, new String[]{"System", "Paper", "Night"}, store.theme, i -> {
            store.theme = i;
            store.save();
            a.recreate();
        }));

        // Widgets
        section(page, "Home screen widgets", "Long-press your home screen, tap Widgets, and find Ichiji. There's a small one for today's character and a wide kana board.");
        page.addView(Ui.label(c, "Colour", p.muted));
        page.addView(Ui.gap(c, 6));
        page.addView(Ui.segmented(c, p, new String[]{"藍 Indigo", "紙 Paper", "夜 Night"}, store.widgetTheme, i -> {
            store.widgetTheme = i;
            store.commit();
        }));
        page.addView(Ui.gap(c, 12));
        page.addView(Ui.label(c, "Board shows", p.muted));
        page.addView(Ui.gap(c, 6));
        page.addView(Ui.segmented(c, p, new String[]{"My track", "Hiragana", "Katakana"}, store.boardTrack, i -> {
            store.boardTrack = i;
            store.commit();
        }));

        // Voice
        section(page, "Voice", a.speech.hasJapanese()
                ? "Japanese voice is ready. Everything is read aloud on this phone."
                : "No Japanese voice found. Install one (for example Google's speech engine, Japanese) in your phone's text-to-speech settings.");
        LinearLayout vrow = Ui.row(c);
        int m = Ui.dp(c, 4);
        vrow.addView(Ui.button(c, p, "Test", Ui.SECONDARY, v -> a.speech.say("にほんご")), Ui.weight(1, m));
        vrow.addView(Ui.button(c, p, "Voice settings", Ui.SECONDARY, v -> Speech.openVoiceSettings(a)), Ui.weight(1, m));
        page.addView(vrow);

        // Data
        section(page, "Your data", "Everything stays in this app on this phone. No account, no servers, no tracking.");
        page.addView(Ui.button(c, p, "Start over", Ui.SECONDARY, v -> confirmReset()));

        // About
        section(page, "About", null);
        TextView about = Ui.text(c, "Ichiji 一字 · one character a day.\n\nStroke order data from KanjiVG, © Ulrich Apel, "
                + "licensed CC BY-SA 3.0 (kanjivg.tagaini.net). Converted for this app and shared under the same licence.", 13.5f, p.muted);
        page.addView(about);
    }

    private void section(LinearLayout page, String name, String hint) {
        Context c = a;
        page.addView(Ui.gap(c, 26));
        page.addView(Ui.medium(c, name, 17, p.ink));
        if (hint != null) {
            TextView h = Ui.text(c, hint, 13.5f, p.muted);
            h.setPadding(0, Ui.dp(c, 4), 0, 0);
            page.addView(h);
        }
        page.addView(Ui.gap(c, 10));
    }

    private void confirmReset() {
        int theme = p.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        new AlertDialog.Builder(a, theme)
                .setTitle("Start over?")
                .setMessage("This erases every character you've learned, your reviews and your streak. It can't be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Erase", (d, w) -> {
                    store.resetAll();
                    a.setRoot(new OnboardingScreen(a));
                })
                .show();
    }
}
