#!/usr/bin/env python3
"""Writes the 16x16 gravestone texture: speckled grey stone with a darker rim. Deterministic."""
import random
import struct
import sys
import zlib

SIZE = 16


def pixel(rng, x, y):
    base = 128 + rng.randint(-14, 14)
    if x in (0, SIZE - 1) or y in (0, SIZE - 1):
        base -= 28
    if rng.random() < 0.08:
        base -= 22  # dark speckles
    v = max(0, min(255, base))
    return bytes((v, v, min(255, v + 4), 255))


def png(width, height, rows):
    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + row for row in rows)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def main(path):
    rng = random.Random(1337)
    rows = [b"".join(pixel(rng, x, y) for x in range(SIZE)) for y in range(SIZE)]
    with open(path, "wb") as f:
        f.write(png(SIZE, SIZE, rows))


if __name__ == "__main__":
    main(sys.argv[1])
