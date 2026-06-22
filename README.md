# MapFix Label Prepend POC

This repository is a local proof of concept for preserving depositor-provided
MRC header labels during OneDep EM map conversion.

The imported Java source comes from the MapFix package bundled with the local
OneDep development VM. The POC change is intentionally small so RCSB can review
the diff and port the same behavior into the authoritative SVN source.

## Current Behavior

`mapFixDep.jar` currently receives a deposition label through `-label D_xxxxx`
and rewrites the MRC header label to a single system line:

```text
::::EMDATABANK.org::::D_xxxxx::::
```

That removes depositor provenance such as `Relion ...` or `ChimeraX ...`.

## Proposed Behavior

For converted maps produced by `mapFixDep.jar`:

- label line 1 is the system label
- existing depositor label lines are shifted down
- preservation is best effort within the 10-line MRC label capacity
- an existing identical system label is not duplicated

This does not change upload milestone semantics or any OneDep Python workflow.

## Build

Build inside the OneDep Vagrant VM or any environment with the same toolchain
layout:

```bash
./scripts/build-mapfixdep.sh
```

The script uses:

```text
/wwpdb/onedep/resources/tools/tools-centos-7/packages/java/jdk-14.0.2
```

Override with `MAPFIX_JDK=/path/to/jdk` if needed.

The rebuilt POC jar is written to:

```text
build/mapFixDep.jar
```

Generated jars are intentionally not committed.

## Run Samples

Example using the Relion-labelled sample:

```bash
./scripts/run-sample.sh \
  sample-data/D_1292121466_em-volume-upload_P1.map.V1 \
  D_1292121466 \
  1.2194290161132812 1.2194290161132812 1.2194290161132812
```

The converted file is written under `output/` unless an explicit output path is
passed as the sixth argument.

Inspect labels with:

```bash
python3 scripts/inspect-labels.py output/D_1292121466_em-volume-upload_P1.map.V1.converted.map
```

## Expected Results

Representative expectations:

- Relion input -> system label on line 1, Relion label on line 2
- ChimeraX input -> system label on line 1, ChimeraX label on line 2
- blank-label input -> system label only
- already system-labelled input -> system label only, no duplicate line 2
- multi-line input -> original labels shifted down until capacity is full
- long-label input -> original label text preserved only as far as MRC label capacity allows

## Sample Data

Actual downloaded MRC map examples live in `sample-data/` and are tracked with
Git LFS. See `sample-data/README.md` and `sample-data/manifest.tsv`.

## Handoff Notes

This repo is not intended to be the official integration branch. The intended
handoff is:

1. Review the Java diff in this Git repo.
2. Rebuild and run the sample commands.
3. Port the accepted Java source change into the official RCSB SVN source.
4. Integrate through the normal OneDep MapFix packaging/release path.
