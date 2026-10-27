# Now

A to-do app with three buckets, built for one person with ADHD, autistic traits and
anxiety. It is not trying to be a productivity system. It is trying to still be
installed in thirty days.

- **Now** — the one thing being done right now. 0 or 1 item.
- **Today** — at most 3 items. Hard cap.
- **Later** — everything else. Quiet. No count badge. Never nags.

No tags. No projects. No kanban. No priorities. No inbox-zero ritual. No red.

**Download / install:** see [INSTALL.md](INSTALL.md). The APK is rebuilt on every
push and published at
`https://github.com/vincentzzy/m3-adhdproductivity/releases/download/now-debug/app-debug.apk`.

---

## How each abandonment cause is addressed

| Why apps get abandoned | What Now does | Where |
|---|---|---|
| Too much friction to add a task | Capture row pinned on the Now screen; QS tile; share target; widget `+`; floating capture activity. Nothing but a title is required. | `ui/now/CaptureField.kt`, `platform/QuickCaptureActivity.kt`, `platform/CaptureTileService.kt`, `platform/ShareCaptureActivity.kt` |
| Too many features / cluttered UI | Three tabs, one sheet, one mode. Later collapsed by default. | `ui/NowApp.kt` |
| Notifications too weak (out of sight) | Exactly one ongoing, silent, non-dismissable-by-default notification: "Today: N things" + the top task + Done / Snooze 30m / Start focus. Rebuilt on every state change and after reboot. | `platform/Notifications.kt`, `platform/NotificationUpdater.kt` |
| Notifications too spammy (muted) | ≤ 3 timed pings/day at the user's anchors, ≤ 2 escalations, hard ceiling 5/day, quiet hours, one-tap "quiet for today". Enforced by pure, unit-tested policy. | `domain/NotificationPolicy.kt`, `platform/PingReceiver.kt` |
| Overdue piles up and turns red | No red exists in the palette (`error` = neutral grey, asserted by a test). Overdue keeps its colour and gains a quiet "carried over" tag. | `ui/theme/Palette.kt`, `ui/components/Common.kt` |
| Streaks break → quits | Two automatic grace days **per month**, refilled at each month boundary; a missed day shows "protected" instead of "broken"; `best` is never reduced. | `domain/StreakMath.kt` |
| The app feels like homework | No required sorting, tagging, reviewing or clearing. A 30-second glance that just reads the notification is a complete interaction. | whole design |
| Nothing surfaces where they look | Home-screen widget (4×2) with working checkboxes, ongoing status-bar notification, QS tile. | `widget/NowWidget.kt`, `platform/Notifications.kt` |
| Doesn't match how they think | Buckets, not projects. Capture defaults to Later so junk never eats Today. Task breakdown yields a ≤5-minute first step. | `domain/BucketRules.kt`, `domain/BreakdownEngine.kt` |
| Bored once novelty wore off | One deliberately novel element, always visible: the focus dial is a live shape morph between two `RoundedPolygon`s whose progress is the session. The completion celebration morphs too. | `ui/components/Shapes.kt`, `ui/focus/FocusScreen.kt` |

---

## Screens

1. **Now** (opens here, always) — capture row, the Now card with one huge Start
   button, Today's three rows with 48dp checkboxes, collapsed Later drawer.
   Rows drag between buckets; the same move is in each row's menu for TalkBack.
2. **Time** — analog clock, countdown to the next deadline, day-budget bar,
   time since the last break.
3. **Progress** — only numbers that can only go up: focus minutes, things
   finished, closest-ever time guess, best streak.
4. **Focus** — a mode, not a tab: adjustable timer, morphing dial, procedural
   brown noise, session logged on exit.

---

## Architecture

```
UI (Compose, single activity)      NowApp → Now / Time / Progress + Focus mode + Settings sheet
        │  StateFlow
ViewModel                          NowViewModel (one, no-arg → default factory)
        │
Domain (pure Kotlin, unit tested)  BucketRules · StreakMath · Multiplier · DayBudgetMath
        │                          BreakdownEngine · NotificationPolicy · TimeMath
        │
Data                               Room (tasks, focus_sessions, streak, anchors)
                                   DataStore Preferences (calm mode, quiet hours, ping budget)
        │
Platform                           ongoing notification + action receiver · exact-alarm pings
                                   WorkManager housekeeping · QS tile · share target · Glance widget
```

