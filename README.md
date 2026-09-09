# Onyx

[![revision][rev-badge]][patch] [![license][license-badge]][isc]

A private Old School RuneScape server, running on **revision 233**, for a handful of friends.

It is a fork of [RS Mod][rsmod], which supplies the engine, the SDK and a good deal of the content.
Everything under `content/custom/`, most of `content/skills/`, the local dev tooling and the website
are ours. There is no ambition here to be a public server or a general-purpose framework — features
land because someone wanted to play them.

Players connect with a patched RuneLite; see [docs/CLIENT.md](docs/CLIENT.md). The hiscores and
guides live at [rsps.onyxleeds.co.uk](https://rsps.onyxleeds.co.uk/).

## Requirements

**[Java 21][java] or later.** It is the runtime only — the codebase is Kotlin, with no `.java`
sources.

<details>
<summary>Where to get Java 21</summary>

- **[Adoptium OpenJDK 21 LTS][adoptium-download]** — recommended, free and open-source
- **[OpenJDK 21][openjdk-download]**

Or via a package manager:

```sh
sdk install java 21.0.7-tem                                    # SDKMAN!, Linux/macOS
brew install openjdk@21                                        # Homebrew, macOS
winget install --id=EclipseAdoptium.Temurin.21.JDK -e          # WinGet, Windows
```
</details>

## Getting started

```sh
git clone git@github.com:Fludem/dylscape.git
cd dylscape
./gradlew install     # first run only: downloads the rev 233 cache into .data/ (~1 min)
./gradlew run
```

For actually playing locally, use the dev stack rather than `./gradlew run` — it runs from
`installDist`, so it does not hold a Gradle daemon, and it can bring the client up with the server:

```sh
tools/local/dev.sh start          # also: stop, status, restart --build
```

See [tools/local/README.md](tools/local/README.md) for the client setup and the accumulated
gotchas.

## Commands

```sh
./gradlew run                    # boot the server
./gradlew packCache              # pack map and npc-spawn data. THE SERVER MUST BE STOPPED
./gradlew test                   # unit tests
./gradlew integration            # integration tests (boots a headless game)
./gradlew konsistTest docTest    # architecture and doc lint
./gradlew build                  # what CI gates on
./gradlew spotlessApply          # ktfmt, 100 columns. Run before committing
```

Two things that have cost time more than once:

- **Gradle reports success having run nothing** when a test task is up to date. Pass `--rerun-tasks`
  when you actually need the tests executed.
- **Gradle does not print test stdout.** Read `system-out` from
  `<module>/build/test-results/integration/TEST-*.xml`.

## Layout

```
engine/       Ticks, entities, pathfinding, map and coord maths, type primitives, event bus.
              Knows nothing about game content.
api/          ~60 modules; the content-facing SDK. ProtectedAccess, the onXxx hooks, type
              references, the combat formulas, persistence.
content/      The game itself, one Gradle module per feature.
server/       Boot and Guice wiring, cache download and packing, classpath scanning.
build-logic/  Convention plugins.
tools/        Local dev stack, cache dumpers, the drop-table and npc-spawn generators.
web/          The Onyx website: hiscores off the save database, and generated guides.
.data/        Runtime data — cache, saves, keys. Gitignored except symbols/.
```

**Nothing is registered anywhere.** `settings.gradle.kts` walks `content/` and includes any
directory with a `build.gradle.kts`; ClassGraph then scans for plugin scripts, modules and type
references at boot. To add a feature, make a directory and re-sync.

## What is built here

Seventeen skills are trainable: agility, construction, cooking, crafting, farming, firemaking,
fishing, fletching, herblore, hunter, magic, mining, prayer, slayer, smithing, thieving and
woodcutting.

Beyond those, `content/custom/` holds the things that make it a server rather than a demo — account
modes (ironman and hardcore, with the death demotion), Barrows, the Dagannoth Kings, twenty-three
city shops, wiki-derived drop tables for ~2,700 monsters, the Knight Waves trial for Chivalry
and Piety, a teleport menu, and the world spawns. Tutorial Island runs end to end, and most of the
interface work — bank, prayer tab, skill guides, world map, the first-login account chooser — is in
`content/interfaces/`.

Much of this is built by reading the cache rather than hand-authoring data. Slayer, for instance,
takes its 127 tasks, every master's weighted assignment table and all the level requirements
straight out of the cache's own dbtables; the drop tables are generated from the wiki. Where a
number could be looked up, it generally is.

## Documentation

These are long, current, and written for this checkout rather than for RS Mod in general.

| Doc | Covers |
|---|---|
| [docs/CHEATSHEET.md](docs/CHEATSHEET.md) | Full orientation: repo map, hook catalogue, `ProtectedAccess`, the type system, dev commands |
| [brief-guide.md](brief-guide.md) | End-to-end checklist for adding a skill module |
| [tools/local/README.md](tools/local/README.md) | The local dev stack, memory limits, and content gotchas |
| [docs/quirks.md](docs/quirks.md) | Upstream's own list of deliberate design compromises |
| [docs/CLIENT.md](docs/CLIENT.md) | The friend-facing client: patched RuneLite, no RSProx, no cache to ship |
| [web/README.md](web/README.md) | The website, and its own deploy |
| [CLAUDE.md](CLAUDE.md) | Conventions and the traps that have actually cost time here |

`docs/DEPLOY.md` covers the live server and is deliberately **not** in git — it carries the host
address and access details.

## In-game dev commands

Typed in chat, on a realm with `dev_mode` enabled: `::master`, `::mypos`, `::tele x y z`,
`::npcadd id`, `::invadd id`, `::varbit id v`, and `::reboot`, which applies packed cache changes
and is the fast iteration loop.

## Upstream and licence

Onyx tracks [RS Mod][rsmod] as the `upstream` remote and carries its licence. RS Mod is available
under the terms of the ISC licence; the full copyright notice and terms are in
[LICENSE.md](LICENSE.md). Credit for the engine, the SDK and the upstream content belongs to the RS
Mod authors.

[isc]: https://opensource.org/licenses/ISC
[license-badge]: https://img.shields.io/badge/license-ISC-informational
[patch]: https://oldschool.runescape.wiki/w/Update:Doom_Combat_Achievements
[rev-badge]: https://img.shields.io/badge/revision-233-important
[rsmod]: https://github.com/rsmod/rsmod
[java]: https://openjdk.java.net/projects/jdk/21/
[adoptium-download]: https://adoptium.net/temurin/releases/?version=21
[openjdk-download]: https://jdk.java.net/archive/
