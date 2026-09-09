# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A fork of [RS Mod](https://github.com/rsmod/rsmod) — an OSRS (rev 233) game-server emulator in
**Kotlin** (no `.java` files; Java 21 is only the runtime). Upstream is `engine/`, `api/`,
`server/` and most of `content/`; our additions live in `content/custom/`, `tools/local/`,
`tools/npc-spawns/` and `.data/symbols/.local/`.

Read these before doing real work — they are long, current, and written for this checkout:

| Doc | Covers |
|---|---|
| `docs/CHEATSHEET.md` | Full orientation: repo map, hook catalogue, `ProtectedAccess`, type system, dev commands |
| `brief-guide.md` | End-to-end checklist for adding a skill module, from the Fletching/Crafting builds |
| `tools/local/README.md` | Local dev stack (RSProx + client), memory limits, and the accumulated content gotchas |
| `docs/quirks.md` | Upstream's own list of deliberate design compromises |

## Commands

```bash
./gradlew install                    # first-time setup: downloads the rev 233 cache into .data/
./gradlew run                        # boot the server (holds a Gradle daemon; see below)
./gradlew packCache                  # pack map/npc-spawn data. SERVER MUST BE STOPPED.

./gradlew test                       # unit tests (src/test)
./gradlew integration                # integration tests (src/integration, boots a headless game)
./gradlew konsistTest docTest        # meta tests (architecture + doc lint); CI runs konsistTest --rerun-tasks
./gradlew build                      # what CI gates on

./gradlew :content:custom:drop-tables:integration --rerun-tasks   # one module
./gradlew :api:cache:integration --tests '*Dump*'                 # one class/pattern
./gradlew spotlessApply              # ktfmt, kotlinlang style, 100 cols. Run before committing.
```

- **Gradle reports success having run nothing when a test task is up to date.** Always pass
  `--rerun-tasks` (or `--rerun`) when you actually need the tests executed.
- **Gradle does not print test stdout.** Read `system-out` from
  `<module>/build/test-results/integration/TEST-*.xml`.
- For playing the game locally, use `tools/local/dev.sh` (start/stop/status/`restart --build`)
  rather than `./gradlew run` — it runs from `installDist` so no Gradle daemon is held.

## Architecture

```
engine/    Pure engine: ticks, entities, pathfinding, map/zone/coord math, type primitives,
           event bus, plugin base classes. Knows nothing about game content.
api/       ~60 modules; the content-facing SDK. api/player (ProtectedAccess), api/script (onXxx
           hooks), api/config (Base*.kt vanilla refs), api/type/*, api/repo, api/testing.
content/   The game itself, one Gradle module per feature. custom/ is ours.
server/    app/ = main + Guice wiring + boot; install/ = cache download/pack/RSA;
           shared/ = classpath scanning that discovers every plugin.
build-logic/  Convention plugins: base-conventions, integration-test-suite, meta-test-suite.
.data/     Runtime data (cache, saves, keys). Gitignored except symbols/.
```

**Nothing is registered anywhere.** `settings.gradle.kts` walks `content/` and includes any
directory holding a `build.gradle.kts`; `server/shared` depends on all of them; ClassGraph scans
`org.rsmod.api` and `org.rsmod.content` for `PluginScript`, `PluginModule` and `*References` /
`*Editor` / `*Builder` subclasses at boot. To add a feature: create a directory with a
`build.gradle.kts` and a class under `org.rsmod.content.…`, then re-sync.

A module directory name **must be unique across every Gradle project** — Gradle takes the project
name from the last path segment, so `content/custom/shops` collides with `api/shops` and fails
with an unrelated-looking circular-dependency error. Prefix it (`city-shops`).

Content module layout: `configs/` (type references, editors, data tables), `scripts/`
(`PluginScript` subclasses whose `startup()` registers `onOpLoc1`/`onOpNpc1`/`onOpHeldU`/… hooks),
`map/` (spawn builders), optional `<Name>Module.kt` for Guice bindings. Handler bodies run with a
receiver, usually `ProtectedAccess`; game logic is written as
`private suspend fun ProtectedAccess.foo()`.

### The type system

No magic numbers. `.data/symbols/*.sym` are name↔id tables; `find("bow_string")` in a
`*References` object yields a typed handle and an unknown name is a **hard boot failure**.

- **References** resolve existing cache types. Subclasses must be `internal` or `public`, never
  `private`, and public if integration tests touch them (separate compilation unit).
- **Editors** mutate cache types and apply on normal boot via config sync — no `packCache`.
- **Builders** create new types or pack map/spawn data — these only take effect after
  `./gradlew packCache` **with the server stopped**.
- Our own symbols go in `.data/symbols/.local/` (merged over `symbols/`, ids from 50000 up).
  `NameLoader` only rejects duplicate ids *within one file*, so an id shared with upstream
  silently aliases. Server-only types (timers, controllers) must be hand-added there —
  `find()` will not mint an id.
- **Type edits are additive once packed.** Deleting an editor does not restore the original value
  in `.data/cache/game`. Keep obsolete tags listed with a comment rather than diverging.
- Lookup maps keyed by obj must key on the raw `Int` id: `find()` gives a `HashedObjType`, the
  runtime hands scripts an `UnpackedObjType`, and they never compare equal.

### Reading the cache instead of guessing

Component `onLoad`/`onOp` arrays and full clientscript bytecode are decoded into `TypeListMap` and
dumpable from a throwaway `GameTestState` test — see `api/cache/src/integration/.../types/`.
Undocumented interface signatures, npc/loc op slots and enum contents are all *readable*. Decode
before binding: op slots are not uniform (furnaces use op2, anvils op1, some decorative twins have
no ops at all), and some named npcs are op-less placeholders whose real types are `*_2op` variants.

## Traps that have actually cost time here

- **Integration test *methods* run in parallel and share one world.** Put
  `@Execution(ExecutionMode.SAME_THREAD)` on any suite that mutates world/player state or touches
  obj transactions (`org.rsmod.objtx`).
- **The test harness clears its message buffer every tick.** An assertion must land on the same
  tick as the message; a synchronous refusal is asserted with no `advance` at all.
- **An open modal makes a player access-protected**, so `player.withProtectedAccess` silently
  no-ops. Answer suspended dialogues with `player.resumePauseButton(component, sub)`; test the
  open and the press as separate cases.
- **A dirtied cache makes `*:integration` hang forever** (the boot-time repack restarts the app
  outside the harness and binds the server port). Run `./gradlew run` once after changing
  configs/refs, then run tests. If a run sits at 0% CPU: `lsof -nP -iTCP:43595 -sTCP:LISTEN`.
- **One port, one server.** Integration tests and the dev server both want the `port` in
  `.data/server.toml` (43595). A second checkout must change it.
- **`ProtectedAccess.mapClock` is an `Int` and shadows an injected `MapClock`** inside any
  extension function — name the injected field something else.
- **`delay(1)` delays exactly 1 tick**, not 2 (deliberately unlike the official convention). A tick
  is 600ms. Always re-validate state after a `delay` or dialogue.
- **All randomness goes through `GameRandom`**, never `kotlin.random`.
- **A green test proves the server half only.** Anything driven by a cs2 panel needs a look at the
  real client before calling it done.

## In-game dev commands

Typed in chat with `::` (see `content/other/commands/.../AdminCommands.kt`): `::master`, `::mypos`,
`::tele x y z`, `::npcadd id`, `::invadd id`, `::varbit id v`, `::reboot` (applies packed changes —
the fast iteration loop for cache-type edits).