- **Single activity**, state-based tabs (no navigation-compose).
- **Manual DI** through `ServiceLocator`; the ViewModel has a no-arg constructor so
  the default `viewModel()` factory works.
- **Flows all the way down**: `NotificationUpdater` collects tasks + prefs once and
  rebuilds the notification and widget, so neither can go stale.
- **Portrait only**, `minSdk 26`, `compileSdk 37`, `targetSdk 36`, Java 17,
  Kotlin 2.2.10, AGP 9.1.0, Gradle 9.3.1, material3 1.5.0-beta02.
- **Offline by construction**: the manifest declares no `INTERNET` permission.

### Notification budget (hard rules, all tested)

```
timed pings:        ≤ 3 per day (at the user's anchors)
escalations:        ≤ 2 per hard-deadline task, then silence
notifications:      ≤ 5 per day total
quiet hours:        default 21:30–08:00, checked before every ping
quiet for today:    one tap, expires at midnight
ongoing anchor:     exactly one, silent, IMPORTANCE_LOW
```

### Motion policy

`MotionPolicy` reads `Settings.Global.ANIMATOR_DURATION_SCALE` on every resume and
combines it with the in-app calm-mode toggle:

- system "remove animations" → every spec becomes `snap()`;
- calm mode → every duration halves and `MotionScheme.standard()` replaces
  `MotionScheme.expressive()`;
- absolute cap → 400 ms, enforced in `MotionPolicy.durationMillis`.

One morphing element is visible at a time: the focus dial, or the celebration
overlay, never both.

---

## Build and test

```bash
./gradlew assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew test              # 48 JVM unit tests
```

`./gradlew` is a small bootstrap script that downloads the pinned Gradle 9.3.1
distribution (the binary wrapper JAR is not committed; `gradle wrapper`
regenerates a canonical one). CI does exactly the above on every push, then
publishes the APK to a release and uploads it as a workflow artifact.

Tests cover the four areas the spec named, plus two more:

- streak grace-day logic (`StreakMathTest`) — including month refill and idempotence
- multiplier calculation (`MultiplierTest`) — thresholds, clamping, "closest ever"
- day-budget maths (`DayBudgetTest`) — clamping, midnight wrap, formatting
- bucket overflow (`BucketRulesTest`) — Today's cap, Now's cap, swap policy
- task breakdown (`BreakdownEngineTest`) — 3–5 steps, first step always ≤ 5 min
- notification budget (`NotificationPolicyTest`) — ceilings, quiet hours, reset
- palette (`PaletteTest`) — no reddish colour, accent contrast ≥ 4.5:1

---

## File tree

