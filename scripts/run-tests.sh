#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

mapfix_jdk="${MAPFIX_JDK:-/wwpdb/onedep/resources/tools/tools-centos-7/packages/java/jdk-14.0.2}"
javac_bin="$mapfix_jdk/bin/javac"
java_bin="$mapfix_jdk/bin/java"

if [[ ! -x "$javac_bin" || ! -x "$java_bin" ]]; then
  echo "Missing JDK at $mapfix_jdk (override with MAPFIX_JDK)" >&2
  exit 1
fi

# The integration tests exercise the real jar, so build it first.
./scripts/build-mapfixdep.sh

junit_jar="$(ls lib/junit-platform-console-standalone-*.jar | head -1)"
test_classes_dir="${MAPFIX_TEST_CLASSES_DIR:-build/test-classes}"

rm -rf "$test_classes_dir"
mkdir -p "$test_classes_dir"

"$javac_bin" -cp "lib/json-20210307.jar:$junit_jar" -d "$test_classes_dir" \
  msdmap/*.java msdmap/mapread/*.java test/msdmap/mapread/*.java

"$java_bin" -jar "$junit_jar" \
  -cp "$test_classes_dir:lib/json-20210307.jar" \
  --select-package msdmap.mapread \
  --disable-banner
