#!/usr/bin/env python3
"""Generate a minimal binary worldlist.ws for RSProx.

Format is taken from rsprox WorldList.decode():
    p4  payload size (bytes following this int)
    p2  world count
    per world:
        p2  id
        p4  properties
        pjstr host      (ASCII, NUL-terminated)
        pjstr activity
        p1  location
        p2  population (signed)
RSProx asserts readerIndex == payloadSize + 4, so the size must be exact.
"""
import struct
import sys

WORLD_ID = 1
PROPERTIES = 0x1  # members
HOST = "127.0.0.1"
ACTIVITY = "RS Mod Dev"
LOCATION = 0
POPULATION = 0


def jstr(s: str) -> bytes:
    return s.encode("ascii") + b"\x00"


def main(path: str) -> None:
    body = b""
    body += struct.pack(">H", WORLD_ID)
    body += struct.pack(">i", PROPERTIES)
    body += jstr(HOST)
    body += jstr(ACTIVITY)
    body += struct.pack(">B", LOCATION)
    body += struct.pack(">h", POPULATION)

    payload = struct.pack(">H", 1) + body  # count + worlds
    out = struct.pack(">i", len(payload)) + payload

    with open(path, "wb") as f:
        f.write(out)
    print(f"wrote {path} ({len(out)} bytes, payload={len(payload)})")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "worldlist.ws")
