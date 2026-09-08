# RS Mod Cheatsheet

A working reference for this repo, written for someone coming from PHP.

---

## 0. The single most important thing

**This is Kotlin, not Java.** There is not one `.java` file in the project. Java 21
is only the *runtime* (the JVM), the way PHP-FPM is the runtime for your PHP. You
will read and write Kotlin exclusively.

That's good news: Kotlin is much closer to modern PHP than Java is. Null safety
(`?`/`?:`), string interpolation (`"$name"`), no getters/setters boilerplate,
lambdas, `val`/`var` instead of typed declarations everywhere.

---

## 1. IntelliJ setup

Everything is pre-configured in `.idea/`. You do **not** need to create run
configurations.

1. **Open the folder** (`File → Open`, pick the repo root). IntelliJ detects Gradle.
2. When prompted, **Load Gradle Project** / **Trust Project**. First sync downloads
   ~everything and takes several minutes.
3. **JDK**: `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to JDK 21
   and auto-downloads it (you currently only have JDK 26 installed locally — that's
   fine, Gradle provisions its own). If IntelliJ complains, go to
   *Settings → Build, Execution, Deployment → Build Tools → Gradle* and set
   **Gradle JVM** to a 21 toolchain.
4. **Run configurations** already shipped in `.idea/runConfigurations/`:
   | Name | What it does |
   |---|---|
   | `GameServer` | Boots the server (`org.rsmod.server.app.GameServerKt`) |
   | `rsmod [unitTests]` | `gradle test` |
   | `rsmod [integrationTests]` | `gradle integration` |
   | `rsmod [metaTests]` | `konsistTest` + `docTest` (architecture/doc lint) |
   | `rsmod [formatCode]` | `spotlessKotlinApply` — run before committing |
   | `rsmod [dependencyUpdates]` | dependency version report |
5. **Formatter**: the project uses **ktfmt** (kotlinlang style, 100 col), enabled via
   `.idea/ktfmt.xml`. Install the *ktfmt* plugin if IntelliJ prompts. Don't fight it —
   CI enforces it via Spotless.
6. **First run is slow**: `GameServer` downloads the OSRS rev 233 cache into `.data/`
   on first boot. Subsequent boots are fast.

### Connecting a client
Use [RSProx](https://github.com/blurite/rsprox). The server listens on the port in
`.data/server.toml` (currently **43595**, realm `dev`, world 1). If you run two
checkouts of this repo at once, they will fight over that port — change one.

---

## 2. PHP → Kotlin/Gradle mental map

| PHP world | Here |
|---|---|
| `composer.json` | `build.gradle.kts` (one per module) |
| `composer.lock` | `gradle/libs.versions.toml` (version catalog) |
| `vendor/` | `~/.gradle/caches/` |
| PSR-4 autoload | package = directory path under `src/main/kotlin/` — **must match** |
| `composer install` | Gradle sync (IntelliJ does it) |
| `php artisan serve` | the `GameServer` run config |
| Namespaces `App\Foo` | `package org.rsmod.content.foo` |
| `use App\Foo;` | `import org.rsmod.content.foo.Foo` |
| PHPUnit | JUnit 5 |
| `phpcs`/`php-cs-fixer` | Spotless + ktfmt |
| A Laravel service provider | a Guice `PluginModule` |
| Laravel event listeners | `PluginScript` + `onXxx { }` hooks |
| DI container / `app()` | Guice, via `@Inject constructor(...)` |

### Kotlin syntax you'll hit immediately
```kotlin
val x = 5              // final, like a PHP const-ish local
var y = 5              // reassignable
fun foo(a: Int): String = "$a"   // expression body + interpolation
player.name?.length ?: 0         // null-safe chain + null coalesce (?? in PHP)
list.filter { it > 3 }.map { it * 2 }   // `it` = implicit single param
```
Two things with no PHP analogue that this codebase leans on hard:

- **Extension functions**: `private fun ProtectedAccess.attempt(...)` defines a
  method *onto* an existing class from outside it. Inside the body, `this` is a
  `ProtectedAccess`. This is why content scripts read like they have magic globals
  (`mes(...)`, `inv`, `player`) — they're members of the receiver type.
- **Trailing lambdas**: `onOpLoc1(content.tree) { attempt(it.loc, it.type) }` — the
  `{ }` is the last argument to the function, moved outside the parens.

---

## 3. Repo map

```
engine/     Pure game engine. Ticks, entities, pathfinding, map/zone/coord math,
            the type system primitives, event bus, plugin base classes.
            Knows nothing about "woodcutting" or "Lumbridge".

