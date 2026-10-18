# Ichiji 一字 — one Japanese character a day (Android)

A calm, widget-first app for learning to read Japanese. Today's character waits on your
home screen: start with hiragana, move on to katakana and the rules for sounds, and finish
with kanji made from simpler pieces you've met before.

## What's in it

- **Placement before day one.** Tick the kana you already read on a real gojūon chart
  (tap a row letter for the whole row). Those count as known and get a quick check in about two weeks.
- **Your pace.** ゆっくり Easygoing (1 a day), ふつう Regular (3) or はやい Fast (5). Miss a day and nothing piles up:
  the next character waits. "Learn one more today" for keen days.
- **Practice sheet.** Reading, example word, notes, Japanese audio (the phone's own voice),
  stroke order that draws itself, a frame per stroke, and **finger tracing** that marks each
  stroke clean, close, drifted or backwards and shows where it should have gone.
- **Reviews.** One batch a day, newest first: recall first, then reveal. Scheduled with **FSRS-5**,
  the open spaced-repetition algorithm. Each character waxes like the moon: 朔 new → 眉 crescent → 弦 half → 望 full.
- **Drills.** Ten fast questions from any deck, or trace a random character.
  Doesn't touch your schedule.
- **Progress.** Streak, stamps at 7 / 30 / 100 days, a days-studied heatmap, and every
  chart shaded by how well each character is held.
- **Widgets.** A small one (today's character, its reading, a red dot until today is done)
  and a wide kana board that fills in as you learn (today's character ringed, full-moon ones
  inverted). Three color styles: 藍 Indigo, 紙 Paper, 夜 Night. Updates just after midnight.
- **Lock screen card.** Android phones don't allow lock-screen widgets, so Ichiji uses a
  silent notification card instead. Turn it off in Settings if you'd rather not.
- **Course:** 46 hiragana, 46 katakana, 14 sound-rule cards, then 33 kanji in three levels
  (unlocks once 100 of the 106 kana cards have passed a review).
- **Private.** No account, no internet permission, no tracking. Everything stays on the phone.
  Privacy policy: https://waelde13-spec.github.io/ichiji/privacy/ (source in `docs/privacy/`).

## Get the APK onto your phone (no computer setup needed)

The project includes a GitHub Actions workflow that builds the app in the cloud for free.

1. Create a free account at github.com and a new **empty** repository (e.g. `ichiji`).
2. Upload everything in this folder to it, including the hidden `.github` folder and
   `app/ichiji-debug.keystore`. (Easiest from a computer with
   `git init && git add . && git commit -m "Ichiji" && git branch -M main && git remote add origin <repo-url> && git push -u origin main`.)
3. Open the repository's **Actions** tab and wait about 5 minutes for "Build APK" to go green.
4. On your phone, open the repository's **Releases** page and tap **Ichiji.apk**.
   Android will ask you to allow installs from your browser; allow it once.
5. Every new push builds a new release that installs over the old one, keeping your progress.

## Publishing on Google Play

Google Play needs a signed **.aab** (not the test APK). The upload key is kept out of this
public repo; GitHub reads it from two repository secrets.

One-time setup (Settings ▸ Secrets and variables ▸ Actions ▸ New repository secret):

| Name | Value |
|---|---|
| `ICHIJI_UPLOAD_KEYSTORE` | the whole contents of `ICHIJI_UPLOAD_KEYSTORE.txt` |
| `ICHIJI_UPLOAD_PASSWORD` | the upload key password |

Each release: **Actions ▸ Release for Google Play ▸ Run workflow**. In about 5 minutes the
signed `Ichiji-release.aab` is in a draft under **Releases** (only you can see drafts).
Upload it in Play Console. Every run gets a higher version code automatically.

Keep `ichiji-upload-key.jks` and its password backed up somewhere safe (a password manager
plus a second copy). With Play App Signing, Google holds the real app-signing key; if the
upload key is ever lost, it can be reset from Play Console, but that takes a few days.

Privacy policy for the store listing: https://waelde13-spec.github.io/ichiji/privacy/

## Or build it in Android Studio

Open this folder in Android Studio (Ladybug or newer), let Gradle sync, plug in a phone with
USB debugging on, and press Run. Command line: `./gradlew assembleDebug`
(output: `app/build/outputs/apk/debug/app-debug.apk`). Needs JDK 17.

## How it's built

Plain Java on the Android framework, **zero third-party libraries**: every screen is built in
code, the charts, stroke animation, tracing, heatmap and widgets are custom-drawn.
minSdk 26 (Android 8), targetSdk 36 (Android 16).

```
app/src/main/java/com/ichiji/kana/
  Store.java          all data + daily logic (batches, reviews, streaks), saved on device
  Fsrs.java           FSRS-5 scheduler (pure Java)
  Card / CardState    a course item / its memory state
  Strokes.java        KanjiVG stroke data + a small SVG path parser
  StrokeView.java     stroke animation and tracing with per-stroke feedback
  *Screen.java        Onboarding, Today, Sheet, Review, Drills, Progress, Settings
  WidgetArt/Widgets   the two home-screen widgets
  Notifier / Alarms   lock-screen card, daily reminder, midnight refresh
app/src/main/assets/  curriculum.json, strokes.json
tools/build_data.py   regenerates the assets (edit the course here)
```

To add more kanji, add rows to `KANJI` in `tools/build_data.py` and run it against a
KanjiVG checkout: `python3 tools/build_data.py path/to/kanjivg/kanji`.

## Notes

- The signing key in `app/` is a throwaway debug key so test builds install over each other.
  It is never used for Google Play builds.
- Audio uses the phone's text-to-speech. If nothing plays, install a Japanese voice
  (Settings ▸ Voice in the app links straight there).

## Licenses

Stroke-order data: [KanjiVG](http://kanjivg.tagaini.net) © Ulrich Apel, CC BY-SA 3.0.
`app/src/main/assets/strokes.json` is a converted subset and is shared under the same license.
