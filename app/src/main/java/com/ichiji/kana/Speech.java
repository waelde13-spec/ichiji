package com.ichiji.kana;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Reads Japanese aloud with the phone's own on-device voice. */
final class Speech implements TextToSpeech.OnInitListener {
    private final Context ctx;
    private final TextToSpeech tts;
    private boolean ready, japanese;
    private boolean warned;

    Speech(Context c) {
        ctx = c;
        tts = new TextToSpeech(c.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        int r = tts.setLanguage(Locale.JAPAN);
        japanese = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
        tts.setSpeechRate(0.8f);
        ready = true;
    }

    boolean hasJapanese() { return ready && japanese; }

    void say(String text) {
        if (text == null || text.isEmpty()) return;
        if (!ready) return;
        if (!japanese) {
            if (!warned) Ui.toast(ctx, "No Japanese voice yet. Settings ▸ Voice shows how to add one.");
            warned = true;
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ichiji-" + System.nanoTime());
    }

    static void openVoiceSettings(Context c) {
        try {
            Intent i = new Intent("com.android.settings.TTS_SETTINGS");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
            } catch (Exception ignored) {
                Ui.toast(c, "Open Settings ▸ Accessibility ▸ Text-to-speech");
            }
        }
    }

    void shutdown() { tts.shutdown(); }
}
