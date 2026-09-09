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

`SkillMulti` (`content/interfaces/skill-multi`) drives interface 270. It is a **pause-button
dialogue**, like `choice2` or the destroy-item confirmation: you open it and suspend, and the press
resumes you where you left off.

```kotlin
private suspend fun ProtectedAccess.openMenu(recipe: CutLogRecipe) {
    val pick =
        skillMulti.open(
            access = this,
            type = SkillMultiType.Cut,
            title = "What would you like to make?",
            objs = recipe.products.map { it.product },
            maxQuantity = carried,      // optional; clamped to 1..28
        ) ?: return
    cut(recipe, recipe.products[pick.slot], count = pick.quantity)
}
```

Verb ids come from client enum 1809 — `SkillMultiDump` prints it; add a new entry to
`SkillMultiType` rather than passing a raw number. Max 10 slots.

**It is not an `IfButton` interface, and assuming it was cost a day.** The menu looked right and
every press vanished. The item buttons carry no events in the cache; their left-click op is set and
handled entirely in cs2 (`cc_setop` + `cc_setonop` in `proc,skillmulti_itembutton_init`), so the
press never leaves the client until `proc,skillmulti_itembutton_triggered` re-targets it with
`cc_find(component, varc,skillmulti_quantity)` and triggers whatever it found. Two things follow:

- The subcomponent of the press *is* the quantity, so the server has to grant a **range**.
- The grant must be `IfEvent.PauseButton`, not `IfEvent.Op1`. Nothing named the subcomponent's op,
  so there is no op to fire; the pause-button grant is the only thing that turns the trigger into a
  packet, and it arrives as `ResumePauseButton`, which `ResumePauseButtonHandler` delivers by
  resuming the suspended coroutine — it publishes no event, so `onIfModalButton` never sees it.

**How to tell which kind an interface is,** before writing any of it: find the cs2 that handles its
button and look for `cc_find` followed by opcode `1121` (trigger). If it is there, the interface is
pause-button driven. Then check what the engine already does for the nearest twin —
`Player.ifConfirmDestroy`, `ifMenu`, `ifChoice` in `PlayerInterfaceExtensions.kt` all grant
`PauseButton` over a subcomponent range and then suspend. `if_setevents` over that range is also
what makes `cc_find` resolve at all; nothing is created by cs2.

### The op-name rule

There is a second, blunter rule that decides whether a panel can be driven at all. **The op *name*
can only come from the cache or from cs2 — the server cannot send one.** There is no `if_setop` in
the protocol; `if_setevents` grants the event, never the label. And the client builds a click from
the label, so an event without one is dead. The census that settles it: of every component in the
rev-233 cache, 2,997 have an op1 event and *all 2,997* also have op1 text. Not one has the event
without it. So, for any panel:

| what the cache gives the component | what it is |
| --- | --- |
| op text **and** the op event | already clickable — open the interface and bind, grant nothing |
| op text, no event | grant the event with `ifSetEvents(component, -1..-1, IfEvent.Op1)` |
| neither, but cs2 does `cc_find` + `1121` on it | pause-button: grant `PauseButton` over the range and suspend |
| neither, and no cs2 | **not drivable** — pick a different interface |

`-1..-1` is the window for a press on the component itself (`ifSetPauseText` in
`PlayerInterfaceExtensions.kt` is the proven example); `0..N` is only for a press the client
re-targets at a subcomponent. Getting that backwards aims the client's event window at children the
component does not have.

Worked out against this cache: **446/6** (jewellery) are row one — every button ships with `op1` and
its event, and no clientscript in the cache even mentions 446, so `Jewellery` opens the panel and
grants nothing. **270** is row three. **324** (the tanner) is row four — blank text components, no
ops, no events, no cs2 — so tanning goes through the make-menu instead, and interface 324 is not
used. Construction's 458 is row four as well: not one of its components has an op or an event, so
`BuildMenu`'s `Op1` grant cannot work either. That one is still unfixed.

## 8. Tests

Two classes: a config test (`runBasicGameTest`) asserting every authored obj/seq/component resolves
in the cache and the cross-table joins hold; and a script test (`runGameTest`) driving real events.

```kotlin
@Execution(ExecutionMode.SAME_THREAD)   // methods share one world otherwise
class FletchingScriptTest {
    @Test fun GameTestState.`cut logs`() =
        runGameTest(LogFletching::class) {
            player.inv[0] = InvObj(objs.knife)
            useOnHeld(objs.knife, FletchingObjs.logs)                // publish HeldUEvents.Type
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)
            assertEquals(15, player.count(FletchingObjs.arrow_shaft))
        }
}
```

Traps that cost a cycle each:

- **Answer a suspended dialogue with `player.resumePauseButton(component, sub)`**, `ifButton` only
  for interfaces that really do send `IfButton`. Both queue the press, so both work with the modal
  open; publishing `IfModalButton` inside `withProtectedAccess` does *not* — an open modal makes the
  player access-protected and the block silently no-ops.
- **A green test proves the server half only.** Both harness helpers hand the message straight to a
  handler, so they say nothing about whether the real client would ever send it. Anything driven by
  a cs2 panel needs a look at the actual client before you call it done.
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
