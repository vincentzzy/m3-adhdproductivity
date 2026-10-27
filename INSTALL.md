# Install TASKD on Android

## Download the APK

1. Open this repository on GitHub and select the latest successful **Android debug build** under **Actions**.
2. In **Artifacts**, download `TASKD-debug-apk`. GitHub downloads it as a ZIP file.
3. On your phone, open the downloaded ZIP in Files and extract `app-debug.apk`.
   - If a GitHub Release is available, you can instead download `app-debug.apk` directly from that release's Assets section.

## Allow this one-time install

1. Tap `app-debug.apk` in Files.
2. If Android blocks it, choose **Settings** on the warning and enable **Allow from this source** for the app you used to open the APK (usually Files or your browser).
3. Go back and tap **Install**.
4. Open TASKD and allow notifications if you want its persistent task reminder. You can deny this and still use the app.

The setting applies only to the source app you selected. You can turn it off again under Android Settings → Apps → Special app access → Install unknown apps. The APK is a debug build and is intended for personal use; it is not Play Protect certified or signed as a public store release.

## Add the quick capture tile and widget

- To add the Quick Settings tile: swipe down twice, tap Edit, and drag **Capture a task** into the active tiles.
- To add the home widget: long-press an empty part of the home screen, choose Widgets, find TASKD, then drag its widget onto the screen.
