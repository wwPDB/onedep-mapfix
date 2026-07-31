# MapFix

MapFix reads, validates, and fixes CCP4/MRC electron-microscopy map headers. This is
the official wwPDB repository for MapFix: OneDep's build pulls `MapFix` directly from
here rather than from the legacy SVN source tree.

## Label Handling Behavior

`mapFixDep.jar` receives a system label (a deposition ID such as `D_1123581321`, later
replaced by the final EMDB accession such as `EMD-1620`) through `-label <id>` and
writes it as line 1 of the MRC header, wrapped as:

```text
::::EMDATABANK.org::::<id>::::
```

Rules for the remaining label lines:

- Any depositor-authored label lines already present (e.g. `Relion ...`,
  `ChimeraX ...`) are preserved and shifted down, after the system label and any
  change-log entries (below), in their original order.
- If a map already carries a system label from an earlier MapFix run (matching the
  `::::...::::` wrapper above) with a *different* ID, it isn't discarded — it's
  demoted into a change-log entry, right after line 1: the deposition ID (or prior
  EMDB accession) stays visible in the header rather than vanishing the moment a
  newer ID supersedes it. A demoted entry keeps the same overall
  `::::X::::<id>::::` shape, but `X` becomes a `yyyyMMddHHmmss` timestamp of the
  moment it was superseded instead of `EMDATABANK.org` — e.g.
  `::::EMDATABANK.org::::D_1234567890::::` demoted at that moment becomes
  `::::20260729143022::::D_1234567890::::` (same length, `D_1234567890` at the same
  offset).
  An existing label identical to the new one is still just left alone, not
  duplicated or demoted.
- The change-log grows with every distinct ID this map has ever been stamped with,
  newest first, but never at the expense of the current label (line 1) or the
  depositor's own content: the MRC format's 10-line cap means the change-log is
  evicted oldest-first (one entry at a time, starting with whichever sits closest
  to the depositor content) as needed to keep both of those intact. Eviction isn't
  silent: the entry that ends up as the new oldest survivor gets a
  `[TRUNCATED: N more line(s) removed]` notice trailing after its own closing
  `::::`, where `N` is the *cumulative* count of IDs evicted from this map's history
  over its entire lifetime (not just this one relabeling) — each time a further
  eviction happens, whatever count the outgoing entry already carried is added to the
  running total before it's re-embedded on whichever entry survives next. Only once
  the *entire* change-log has been evicted and it's still over capacity — only
  possible when depositor content alone already fills all 10 slots, with no ID
  history involved at all — does the depositor-overflow fallback kick in: as many
  depositor lines as fit are kept untouched, and the last one that fits is annotated
  in place — its own real content, ellipsis-truncated only as far as needed, followed
  by the same `[TRUNCATED: N more line(s) removed]` notice — instead of sacrificing a
  whole line purely for a notice or silently dropping whatever doesn't fit.

This does not change upload milestone semantics or any OneDep Python workflow.

### Examples

**Overflow.** A map whose original depositor content already fills all 10 label slots:

```text
1. Relion    21-Mar-21  22:48:12
2. cryoSPARC v4.4 non-uniform refinement job 231
3. UCSF ChimeraX volume rendering session
4. Coot 0.9.8 model-building checkpoint
5. Phenix real_space_refine resolution 3.1A
6. IMOD 4.11 tomogram reconstruction
7. cisTEM auto-refine 3D class 2
8. Xmipp 3.24.06 3D classification run
9. EMAN2 e2refine_easy.py output: iteration 12 of gold-standard refinement,
   half-map FSC resolution 3.2 Angstrom at the 0.143 cutoff threshold
10. Bsoft 2.1.4 map post-processing and B-factor sharpening
```

Running `-label D_1234567890` produces:

```text
Line 1:  ::::EMDATABANK.org::::D_1234567890::::
Line 2:  Relion    21-Mar-21  22:48:12
Line 3:  cryoSPARC v4.4 non-uniform refinement job 231
Line 4:  UCSF ChimeraX volume rendering session
Line 5:  Coot 0.9.8 model-building checkpoint
Line 6:  Phenix real_space_refine resolution 3.1A
Line 7:  IMOD 4.11 tomogram reconstruction
Line 8:  cisTEM auto-refine 3D class 2
Line 9:  Xmipp 3.24.06 3D classification run
Line 10: EMAN2 e2refine_easy.py output: iteration ... [TRUNCATED: 1 more line(s) removed]
```

Line 9's content alone is 139 characters — well over the 80-character MRC label
limit — so it's ellipsis-truncated (`...`) only as far as needed to make room for
the `[TRUNCATED: N more line(s) removed]` suffix, rather than being cut off blindly
at 80 characters with no indication anything was lost. "Bsoft 2.1.4 map
post-processing and B-factor sharpening" (line 10) is the one line fully dropped,
silently — accounted for only by the "1 more line(s) removed" count on line 10.

