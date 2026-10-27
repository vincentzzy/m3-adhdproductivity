# Decisions

Every judgement call, and every place the spec named an API that this
toolchain does not actually ship. One line each: the decision, then the reason.

## Product

| # | Decision | Reason |
|---|----------|--------|
| 1 | Name: **Now**. Rejected alternatives: *Three* and *Quiet*. | "Now" is the bucket the user should look at first and the shortest possible word to type in a launcher. |
| 2 | Package `com.nowapp`, app label "Now". | No brand, no account, nothing to sign up for. |
| 3 | Capture defaults to **Later**; the chip can put a task in Today. | Junk capture must not consume one of the three precious Today slots, and a default means capture never asks a question. |
| 4 | A full Today **swaps** the oldest item back to Later instead of refusing. | A hard cap that produces a blocking error is the moment people stop opening the app. |
| 5 | Promoting a task to Now pushes the previous Now task to Today (or Later if full). | "0 or 1 in Now" is a hard rule, and silent exchange keeps it true without a dialog. |
| 6 | Breakdown applies as: first step (<= 5 min) becomes the task title, remaining steps go into notes; existing estimate is replaced by the first step's minutes. | Creating 4 child tasks would blow the three-item Today cap and add maintenance burden. |
| 7 | Estimate-vs-actual is asked only if a task already has an estimate; no prompt appears otherwise. | A required "how long did that take?" dialog is friction exactly at the moment of reward. |
| 8 | Welcome-back triggers after 3+ days unopened, is dismissible, and changes nothing in the data. | The failure mode is a backlog wall, not missing content: so the message reassures and shows today. |
| 9 | "1 of 3 done" is celebrated with the same wording ("That counts") as 3 of 3. | The user's failure story is about falling behind, so completion is never graded. |
| 10 | No count badge for Later, no total task count on the Now screen. | Visibility of backlog is itself the abandonment trigger. |

## Build and toolchain

| # | Decision | Reason |
|---|----------|--------|
| 11 | AGP 9.0.1, Gradle 9.1.0, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Java 17. | This exact combination built green in this repo earlier; no reason to gamble. |
| 12 | `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`. | material3 1.5.0-beta02 and compose-ui 1.12.1 declare `minCompileSdk=37` in their AAR metadata, so 37 is mandatory; targetSdk stays at 36 (behaviours we can reason about, and it is a sideloaded personal app). |
| 13 | Gradle wrapper committed as a bootstrap `gradlew` script that fetches the pinned 9.1.0 distribution; the binary wrapper JAR is not committed. | The JAR could not be fetched in the authoring sandbox; the pinned distribution URL in gradle-wrapper.properties is authoritative and `gradle wrapper` regenerates a canonical wrapper. |
| 14 | **material3 = 1.5.0-beta02** instead of a "stable" version. | Verified by bytecode inspection: 1.4.0 (latest stable) ships **no** expressive components at all — no MaterialShapes, ButtonGroup, SplitButton, ToggleButton, LoadingIndicator. 1.5.0-beta02 is the only published line that has them. Everything used was checked against the actual artifact before writing the call site. |
| 15 | Compose BOM 2026.09.00 for ui/foundation/runtime (1.12.1), with material3 pinned separately. | The BOM pins material3 1.4.0, so material3 must be overridden to get expressive; the rest of the BOM keeps the compose versions mutually consistent. |
| 16 | No navigation-compose: tab state is a single `rememberSaveable` int and focus mode is a nullable target. | Three destinations; a navigation graph adds a dependency and a cold-start cost for nothing. |
| 17 | Manual `ServiceLocator` instead of Hilt/Koin. | One screen graph, one user: DI frameworks are maintenance burden with no payoff here. |
| 18 | Room 2.8.5 (not Room3 3.1.0-alpha) with `exportSchema = false`. | Room 2.8.5 is stable and well-known; schema export adds a KSP arg for no benefit in a single-user app. |
| 19 | DataStore Preferences for settings, Room for entities. | Settings are single values read at startup; Room is the wrong shape for that. |
| 20 | Backup format is hand-written `org.json` rather than kotlinx-serialization. | One fewer compiler plugin, and the JSON is human-readable in a file manager. |
| 21 | Unit tests are JVM tests only (no instrumented tests). | The runner has no emulator, and all the risky logic (caps, grace days, multiplier, budget, breakdown, notification budget) is deliberately pure Kotlin. |

## Material 3 Expressive — every API checked against the artifact

