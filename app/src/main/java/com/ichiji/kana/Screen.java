package com.ichiji.kana;

import android.widget.FrameLayout;

/** One page of the app. Screens rebuild their views from their own state whenever shown. */
abstract class Screen {
    final MainActivity a;
    final Palette p;
    final Store store;

    Screen(MainActivity a) {
        this.a = a;
        this.p = a.pal;
        this.store = a.store;
    }

    /** Fill the host with this screen's content. */
    abstract void build(FrameLayout host);

    /** Main tabs show the bottom navigation; pushed screens don't. */
    boolean isTab() { return false; }

    /** Index of the bottom-nav tab this screen belongs to. */
    int tabIndex() { return -1; }

    /** Whether returning to the app should rebuild this screen (dates may have changed). */
    boolean rebuildOnResume() { return true; }

    /** Return true if the screen handled Back itself. */
    boolean handleBack() { return false; }

    void onLeave() {}
}
