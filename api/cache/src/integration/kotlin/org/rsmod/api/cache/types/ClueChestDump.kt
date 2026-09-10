package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * What the rev-233 cache knows about clue keys, reward caskets and the clue reward screen, dumped
 * for `content/custom/clue-chest`.
 *
 * **Every clue key looks the same.** The eleven `trail_clue_medium_riddle*_key` objs and
 * `trail_elite_riddle_key32` are all model 2372 with no recolour, no inventory ops, 20gp, members,
 * tradeable, category 180; only the name differs. There is no beginner, easy, hard or master key at
 * all, which is why `ClueKeyBuilds` mints three with the same fields.
 *
 * **The Edgeville bank's "Loot Chest" is free to bind.** `wildy_hub_loot_chest_multi` (43486) is a
 * multiloc on varbit 13651 over `wildy_hub_loot_chest_closed` (43484) and `_open` (43485), both
 * `Loot` on op1.
 *
 * **The reward screen drives itself.** `trail_rewardscreen` (73) component 0 carries `onLoad=[2088,
 * event_com, 73:3]`; `trail_rewardscreen_init` pushes inv 141, draws it through proc 2090 and
 * registers `if_setoninvtransmit(trail_rewardscreen_draw, ..., 141)`. So it is filled and opened
 * exactly like `barrows_reward`. Inv 141 (`trail_rewardinv`) holds 12.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ClueChestDump {
    @Test
    fun GameTestState.`dump clue key and casket objs`() = runBasicGameTest {
        val objs =
            cacheTypes.objs.values
                .filter {
                    val n = it.internalName ?: return@filter false
                    (n.startsWith("trail_") && n.contains("key")) ||
                        n.startsWith("trail_reward_casket") ||
                        n.startsWith("trail_key_") ||
                        n == "trail_clue_medium_map002"
                }
                .sortedBy { it.id }
        for (o in objs) {
            println(
                "OBJ ${o.id} sym=${o.internalName} name='${o.name}' desc='${o.desc}' " +
                    "model=${o.model} zoom2d=${o.zoom2d} xan2d=${o.xan2d} yan2d=${o.yan2d} " +
                    "zan2d=${o.zan2d} xof2d=${o.xof2d} yof2d=${o.yof2d} " +
                    "recolS=${o.recolS.toList()} recolD=${o.recolD.toList()} " +
                    "retexS=${o.retexS.toList()} retexD=${o.retexD.toList()} " +
                    "iop=${o.iop.toList()} op=${o.op.toList()} stackable=${o.stackable} " +
                    "cost=${o.cost} members=${o.members} tradeable=${o.tradeable} " +
                    "category=${o.category} weight=${o.weight} certlink=${o.certlink} " +
                    "placeholderlink=${o.placeholderlink} resize=${o.resizeX},${o.resizeY}," +
                    "${o.resizeZ} ambient=${o.ambient} contrast=${o.contrast} " +
                    "dummyitem=${o.dummyitem} contentGroup=${o.contentGroup} " +
                    "params=${o.paramMap?.primitiveMap}"
            )
        }
    }

    @Test
    fun GameTestState.`dump edgeville loot chest locs`() = runBasicGameTest {
        for (id in listOf(43484, 43485, 43486)) {
            val loc = cacheTypes.locs[id] ?: continue
            val ops = loc.op.withIndex().filter { it.value != null }
            println(
                "LOC ${loc.id} sym=${loc.internalName} name='${loc.name}' " +
                    "size=${loc.width}x${loc.length} ops=${ops.map { "op${it.index + 1}='${it.value}'" }} " +
                    "multiVarBit=${loc.multiVarBit} multiVarp=${loc.multiVarp} " +
                    "multiLoc=${loc.multiLoc.toList()} default=${loc.multiLocDefault}"
            )
        }
    }

    @Test
    fun GameTestState.`dump trail reward screen`() = runBasicGameTest {
        val comps =
            cacheTypes.components.values
                .filter { it.interfaceId == TRAIL_REWARD }
                .sortedBy { it.component }
        val scriptIds = mutableSetOf<Int>()
        println("=== INTERFACE $TRAIL_REWARD (${comps.size} components) ===")
        for (comp in comps) {
            println(
                "COMP $TRAIL_REWARD:${comp.component} sym=${comp.internalName} " +
                    "type=${comp.type} text='${comp.text}'"
            )
            val hooks =
                listOf(
                    "onLoad" to comp.onLoad,
                    "onVarTransmit" to comp.onVarTransmit,
                    "onInvTransmit" to comp.onInvTransmit,
                    "onOp" to comp.onOp,
                    "onTimer" to comp.onTimer,
                )
            for ((label, hook) in hooks) {
                if (hook == null) continue
                println("  $label=${hook.contentToString()}")
                (hook.firstOrNull() as? Int)?.let(scriptIds::add)
            }
        }
        val scripts =
            cacheTypes.clientscripts.values.filter {
                it.id in scriptIds || it.internalName?.contains("trail_reward") == true
            }
        for (script in scripts.sortedBy { it.id }) {
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
        val inv = cacheTypes.invs[141]
        println("INV 141 sym=${inv?.internalName} size=${inv?.size} flags=${inv?.flags}")
        println(
            "TRAIL_SCRIPT_NAMES=" +
                cacheTypes.clientscripts.values
                    .mapNotNull { it.internalName }
                    .filter { it.contains("trail") || it.contains("casket") }
                    .sorted()
        )
    }

    private companion object {
        const val TRAIL_REWARD = 73
    }
}