**Pre-existing system labels.** The rule is always the same regardless of *why* a
system-style label is already there: an existing wrapped line with the *same* ID as
what's being written is left alone (no duplicate); a *different* ID is demoted into a
change-log entry rather than discarded, and the new label always ends up on line 1.

- *Same deposition ID re-uploaded* — file already has
  `::::EMDATABANK.org::::D_1234567890::::` on line 1, run again with
  `-label D_1234567890`: the old line matches the new one exactly, so nothing changes.
- *Different deposition ID* (a depositor reuses a map from a separate, earlier
  deposition of theirs) — file has `::::EMDATABANK.org::::D_1123581321::::` on line 1
  and `Relion    21-Mar-21  22:48:12` on line 2, run with `-label D_1234567890`:

  ```text
  Line 1: ::::EMDATABANK.org::::D_1234567890::::
  Line 2: ::::20260729143022::::D_1123581321::::
  Line 3: Relion    21-Mar-21  22:48:12
  ```

  `D_1123581321` survives as a demoted change-log entry rather than disappearing.
- *A map already annotated re-enters the same deposition* — the full lifecycle: the
  depositor's map was first stamped `-label D_1123581321` during deposition (with
  `ChimeraX 0.1 Wed Jan 21 12:53:36 2026` on line 2), then `mapFixAnot.jar` (the
  annotation-stage tool, sharing this same `-label` mechanism) stamped
  `-label EMD-1620` during annotation — demoting `D_1123581321` to line 2, correctly
  sitting *below* the EMDB accession once annotation happens. The depositor
  re-uploads that same file back into the *same* deposition, so it's run again with
  the *same* deposition ID as before, `-label D_1123581321`:

  ```text
  Line 1: ::::EMDATABANK.org::::D_1123581321::::
  Line 2: ::::20260729150130::::EMD-1620::::
  Line 3: ::::20260729143022::::D_1123581321::::
  Line 4: ChimeraX 0.1 Wed Jan 21 12:53:36 2026
  ```

  `D_1123581321` now appears twice — once as the freshly re-stamped current label
  (line 1), and once still sitting in the change-log from before (line 3, with its
  original, older timestamp). That's expected: `changeLabel` only checks the
  *current* line for an exact match before deciding whether to demote it, not the
  whole change-log, so a repeated ID legitimately shows up at both points in time it
  was current.

**Change-log eviction.** The same `D_1123581321` → `EMD-1620` → `D_1123581321`
journey, but on a map that carries 8 depositor label lines from the start (reusing
the first 8 lines of content from the overflow example above) rather than just one
ChimeraX line. By the 3rd relabeling round, the change-log no longer fits alongside
all 8 depositor lines and the current label:

```text
Line 1:  ::::EMDATABANK.org::::D_1123581321::::
Line 2:  ::::<timestamp>::::EMD-1620:::: [TRUNCATED: 1 more line(s) removed]
Line 3:  Relion    21-Mar-21  22:48:12
Line 4:  cryoSPARC v4.4 non-uniform refinement job 231
Line 5:  UCSF ChimeraX volume rendering session
Line 6:  Coot 0.9.8 model-building checkpoint
Line 7:  Phenix real_space_refine resolution 3.1A
Line 8:  IMOD 4.11 tomogram reconstruction
Line 9:  cisTEM auto-refine 3D class 2
Line 10: Xmipp 3.24.06 3D classification run
```

Line 1 (current) and all 8 depositor lines are never evicted. The *old*
`D_1123581321` change-log entry — from when it was first superseded by `EMD-1620` —
is the oldest entry, sitting immediately above the depositor content, so it's the
one evicted to make room for `EMD-1620`'s own demotion, and its loss is recorded as
the `1` in the surviving `EMD-1620` entry's notice rather than vanishing without a
trace. The freshly re-stamped `D_1123581321` on line 1 is untouched — eviction only
ever removes change-log entries, never the current line.

Two more relabeling rounds (`-label EMD-1620`, then `-label D_1123581321` again)
each evict the same way, and the notice's count keeps accumulating rather than
resetting:

```text
Line 1:  ::::EMDATABANK.org::::EMD-1620::::
Line 2:  ::::<timestamp>::::D_1123581321:::: [TRUNCATED: 2 more line(s) removed]
Line 3-10: (unchanged - the same 8 depositor lines)
```

```text
Line 1:  ::::EMDATABANK.org::::D_1123581321::::
Line 2:  ::::<timestamp>::::EMD-1620:::: [TRUNCATED: 3 more line(s) removed]
Line 3-10: (unchanged - the same 8 depositor lines)
```

The `3` reflects all three IDs evicted across this map's full history so far (the
first `D_1123581321`, then `EMD-1620`, then `D_1123581321` again) — not just
whichever single eviction happened most recently.

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