| # | Spec asked for | What shipped in material3 1.5.0-beta02 | Decision |
|---|----------------|----------------------------------------|----------|
| 22 | `MaterialExpressiveTheme` | Present, but in `MaterialThemeKt`, and takes `(colorScheme, motionScheme, shapes, typography, content)` | Used as-is. |
| 23 | `dynamicExpressiveDarkColorScheme()` | **Absent.** Only `dynamicLight/DarkColorScheme` exist. | Hand-built expressive dark scheme (`ui/theme/Theme.kt`) with one accent hue. Also guarantees the single-hue rule, which dynamic colour would break. |
| 24 | `MotionScheme.expressive()` | Present (`MotionScheme.Companion.expressive()`) | Used; calm mode or system "remove animations" switches to `MotionScheme.standard()` and snaps all tweens. |
| 25 | `MaterialShapes` | Present (circle … heart, 35 shapes) | Used as container accents (`Clover4Leaf` for the Now marker, `Pill`/`SoftBurst` available). |
| 26 | `Morph` + `RoundedPolygon` | `androidx.graphics.shapes.Morph` present; `toPath` exists in both graphics-shapes (native Path) and material3 (`@Composable` extension) | Used the non-composable graphics-shapes `Morph.toPath(progress, androidPath)` and drew it through `Canvas.nativeCanvas`; avoids a composable-only API and any opt-in surprise. |
| 27 | `ButtonGroup` for the three buckets | Present but its signature takes a `ButtonGroupMenuState` receiver, an overflow indicator and a `ButtonGroupScope` — it is a *connected overflow menu*, not a segmented control | **Not used.** The three buckets are stacked sections with drag-to-move plus an explicit "move to" menu, which is fewer taps than a segmented control and works with TalkBack. |
| 28 | `SplitButton` for Start/Snooze | `SplitButtonLayout(leadingButton, trailingButton, …)` present | Used for **Start 25 minutes / Done** (the two most frequent actions sit together); Snooze is a text button beneath — logged as a deviation. |
| 29 | `FloatingActionButtonMenu` for capture modes | Present | **Not used.** The capture field is pinned on screen, so a FAB menu would add one tap to the single most important action. Voice capture is a "Speak" button next to the field instead. |
| 30 | `LinearWavyProgressIndicator` for the day budget | **Absent** from 1.5.0-beta02 (`ProgressIndicatorKt` has no wavy member) | Substituted `LinearProgressIndicator`, keeping the intent (a single calm bar for planned-vs-awake time). |
| 31 | `LoadingIndicator` for indeterminate states | Present | **Not used** — no operation in the app waits long enough to need one; adding it would be decoration. |
| 32 | Expressive/emphasized type styles | `Typography` does expose `…Emphasized` getters | Used heavier `FontWeight.SemiBold` on headings rather than the emphasized variants, to keep every call site on stable type APIs. |
| 33 | Calm mode + system "remove animations" | `Settings.Global.ANIMATOR_DURATION_SCALE` | `MotionPolicy` reads the system scale on every resume, clamps every animation to 400 ms, halves them in calm mode, and returns `snap()` at scale 0. |

## Notification policy

| # | Decision | Reason |
|---|----------|--------|
| 34 | Exactly one ongoing notification, `setOngoing(true)` + `FLAG_NO_CLEAR`, `IMPORTANCE_LOW`, silent, rebuilt on every data change. | It is a reference line, not an alert; a silent always-there line beats a loud sometimes-there one. |
| 35 | Android 14+ can still let a user dismiss a non-foreground ongoing notification; `DailyWorker` (every 6 h) and every data change re-post it. | Claiming absolute un-dismissability would require a foreground service and a permanent "running" chip; re-posting is the honest, lower-cost trade. |
| 36 | Max 3 timed pings, max 2 escalations, hard ceiling 5/day, all enforced by pure `NotificationPolicy` and persisted in DataStore. | The budget is the feature: a number the user can trust is the difference between "useful" and "muted". |
| 37 | Escalation only for tasks with `isHardDeadline`, and only while the task is still open. | Escalating non-deadline tasks is how a helpful app becomes a nag. |
| 38 | Exact alarms via `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`, else `setAndAllowWhileIdle`. | Never crash or silently drop the anchor because a permission is missing. |
| 39 | Quiet hours default 21:30–08:00 and "quiet for today" are checked before every ping, and quiet-for-today is stored as a day key. | Quiet has to be one tap and self-expiring, or it never gets used. |

## Platform and data

| # | Decision | Reason |
|---|----------|--------|
| 40 | Share target writes straight to Later and finishes with no UI. | Sharing into an app that then shows a form is not "instant capture". |
| 41 | Quick Settings tile opens a floating capture activity (`excludeFromRecents`, keyboard up). | One tap from inside any app; no navigation into the app shell. |
| 42 | Voice capture through `RecognizerIntent`, not `SpeechRecognizer`. | No `RECORD_AUDIO` permission, no microphone lifecycle to babysit, and the system UI already handles "no speech". |
| 43 | Brown noise generated with `AudioTrack` + a leaky integrator, 44.1 kHz mono, 0.5 s fade-in, no assets. | Zero binary assets, no licensing, and the fade-in stops start-up from being a jolt. |
| 44 | Brown noise stops when the focus screen is disposed; the completed minutes are still logged. | A background audio service would need a foreground-service notification, which would break the "exactly one notification" rule. |
| 45 | Focus timer derives elapsed time from `SystemClock.elapsedRealtime()`, not from a tick counter. | Backgrounding cannot make the session drift. |
| 46 | Widget checkbox is a Glance `Box` + `actionRunCallback`, not the `Checkbox` widget. | Guarantees the tap completes the task through the repository and re-renders, on every launcher. |
| 47 | No INTERNET permission in the manifest. | The app is offline by construction; airplane mode is a supported state, not a degraded one. |
| 48 | No foreground service, no analytics SDK, no ads, no login. | Every one of those is a maintenance or trust cost with no upside for one user. |
| 49 | `error` color role set to neutral grey in the `ColorScheme`. | Even an accidental use of the error colour cannot paint a red pixel. |
| 50 | Palette lives as `Long` ARGB constants so `PaletteTest` can assert "no red" and accent contrast without an Android runtime. | Makes the "zero red pixels" rule a test rather than a promise. |
