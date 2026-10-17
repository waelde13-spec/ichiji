package com.ichiji.kana;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.ArrayList;
import java.util.List;

/** The only activity. Holds a small stack of screens and the bottom navigation. */
public class MainActivity extends Activity {
    Store store;
    Palette pal;
    Speech speech;

    private LinearLayout root;
    private FrameLayout content;
    private LinearLayout nav;
    private final List<Screen> stack = new ArrayList<>();
    private final TextView[][] navItems = new TextView[4][];
    private OnBackInvokedCallback backCallback;

    private static final String[][] TABS = {{"今日", "Today"}, {"練習", "Practice"}, {"記録", "Progress"}, {"設定", "Settings"}};

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = Store.get(this);
        pal = Palette.forApp(this, store.theme);
        speech = new Speech(this);
        setupWindow();

        root = Ui.col(this);
        root.setBackgroundColor(pal.bg);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));
        nav = buildNav();
        root.addView(nav, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        setContentView(root);
        applyInsets();

        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::back;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }

        Alarms.scheduleAll(this);
        if (!store.onboarded) {
            setRoot(new OnboardingScreen(this));
        } else {
            showTab(saved != null ? saved.getInt("tab", 0) : 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        Screen top = top();
        out.putInt("tab", top != null && top.tabIndex() >= 0 ? top.tabIndex() : 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Screen top = top();
        if (top != null && top.rebuildOnResume()) render();
    }

    @Override
    protected void onDestroy() {
        speech.shutdown();
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        back();
    }

    // ---------------------------------------------------------------- navigation

    Screen top() { return stack.isEmpty() ? null : stack.get(stack.size() - 1); }

    void setRoot(Screen s) {
        for (Screen old : stack) old.onLeave();
        stack.clear();
        stack.add(s);
        render();
    }

    void showTab(int i) {
        Screen s;
        switch (i) {
            case 1: s = new QuickScreen(this); break;
            case 2: s = new ProgressScreen(this); break;
            case 3: s = new SettingsScreen(this); break;
            default: s = new TodayScreen(this);
        }
        setRoot(s);
    }

    void push(Screen s) {
        stack.add(s);
        render();
    }

    void back() {
        if (top() != null && top().handleBack()) return;
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1).onLeave();
            render();
        } else if (top() != null && top().tabIndex() > 0) {
            showTab(0);
        } else {
            finish();
        }
    }

    /** Rebuilds the visible screen from its state. */
    void render() {
        Screen top = top();
        if (top == null) return;
        content.removeAllViews();
        top.build(content);
        int vis = top.isTab() ? View.VISIBLE : View.GONE;
        if (nav.getVisibility() != vis) {
            nav.setVisibility(vis);
            refreshInsets();
        }
        int tab = top.tabIndex();
        for (int i = 0; i < navItems.length; i++) {
            boolean on = i == tab;
            navItems[i][0].setTextColor(on ? pal.accent : pal.muted);
            navItems[i][1].setTextColor(on ? pal.accent : pal.muted);
        }
    }

    private LinearLayout buildNav() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(pal.surface);
        bar.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            LinearLayout item = Ui.col(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            item.setBackground(Ui.pressable(Ui.rounded(0, Ui.dp(this, 12), 0, 0), Palette.alpha(pal.ink, 0.08f), Ui.dp(this, 12)));
            TextView k = Ui.jp(this, TABS[i][0], 17, pal.muted);
            k.setGravity(Gravity.CENTER);
            TextView l = Ui.label(this, TABS[i][1], pal.muted);
            l.setTextSize(10);
            l.setGravity(Gravity.CENTER);
            item.addView(k);
            item.addView(l);
            item.setOnClickListener(v -> showTab(idx));
            navItems[i] = new TextView[]{k, l};
            bar.addView(item, Ui.weight(1, Ui.dp(this, 4)));
        }
        return bar;
    }

    // ---------------------------------------------------------------- window

    @SuppressWarnings("deprecation")
    private void setupWindow() {
        Window w = getWindow();
        w.getDecorView().setBackgroundColor(pal.bg);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            w.setStatusBarColor(0x00000000);
            w.setNavigationBarColor(0x00000000);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(pal.dark ? 0 : light, light);
            }
        } else {
            w.setStatusBarColor(pal.bg);
            w.setNavigationBarColor(pal.surface);
            int flags = w.getDecorView().getSystemUiVisibility();
            if (!pal.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void applyInsets() {
        if (Build.VERSION.SDK_INT < 30) return;
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            int bottom = Math.max(bars.bottom, ime.bottom);
            if (nav.getVisibility() == View.VISIBLE) {
                nav.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6) + bottom);
                content.setPadding(0, 0, 0, 0);
            } else {
                content.setPadding(0, 0, 0, bottom);
            }
            return insets;
        });
    }

    /** Re-apply insets after nav visibility changes. */
    void refreshInsets() { root.requestApplyInsets(); }

    // ---------------------------------------------------------------- permissions

    boolean canNotify() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    void askNotifyPermission() {
        if (!canNotify() && Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        Notifier.refreshLockCard(this);
    }
}
