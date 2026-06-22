#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

mapfix_jdk="${MAPFIX_JDK:-/wwpdb/onedep/resources/tools/tools-centos-7/packages/java/jdk-14.0.2}"
javac_bin="$mapfix_jdk/bin/javac"
jar_bin="$mapfix_jdk/bin/jar"

if [[ ! -x "$javac_bin" ]]; then
  echo "Missing javac: $javac_bin" >&2
  exit 1
fi

if [[ ! -x "$jar_bin" ]]; then
  echo "Missing jar: $jar_bin" >&2
  exit 1
fi

build_dir="${MAPFIX_BUILD_DIR:-$repo_root/build}"
classes_dir="$build_dir/classes"
jar_path="${MAPFIX_JAR:-$build_dir/mapFixDep.jar}"

rm -rf "$classes_dir"
mkdir -p "$classes_dir"

"$javac_bin" --release 8 -cp lib/json-20210307.jar -d "$classes_dir" \
  msdmap/*.java msdmap/mapread/*.java

mkdir -p "$(dirname "$jar_path")"
mkdir -p "$(dirname "$jar_path")/lib"
cp lib/json-20210307.jar "$(dirname "$jar_path")/lib/json-20210307.jar"

"$jar_bin" -cmvf META-INF/MANIFEST.FixDep.MF "$jar_path" \
  -C "$classes_dir" msdmap

echo "Built $jar_path"
