package com.ichiji.kana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms don't survive a reboot or a clock change; this puts them back. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Alarms.scheduleAll(c);
        Widgets.updateAll(c);
        Notifier.refreshLockCard(c);
    }
}
