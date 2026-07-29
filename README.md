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
- The MRC format caps headers at 10 label lines. As long as the system label plus
  preserved depositor lines fit within that, nothing special happens. If they don't
  (an 11th line would be needed — only possible when depositor content already fills
  all 10 slots), as many depositor lines as fit are kept untouched, and the last one
  that fits is annotated in place — its own real content, ellipsis-truncated only as
  far as needed, followed by a `[TRUNCATED: N more line(s) removed]` notice — instead
  of sacrificing a whole line purely for a notice or silently dropping whatever
  doesn't fit.
- An existing label identical to the new system label is not duplicated.

This does not change upload milestone semantics or any OneDep Python workflow.

### Examples

**Overflow.** A map whose original depositor content already fills all 10 label slots:

```text
1. Relion reconstruction metadata
2. Depositor note 2
3. Depositor note 3
4. Depositor note 4
5. Depositor note 5
6. Depositor note 6
7. Depositor note 7
8. Depositor note 8
9. Depositor note 9
10. Depositor note 10
```

Running `-label D_1234567890` produces:

```text
Line 1:  ::::EMDATABANK.org::::D_1234567890::::
Line 2:  Relion reconstruction metadata
Line 3:  Depositor note 2
Line 4:  Depositor note 3
Line 5:  Depositor note 4
Line 6:  Depositor note 5
Line 7:  Depositor note 6
Line 8:  Depositor note 7
Line 9:  Depositor note 8
Line 10: Depositor note 9 [TRUNCATED: 1 more line(s) removed]
```

"Depositor note 10" is the one line fully dropped, silently — accounted for only by
the "1 more line(s) removed" count on line 10.

**Pre-existing system labels.** The rule is always the same regardless of *why* a
system-style label is already there: the existing wrapped line is discarded, and the
new label passed to `-label` is always written to line 1.

- *Same deposition ID re-uploaded* — file already has
  `::::EMDATABANK.org::::D_1234567890::::` on line 1, run again with
  `-label D_1234567890`: the old line matches the new one exactly, so it's simply not
  duplicated. No effective change.
- *Different deposition ID* (a depositor reuses a map from a separate, earlier
  deposition of theirs) — file has `::::EMDATABANK.org::::D_1123581321::::` on line 1
  and `Relion reconstruction metadata` on line 2, run with `-label D_1234567890`:

  ```text
  Line 1: ::::EMDATABANK.org::::D_1234567890::::
  Line 2: Relion reconstruction metadata
  ```

  `D_1123581321` is gone with no trace and no warning — this isn't a truncation case,
  just the ordinary supersession rule.
- *A map already annotated re-enters the same deposition* — `mapFixAnot.jar` (the
  annotation-stage tool, sharing this same `-label` mechanism) already stamped
  `::::EMDATABANK.org::::EMD-1620::::` on line 1 during annotation, with
  `ChimeraX 0.1 Wed Jan 21 12:53:36 2026` on line 2. The depositor re-uploads that same
  file back into the same deposition, and it's run again with `-label D_9999999999`:

  ```text
  Line 1: ::::EMDATABANK.org::::D_9999999999::::
  Line 2: ChimeraX 0.1 Wed Jan 21 12:53:36 2026
  ```

  The annotation stage's `EMD-1620` label is superseded the same way a deposition ID
  supersedes an earlier one — any prior EMDataBank-style label is always treated as
  disposable system bookkeeping from an earlier processing stage, never as
  depositor-authored content.

## Build

Build with the same JDK OneDep's own build uses:

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
