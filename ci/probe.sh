#!/usr/bin/env bash
# One-shot API discovery: downloads the exact artifacts we pin and dumps the
# public API we intend to call, so we never guess an expressive API signature.
set -u
OUT=/tmp/probe-report.txt
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
G=https://dl.google.com/dl/android/maven2
mkdir -p /tmp/probe && cd /tmp/probe
get() { curl -sfL "$1" -o "$2" && log "OK $2 $(stat -c%s "$2")" || log "FAIL $1"; }
get $G/androidx/compose/material3/material3-android/1.4.0/material3-android-1.4.0.aar m3.aar
get $G/androidx/graphics/graphics-shapes/1.0.1/graphics-shapes-1.0.1.aar gs.aar
get $G/androidx/glance/glance-appwidget/1.1.1/glance-appwidget-1.1.1.aar glance.aar
log "===== ENV"
log "which javap: $(command -v javap || echo MISSING)"
log "which unzip: $(command -v unzip || echo MISSING)"
java -version 2>&1 | head -3 | tee -a "$OUT"
for f in m3.aar gs.aar glance.aar; do log "--- $f: $(file -b "$f" 2>/dev/null)"; done
log "--- m3.aar top entries"; unzip -l m3.aar 2>&1 | head -12 | tee -a "$OUT"
log "--- gs.aar top entries"; unzip -l gs.aar 2>&1 | head -12 | tee -a "$OUT"
for a in m3 gs glance; do
  if [ -f $a.aar ]; then mkdir -p $a && (cd $a && unzip -qo ../$a.aar classes.jar 2>/dev/null && log "unzipped $a"); fi
done
J3=/tmp/probe/m3/classes.jar; JGS=/tmp/probe/gs/classes.jar; JG=/tmp/probe/glance/classes.jar
log "===== MATERIAL3 interesting classes"
unzip -l $J3 2>/dev/null | grep -oE 'androidx/compose/material3/[A-Za-z0-9_]+Kt\.class' | grep -iE 'expressive|dynamic|motion|shape|buttongroup|splitbutton|wavy|loading|fabmenu|toggle|theme' | sort -u
log "===== MaterialShapes"
javap -classpath $J3 androidx.compose.material3.MaterialShapes 2>&1 | head -40
log "===== MaterialThemeKt"
javap -classpath $J3 androidx.compose.material3.MaterialThemeKt 2>&1 | head -30
log "===== MotionScheme"
javap -classpath $J3 androidx.compose.material3.MotionScheme 2>&1 | head -30
log "===== ButtonGroup"
javap -classpath $J3 'androidx.compose.material3.ButtonGroupKt' 2>&1 | head -30
log "===== SplitButton"
javap -classpath $J3 'androidx.compose.material3.SplitButtonKt' 2>&1 | head -40
log "===== FloatingActionButtonMenu"
javap -classpath $J3 'androidx.compose.material3.FloatingActionButtonMenuKt' 2>&1 | head -20
log "===== Wavy progress"
javap -classpath $J3 androidx.compose.material3.ProgressIndicatorKt 2>&1 | grep -i wavy | head
log "===== LoadingIndicator"
javap -classpath $J3 androidx.compose.material3.LoadingIndicatorKt 2>&1 | head -20
log "===== graphics-shapes Morph"
javap -classpath $JGS androidx.graphics.shapes.Morph 2>&1 | head -20
log "===== graphics-shapes MorphKt"
javap -classpath $JGS androidx.graphics.shapes.MorphKt 2>&1 | head -20
log "===== graphics-shapes classes"
unzip -l $JGS 2>/dev/null | grep -oE 'androidx/graphics/shapes/[A-Za-z0-9_]+\.class' | sort -u | head -20
log "===== glance appwidget classes"
unzip -l $JG 2>/dev/null | grep -oE 'androidx/glance/appwidget/[A-Za-z0-9_]+\.class' | sort -u | head -60
log "===== glance action classes"
unzip -l $JG 2>/dev/null | grep -oE 'androidx/glance/action/[A-Za-z0-9_]+\.class' | sort -u | head -30
log "===== dynamic color (material-color-utilities)"
get $G/androidx/compose/material3/material3-android/1.4.0/material3-android-1.4.0.pom m3.pom
javap -classpath $J3 androidx.compose.material3.DynamicTonalPaletteKt 2>&1 | head -20
