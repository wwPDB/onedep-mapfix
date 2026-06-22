#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 5 || $# -gt 6 ]]; then
  echo "Usage: $0 <input.map> <D_label> <voxel_x> <voxel_y> <voxel_z> [output.map]" >&2
  exit 2
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

input_path="$1"
label_arg="$2"
voxel_x="$3"
voxel_y="$4"
voxel_z="$5"
output_path="${6:-output/$(basename "$input_path").converted.map}"

mapfix_jar="${MAPFIX_JAR:-build/mapFixDep.jar}"

if [[ ! -f "$mapfix_jar" ]]; then
  echo "Missing $mapfix_jar. Run ./scripts/build-mapfixdep.sh first." >&2
  exit 1
fi

mkdir -p "$(dirname "$output_path")"

java -Xms256m -Xmx256m -jar "$mapfix_jar" \
  -in "$input_path" \
  -out "$output_path" \
  -voxel "$voxel_x" "$voxel_y" "$voxel_z" \
  -label "$label_arg"

echo "Wrote $output_path"
