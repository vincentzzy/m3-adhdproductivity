#!/usr/bin/env sh
# Minimal Gradle bootstrap. The binary wrapper JAR is intentionally not
# committed (this sandbox cannot fetch it); the pinned distribution URL in
# gradle/wrapper/gradle-wrapper.properties is authoritative, and ordinary
# `gradle wrapper` can regenerate a canonical wrapper at any time.
set -eu
VERSION=9.3.1
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${VERSION}-bin/now"
GRADLE="$BASE/gradle-${VERSION}/bin/gradle"
if [ ! -x "$GRADLE" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-${VERSION}-bin.zip"
  echo "Downloading pinned Gradle ${VERSION}..."
  curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip" -o "$ZIP"
  unzip -q -o "$ZIP" -d "$BASE"
  rm -f "$ZIP"
fi
exec "$GRADLE" "$@"
