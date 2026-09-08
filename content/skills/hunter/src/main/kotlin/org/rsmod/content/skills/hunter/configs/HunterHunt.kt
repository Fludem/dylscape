package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.builders.hunt.HuntModeBuilder
import org.rsmod.api.type.refs.hunt.HuntModeReferences
import org.rsmod.api.type.script.dsl.HuntModePluginBuilder
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.hunt.HuntNobodyNear
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis

/**
 * What makes a creature walk into a trap.
 *
 * Each mode points a creature family at the invisible attractor npc a laid trap spawns. Once the
 * hunt processor finds one it issues `opNpc1`, which both takes the creature out of the wander
 * processor and routes it to the trap tile; arriving there publishes the AI op the trap engine
 * listens for. That is the vanilla mechanism, and the reason the cache ships those npcs at all.
 *
 * **One mode per trap kind, not one shared mode.** `checkNpc` only matches a single npc id here
 * because the category branch is broken upstream: `NpcHuntProcessor.huntNpc` compares `check.npc`
 * (an npc id) against `npc.type.category`, so a category-keyed mode silently matches nothing.
 *
 * Field choices worth keeping:
 * - [HuntVis.LineOfWalk] — a ground trap has to be walkable to, not merely visible.
 * - `rate = 5` — the builder floors non-player hunts at 3; 5 gives roughly a check every few ticks.
 * - `findKeepHunting = true` — without it `consumeHuntTarget` zeroes the hunt clock after one
 *   attempt and a creature that failed to be caught would never approach a trap again.
 * - [HuntNobodyNear.KeepHunting] — **not** `PauseHunt`, which is the tempting choice. `PauseHunt`
 *   gates the hunt clock on `Npc.isAnyoneNear()`, which is npc-info-protocol activity rather than
 *   real proximity, so a creature a few tiles off-screen stops hunting entirely and traps go dead.
 *   There is nothing to save here anyway: a trap only exists because a player laid it, and it
 *   collapses on its own once its owner leaves.
 */
object HunterHunt : HuntModeReferences() {
    val snare = find("hunter_snare_hunt")
    val boxtrap = find("hunter_boxtrap_hunt")
    val nettrap = find("hunter_nettrap_hunt")
    val deadfall = find("hunter_deadfall_hunt")
}

internal object HunterHuntBuilder : HuntModeBuilder() {
    init {
        build("hunter_snare_hunt") {
            checkNpc { npc = HunterNpcs.snare_trap_npc }
            trapDefaults()
        }
        build("hunter_boxtrap_hunt") {
            checkNpc { npc = HunterNpcs.box_trap_npc }
            trapDefaults()
        }
        build("hunter_nettrap_hunt") {
            checkNpc { npc = HunterNpcs.net_trap_npc }
            trapDefaults()
        }
        build("hunter_deadfall_hunt") {
            checkNpc { npc = HunterNpcs.deadfall_trap_npc }
            trapDefaults()
        }
    }

    private fun HuntModePluginBuilder.trapDefaults() {
        type = HuntType.Npc
        checkVis = HuntVis.LineOfWalk
        findKeepHunting = true
        findNewMode = NpcMode.OpNpc1
        nobodyNear = HuntNobodyNear.KeepHunting
        rate = 5
    }
}