```
.github/workflows/build.yml          Android debug build, tests, APK release + artifact, verify, Drive upload
ci/report.sh                         posts build/test/APK reports back as commit comments
DECISIONS.md                         every judgement call and API substitution
INSTALL.md                           plain-language sideloading instructions
README.md                            this file
build.gradle.kts
settings.gradle.kts
gradle.properties
gradle/libs.versions.toml            all versions pinned explicitly
gradle/wrapper/gradle-wrapper.properties
gradlew                              pinned bootstrap for Gradle 9.3.1
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/nowapp/
  MainActivity.kt                    single activity, portrait, no splash gate
  NowApplication.kt                  startup wiring (no main-thread I/O)
  ServiceLocator.kt                  manual dependency container
  data/
    AnchorRepository.kt              the user's daily anchor times
    Backup.kt                        JSON export/import (org.json, no extra plugin)
    FocusRepository.kt
    Prefs.kt                         DataStore: calm mode, quiet hours, ping budget
    StreakRepository.kt
    TaskRepository.kt                every mutation, caps enforced here
    db/Daos.kt
    db/Entities.kt                   Task, FocusSession, Streak, Anchor (+ mappers)
    db/NowDatabase.kt
  domain/
    BreakdownEngine.kt               offline heuristic breakdown, ≤5-minute first step
    BucketRules.kt                   caps, overflow swaps, capture destination
    DayBudget.kt                     day budget + duration formatting
    Multiplier.kt                    estimate-vs-actual calibration (derived, never stored)
    NotificationPolicy.kt            ping budget, quiet hours, escalation limits
    StreakMath.kt                    grace days, month refill, best record
    Task.kt                          domain model + Bucket
    TimeMath.kt                      countdown, day boundaries, clock formatting
  platform/
    BootReceiver.kt
    BrownNoiseEngine.kt              procedural brown noise via AudioTrack
    CaptureTileService.kt            Quick Settings tile
    DailyWorker.kt                   WorkManager housekeeping (streak, anchor, widget)
    NotificationActionReceiver.kt    Done / Snooze without opening the app
    NotificationUpdater.kt           single collector that keeps the anchor fresh
    Notifications.kt                 the one ongoing notification + ping notifications
    PingReceiver.kt                  anchor ping, budget-checked
    PingScheduler.kt                 exact alarm, graceful inexact fallback
    QuickCaptureActivity.kt          floating capture surface
    ShareCaptureActivity.kt          share-sheet target, captures straight to Later
    WidgetActions.kt                 widget checkbox callback
  ui/
    NowApp.kt                        shell: tabs, focus mode, settings, onboarding, celebration
    NowViewModel.kt                  one ViewModel for a one-user app
    components/Celebration.kt        morphing completion celebration
    components/Common.kt             BigCheckbox, SectionHeader, StatCard, QuietTag, TaskTitle
    components/MinutePickerDialog.kt estimate / actual-minute picker
    components/Shapes.kt             MaterialShapes accents + MorphSurface (the one morphing element)
    focus/FocusScreen.kt             timer + dial + noise toggle
    now/CaptureField.kt
    now/NowScreen.kt                 Now card, Today's three, Later drawer, drag targets
    now/TaskRow.kt                   checkbox, overflow menu, drag between buckets
    progress/ProgressScreen.kt       only-goes-up stats
    settings/SettingsSheet.kt        calm mode, quiet hours, anchors, backup
    theme/Motion.kt                  MotionPolicy (system scale + calm mode)
    theme/Palette.kt                 one accent hue, no red (unit tested)
    theme/Theme.kt                   MaterialExpressiveTheme + hand-built dark scheme
    time/AnalogClock.kt
    time/TimeScreen.kt
  widget/NowWidget.kt                Glance 4x2 widget: count, 3 checkboxes, capture
app/src/main/res/
  drawable/ic_launcher_foreground.xml
  drawable/ic_tile.xml
  mipmap-anydpi-v26/ic_launcher.xml
  values/colors.xml
  values/strings.xml
  values/themes.xml
  xml/now_widget_info.xml
app/src/test/java/com/nowapp/domain/
  BreakdownEngineTest.kt
  BucketRulesTest.kt
  DayBudgetTest.kt
  MultiplierTest.kt
  NotificationPolicyTest.kt
  PaletteTest.kt
  StreakMathTest.kt
```

---

## Accessibility

- Every interactive element has a TalkBack description; custom controls
  (`BigCheckbox`) use `toggleable` with `Role.Checkbox`.
- Minimum 44dp touch targets (most are 48–56dp).
- Text uses the Material type scale with `copy()` weight changes only, so large
  system font scaling flows through; screens scroll rather than clip.
- Drag-and-drop is always duplicated by a menu action, because dragging is a poor
  screen-reader gesture.
- Calm mode and the system animation setting are both honoured everywhere.

## Deliberate limitations (v1)

- The ongoing notification can still be dismissed by the user on Android 14+; the
  app re-posts it on every change and every 6 hours. Truly non-dismissable
  requires a foreground service, which would add a second permanent notification.
- Brown noise stops when the Focus screen is left (a background audio service
  would need its own foreground notification).
- No lock-screen widget, no cloud sync, no body doubling, no Wear OS, no LLM
  calls — the seams exist (repository interfaces, `focus` mode launcher,
  `Backup.Snapshot`) but nothing is implemented.
