package com.ichiji.kana;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * All learner data lives here, on the device, in one SharedPreferences JSON blob.
 * Also owns the daily logic: today's new characters, the review queue, streaks.
 */
public final class Store {
    public static final int KANA_TOTAL = 106;
    public static final int KANJI_UNLOCK = 96;

    private static Store instance;

    public static synchronized Store get(Context c) {
        if (instance == null) instance = new Store(c.getApplicationContext());
        return instance;
    }

    private final Context ctx;
    private final SharedPreferences prefs;
    public final List<Card> cards = new ArrayList<>();
    private final Map<String, Card> byId = new HashMap<>();
    private final Map<String, CardState> states = new HashMap<>();
    private final TreeMap<Long, Integer> activity = new TreeMap<>();
    private long batchDate = -1;
    private final List<String> batch = new ArrayList<>();

    // Settings
    public boolean onboarded;
    public int pace = 1;            // new characters per day: 1, 3 or 5
    public int reminderHour = 8;
    public boolean reminders = true;
    public boolean lockCard = true; // silent lock-screen notification with today's character
    public int theme;               // 0 system, 1 paper, 2 night
    public int widgetTheme;         // 0 indigo, 1 paper, 2 night
    public int boardTrack;          // 0 follow course, 1 hiragana, 2 katakana

