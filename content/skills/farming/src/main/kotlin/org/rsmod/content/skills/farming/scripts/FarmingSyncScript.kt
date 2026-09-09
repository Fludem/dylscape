package org.rsmod.content.skills.farming.scripts

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.skills.farming.configs.FarmingTimers
import org.rsmod.content.skills.farming.data.FarmingCrops
import org.rsmod.content.skills.farming.data.FarmingPatch
import org.rsmod.content.skills.farming.data.FarmingPatches
import org.rsmod.content.skills.farming.data.FarmingRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Keeps the fourteen farming transmit varbits pointed at the patches around the player.
 *
 * There is no per-patch storage on the client: `farming_transmit_a` is not "the Falador north
 * allotment", it is "whichever patch using slot A you are currently standing near". Jagex never
 * puts two patches sharing a slot in the same view, so writing the *nearest* patch for each slot
 * reproduces the real thing exactly, and walking from one farm to another rewrites them.
 *
 * A soft timer rather than a movement hook, because a patch's appearance changes on its own: a crop
 * that matures, a fruit that grows back, or weeds creeping over bare soil all need pushing without
 * the player doing anything. Growth itself is still not ticked -- see
 * [org.rsmod.content.skills .farming.data.PatchState] -- this only publishes what the timestamps
 * already say.
 */
class FarmingSyncScript @Inject constructor(private val registry: FarmingRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            transmit(player, registry)
            player.softTimer(FarmingTimers.sync, SYNC_CYCLES)
        }
        onPlayerSoftTimer(FarmingTimers.sync) { transmit(player, registry) }
        // Saving reads the registry, and saving happens after logout; see the account pipeline.
        onEvent<SessionStateEvent.Delete> { registry.remove(player) }
    }

    companion object {
        /**
         * Three seconds. Fast enough that a crop coming ready or a patch being raked shows up
         * without a noticeable wait, slow enough that it costs nothing: the pass is fourteen
         * comparisons and only writes a varbit that actually changed.
         */
        private const val SYNC_CYCLES: Int = 5

        /**
         * How close a patch has to be to claim its transmit slot: the radius of the scene the
         * client keeps loaded.
         *
         * Two patches sharing a slot can both be loaded -- the Falador allotment and the Draynor
         * belladonna patch are 45 tiles apart -- so the nearest wins, which means the patch the
         * player is standing at is always the one drawn correctly. The further one may show its
         * neighbour's crop, exactly as the real client does with the same fourteen varbits.
         */
        private const val RANGE: Int = 52

        fun transmit(player: Player, registry: FarmingRegistry) {
            val patches = registry[player]
            val now = System.currentTimeMillis()
            for (sharing in FarmingPatches.byVarBit.values) {
                val patch = nearest(player, sharing) ?: continue
                val state = patches[patch]
                val crop = if (state.empty) null else FarmingCrops.forSeed(state.seedId)
                val display = state.displayState(patch.kind, crop, now)
                if (player.vars[patch.varbit] != display) {
                    VarPlayerIntMapSetter.set(player, patch.varbit, display)
                }
            }
        }

        /**
         * Level is deliberately not part of the comparison. The redwood patch is placed across
         * three levels and the underwater seaweed patches sit on level 1, so a patch is claimed by
         * how close it is on the map, not by which floor its primary loc happens to be on.
         */
        private fun nearest(player: Player, patches: List<FarmingPatch>): FarmingPatch? {
            var best: FarmingPatch? = null
            var bestDistance = RANGE + 1
            for (patch in patches) {
                val distance =
                    maxOf(
                        abs(patch.coords.x - player.coords.x),
                        abs(patch.coords.z - player.coords.z),
                    )
                if (distance < bestDistance) {
                    best = patch
                    bestDistance = distance
                }
            }
            return best
        }
    }
}