api/        The content-facing SDK. ~60 modules. This is what you call from content.
            Key ones: api/player (ProtectedAccess), api/script (the onXxx hooks),
            api/config (BaseObjs, BaseNpcs, BaseLocs... symbol constants),
            api/type/* (references/builders/editors), api/repo (spawn/despawn),
            api/testing (GameTestState).

content/    The actual game. One Gradle module per feature.
            skills/, interfaces/, areas/, generic/, travel/, other/,
            and custom/ — your own additions (drop-tables, city-shops,
            toll-gate, leagues).

server/     app/ = main() + Guice wiring + boot sequence
            install/ = cache download, cache packing, RSA keygen
            shared/ = classpath scanning that discovers all plugins
            services/, logging/

build-logic/  Gradle convention plugins (base-conventions, etc.)
tools/local/  local scratch tooling
.data/        Runtime data — cache, saves, keys, symbols. Gitignored except symbols/.
docs/quirks.md  Read this. Deliberate design compromises, documented.
```

### How modules get discovered (the nicest part)
`settings.gradle.kts` **walks the directory tree** and includes any folder
containing a `build.gradle.kts`. `server/shared/build.gradle.kts` then does
`project(":content").subprojects.filter { it.buildFile.exists() }` and puts them all
on the runtime classpath. At boot, `PluginScriptLoader` uses ClassGraph to scan
packages `org.rsmod.api` and `org.rsmod.content` for subclasses of `PluginScript`
and `PluginModule`.

**Net effect: to add a new feature you create a directory with a `build.gradle.kts`
and a class, then re-sync Gradle. There is no registration list to edit anywhere.**
It's PSR-4 autoloading for game features.

Minimal new module — `content/custom/my-thing/build.gradle.kts`:
```kotlin
plugins {
    id("base-conventions")
    id("integration-test-suite")   // only if you want src/integration tests
}

dependencies {
    implementation(projects.api.pluginCommons)
}
```
Then `content/custom/my-thing/src/main/kotlin/org/rsmod/content/custom/mything/MyScript.kt`.

---

## 4. Anatomy of a content module

Convention (see `content/skills/woodcutting` or `content/custom/toll-gate`):

```
src/main/kotlin/org/rsmod/content/<area>/<name>/
    configs/      Type references, params, editors — the "data"
    scripts/      PluginScript classes — the "behaviour"
    map/          Map/npc/loc spawn builders (packed into the cache)
    <Name>Module.kt   Optional Guice bindings
src/integration/kotlin/...   Integration tests (need `integration-test-suite`)
src/test/kotlin/...          Plain unit tests
```

### A script
```kotlin
class Woodcutting @Inject constructor(
    private val objTypes: ObjTypeList,      // Guice injects these
    private val locRepo: LocRepository,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(content.tree) { attempt(it.loc, it.type) }
        onOpLoc3(content.tree) { cut(it.loc, it.type) }
        onOpLocU(content.tree, content.woodcutting_axe) { cut(it.loc, it.type) }
    }

    private fun ProtectedAccess.attempt(tree: BoundLocInfo, type: UnpackedLocType) {
        if (player.woodcuttingLvl < type.treeLevelReq) {
            mes("You need a Woodcutting level of ${type.treeLevelReq}...")
            return
        }
        ...
    }
}
```
`startup()` runs once at boot and registers handlers. Handler bodies run with a
receiver — usually `ProtectedAccess` (a player who is allowed to act right now) or
`StandardNpcAccess`.

### A Guice module
```kotlin
class WoodcuttingModule : PluginModule() {
    override fun bind() {
        addSetBinding<InvisibleLevelMod>(WoodcuttingLevelBoosts::class.java)
    }
}
```

---

## 5. The hook catalogue

All of these are `ScriptContext.onXxx` functions from `api/script`. Naming follows
RuneScape's own script conventions:

- **`op`** = the player has *arrived* and is performing the operation.
- **`ap`** = "approach": fires at range, before/instead of walking all the way.
- The trailing number is the right-click menu option index (1 = left-click).
- **`U`** suffix = "use X on Y". **`T`** suffix = "use spell on Y".

```
Locs (scenery/objects in the world)
  onOpLoc1..5, onApLoc1..5, onOpLocU, onApLocU, onOpLocT, onApLocT
Npcs
  onOpNpc1..5, onApNpc1..5, onOpNpcU, onApNpcU, onOpNpcT, onApNpcT
Ground objs / inventory items / worn items
  onOpObj1..5            (obj on the ground)
  onOpHeld1..5, onOpHeldU (item in inventory)
  onOpWorn1..9, onEquipObj, onUnequipObj
Players
  onOpPlayerU, onOpPlayerT, onApPlayerU, onApPlayerT
Interfaces
  onIfOpen, onIfClose, onIfOverlayButton, onIfModalButton,
  onIfOverlayButtonT, onIfModalButtonT, onIfOverlayDrag, onIfModalDrag
Lifecycle & scheduling
  onGameStartup, onPlayerInit, onPlayerLogin, onPlayerLogout
  onPlayerQueue, onPlayerSoftQueue, onPlayerTimer, onPlayerSoftTimer
  onNpcQueue, onNpcTimer, onConQueue, onConTimer
  onAiTimer, onAiConQueue, onAiConTimer, onAiOpPlayer1..5, onAiApPlayer1..5
Misc
  onArea, onAreaExit, onCommand, onDropTrigger,
  onNpcHit, onModifyNpcHit, onPlayerWalkTrigger, onNpcWalkTrigger
```

**Queues vs timers**: a *queue* is a one-shot deferred action; a *timer* repeats.
Npc queues resolve `type → content group → default` and stop at the first match —
see the comment in `content/custom/drop-tables/.../DropTableScript.kt`, which
exploits that to fully replace the default death handler.

---

## 6. `ProtectedAccess` — your main API

Almost all player code is written as `private fun ProtectedAccess.doThing()`.
"Protected" means the engine has confirmed the player isn't mid-action, so it's safe
to act. It's ~2000 lines in
`api/player/src/main/kotlin/org/rsmod/api/player/protect/ProtectedAccess.kt`.
Ctrl+click into it constantly; the highlights:

```kotlin
mes("text")                    // chatbox message
spam("text")                   // filterable message
anim(seq); spotanim(spot)      // animation / graphic
soundSynth(synths.foo)
say("text")                    // overhead chat

inv.isFull(); invAdd(obj, count); invDel(obj); invReplace(...)
player.righthand; player.woodcuttingLvl
stat(stats.woodcutting); statAdvance(stat, xp)

walk(dest); teleport(dest); telejump(dest)
playerWalk(dest)               // suspend — see below
faceLoc(loc); faceEntitySquare(npc)
distanceTo(loc); lineOfSight(a, b); isWithinDistance(other, 3)

opLoc1(loc); opNpc1(npc)       // trigger another script's handler
queueHit(...); queueDeath()
delay(1)                       // NOTE: delays exactly 1 tick, not 2 (docs/quirks.md)
```

`suspend fun` = a coroutine. Any function that waits across game ticks is `suspend`
and can only be called from another `suspend` context. If the compiler yells
"suspension functions can only be called within coroutine body", mark your function
`suspend` too.

A **tick** is 600ms. `mapClock` is the global tick counter; the idiom
`actionDelay = mapClock + 3` means "busy for 3 ticks".

---

## 7. The type system (references / builders / editors)

This is the part with no PHP analogue, and the thing most likely to confuse you.

Game content (items, npcs, scenery, interfaces) lives in the **OSRS cache** — a
binary blob under `.data/cache/`, addressed by integer ID. The project refuses to
let you write magic numbers. Instead:

**`.data/symbols/*.sym`** are name↔id tables (`loc.sym` has 57k entries,
`obj.sym` 31k, `npc.sym` 14k). Grep these to find the internal name of anything.

**References** — "this thing exists in the cache, give me a typed handle":
```kotlin
object TollGateLocs : LocReferences() {
    val closed_left = find("kharidmetalgateclosedl")
}
internal typealias tollgate_locs = TollGateLocs   // house style: lowercase alias
```
Verified at boot. A typo fails startup instead of silently spawning the wrong thing.
Pre-made ones for vanilla content live in `api/config/.../refs/Base*.kt` and are
used via `objs.`, `npcs.`, `locs.`, `content.`, `params.`, `stats.`, `synths.`,
`queues.`, `varps.`, `controllers.` …

**Editors** — mutate an existing cache type:
```kotlin
internal object TollGateNpcEditor : NpcEditor() {
    init { edit(tollgate_npcs.borderguard_lumbridge) { wanderRange = 0 } }
}
```
Applied by the boot-time config sync. **No `packCache` needed.**

**Builders** — create *new* types, or pack map data (npc/loc spawns):
```kotlin
object TollGateNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<TollGateScript>("npcs.toml") }
}
```
⚠️ **Map/spawn builders only take effect after `./gradlew packCache` with the server
stopped.** A normal server boot does *not* repack them. This is a known foot-gun.

---

## 8. Gradle tasks

Run from the repo root (`./gradlew <task>`), or use the IntelliJ run configs.

```
./gradlew run                # boot the server (same as GameServer config)
./gradlew install            # full first-time install (cache + rsa + logback)
./gradlew cleanInstall       # wipe partial/corrupt install artifacts
./gradlew downloadCache      # (re)download the vanilla cache
./gradlew packCache          # pack custom types + map/spawn data. SERVER MUST BE OFF.
./gradlew generateRsa        # regenerate network RSA keys

./gradlew test               # unit tests
./gradlew integration        # integration tests (boots a headless game)
./gradlew konsistTest docTest  # architecture + doc lint
./gradlew spotlessKotlinApply  # format. run before every commit.
./gradlew :content:custom:drop-tables:test   # single module
```

---

## 9. Testing

Two flavours:

**Unit** (`src/test/`) — plain JUnit 5, no game.
```kotlin
@Test fun `weighted tables are rolled out of 128`() { ... }
```
Backtick-quoted test names are idiomatic Kotlin; use them.

**Integration** (`src/integration/`) — boots the real type system and a map. Needs
`id("integration-test-suite")` in the module's `build.gradle.kts`.
```kotlin
class LumbridgeDropTablesTest {
    @Test
    fun GameTestState.`every drop resolves to an obj in the cache`() = runBasicGameTest {
        val resolved = cacheTypes.objs[obj]
        assertTrue(resolved.name.isNotBlank())
    }
}
```
Note the receiver on the test function itself (`fun GameTestState.foo()`) — that's
the harness pattern here.

Gotchas from `docs/quirks.md` and hard experience:
- Tests touching obj transactions (`org.rsmod.objtx`) are **not thread-safe** —
  annotate the class `@Execution(ExecutionMode.SAME_THREAD)`.
- Concurrent test methods share the map clock; message capture is per-tick.

---

## 10. In-game dev commands

Typed in chat with a `::` prefix. Defined in
`content/other/commands/.../AdminCommands.kt`:

```
::master        max all stats          ::reset       reset all stats
::mypos         print coords           ::tele x y z  teleport
::telezone      teleport to zone key   ::anim id     play animation
::spot id       play spotanim          ::locadd id   spawn scenery
::locdel        remove scenery         ::npcadd id   spawn npc
::invadd id     spawn item             ::invclear    empty inventory
::varp id v     set varp               ::varbit id v set varbit
::reboot        reboot world, applying packed changes
```
`::reboot` is your fast iteration loop for cache-type changes.

---

## 11. Gotchas worth knowing up front

- **Kotlin, not Java.** Don't go looking for `.java` files or Maven.
- **`packCache` for spawns.** Loc/npc spawn files only apply after
  `./gradlew packCache` with the server stopped. Editors (`NpcEditor` etc.) apply on
  normal boot.
- **`.data/` is gitignored** apart from `symbols/`. Your cache, saves and keys are
  local-only. `.data/symbols/.local/` overrides root symbols if names collide.
- **Two checkouts = collisions.** Same port (43595), same `.data`, same Gradle build
  cache. Change the port in `.data/server.toml` if you run a second instance.
- **`delay(1)` delays 1 tick**, not 2 — this deliberately differs from the official
  script convention.
- **All randomness goes through `GameRandom`**, never `kotlin.random`. Tests stub it.
- **Format before committing.** Spotless/ktfmt at 100 columns; CI fails otherwise.
- **`internal` not `private`** for type reference subclasses — private ones aren't
  allowed by the loader.
- **Read `docs/quirks.md`.** It's a genuine list of "we know, here's why" decisions
  and will save you hours of confusion.

---

## 12. Where to look when you're stuck

| I want to… | Go to |
|---|---|
| See how a skill is written | `content/skills/woodcutting/` |
| See how custom content is written | `content/custom/toll-gate/` (small, well-commented) |
| Find what a player can do | `api/player/.../protect/ProtectedAccess.kt` |
| Find the right hook | `api/script/src/main/kotlin/org/rsmod/api/script/*.kt` |
| Find an item/npc/loc id | `grep -n "name" .data/symbols/obj.sym` (or npc/loc) |
| Find a vanilla constant | `api/config/.../refs/Base*.kt` |
| Understand boot order | `server/app/.../GameServer.kt` |
| Understand DI wiring | `server/app/.../modules/GameModule.kt` |
