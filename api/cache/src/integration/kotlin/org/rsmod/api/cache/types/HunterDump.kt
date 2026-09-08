package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Throwaway dump used to author `content/skills/hunter` against the real cache rather than against
 * assumptions. The op slots for "Lay" on the trap items and "Check"/"Dismantle" on the trap locs
 * are the whole point: mining learned the hard way that ops must be decoded, not guessed.
 *
 * Run with:
 * ```
 * ./gradlew :api:cache:integration --tests "org.rsmod.api.cache.types.HunterDump" -i
 * ```
 */
class HunterDump {
    @Test
    fun GameTestState.`dump hunter objs`() = runBasicGameTest {
        val wanted =
            Regex(
                "^(hunting_(ojibway_bird_snare|box_trap|snare|butterfly_net|teasing_stick)" +
                    "|chinchompa_(captured|big_captured)" +
                    "|huntingbeast_[a-z0-9]+" +
                    "|spit_raw_bird_meat|feather|rope|net|knife|logs)$"
            )
        val matches =
            cacheTypes.objs.values
                .filter { it.internalName?.matches(wanted) == true }
                .sortedBy { it.id }
        for (obj in matches) {
            println(
                "OBJ ${obj.id} sym=${obj.internalName} name='${obj.name}' " +
                    "op=${obj.op.toList()} iop=${obj.iop.toList()} " +
                    "stack=${obj.stackable} content=${obj.contentGroup} cat=${obj.category}"
            )
        }
        println("TOTAL_OBJS=${matches.size}")
    }

    @Test
    fun GameTestState.`dump hunter locs`() = runBasicGameTest {
        val matches =
            cacheTypes.locs.values
                .filter { it.internalName?.startsWith("hunting_") == true }
                .sortedBy { it.internalName }
        for (loc in matches) {
            println(
                "LOC ${loc.id} sym=${loc.internalName} name='${loc.name}' " +
                    "ops=${loc.op.toList()} ${loc.width}x${loc.length} " +
                    "content=${loc.contentGroup} cat=${loc.category}"
            )
        }
        println("TOTAL_LOCS=${matches.size}")
    }

    @Test
    fun GameTestState.`dump hunter npcs`() = runBasicGameTest {
        val matches =
            cacheTypes.npcs.values
                .filter {
                    val name = it.internalName ?: return@filter false
                    name.startsWith("hunting_") ||
                        name.startsWith("huntingbeast_") ||
                        name.startsWith("salamander_")
                }
                .sortedBy { it.id }
        for (npc in matches) {
            println(
                "NPC ${npc.id} sym=${npc.internalName} name='${npc.name}' size=${npc.size} " +
                    "ops=${npc.op.toList()} move=${npc.moveRestrict} walk=${npc.blockWalk} " +
                    "wander=${npc.wanderRange} max=${npc.maxRange} " +
                    "huntRange=${npc.huntRange} huntMode=${npc.huntMode} " +
                    "content=${npc.contentGroup} cat=${npc.category}"
            )
        }
        println("TOTAL_NPCS=${matches.size}")
    }
}
