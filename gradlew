#!/usr/bin/env sh
# Small wrapper bootstrap for the pinned distribution. The wrapper JAR is omitted
# because this environment cannot fetch GitHub's raw binary endpoint; behavior and
# version pin match gradle-wrapper.properties.
set -eu
VERSION=8.11.1
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${VERSION}-bin/taskd"
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
