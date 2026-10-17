package com.ichiji.kana;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;

/** The quiet lock-screen card and the once-a-day reminder. */
final class Notifier {
    private static final String CH_LOCK = "lock_card", CH_REMIND = "reminder";
    private static final int ID_LOCK = 1, ID_REMIND = 2;

    private Notifier() {}

    private static NotificationManager nm(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return null;
        NotificationChannel lock = new NotificationChannel(CH_LOCK, "Today's character", NotificationManager.IMPORTANCE_LOW);
        lock.setDescription("A silent card with today's character, for your lock screen.");
        lock.setShowBadge(false);
        lock.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationChannel rem = new NotificationChannel(CH_REMIND, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT);
        rem.setDescription("One reminder a day, only if you haven't studied yet.");
        nm.createNotificationChannel(lock);
        nm.createNotificationChannel(rem);
        return nm;
    }

    private static PendingIntent openApp(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 10, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void refreshLockCard(Context c) {
        NotificationManager nm = nm(c);
        if (nm == null) return;
        Store store = Store.get(c);
        Card card = store.onboarded ? store.featured() : null;
        if (!store.onboarded || !store.lockCard || card == null || !nm.areNotificationsEnabled()) {
            nm.cancel(ID_LOCK);
            return;
        }
        int due = store.dueReviews().size();
        boolean done = store.doneToday();
        String reading = card.isKanji() ? card.kana + " · " + card.meaning : card.rd;
        String status = done ? "Done for today" : due > 0 ? due + (due == 1 ? " review waiting" : " reviews waiting") : "Today's character";
        int streak = store.streak();
        if (streak > 1) status += " · " + streak + " days";

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.notif_card);
        rv.setTextViewText(R.id.notif_char, card.ch);
        rv.setTextViewText(R.id.notif_title, reading);
        rv.setTextViewText(R.id.notif_text, status);

        Notification n = new Notification.Builder(c, CH_LOCK)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(card.ch + "  " + reading)
                .setContentText(status)
                .setCustomContentView(rv)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_STATUS)
                .setContentIntent(openApp(c))
                .build();
        nm.notify(ID_LOCK, n);
    }

    static void remind(Context c) {
        Store store = Store.get(c);
        if (!store.onboarded || !store.reminders || store.doneToday()) return;
        NotificationManager nm = nm(c);
        if (nm == null || !nm.areNotificationsEnabled()) return;
        List<Card> batch = store.todayBatch();
        int due = store.dueReviews().size();
        StringBuilder title = new StringBuilder();
        for (Card card : batch) if (!store.isLearned(card)) title.append(card.ch).append(' ');
        String t = title.length() > 0 ? "今日: " + title.toString().trim() : "Reviews are ready";
        String text = due > 0 ? due + (due == 1 ? " review" : " reviews") + " waiting. Two minutes is enough." : "Today's character is waiting.";
        Notification n = new Notification.Builder(c, CH_REMIND)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(t)
                .setContentText(text)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(openApp(c))
                .build();
        nm.notify(ID_REMIND, n);
    }
}
