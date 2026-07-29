# Sample Data

This directory contains representative MRC map files used as regression fixtures for
MapFix's label-handling behavior, exercised by `MapFixDepIntegrationTest`
(`test/msdmap/mapread/MapFixDepIntegrationTest.java`) and by
`scripts/build-mapfixdep.sh` output for manual spot-checks.

These are real deposition examples downloaded during the OneDep MRC header-label
investigation, tracked with Git LFS because they are large binary files. See
`manifest.tsv` for what each one represents and the label outcome it's expected to
produce.

Two additional scenarios — a map whose depositor labels already fill the 10-line MRC
capacity, and a map that already carries a system label from an earlier MapFix run —
don't have committed files here: `MapFixDepIntegrationTest` builds them on the fly via
`TestFixtures.writeFixture(...)`, since they're synthetic and don't need to be real
downloaded deposition data.
