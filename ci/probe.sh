#!/usr/bin/env bash
# Version discovery + API surface probe.
set -u
OUT="${PROBE_OUT:-/tmp/probe-report.txt}"; : > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
G=https://dl.google.com/dl/android/maven2
WORK=/tmp/probe; rm -rf "$WORK"; mkdir -p "$WORK"; cd "$WORK"
vers() { echo "--- $1"; curl -s "$G/$1/maven-metadata.xml" | grep -oE '<version>[^<]+' | sed 's/<version>//' | tail -14 | tr '\n' ' ' | tee -a "$OUT"; echo | tee -a "$OUT"; }
for p in compose/material3/material3 room/room-runtime glance/glance-appwidget datastore/datastore-preferences work/work-runtime-ktx graphics/graphics-shapes graphics/graphics-shapes-android compose/compose-bom lifecycle/lifecycle-runtime-compose lifecycle/lifecycle-viewmodel-compose activity/activity-compose core/core-ktx navigation/navigation-compose; do vers "androidx/$p"; done
log "===== BOM 2026.06.01 pins"
curl -s "$G/androidx/compose/compose-bom/2026.06.01/compose-bom-2026.06.01.pom" | grep -B1 -A1 -E "<artifactId>(ui|material3|foundation|runtime)</artifactId>" | grep -E "artifactId|version" | paste - - | tee -a "$OUT"
log "===== expressive class presence per material3 version"
probe_m3() {
  v=$1
  curl -sfL "$G/androidx/compose/material3/material3-android/$v/material3-android-$v.aar" -o "m3-$v.aar" || { log "$v DOWNLOAD-FAIL"; return; }
  mkdir -p "m3-$v" && (cd "m3-$v" && unzip -qo "../m3-$v.aar" classes.jar)
  list=$(jar tf "m3-$v/classes.jar" 2>/dev/null)
  hits=""
  for k in MaterialShapes MaterialExpressiveTheme ButtonGroupKt SplitButtonKt FloatingActionButtonMenuKt LoadingIndicatorKt LinearWavyProgressIndicator MotionSchemeKt MaterialTheme; do
    if echo "$list" | grep -q "androidx/compose/material3/$k"; then hits="$hits $k"; fi
  done
  log "$v =>$hits"
}
for v in 1.4.0 1.4.1 1.5.0 1.5.1 1.5.2 1.6.0-alpha01; do probe_m3 $v; done
log "===== MaterialShapes members (newest version that has it)"
for v in 1.6.0-alpha01 1.5.2 1.5.1 1.5.0; do
  if [ -f "m3-$v/classes.jar" ] && jar tf "m3-$v/classes.jar" 2>/dev/null | grep -q MaterialShapes; then
    log "using $v"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.MaterialShapes 2>&1 | tee -a "$OUT"
    log "--- MaterialExpressiveThemeKt"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.MaterialExpressiveThemeKt 2>&1 | head -20 | tee -a "$OUT"
    log "--- dynamicExpressive colors"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.DynamicTonalPaletteKt 2>&1 | grep -i expressive | tee -a "$OUT"
    log "--- ButtonGroupKt"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.ButtonGroupKt 2>&1 | head -24 | tee -a "$OUT"
    log "--- SplitButtonKt"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.SplitButtonKt 2>&1 | head -30 | tee -a "$OUT"
    log "--- FloatingActionButtonMenuKt"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.FloatingActionButtonMenuKt 2>&1 | head -16 | tee -a "$OUT"
    log "--- wavy + loading"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.ProgressIndicatorKt 2>&1 | grep -i wavy | head -6 | tee -a "$OUT"
    javap -public -classpath "m3-$v/classes.jar" androidx.compose.material3.LoadingIndicatorKt 2>&1 | head -8 | tee -a "$OUT"
    log "--- MotionScheme companion + shapes conversion"
    javap -public -classpath "m3-$v/classes.jar" 'androidx.compose.material3.MotionScheme$Companion' 2>&1 | head -12 | tee -a "$OUT"
    jar tf "m3-$v/classes.jar" | grep -iE "material3/(Shape|Motion)[A-Za-z]*Kt\.class" | head -10 | tee -a "$OUT"
    break
  fi
done
log "DONE"
