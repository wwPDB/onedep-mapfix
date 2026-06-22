#!/usr/bin/env python3
import sys

import mrcfile


def main() -> int:
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <map-file>", file=sys.stderr)
        return 2

    path = sys.argv[1]
    with mrcfile.open(path, permissive=True) as mrc:
        nlabels = int(mrc.header.nlabl)
        print(f"{path}")
        print(f"nlabl: {nlabels}")
        for index, raw in enumerate(mrc.header.label[:nlabels], start=1):
            text = bytes(raw).decode("ascii", errors="replace").rstrip("\x00 ")
            print(f"label[{index}]: {text!r}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

