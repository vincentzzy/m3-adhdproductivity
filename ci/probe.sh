#!/usr/bin/env bash
# One-shot API discovery: downloads the exact artifacts we pin and dumps the
# public API surface we intend to call. Guarantees we never guess a new
# Material Expressive / Glance signature.
set -u
OUT="${PROBE_OUT:-/tmp/probe-report.txt}"
: > "$OUT"
dump() { "$@" 2>&1 | head -n "${DUMP_LINES:-60}" | tee -a "$OUT"; }
log()  { echo "$@" | tee -a "$OUT"; }
fetch() { curl -sfL "$1" -o "$2" && log "OK $2 $(stat -c%s "$2")" || log "FAIL $1"; }
G=https://dl.google.com/dl/android/maven2
WORK=/tmp/probe; rm -rf "$WORK"; mkdir -p "$WORK"; cd "$WORK"

fetch $G/androidx/compose/material3/material3-android/1.4.0/material3-android-1.4.0.aar m3.aar
fetch $G/androidx/graphics/graphics-shapes-android/1.0.1/graphics-shapes-android-1.0.1.aar gs.aar
fetch $G/androidx/glance/glance-appwidget/1.1.1/glance-appwidget-1.1.1.aar glance.aar
fetch $G/androidx/glance/glance-material3/1.1.1/glance-material3-1.1.1.aar glancem3.aar
fetch $G/androidx/compose/compose-bom/2026.06.01/compose-bom-2026.06.01.pom bom.pom

for a in m3 gs glance glancem3; do
  if [ -f $a.aar ]; then mkdir -p $a && (cd $a && unzip -qo ../$a.aar classes.jar 2>/dev/null); fi
done
J3=$WORK/m3/classes.jar; JGS=$WORK/gs/classes.jar; JG=$WORK/glance/classes.jar; JGM=$WORK/glancem3/classes.jar

log "===== BOM pins (compose ui / material3)"
grep -A2 -E "artifactId>(ui|material3|foundation|runtime)<" bom.pom | grep -E "artifactId|version" | paste - - | head -8

log "===== material3: expressive-related top-level classes"
unzip -l $J3 2>/dev/null | grep -oE 'androidx/compose/material3/[A-Za-z0-9_]+\$?[A-Za-z0-9_]*\.class' | sed 's/\.class//' | grep -iE 'expressive|motion|shapes|buttongroup|splitbutton|wavy|loading|fabmenu|togglebutton' | sort -u
log "===== MaterialShapes (public members)"
javap -public -classpath $J3 androidx.compose.material3.MaterialShapes 2>&1 | tee -a "$OUT"
log "===== MaterialTheme (public members)"
dump javap -public -classpath $J3 androidx.compose.material3.MaterialTheme
log "===== MaterialExpressiveTheme signatures"
dump javap -public -classpath $J3 androidx.compose.material3.MaterialExpressiveThemeKt
log "===== MotionScheme + MotionSchemeKt"
dump javap -public -classpath $J3 androidx.compose.material3.MotionScheme
dump javap -public -classpath $J3 androidx.compose.material3.MotionSchemeKt
log "===== dynamic color entry points"
dump javap -public -classpath $J3 androidx.compose.material3.DynamicTonalPaletteKt
log "===== ButtonGroup"
dump javap -public -classpath $J3 androidx.compose.material3.ButtonGroupKt
log "===== SplitButton"
dump javap -public -classpath $J3 androidx.compose.material3.SplitButtonKt
log "===== FloatingActionButtonMenu"
dump javap -public -classpath $J3 androidx.compose.material3.FloatingActionButtonMenuKt
log "===== ToggleButton"
unzip -l $J3 | grep -oE 'androidx/compose/material3/ToggleButton[A-Za-z0-9_]*\.class' | sort -u
log "===== Wavy progress indicator"
javap -public -classpath $J3 androidx.compose.material3.ProgressIndicatorKt 2>&1 | grep -iE "wavy" | head -12 | tee -a "$OUT"
log "===== LoadingIndicator"
dump javap -public -classpath $J3 androidx.compose.material3.LoadingIndicatorKt
log "===== graphics-shapes: classes"
unzip -l $JGS | grep -oE 'androidx/graphics/shapes/[A-Za-z0-9_]+\.class' | sort -u
log "===== Morph"
dump javap -public -classpath $JGS androidx.graphics.shapes.Morph
log "===== MorphKt"
dump javap -public -classpath $JGS androidx.graphics.shapes.MorphKt
log "===== RoundedPolygon"
dump javap -public -classpath $JGS androidx.graphics.shapes.RoundedPolygon
log "===== glance-appwidget classes"
unzip -l $JG | grep -oE 'androidx/glance/appwidget/[A-Za-z0-9_]+\.class' | sort -u | head -70 | tee -a "$OUT"
log "===== glance action classes"
unzip -l $JG | grep -oE 'androidx/glance/action/[A-Za-z0-9_]+\.class' | sort -u | head -20 | tee -a "$OUT"
log "===== GlanceTheme (glance-material3)"
dump javap -public -classpath $JGM androidx.glance.material3.GlanceThemeKt
log "===== GlanceAppWidget / Entity / provideContent"
for c in androidx.glance.appwidget.GlanceAppWidget androidx.glance.appwidget.GlanceAppWidgetReceiver androidx.glance.GlanceModifier; do dump javap -public -classpath "$JG:$JGM" $c; done
log "DONE"
