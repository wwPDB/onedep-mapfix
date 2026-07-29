# Contributing

## Build

```bash
./scripts/build-mapfixdep.sh
```

Builds `build/mapFixDep.jar`, the jar OneDep's build consumes directly from this repo.
Uses the JDK at `/wwpdb/onedep/resources/tools/tools-centos-7/packages/java/jdk-14.0.2`
by default; override with `MAPFIX_JDK=/path/to/jdk` for local development.

## Test

```bash
./scripts/run-tests.sh
```

Builds the jar, then runs the full JUnit suite: `MapHeaderTest` (unit tests for the
label-handling logic in `msdmap/mapread/MapHeader.java`) and
`MapFixDepIntegrationTest` (end-to-end, runs the real jar against the fixtures in
`sample-data/`). Run this before submitting any change to `MapHeader.changeLabel` or
related label-handling code — see the "Label Handling Behavior" section of
`README.md` for the rules those tests enforce.

To inspect a map's header labels by hand, use `java -jar mapTest.jar <file>` (see
"Other Executables" in `README.md`).

## Versioning

`msdmap/Version.java` carries an informal version-date banner, printed on every run.
Now that this is an official repository, pair any future bump to that date with a git
tag for the corresponding release, rather than relying on the date string alone.

## Other executables

The `MakeAll` script builds the other standalone tools (`MapFix`, `MapFixBig`,
`MapTest`, `MapTestBig`, `MapFixAnot`) that ship alongside `mapFixDep.jar` — see
"Other Executables (MakeAll)" in `README.md`.
