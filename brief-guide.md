# Adding a skill

How Fletching and Crafting were built, as a checklist. Every path below is real; copy from
`content/skills/fletching` or `content/skills/smithing` rather than from scratch.

## 1. Make the module

```
content/skills/<skill>/
  build.gradle.kts
  src/main/kotlin/org/rsmod/content/skills/<skill>/
    configs/   <Skill>Configs.kt      # type references
    configs/   <Skill>Recipes.kt      # the data tables
    scripts/   *.kt                   # one PluginScript per activity
  src/integration/kotlin/org/rsmod/content/skills/<skill>/
    configs/   <Skill>ConfigTest.kt
    scripts/   <Skill>ScriptTest.kt
```

`build.gradle.kts` is the whole registration:

```kotlin
plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.skillMulti)   // only if you use the make-menu
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.interfaces.skillMulti)
}
```

**There is nothing else to register.** `settings.gradle.kts` walks `content/` and includes any
directory holding a `build.gradle.kts`; `server/shared/build.gradle.kts` makes every such
subproject an `api` dependency; ClassGraph scans `org.rsmod.api` and `org.rsmod.content` for
`PluginScript`, `PluginModule` and every `*References` / `*Editor` / `*Builder` subclass. Your
package must start with `org.rsmod.content`, and reference objects must not be `private` — make
them `public` if the integration tests touch them, since that is a separate compilation unit.

The stat plumbing already exists for all 23 skills: `stats.<skill>` in
`api/config/.../refs/BaseStats.kt` and `Player.<skill>Lvl` in
`api/player/.../stat/PlayerStatProperties.kt`.

## 2. Find out what the cache already knows

Do this *before* writing tables. Most of a skill is already in the cache — obj names, animations,
whole interfaces — and guessing wastes a day.

- `grep` the symbol tables in `.data/symbols/`: `obj.sym`, `seq.sym`, `loc.sym`, `npc.sym`,
  `component.sym`, `interface.sym`. Component names go well past the `com_N` placeholders, so read
  the whole block before concluding an interface is unnamed — that mistake nearly cost the real
  tanning panel.
- For anything interface- or clientscript-shaped, add a dump test under
  `api/cache/src/integration/.../types/` (copy `SkillMultiDump` or `CraftingInterfaceDump`) and run
  `./gradlew :api:cache:integration --tests '*Dump*'`. **Gradle does not print test stdout** — read
  `system-out` out of `api/cache/build/test-results/integration/TEST-*.xml`. Put
  `@Execution(ExecutionMode.SAME_THREAD)` on the dump class or the methods' output interleaves.
- Keep the dump as a committed test. It re-runs against whatever cache is installed; notes do not.

## 3. Write the config references

One file, one object per reference kind — see `FletchingConfigs.kt`:

```kotlin
object FletchingObjs : ObjReferences() { val bow_string = find("bow_string") /* ... */ }
object FletchingSeqs : SeqReferences() { val cut_logs = find("human_fletching") }
```

