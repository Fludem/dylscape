package org.rsmod.api.cache.types

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * What the rev-233 cache knows about Barrows, dumped rather than guessed.
 *
 * The cache ships the whole client half - six brothers, ~115 locs, three interfaces with their
 * clientscripts, and the full varbit state machine - and none of it was referenced from Kotlin
 * before `content/custom/barrows`. These dumps settled four questions, and the answers are recorded
 * here so nobody has to ask them twice.
 *
 * **The brothers are already statted.** `api/cache-enricher` gives them only an examine string, but
 * the *cache* carries combat levels, attack and defence bonuses, melee/ranged strength and
 * `attackrate` - all matching live. What they do not carry is `attack_melee` (which is the bonus
 * `NvPMeleeAccuracy` actually reads, so without it the melee brothers barely land a hit),
 * `defence_ranged`, `defence_light`/`_standard`/`_heavy`, `npc_attack_type`, and any attack, defend
 * or death animation. They also default to `Wander` with no hunt mode, so they ignore the player.
 *
 * **The doors, the ladders and the chest are multilocs over the vanilla varbits**, so their state
 * is a varbit write and never a `locRepo` change. `barrows_door_a_r` switches on varbit 469 between
 * `barrows_door_unlocked_r` (op1 `Open`) and `barrows_door_locked_r` (no ops), and every door from
 * `a` to `p` follows on varbits 469-484. `barrows_stone_chest` switches on varbit 1394 between
 * closed (op1 `Open`) and open (op1 `Search`, op2 `Close`), and the four `barrows_ladder_*` appear
 * on varbit 4743.
 *
 * **The tunnels do not exist.** This is the important negative result. `scan whole map for barrows
 * tunnel locs` walks all 2048x2048x4 zones and finds *zero* placements of any barrows door, ladder,
 * chest or rockslide - the multiloc wiring above is real but nothing in the map ever references it.
 * Mapsquare 55_151 holds locs on level 3 only (the six crypts) plus four torch fires on level 1;
 * level 0, where OSRS puts the tunnel maze, has no locs and not one walkable tile. So a reward
 * chest has to be placed by us; there is no tunnel complex to walk into.
 *
 * **The spade already carries `Dig`** on iop1, which is what makes `onOpHeld1(objs.spade)` a live
 * binding rather than a dead one - the client builds a click from the op's *name*, and the server
 * cannot send one. The mounds themselves carry no loc at all, so a dig on a coordinate is the only
 * way in.
 *
 * **Both interfaces drive themselves.** `barrows_overlay` (24) component 0 carries
 * `onLoad=[clientscript,barrows_overlay_init, ...]` with all twelve of its own component refs, so
 * the server only opens it and writes varbits. `barrows_reward` (155) component 3 carries
 * `onLoad=[clientscript,barrows_reward_init]`, and that script pushes inv **141**
 * (`trail_rewardinv`, the shared clue-scroll reward inv), draws from it with `inv_getobj` and
 * registers `if_setoninvtransmit` on it. So the reward panel is driven exactly like a shop: fill
 * `trail_rewardinv`, `startInvTransmit`, open 155.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BarrowsDump {
    @Test
    fun GameTestState.`dump barrows locs`() = runBasicGameTest {
        val matches =
            cacheTypes.locs.values
                .filter { it.internalName?.startsWith("barrow") == true }
                .sortedBy { it.id }
        for (loc in matches) {
            val ops = loc.op.withIndex().filter { it.value != null }
            val multi =
                if (loc.multiLoc.isNotEmpty()) {
                    " multiVarBit=${loc.multiVarBit} multiVarp=${loc.multiVarp}" +
                        " multiDefault=${loc.multiLocDefault} multiLoc=${loc.multiLoc.toList()}"
                } else {
                    ""
                }
            println(
                "LOC ${loc.id} sym=${loc.internalName} name='${loc.name}' " +
                    "size=${loc.width}x${loc.length} blockWalk=${loc.blockWalk} " +
                    "ops=${ops.map { "op${it.index + 1}='${it.value}'" }}$multi"
            )
        }
        println("TOTAL_LOCS=${matches.size}")
    }

    /** Combat stats and every param, with param ids resolved to their names. */
    @Test
    fun GameTestState.`dump barrows npcs`() = runBasicGameTest {
        val matches =
            cacheTypes.npcs.values
                .filter { it.internalName?.startsWith("barrows_") == true }
                .sortedBy { it.id }
        for (npc in matches) {
            val ops = npc.op.withIndex().filter { it.value != null }
            println(
                "NPC ${npc.id} sym=${npc.internalName} name='${npc.name}' size=${npc.size} " +
                    "vislevel=${npc.vislevel} hp=${npc.hitpoints} att=${npc.attack} " +
                    "str=${npc.strength} def=${npc.defence} rng=${npc.ranged} " +
                    "magic=${npc.magic} wander=${npc.wanderRange} maxRange=${npc.maxRange} " +
                    "attackRange=${npc.attackRange} huntRange=${npc.huntRange} " +
                    "huntMode=${npc.huntMode} defaultMode=${npc.defaultMode} " +
                    "readyAnim=${npc.readyAnim} walkAnim=${npc.walkAnim} " +
                    "ops=${ops.map { "op${it.index + 1}='${it.value}'" }}"
            )
            val params = npc.paramMap
            if (params == null) {
                println("  params=NONE")
                continue
            }
            for ((key, value) in params.primitiveMap) {
                println("  param $key (${cacheTypes.params[key]?.internalName ?: "?"}) = $value")
            }
        }
        println("TOTAL_NPCS=${matches.size}")
    }

    /** Component hooks for the overlay, the puzzle and the reward panel. */
    @Test
    fun GameTestState.`dump barrows interface components`() = runBasicGameTest {
        for (interfaceId in listOf(BARROWS_OVERLAY, BARROWS_PUZZLE, BARROWS_REWARD)) {
            val comps =
                cacheTypes.components.values
                    .filter { it.interfaceId == interfaceId }
                    .sortedBy { it.component }
            println("=== INTERFACE $interfaceId (${comps.size} components) ===")
            for (comp in comps) {
                val ops = comp.op.withIndex().filter { it.value.isNotBlank() }
                println(
                    "COMP $interfaceId:${comp.component} sym=${comp.internalName} " +
                        "type=${comp.type} text='${comp.text}' " +
                        "ops=${ops.map { "op${it.index + 1}='${it.value}'" }}"
                )
                val hooks =
                    listOf(
                        "onLoad" to comp.onLoad,
                        "onVarTransmit" to comp.onVarTransmit,
                        "onOp" to comp.onOp,
                        "onMouseOver" to comp.onMouseOver,
                        "onMouseLeave" to comp.onMouseLeave,
                        "onClick" to comp.onClick,
                        "onTimer" to comp.onTimer,
                    )
                for ((label, hook) in hooks) {
                    if (hook == null) continue
                    println("  $label=${hook.contentToString()}")
                }
            }
        }
    }

    /**
     * The overlay and reward clientscripts. `barrows_reward_init` is the one that matters: it names
     * inv 141 as the panel's source.
     */
    @Test
    fun GameTestState.`dump barrows clientscripts`() = runBasicGameTest {
        val scripts =
            cacheTypes.clientscripts.values
                .filter { it.internalName?.contains("barrows") == true }
                .sortedBy { it.id }
        for (script in scripts) {
            println(
                "=== SCRIPT ${script.id} ${script.internalName} " +
                    "intArgs=${script.intArgumentCount} strArgs=${script.stringArgumentCount} " +
                    "ops=${script.commands.size}"
            )
            for (i in script.commands.indices) {
                val str = script.stringOperands[i]
                if (str != null) {
                    println("  $i: cmd=${script.commands[i]} str=\"$str\"")
                } else {
                    println("  $i: cmd=${script.commands[i]} op=${script.intOperands[i]}")
                }
            }
        }
        println("TOTAL_SCRIPTS=${scripts.size}")
    }

    /**
     * Where the crypt locs actually sit. `locRegistry` is the real map, unlike the empty scoped
     * world a plain `runGameTest` gets, so this is the only honest source for the coordinate table
     * in `BarrowsMap`.
     */
    @Test
    fun GameTestState.`dump barrows loc placements`() = runAdvancedGameTest { advanced ->
        val registry = advanced.readOnly.locRegistry
        for ((label, zoneZBase) in listOf("SURFACE" to SURFACE_ZONE_Z, "UNDER" to UNDER_ZONE_Z)) {
            println("=== PLACEMENTS $label ===")
            var count = 0
            for (level in 0 until LEVELS) {
                for (zoneX in MAPSQUARE_ZONE_X until MAPSQUARE_ZONE_X + ZONES_PER_SQUARE) {
                    for (zoneZ in zoneZBase until zoneZBase + ZONES_PER_SQUARE) {
                        for (loc in registry.findAll(ZoneKey(zoneX, zoneZ, level))) {
                            val name = cacheTypes.locs[loc.id]?.internalName ?: continue
                            if (!name.startsWith("barrow")) continue
                            println(
                                "PLACED $name (${loc.id}) at ${loc.coords} " +
                                    "shape=${loc.shape} angle=${loc.angle}"
                            )
                            count++
                        }
                    }
                }
            }
            println("PLACEMENT_COUNT_$label=$count")
        }
    }

    /**
     * The negative result the whole design turns on: no barrows door, ladder, chest or rockslide is
     * placed anywhere in the map. Prints `SCAN_FOUND=0` against this cache.
     */
    @Test
    fun GameTestState.`scan whole map for barrows tunnel locs`() = runAdvancedGameTest { advanced ->
        val registry = advanced.readOnly.locRegistry
        val wanted =
            cacheTypes.locs.values
                .filter {
                    val n = it.internalName ?: return@filter false
                    n.startsWith("barrows_door") ||
                        n.startsWith("barrows_ladder") ||
                        n.startsWith("barrows_stone_chest") ||
                        n.startsWith("barrows_rockslide_")
                }
                .associate { it.id to it.internalName }
        println("SCANNING for ${wanted.size} loc ids")
        var found = 0
        for (level in 0 until LEVELS) {
            for (zoneX in 0 until MAP_ZONE_LENGTH) {
                for (zoneZ in 0 until MAP_ZONE_LENGTH) {
                    for (loc in registry.findAll(ZoneKey(zoneX, zoneZ, level))) {
                        val name = wanted[loc.id] ?: continue
                        println("SCANHIT $name (${loc.id}) at ${loc.coords} shape=${loc.shape}")
                        found++
                    }
                }
            }
        }
        println("SCAN_FOUND=$found")
    }

    /**
     * ASCII map of the crypt complex, which is what the coordinate table was read off. Level 3 is
     * the six chambers; level 1 prints as bare floor and level 0 as solid rock, which is the other
     * half of the "there are no tunnels" finding.
     */
    @Test
    fun GameTestState.`render the barrows underground square`() =
        runInjectedGameTest(BarrowsCollision::class) { deps ->
            for (level in 0 until LEVELS) {
                println("=== RENDER level=$level (x $SQUARE_BASE_X.., z $UNDER_BASE_Z..) ===")
                for (z in UNDER_BASE_Z + SQUARE_TILES - 1 downTo UNDER_BASE_Z) {
                    val row = StringBuilder()
                    for (x in SQUARE_BASE_X until SQUARE_BASE_X + SQUARE_TILES) {
                        val flags = deps.collision[CoordGrid(x, z, level)]
                        row.append(if (flags and CollisionFlag.BLOCK_WALK != 0) '#' else '.')
                    }
                    println("R$level ${z.toString().padStart(5)} $row")
                }
            }
        }

    /**
     * Walkability around the six surface dig spots and the six crypt staircases, which is what the
     * mound-to-crypt coordinate pairs in `BarrowsMap` were checked against. The mounds are bare
     * terrain - no loc is placed on them - so entry has to be a dig on a coordinate, the way OSRS
     * does it, rather than an op on a loc.
     */
    @Test
    fun GameTestState.`check barrows entry and exit tiles`() =
        runInjectedGameTest(BarrowsCollision::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            fun render(label: String, centre: CoordGrid) {
                println("=== $label $centre ===")
                for (dz in 4 downTo -4) {
                    val row = StringBuilder()
                    for (dx in -4..4) {
                        val at = CoordGrid(centre.x + dx, centre.z + dz, centre.level)
                        val open = deps.collision[at] and blocked == 0
                        row.append(
                            if (dx == 0 && dz == 0) (if (open) '@' else '!')
                            else if (open) '.' else '#'
                        )
                    }
                    println("  $row")
                }
            }
            for ((brother, mound) in MOUNDS) {
                render("MOUND $brother", mound)
            }
            for ((brother, stairs) in STAIRCASES) {
                render("STAIRS $brother", stairs)
            }
        }

    /**
     * The spade's inventory ops. Digging is bound with `onOpHeld1`, and an op the cache does not
     * *name* is dead - the client builds a click from the label, so an event without one never
     * leaves the client. This is the check that says the binding can work at all.
     */
    @Test
    fun GameTestState.`dump spade ops`() = runBasicGameTest {
        val spade = cacheTypes.objs.values.single { it.internalName == "spade" }
        println("OBJ ${spade.id} sym=${spade.internalName} name='${spade.name}'")
        println("  iop=${spade.iop.toList()}")
        println("  op=${spade.op.toList()}")
    }

    private companion object {
        const val BARROWS_OVERLAY = 24
        const val BARROWS_PUZZLE = 25
        const val BARROWS_REWARD = 155

        /** Mapsquare 55: the surface at z-square 51, the crypts at 151. */
        const val MAPSQUARE_ZONE_X = 55 * 8
        const val SURFACE_ZONE_Z = 51 * 8
        const val UNDER_ZONE_Z = 151 * 8
        const val ZONES_PER_SQUARE = 8
        const val SQUARE_BASE_X = 55 * 64
        const val UNDER_BASE_Z = 151 * 64
        const val SQUARE_TILES = 64
        const val LEVELS = 4

        /** The six surface dig spots, as OSRS documents them. */
        val MOUNDS =
            listOf(
                "ahrim" to CoordGrid(3565, 3288, 0),
                "dharok" to CoordGrid(3575, 3298, 0),
                "guthan" to CoordGrid(3577, 3281, 0),
                "karil" to CoordGrid(3565, 3276, 0),
                "torag" to CoordGrid(3554, 3283, 0),
                "verac" to CoordGrid(3557, 3298, 0),
            )

        /** Read off `dump barrows loc placements`. */
        val STAIRCASES =
            listOf(
                "ahrim" to CoordGrid(3558, 9703, 3),
                "dharok" to CoordGrid(3557, 9718, 3),
                "guthan" to CoordGrid(3534, 9705, 3),
                "karil" to CoordGrid(3546, 9685, 3),
                "torag" to CoordGrid(3565, 9683, 3),
                "verac" to CoordGrid(3578, 9703, 3),
            )

        /** 16384 tiles across, eight tiles to a zone. */
        const val MAP_ZONE_LENGTH = 2048
    }
}

private class BarrowsCollision @Inject constructor(val collision: CollisionFlagMap)