    private Store(Context c) {
        ctx = c;
        prefs = c.getSharedPreferences("ichiji", Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(readAsset(c, "curriculum.json"));
            for (int i = 0; i < arr.length(); i++) {
                Card card = new Card(arr.getJSONObject(i), i);
                cards.add(card);
                byId.put(card.id, card);
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Bad curriculum", e);
        }
        load();
    }

    static String readAsset(Context c, String name) {
        try (InputStream in = c.getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing asset " + name, e);
        }
    }

    // ---------------------------------------------------------------- persistence

    private synchronized void load() {
        String raw = prefs.getString("data", null);
        if (raw == null) return;
        try {
            JSONObject o = new JSONObject(raw);
            JSONObject s = o.optJSONObject("settings");
            if (s != null) {
                onboarded = s.optBoolean("onboarded");
                pace = s.optInt("pace", 1);
                reminderHour = s.optInt("hour", 8);
                reminders = s.optBoolean("rem", true);
                lockCard = s.optBoolean("lock", true);
                theme = s.optInt("theme");
                widgetTheme = s.optInt("wtheme");
                boardTrack = s.optInt("board");
            }
            JSONObject st = o.optJSONObject("states");
            if (st != null) {
                Iterator<String> it = st.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    if (byId.containsKey(k)) states.put(k, CardState.fromJson(st.getJSONObject(k)));
                }
            }
            JSONObject act = o.optJSONObject("act");
            if (act != null) {
                Iterator<String> it = act.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    activity.put(Long.parseLong(k), act.getInt(k));
                }
            }
            JSONObject b = o.optJSONObject("batch");
            if (b != null) {
                batchDate = b.optLong("date", -1);
                JSONArray ids = b.optJSONArray("ids");
                if (ids != null) for (int i = 0; i < ids.length(); i++) batch.add(ids.getString(i));
            }
        } catch (JSONException | NumberFormatException e) {
            // Corrupt data: keep whatever loaded cleanly.
        }
    }

    public synchronized void save() {
        try {
            JSONObject o = new JSONObject();
            JSONObject s = new JSONObject();
            s.put("onboarded", onboarded);
            s.put("pace", pace);
            s.put("hour", reminderHour);
            s.put("rem", reminders);
            s.put("lock", lockCard);
            s.put("theme", theme);
            s.put("wtheme", widgetTheme);
            s.put("board", boardTrack);
            o.put("settings", s);
            JSONObject st = new JSONObject();
            for (Map.Entry<String, CardState> e : states.entrySet()) st.put(e.getKey(), e.getValue().toJson());
            o.put("states", st);
            JSONObject act = new JSONObject();
            for (Map.Entry<Long, Integer> e : activity.entrySet()) act.put(String.valueOf(e.getKey()), e.getValue());
            o.put("act", act);
            JSONObject b = new JSONObject();
            b.put("date", batchDate);
            b.put("ids", new JSONArray(batch));
            o.put("batch", b);
            prefs.edit().putString("data", o.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    /** Saves and refreshes everything that shows progress outside the app. */
    public void commit() {
        save();
        Widgets.updateAll(ctx);
        Notifier.refreshLockCard(ctx);
    }

    // ---------------------------------------------------------------- lookups

    public static long today() { return LocalDate.now().toEpochDay(); }

    public Card card(String id) { return byId.get(id); }

    public synchronized CardState state(Card c) { return states.get(c.id); }

    public synchronized boolean isLearned(Card c) { return states.containsKey(c.id); }

    public List<Card> cardsOfKind(String kind) {
        List<Card> out = new ArrayList<>();
        for (Card c : cards) if (c.kind.equals(kind)) out.add(c);
        return out;
    }

    public synchronized int learnedCount(String kind) {
        int n = 0;
        for (Card c : cards) if (c.kind.equals(kind) && states.containsKey(c.id)) n++;
        return n;
    }

    public synchronized int kanaPassed() {
        int n = 0;
        for (Card c : cards) {
            if (!c.isKana()) continue;
            CardState s = states.get(c.id);
            if (s != null && s.passed()) n++;
        }
        return n;
    }

    public boolean kanjiUnlocked() { return kanaPassed() >= KANJI_UNLOCK; }

    public synchronized int[] stageCounts() {
        int[] out = new int[4];
        for (CardState s : states.values()) out[s.stage()]++;
        return out;
    }

    // ---------------------------------------------------------------- daily batch

    /** Today's new characters. If yesterday's were never learned, they wait instead of piling up. */
    public synchronized List<Card> todayBatch() {
        long t = today();
        if (batchDate != t) {
            List<String> carry = new ArrayList<>();
            for (String id : batch) if (!states.containsKey(id)) carry.add(id);
            batch.clear();
            if (!carry.isEmpty()) batch.addAll(carry);
            else for (Card c : nextNew(onboarded ? pace : 0)) batch.add(c.id);
            batchDate = t;
            save();
        }
        List<Card> out = new ArrayList<>();
        for (String id : batch) {
            Card c = byId.get(id);
            if (c != null) out.add(c);
        }
        return out;
    }

    /** Adds one more new character to today, for eager days. Returns it, or null if none is available. */
    public synchronized Card addExtra() {
        todayBatch();
        List<Card> next = nextNew(1);
        if (next.isEmpty()) return null;
        batch.add(next.get(0).id);
        save();
        return next.get(0);
    }

    private List<Card> nextNew(int n) {
        List<Card> out = new ArrayList<>();
        if (n <= 0) return out;
        boolean kanjiOk = kanjiUnlocked();
        for (Card c : cards) {
            if (out.size() >= n) break;
            if (states.containsKey(c.id) || batch.contains(c.id)) continue;
            if (c.isKanji() && !kanjiOk) break;
            out.add(c);
        }
        return out;
    }

    /** True when there is nothing new left that could be taught right now. */
    public synchronized boolean waitingForKanji() {
        for (Card c : cards) {
            if (states.containsKey(c.id)) continue;
            return c.isKanji() && !kanjiUnlocked();
        }
        return false;
    }

    public synchronized boolean courseFinished() {
        for (Card c : cards) if (!states.containsKey(c.id)) return false;
        return true;
    }

    public synchronized void learn(Card c) {
        if (states.containsKey(c.id)) return;
        states.put(c.id, CardState.learnedOn(today()));
        bump();
        commit();
    }

    public synchronized void finishOnboarding(Collection<String> knownIds) {
        markKnown(knownIds);
        onboarded = true;
        batch.clear();
        batchDate = -1;
        todayBatch();
        commit();
    }

    /** Placement: what the learner already reads starts as known, with a light check two weeks in. */
    public synchronized void markKnown(Collection<String> ids) {
        long t = today();
        int i = 0;
        for (Card c : cards) {
            if (!ids.contains(c.id) || states.containsKey(c.id)) continue;
            states.put(c.id, CardState.knownOn(t, t + 14 + i / 4));
            i++;
        }
        save();
    }

    // ---------------------------------------------------------------- reviews

    /** Everything due today or earlier, freshest first. */
    public synchronized List<Card> dueReviews() {
        long t = today();
        List<Card> out = new ArrayList<>();
        for (Card c : cards) {
            CardState s = states.get(c.id);
            if (s != null && s.due <= t) out.add(c);
        }
        Collections.sort(out, (a, b) -> Long.compare(states.get(b.id).learned, states.get(a.id).learned));
        return out;
    }

    public synchronized int dueTomorrow() {
        long t = today() + 1;
        int n = 0;
        for (CardState s : states.values()) if (s.due == t) n++;
        return n;
    }

    public synchronized void grade(Card c, int g) {
        CardState s = states.get(c.id);
        if (s == null) return;
        s.review(today(), g);
        bump();
        commit();
    }

    public synchronized int previewDays(Card c, int g) {
        CardState s = states.get(c.id);
        return s == null ? 1 : s.previewDays(today(), g);
    }

    // ---------------------------------------------------------------- habit

    public synchronized void bump() { activity.merge(today(), 1, Integer::sum); }

    public synchronized int streak() {
        long d = today();
        if (!activity.containsKey(d)) d--;
        int n = 0;
        while (activity.containsKey(d)) { n++; d--; }
        return n;
    }

    public synchronized int bestStreak() {
        int best = 0, run = 0;
        long prev = Long.MIN_VALUE;
        for (long d : activity.keySet()) {
            run = (d == prev + 1) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    public synchronized int daysStudied() { return activity.size(); }

    public synchronized int activityOn(long day) {
        Integer v = activity.get(day);
        return v == null ? 0 : v;
    }

    public synchronized boolean doneToday() {
        for (Card c : todayBatch()) if (!states.containsKey(c.id)) return false;
        return dueReviews().isEmpty();
    }

    /** The character the widgets should feature right now. */
    public synchronized Card featured() {
        for (Card c : todayBatch()) if (!states.containsKey(c.id)) return c;
        List<Card> b = todayBatch();
        if (!b.isEmpty()) return b.get(b.size() - 1);
        Card latest = null;
        long when = Long.MIN_VALUE;
        for (Card c : cards) {
            CardState s = states.get(c.id);
            if (s != null && !s.known && s.learned >= when) { when = s.learned; latest = c; }
        }
        return latest;
    }

    /** Which kana board the home-screen widget should show: true = katakana. */
    public synchronized boolean boardIsKatakana() {
        if (boardTrack == 1) return false;
        if (boardTrack == 2) return true;
        int hira = learnedCount(Card.HIRA);
        return hira >= 46 || (featured() != null && !Card.HIRA.equals(featured().kind) && learnedCount(Card.KATA) > 0);
    }

    public synchronized void resetAll() {
        states.clear();
        activity.clear();
        batch.clear();
        batchDate = -1;
        onboarded = false;
        commit();
    }
}
