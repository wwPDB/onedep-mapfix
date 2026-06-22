#!/usr/bin/env python3
from pathlib import Path

import mrcfile
import numpy as np


def write_fixture(path: Path, labels: list[str]) -> None:
    data = np.arange(64, dtype=np.float32).reshape((4, 4, 4))
    with mrcfile.new(path, overwrite=True) as mrc:
        mrc.set_data(data)
        mrc.voxel_size = (1.0, 1.0, 1.0)
        mrc.header.nlabl = len(labels)
        for index in range(10):
            mrc.header.label[index] = b""
        for index, label in enumerate(labels[:10]):
            mrc.header.label[index] = label.encode("ascii", errors="replace")[:80]


def main() -> int:
    out_dir = Path("sample-data/generated")
    out_dir.mkdir(parents=True, exist_ok=True)

    write_fixture(
        out_dir / "multi-label.map",
        [
            "Depositor line 1: RELION reconstruction metadata",
            "Depositor line 2: additional provenance survives when shifted",
            "Depositor line 3: final retained line",
        ],
    )
    write_fixture(
        out_dir / "long-label.map",
        [
            "LONG-LABEL-" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" * 4,
            "Second line should shift below the system label if capacity remains",
        ],
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
