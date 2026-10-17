package com.ichiji.kana;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One item of the course: a kana, a sound rule, or a kanji. Immutable. */
public final class Card {
    public static final String HIRA = "hira", KATA = "kata", RULE = "rule", KANJI = "kanji";

    public final int index;
    public final String id, kind, ch, rd, word, wordRomaji, wordMeaning, note, strokeChar, pair, name, kana, meaning;
    public final int level;
    public final List<String[]> parts;

    Card(JSONObject o, int index) throws JSONException {
        this.index = index;
        id = o.getString("id");
        kind = o.getString("kind");
        ch = o.getString("ch");
        rd = o.optString("rd");
        word = o.optString("w");
        wordRomaji = o.optString("wr");
        wordMeaning = o.optString("wm");
        note = o.optString("note");
        strokeChar = o.optString("st");
        pair = o.optString("pair");
        name = o.optString("name");
        kana = o.optString("kana");
        meaning = o.optString("mean");
        level = o.optInt("lv", 0);
        List<String[]> p = new ArrayList<>();
        JSONArray arr = o.optJSONArray("parts");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONArray a = arr.getJSONArray(i);
                p.add(new String[]{a.getString(0), a.getString(1)});
            }
        }
        parts = Collections.unmodifiableList(p);
    }

    public boolean isKanji() { return KANJI.equals(kind); }
    public boolean isKana() { return !isKanji(); }

    public String kindLabel() {
        switch (kind) {
            case HIRA: return "Hiragana";
            case KATA: return "Katakana";
            case RULE: return "Sound rule · " + name;
            default: return "Kanji · level " + level;
        }
    }

    /** Short label used as the big answer (romaji for kana, meaning for kanji). */
    public String answer() {
        if (isKanji()) return meaning;
        return rd;
    }

    /** What the Japanese voice should read for this card. */
    public String speakText() {
        if (RULE.equals(kind)) return word;
        if (isKanji()) return kana;
        if ("を".equals(ch) || "ヲ".equals(ch)) return "お";
        return ch;
    }

    public boolean hasStrokes() { return strokeChar != null && !strokeChar.isEmpty(); }
}
