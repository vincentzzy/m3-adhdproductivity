#!/usr/bin/env bash
set -u
OUT="${PROBE_OUT:-/tmp/probe-report.txt}"; : > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
j() { javap -public -classpath "$1" "$2" 2>&1 | head -n "${3:-40}" | tee -a "$OUT"; }
G=https://dl.google.com/dl/android/maven2
W=/tmp/probe; rm -rf $W; mkdir -p $W; cd $W
aar() { # $1 path-without-ext under G e.g. androidx/compose/material3/material3-android/1.5.0-beta02/material3-android-1.5.0-beta02
  local p="$1" n="$2"
  curl -sfL "$G/$p.aar" -o "$n.aar" && mkdir -p "$n" && (cd "$n" && unzip -qo "../$n.aar" classes.jar && unzip -qo "../$n.aar" 'META-INF/com/android/build/gradle/aar-metadata.properties' 2>/dev/null && log "minSdk of $n: $(grep -h minCompileSdk META-INF/com/android/build/gradle/aar-metadata.properties 2>/dev/null)" ) || log "FAIL $p"
}
log "===== aar metadata (minCompileSdk)"
aar androidx/compose/material3/material3-android/1.5.0-beta02/material3-android-1.5.0-beta02 m3
aar androidx/glance/glance-appwidget/1.2.0-rc01/glance-appwidget-1.2.0-rc01 g2
aar androidx/glance/glance-material3/1.2.0-rc01/glance-material3-1.2.0-rc01 gm2
aar androidx/compose/ui/ui-android/1.12.1/ui-android-1.12.1 ui
aar androidx/activity/activity-compose/1.12.4/activity-compose-1.12.4 ac
aar androidx/lifecycle/lifecycle-runtime-compose-android/2.12.0/lifecycle-runtime-compose-android-2.12.0 lrc
aar androidx/room/room-runtime-android/2.8.5/room-runtime-android-2.8.5 room
aar androidx/core/core-ktx/1.19.1/core-ktx-1.19.1 core
log "===== material3 1.5.0-beta02 pom deps (compose versions)"
curl -s "$G/androidx/compose/material3/material3-android/1.5.0-beta02/material3-android-1.5.0-beta02.pom" | grep -E "artifactId|version" | paste - - | grep -E "compose|kotlin" | head -20 | tee -a "$OUT"
J3=$W/m3/classes.jar
log "===== MaterialTheme params"
j $J3 androidx.compose.material3.MaterialThemeKt 12
log "===== all dynamic* color functions"
javap -public -classpath $J3 androidx.compose.material3.DynamicTonalPaletteKt 2>&1 | grep -oE "ColorScheme dynamic[A-Za-z0-9]+" | tee -a "$OUT"
log "===== MotionScheme companion"
j $J3 'androidx.compose.material3.MotionScheme$Companion' 12
log "===== MaterialShapes companion (shapes only)"
javap -public -classpath $J3 'androidx.compose.material3.MaterialShapes$Companion' 2>&1 | grep -E "RoundedPolygon (get)?[A-Z][A-Za-z0-9]*\(" | head -40 | tee -a "$OUT"
log "===== MaterialShapesKt.toShape"
j $J3 androidx.compose.material3.MaterialShapesKt 12
log "===== ToggleButton"
j $J3 androidx.compose.material3.ToggleButtonKt 12
log "===== SplitButton"
javap -public -classpath $J3 androidx.compose.material3.SplitButtonKt 2>&1 | head -20 | tee -a "$OUT"
log "===== ButtonGroup full"
javap -public -classpath $J3 androidx.compose.material3.ButtonGroupKt 2>&1 | head -20 | tee -a "$OUT"
log "===== FAB menu"
javap -public -classpath $J3 androidx.compose.material3.FloatingActionButtonMenuKt 2>&1 | head -14 | tee -a "$OUT"
log "===== wavy + loading signatures"
javap -public -classpath $J3 androidx.compose.material3.ProgressIndicatorKt 2>&1 | grep -i "wavy" | head -6 | tee -a "$OUT"
javap -public -classpath $J3 androidx.compose.material3.LoadingIndicatorKt 2>&1 | head -8 | tee -a "$OUT"
log "===== Typography emphasized sample"
javap -public -classpath $J3 androidx.compose.material3.Typography 2>&1 | grep -iE "getHeadlineLarge|DisplayLarge" | head -6 | tee -a "$OUT"
log "===== glance 1.2.0-rc01: appwidget action + modifier + GlanceTheme"
J2=$W/g2/classes.jar
jar tf $J2 | grep -E "glance/appwidget/action/(Action|ActionKt)" | head | tee -a "$OUT"
jar tf $J2 | grep -E "glance/(GlanceModifier|action/Action)" | head -8 | tee -a "$OUT"
j $W/gm2/classes.jar androidx.glance.material3.GlanceThemeKt 12
javap -public -classpath $J2 androidx.glance.appwidget.GlanceAppWidgetKt 2>&1 | head -12 | tee -a "$OUT"
log "===== glance 1.2.0-rc01 Button/Text/Column/Switcher"
for c in androidx.glance.ButtonKt androidx.glance.text.TextKt androidx.glance.layout.ColumnKt androidx.glance.layout.RowKt androidx.glance.appwidget.AndroidRemoteViewsKt; do echo "-- $c"; javap -public -classpath $J2 $c 2>&1 | head -6 | tee -a "$OUT"; done
log "===== core-ktx latest 1.19.1 minCompileSdk vs 1.17.0"
aar androidx/core/core-ktx/1.17.0/core-ktx-1.17.0 core17
log DONE
