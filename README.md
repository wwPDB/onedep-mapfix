# MapFix

MapFix reads, validates, and fixes CCP4/MRC electron-microscopy map headers. This is
the official wwPDB repository for MapFix: OneDep's build pulls `MapFix` directly from
here rather than from the legacy SVN source tree.

## Label Handling Behavior

`mapFixDep.jar` receives a system label (a deposition ID such as `D_1234567890`, later
replaced by the final EMDB accession such as `EMD-112358`) through `-label <id>` and
writes it as line 1 of the MRC header, wrapped as:

```text
::::EMDATABANK.org::::<id>::::
```

Rules for the remaining label lines:

- Any depositor-authored label lines already present (e.g. `Relion ...`,
  `ChimeraX ...`) are preserved and shifted down, starting at line 2, in their original
  order.
- If a map already carries a system label from an earlier MapFix run (matching the
  `::::...::::` wrapper above), that line is recognized and replaced rather than
  shifted down as if it were depositor content — this is what lets the final EMDB
  accession cleanly replace an earlier deposition ID without duplicating or losing
  real depositor lines.
- The MRC format caps headers at 10 label lines. If the system label plus preserved
  depositor lines would exceed that (an 11th line would be needed), the very first
  depositor line is guaranteed a spot on line 10 rather than being silently dropped,
  and line 9 is annotated in place — its own real content, ellipsis-truncated only as
  far as needed, followed by a `[TRUNCATED: N more line(s) removed]` notice — instead
  of sacrificing a whole line purely for a notice.
- An existing label identical to the new system label is not duplicated.

This does not change upload milestone semantics or any OneDep Python workflow.

## Build

Build inside the OneDep Vagrant VM or any environment with the same toolchain layout:

```bash
./scripts/build-mapfixdep.sh
```

The script uses:

```text
/wwpdb/onedep/resources/tools/tools-centos-7/packages/java/jdk-14.0.2
```

Override with `MAPFIX_JDK=/path/to/jdk` if needed.

The rebuilt jar is written to:

```text
build/mapFixDep.jar
```

Generated jars are intentionally not committed.

## Testing

```bash
./scripts/run-tests.sh
```

This builds `mapFixDep.jar` and runs the full JUnit suite: `MapHeaderTest` (unit tests
for the label-handling logic above, including the overflow and system-label-replacement
scenarios) and `MapFixDepIntegrationTest` (runs the real jar end-to-end against the
sample fixtures below via `ProcessBuilder` and checks the resulting labels). Override
`MAPFIX_JDK` the same way as for the build.

To inspect any map's header labels by hand, use the existing `mapTest.jar` (see
"Other Executables" below):

```bash
java -jar mapTest.jar <file>
```

## Sample Data

Real downloaded MRC map examples used by `MapFixDepIntegrationTest` live in
`sample-data/` and are tracked with Git LFS. See `sample-data/README.md` and
`sample-data/manifest.tsv`. The overflow and system-label-replacement scenarios don't
need committed files — `MapFixDepIntegrationTest` builds those fixtures on the fly.

## Other Executables (MakeAll)

Besides `mapFixDep.jar` (built by `scripts/build-mapfixdep.sh` for the OneDep
integration above), the source tree also builds a set of standalone executables via
the original `MakeAll` script:

```bash
./MakeAll
```

- `MapFix` / `MapFixBig`: general-purpose header fix/rewrite tools (`MapFixBig` for
  huge maps with limited functionality).
- `MapTest` / `MapTestBig`: read-only header/data inspection tools.
- `MapFixAnot`: header fix tool used by the annotation (D&A) project.

Run them the same way as `mapFixDep.jar`, e.g.:

```bash
java -jar mapFix.jar -in testmap/normal.map -out crap.map -all
java -jar mapTest.jar testmap/normal.map
```

`-help` on any of them lists their full set of run-time parameters.

## Integration with OneDep

This repository is the canonical source `MapFix` is built from: OneDep's build
consumes `mapFixDep.jar` directly from here, so a change merged here is a change
merged into OneDep — no separate SVN port-back step.
