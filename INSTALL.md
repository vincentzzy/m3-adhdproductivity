# Installing Now on your phone (no computer needed)

You do not need Android Studio, a laptop, or an account. You need the phone and a
browser. The whole app is about 16 MB and works completely offline.

## 1. Download the app file

On the phone, open this link:

**https://github.com/vincentzzy/m3-adhdproductivity/releases/download/now-debug/app-debug.apk**

It is the newest automatic build; every update to the app republishes it at the
same address, so this link never goes stale.

If you prefer clicking: go to
`https://github.com/vincentzzy/m3-adhdproductivity/releases` and tap the newest
release named **"Now — latest debug build"**, then tap **app-debug.apk**.

Chrome will warn you that "this type of file can harm your device". That warning
appears for every APK downloaded from a browser. Tap **Download anyway**.

## 2. Allow this one app to install

Android blocks apps from outside the Play Store until you allow it, per app:

1. When the download finishes, tap it (or open **Chrome → Downloads → app-debug.apk**).
2. Android says *"For your security, your phone is not allowed to install unknown
   apps from this source."* Tap **Settings**.
3. Turn on **Allow from this source** (the toggle is for Chrome, not for
   everything).
4. Tap the back arrow, then tap **Install**.

On Samsung/Xiaomi/Oppo the wording differs slightly ("Install unknown apps",
"Unknown sources"), but the path is the same: the toggle belongs to the browser
you downloaded with, and you only have to do this once.

If Play Protect offers to scan the app, let it. If it says the app is
"unrecognised", choose **Install anyway** — the APK is signed with a debug
certificate because it is built by you, for you.

## 3. First launch (three taps)

1. Tap **Start**.
2. Android asks to allow notifications — tap **Allow**. This is the only reason the
   app works: the persistent "Today: N things" line is the anchor.
3. You land on **Now**. Type one thing and press return. Done.

Optional but recommended:

- **Home-screen widget:** long-press the home screen → Widgets → **Now** → drag
  the 4×2 widget out. Its checkboxes work without opening the app.
- **Quick Settings tile:** pull down the notification shade twice → pencil/edit →
  drag **Capture a task** into the tiles. One tap from inside any app.
- **Exact alarms:** Settings (in the app) → notifications row is enough for most
  phones. If your phone reports that anchor pings are delayed, open Android
  Settings → Apps → Now → **Alarms & reminders** → allow.

## 4. Updating

Download the same link again and install over the old version. Your data stays:
it lives in the app's local database and is only replaced if you restore a backup.

## 5. Backing up (30 seconds, optional)

In the app: **Settings (gear) → Export**. Pick a folder; you get a readable
`now-backup-YYYY-MM-DD.json` file. **Import** restores it. There is no cloud and no
account — the file is the backup.

## Troubleshooting

| Symptom | Fix |
|---|---|
| "App not installed" | An older copy signed differently is installed. Uninstall the old Now, then install again (your data will be lost unless you exported first). |
| No notification appears | Android Settings → Apps → Now → Notifications → allow. Then open the app once. |
| Pings arrive late | Allow "Alarms & reminders" for Now; battery optimisation may also need to exclude Now. |
| Widget shows "Can't load widget" | Open the app once after installing, then re-add the widget. |
| Nothing in Later | Correct. Later has no badge and no reminder by design. |

## Building it yourself (only if you want to)

No local Android SDK is required: every push builds and tests automatically in
GitHub Actions, and the APK is attached to the `now-debug` release. Locally, with
JDK 17 and the Android SDK 37 installed:

```bash
./gradlew assembleDebug
./gradlew test
```
