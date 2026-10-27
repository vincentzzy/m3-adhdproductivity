# TASKD

A local-first Android task app designed around one decision at a time: **NOW** (one task), **TODAY** (up to three), and quiet **LATER**. The app intentionally has no projects, tags, streak penalties, red overdue state, account, or required network access.

Alternatives considered: **Nextlight** and **Smallstep**. Chosen name: **TASKD**, because it is short and easy to find in the launcher.

## Architecture

- **UI:** single-activity Jetpack Compose app; default Now screen and bottom navigation to Time and Progress.
- **Domain:** `Rules` contains pure bucket-cap, budget, multiplier, streak-grace, and breakdown behavior. Unit tests exercise the central arithmetic/anti-abandonment rules.
- **Persistence:** Room stores tasks, focus sessions, anchors, and streak state. A small repository enforces the NOW/TODAY limits. Preferences use DataStore.
- **Platform:** Android notification actions, a Quick Settings capture tile, plain-text share target, and a Glance widget launch capture/app.
- **Offline:** no `INTERNET` permission or remote service. Speech recognition is requested from the installed system recognizer and may itself need a device-provided service.
- **Design:** dark charcoal surfaces, one lavender accent, large touch targets, calm-mode control, no cold-start motion, and a single focus-dial silhouette transition.

## Build

- Java 17, Android SDK platform 37, minSdk 26, targetSdk 37.
- Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10 (AGP built-in Kotlin plus the Kotlin Compose plugin), KSP 2.2.10-2.0.2.
- Dependency versions are pinned in `gradle/libs.versions.toml` (Compose BOM 2026.09.00, Material 3 1.4.0, Room 2.7.0, DataStore 1.1.4, WorkManager 2.10.0, Glance 1.1.1, graphics-shapes 1.0.1, among others).
- Run `./gradlew assembleDebug` and `./gradlew test`. The CI workflow builds the debug APK and uploads it as an artifact on pushes to main; when a GitHub Release is published, it also attaches the APK.
- `gradlew` is a small shell bootstrap for the pinned distribution because the runner could not retrieve the standard wrapper JAR. It needs a JDK, curl, and unzip.

## Product coverage and known gaps

This is an MVP scaffold, not a claim that every item of the much larger request is complete. The functioning core is local task capture, enforced bucket placement, completion, voice/share/tile entry points, a persistent notification surface, basic time/progress screens, adjustable focus entry, procedural noise, and pure tested helper rules. The known omissions are recorded in [`DECISIONS.md`](DECISIONS.md): notably interactive task rows in the widget, timed/escalation pings, persisted focus timing, task estimate logging, streak UI updates, welcome-back copy, and JSON export/import. No build or device test has been run in this workspace because it has no JDK, Gradle installation, or Android SDK; CI must validate compilation before distributing an APK.

## Full file tree

```text
.
├── .gitignore
├── .github/workflows/build.yml
├── app
│   ├── build.gradle.kts
│   └── src
│       ├── main
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/taskd/app
│       │   │   ├── MainActivity.kt
│       │   │   ├── data
│       │   │   │   ├── TaskData.kt
│       │   │   │   └── TaskRepository.kt
│       │   │   ├── domain/Rules.kt
│       │   │   ├── platform
│       │   │   │   ├── CaptureTileService.kt
│       │   │   │   ├── Notifications.kt
│       │   │   │   └── QuickCaptureActivity.kt
│       │   │   ├── ui/TaskdApp.kt
│       │   │   └── widget/TaskdWidget.kt
│       │   └── res
│       │       ├── values/{strings.xml,styles.xml}
│       │       └── xml/taskd_widget_info.xml
│       └── test/java/com/taskd/app/domain/RulesTest.kt
├── gradle
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── gradlew
├── INSTALL.md
├── README.md
└── settings.gradle.kts
```

## Decisions

See [`DECISIONS.md`](DECISIONS.md) for the one-line rationale for each product, design, dependency, API, and verification choice.
