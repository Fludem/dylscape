# Rooftop agility cache dumpers

Three standalone Java dumpers that answer "where is every obstacle, and where can a player
actually stand" straight from `.data/cache/game`. They are the source of every coordinate in
`content/skills/agility`.

They follow the `tools/npc-spawns` pattern: compile against the installed server jars and run
without Gradle or a server boot, so they work while the build is busy.

```bash
CP=$(ls server/app/build/install/app/lib/*.jar | tr '\n' ':')
javac -cp "$CP" -d /tmp/agility tools/agility/*.java

java -cp "$CP:/tmp/agility" DumpLocPlacements .data/cache/game .data/cache/xteas.json /tmp/locs.tsv
java -cp "$CP:/tmp/agility" DumpLocTypes     .data/cache/game /tmp/loctypes.tsv
java -cp "$CP:/tmp/agility" DumpMapTiles     .data/cache/game /tmp/tiles.tsv
```

Ignore the exit-time `BufferedFileChannel.close` reference-count exception; the output file is
complete by then.

| Tool | Output | Rows in rev 233 |
|---|---|---|
| `DumpLocPlacements` | `id  x  z  level  shape  angle` for every loc placed in the world | 4,260,987 |
| `DumpLocTypes` | `id  name  width  length  blockWalk  multiVarBit  multiLoc  ops` | 57,690 |
| `DumpMapTiles` | `x  z  level  settings` for every tile with non-zero settings | 8,380,276 |

Join any of them against `.data/symbols/loc.sym` (`id<TAB>name`) for internal names - the
standalone decode does not merge the sym files, so `internalName` comes back null.

## Things that cost time

- **Do not call `MapLocListDecoder` from Java.** `InlineByteBuf`, `MapLocDefinition` and `Cursor`
  are Kotlin value classes and are erased, so the methods are unreachable. `DumpLocPlacements`
  reimplements the walk: `id += shortSmart`, `pos += shortSmart - 1`, with
  `shortSmart = peek < 128 ? readUnsignedByte() : readUnsignedShort() - 32768`, and
  `localZ = pos & 0x3F`, `localX = (pos >> 6) & 0x3F`, `level = (pos >> 12) & 0x3`.
- **`m{x}_{z}` opcodes are shorts, not bytes**, and the overlay id after a `2..49` opcode is also a
  short. Reading them as bytes desynchronises immediately and silently truncates the dump.
- **Buffer rows per mapsquare and check the trailing byte count before emitting.** A clean decode
  leaves exactly 1 trailing byte on 2382 of 2383 map squares and 0 on the loc lists; anything else
  means the walk is wrong. Without that check one bad square throws and the dump merely looks like
  it finished early.
- **`DumpLocPlacements` reports raw placement levels.** The game shifts a loc down one level when
  the tile above carries `LINK_BELOW` (`GameMapDecoder.putLocs`), so a loc dumped at level 3 can be
  at level 2 in game. Al Kharid's `rooftops_kharid_tightrope_end_1` is one of these. Check
  `DumpMapTiles`' settings bit 2 before trusting a level near a bridge or a stepped roof.
- **A blocked tile is not the same as an unreachable one.** Tightrope ends sit on blocked terrain -
  the rope spans a void and the loc provides the surface - so a landing tile has to be the platform
  beyond the rope, not the rope's own end marker.
