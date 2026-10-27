#!/usr/bin/env bash
set -u
OUT="${PROBE_OUT:-/tmp/probe-report.txt}"; : > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
j() { javap -public -classpath "$1" "$2" 2>&1 | head -n "${3:-40}" | tee -a "$OUT"; }
G=https://dl.google.com/dl/android/maven2
WORK=/tmp/probe; rm -rf "$WORK"; mkdir -p "$WORK"; cd "$WORK"
V=1.5.0-beta02
curl -sfL "$G/androidx/compose/material3/material3-android/$V/material3-android-$V.aar" -o m3.aar && log "OK m3 $V $(stat -c%s m3.aar)" || log "FAIL m3"
mkdir -p m3 && (cd m3 && unzip -qo ../m3.aar classes.jar)
J3=$WORK/m3/classes.jar
log "===== BOM 2026.09.00 pins"
curl -s "$G/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom" | grep -E "<artifactId>(ui|material3|foundation|runtime)</artifactId>" -A1 | grep -E "artifactId|version" | paste - - | tee -a "$OUT"
log "===== expressive class presence in $V"
jar tf $J3 | grep -oE 'androidx/compose/material3/(MaterialShapes|MaterialExpressiveThemeKt|ButtonGroupKt|SplitButtonKt|FloatingActionButtonMenuKt|LoadingIndicatorKt|ToggleButtonKt|ShapeKt|MotionSchemeKt)[^/]*\.class' | sort -u | tee -a "$OUT"
log "===== MaterialShapes"
j $J3 androidx.compose.material3.MaterialShapes 80
log "===== MaterialExpressiveTheme"
j $J3 androidx.compose.material3.MaterialExpressiveThemeKt 20
log "===== dynamicExpressive colors"
javap -public -classpath $J3 androidx.compose.material3.DynamicTonalPaletteKt 2>&1 | grep -i -E "expressive" | tee -a "$OUT"
log "===== ButtonGroup"
j $J3 androidx.compose.material3.ButtonGroupKt 30
log "===== ToggleButton"
j $J3 androidx.compose.material3.ToggleButtonKt 20
log "===== SplitButton"
j $J3 androidx.compose.material3.SplitButtonKt 40
log "===== FloatingActionButtonMenu"
j $J3 androidx.compose.material3.FloatingActionButtonMenuKt 20
log "===== wavy progress"
javap -public -classpath $J3 androidx.compose.material3.ProgressIndicatorKt 2>&1 | grep -i wavy | head -8 | tee -a "$OUT"
log "===== LoadingIndicator"
j $J3 androidx.compose.material3.LoadingIndicatorKt 10
log "===== Typography emphasized"
javap -public -classpath $J3 androidx.compose.material3.Typography 2>&1 | grep -iE "emphasi" | head -12 | tee -a "$OUT"
log "===== MotionScheme companion"
j $J3 'androidx.compose.material3.MotionScheme$Companion' 14
log "===== graphics-shapes 1.1.0"
curl -sfL "$G/androidx/graphics/graphics-shapes-android/1.1.0/graphics-shapes-android-1.1.0.aar" -o gs.aar && mkdir -p gs && (cd gs && unzip -qo ../gs.aar classes.jar) && log "OK gs"
JGS=$WORK/gs/classes.jar
jar tf $JGS | grep -oE 'androidx/graphics/shapes/[A-Za-z0-9_]+\.class' | sort -u | head -25 | tee -a "$OUT"
log "--- toPath / interop"
for c in $(jar tf $JGS | grep -oE 'androidx/graphics/shapes/[A-Za-z0-9_]+Kt\.class' | sed 's#/#.#g;s/\.class//'); do echo "== $c"; javap -public -classpath $JGS "$c" 2>&1 | head -12 | tee -a "$OUT"; done
log "===== glance 1.2.0-rc01 vs 1.1.1 checkbox/actionRunCallback"
curl -sfL "$G/androidx/glance/glance-appwidget/1.2.0-rc01/glance-appwidget-1.2.0-rc01.aar" -o g2.aar && mkdir -p g2 && (cd g2 && unzip -qo ../g2.aar classes.jar) && log "OK glance 1.2.0-rc01"
JG2=$WORK/g2/classes.jar
javap -public -classpath $JG2 androidx.glance.appwidget.CheckBoxKt 2>&1 | head -10 | tee -a "$OUT"
javap -public -classpath $JG2 androidx.glance.appwidget.action.ActionCallbackKt 2>&1 | head -10 | tee -a "$OUT"
jar tf $JG2 | grep -iE "appwidget/action/.*(Toggle|Callback|Action)" | head -12 | tee -a "$OUT"
log "===== glance state/GlanceModifier clickable"
jar tf $JG2 | grep -E "androidx/glance/(state|action)/" | head -30 | tee -a "$OUT"
log DONE