An unknown name is a **hard boot failure**, so verify every string against the `.sym` file first.
`LocReferences`, `NpcReferences`, `InterfaceReferences`, `ComponentReferences`,
`ContentReferences` all work the same way. You can `find()` a content group another module owns
(Crafting reaches Smithing's furnace by name) instead of taking a module dependency for one ref.

## 4. Prefer obj pairs over content groups

Fletching and Crafting added **no** content groups, params or enums, and touched no cache. Two
reasons:

- A content-group edit is **additive once packed** — deleting the editor later does not restore the
  obj's original group in `.data/cache/game`.
- An obj has exactly **one** content group, and two editors tagging the same type conflict
  nondeterministically. Logs were already claimed by `firemaking_logs`.

So enumerate the pairs: `for (r in recipes) onOpHeldU(objs.knife, r.log) { ... }`. Nine bindings,
zero cache risk. Reach for a group only when you genuinely need "any obj in this family" and
nothing else owns them.

If you *do* need new symbols, append to `.data/symbols/.local/{content,param,enum}.sym` and read
the header comment in each — it explains the id bands that keep you clear of upstream.

## 5. Write the tables

Plain Kotlin data classes plus an `object` holding `val all: List<...>`, like
`SmeltingRecipes.kt` / `FletchingRecipes.kt`. Validate in `init` — level in `1..99`, positive XP,
no duplicate ingredient pair — so a typo fails at class-load instead of silently paying the wrong
experience forever.

**Key lookup maps by raw `Int` id, never by `ObjType`.** `find(...)` yields a `HashedObjType`; the
runtime hands your script an `UnpackedObjType`, and the two never compare equal. (Tables you only
iterate at startup can hold `ObjType` directly.)

## 6. Write the scripts

```kotlin
class LogFletching @Inject constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.knife, recipe.log) { openMenu(recipe) }
    }
}
```

Bindings live in `api/script/`: `onOpHeldU` (item on item), `onOpLoc1..5` / `onOpLocU`,
`onOpNpc1..5`, `onIfModalButton`, `onIfClose`, `onEvent<T>`. Game logic goes in
`private suspend fun ProtectedAccess.foo()`.

**Check the op before you bind it.** All four tanners carry `Talk-to` on op1 and `Trade` on op3 —
binding the whole set would have swallowed the dialogue. Locs are less consistent: furnaces and
spinning wheels vary, so binding both op1 and op2 is the normal move there.

The make loop, copied verbatim from `Smelting.smelt`:

```kotlin
var made = 0
while (made < count && canAfford(recipe)) {
    anim(seq)
    delay(TICKS)
    if (!canAfford(recipe)) break   // re-check: inputs can be banked mid-animation
    invDel(inv, ingredient, n)
    invAdd(inv, product)
    statAdvance(stats.fletching, recipe.xp * xpMods.get(player, stats.fletching))
    made++
}
resetAnim()
if (made == 0) mes("You don't have ...")
```

Always re-validate after a `delay` or a dialogue. Always route XP through `xpMods`. There is no
shared "you need level X" helper — every module writes its own `mes` inline.

## 7. The make-menu

`SkillMulti` (`content/interfaces/skill-multi`) owns interface 270's ten buttons; the event bus
allows one binding per component, so go through it, never bind them yourself.

```kotlin
skillMulti.open(
    access = this,
    type = SkillMultiType.Cut,
    title = "What would you like to make?",
    objs = recipe.products.map { it.product },
    maxQuantity = carried,          // optional; clamped to 1..28
) { pick -> cut(recipe, recipe.products[pick.slot], count = pick.quantity) }
```

The callback runs in the *click's* protected-access scope, not the opener's, so `open` returns
immediately. Verb ids come from client enum 1809 — `SkillMultiDump` prints it; add a new entry to
`SkillMultiType` rather than passing a raw number. Max 10 slots.

## 8. Tests

Two classes: a config test (`runBasicGameTest`) asserting every authored obj/seq/component resolves
in the cache and the cross-table joins hold; and a script test (`runGameTest`) driving real events.

```kotlin
@Execution(ExecutionMode.SAME_THREAD)   // methods share one world otherwise
class FletchingScriptTest {
    @Test fun GameTestState.`cut logs`() =
        runGameTest(LogFletching::class, SkillMultiScript::class) {  // list the menu owner too
            player.inv[0] = InvObj(objs.knife)
            useOnHeld(objs.knife, FletchingObjs.logs)                // publish HeldUEvents.Type
            player.ifButton(SkillMultiComponents.slot_a, comsub = 1)
            advance(ticks = 6)
            assertEquals(15, player.count(FletchingObjs.arrow_shaft))
        }
}
```

Traps that cost a cycle each:

- **`player.ifButton` queues the click**; it works with the menu open. Publishing `IfModalButton`
  inside `withProtectedAccess` does *not* — an open modal makes the player access-protected and the
  block silently no-ops.
- **The message buffer clears every tick.** A refusal raised from a queued click needs
  `advance(ticks = 1)` then the assert — not a bare assert, not "a few extra ticks".
- **Non-stackable objs**: `InvObj(logs, count = 5)` is not five logs. Fill five slots.
- Gradle reports success having run nothing when a task is up to date — confirm with
  `--rerun-tasks`.

More in `docs/quirks.md`.

## 9. Finish

```bash
./gradlew :content:skills:<skill>:spotlessApply     # required; wraps at 100 cols
./gradlew :content:skills:<skill>:integration --rerun-tasks
./gradlew build
```

The shared test-harness setup runs `startup()` on every script in the repo, so a green integration
run also proves your bindings do not collide with anyone else's.

No `packCache` is needed unless you edited map resources or npc spawns — see the memory on that
trap. Type edits (obj/loc params, content groups) are picked up by the boot-time config sync.
