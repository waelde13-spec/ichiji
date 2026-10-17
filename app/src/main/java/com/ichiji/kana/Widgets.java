package com.ichiji.kana;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;

/** Refreshes the home-screen widgets. */
final class Widgets {
    private Widgets() {}

    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        if (mgr == null) return;
        for (int id : mgr.getAppWidgetIds(new ComponentName(c, SmallWidget.class))) update(c, mgr, id, false);
        for (int id : mgr.getAppWidgetIds(new ComponentName(c, BoardWidget.class))) update(c, mgr, id, true);
    }

    static void update(Context c, AppWidgetManager mgr, int id, boolean board) {
        Store store = Store.get(c);
        Bundle o = mgr.getAppWidgetOptions(id);
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        boolean portrait = dm.heightPixels >= dm.widthPixels;
        int wDp = o.getInt(portrait ? AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH : AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, board ? 320 : 150);
        int hDp = o.getInt(portrait ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, board ? 160 : 150);
        if (wDp <= 0) wDp = board ? 320 : 150;
        if (hDp <= 0) hDp = board ? 160 : 150;
        float scale = Math.min(dm.density, 900f / Math.max(wDp, hDp)); // keep bitmaps well inside the RemoteViews limit
        int w = Math.round(wDp * scale), h = Math.round(hDp * scale);
        Bitmap bmp = board ? WidgetArt.board(c, store, w, h) : WidgetArt.small(c, store, w, h);

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_image);
        rv.setImageViewBitmap(R.id.widget_image, bmp);
        Card f = store.featured();
        rv.setContentDescription(R.id.widget_image, f == null ? "Ichiji" : "Today: " + f.ch + ", " + f.answer());
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, board ? 2 : 1, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_image, pi);
        mgr.updateAppWidget(id, rv);
    }

    abstract static class Base extends AppWidgetProvider {
        abstract boolean board();

        @Override
        public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
            for (int id : ids) update(c, mgr, id, board());
            Alarms.scheduleAll(c);
        }

        @Override
        public void onAppWidgetOptionsChanged(Context c, AppWidgetManager mgr, int id, Bundle newOptions) {
            update(c, mgr, id, board());
        }
    }
}
