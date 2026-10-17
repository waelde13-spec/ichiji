package com.ichiji.kana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Two daily wake-ups: just after midnight (new character on the widgets) and the reminder hour. */
final class Alarms {
    static final String MIDNIGHT = "com.ichiji.kana.MIDNIGHT";
    static final String REMINDER = "com.ichiji.kana.REMINDER";

    private Alarms() {}

    private static PendingIntent pi(Context c, String action) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, action.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Store store = Store.get(c);
        ZoneId zone = ZoneId.systemDefault();

        long midnight = LocalDate.now().plusDays(1).atStartOfDay(zone).plusSeconds(20).toInstant().toEpochMilli();
        am.set(AlarmManager.RTC, midnight, pi(c, MIDNIGHT));

        PendingIntent rem = pi(c, REMINDER);
        if (store.reminders && store.onboarded) {
            ZonedDateTime next = LocalDateTime.now().withHour(store.reminderHour).withMinute(0).withSecond(0).withNano(0).atZone(zone);
            if (!next.isAfter(ZonedDateTime.now(zone))) next = next.plusDays(1);
            am.setWindow(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), 10 * 60 * 1000L, rem);
        } else {
            am.cancel(rem);
        }
    }

    /** Wakes for the two alarms above. */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (REMINDER.equals(action)) Notifier.remind(c);
            Widgets.updateAll(c);
            Notifier.refreshLockCard(c);
            scheduleAll(c);
        }
    }
}
