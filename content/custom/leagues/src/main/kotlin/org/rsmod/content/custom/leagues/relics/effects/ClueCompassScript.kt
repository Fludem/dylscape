package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.content.custom.cluechest.ClueKeyOpener
import org.rsmod.content.custom.cluechest.ClueTier
import org.rsmod.content.custom.cluechest.configs.ClueKeys
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Clue Compass relic, reworked around the clue keys this server has instead of trails:
 * - Every npc kill has a 1 in [KILL_CHANCE] chance to give a key, tiered by the npc's combat level.
 * - op1 Open-keys opens every key carried at once, from anywhere, through the chest's own
 *   [ClueKeyOpener].
 * - op2 Check counts the keys carried.
 *
 * The casket upgrade and double rewards are perks, applied by [ClueKeyOpener] wherever a key is
 * opened. `LeagueObjEditor` renames the compass's ops to match.
 */
class ClueCompassScript
@Inject
constructor(private val opener: ClueKeyOpener, private val rewards: ClueKeyRewards) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(league_objs.clue_compass) {
            if (holdsRelic()) {
                opener.openAll(this)
            }
        }
        onOpHeld2(league_objs.clue_compass) {
            if (holdsRelic()) {
                countKeys()
            }
        }
        onEvent<NpcDeathEvents.Killed> {
            val hunter = killer ?: return@onEvent
            if (!hunter.hasRelic(Relic.ClueCompass)) {
                return@onEvent
            }
            val tier = ClueKeyRewards.tierForCombatLevel(npc.visType.vislevel)
            if (rewards.roll(hunter, KILL_CHANCE, tier, npc.coords)) {
                hunter.mes("Your compass points you to a ${tier.name.lowercase()} clue key!")
            }
        }
    }

    private fun ProtectedAccess.holdsRelic(): Boolean {
        if (player.hasRelic(Relic.ClueCompass)) {
            return true
        }
        mes("The needle will not move for you.")
        return false
    }

    private fun ProtectedAccess.countKeys() {
        val counts = IntArray(ClueTier.entries.size)
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            val tier = ClueKeys.tiers[obj.id] ?: continue
            counts[tier.ordinal] += obj.count
        }
        if (counts.sum() == 0) {
            mes(
                "You are not carrying any clue keys. Every monster you kill has a 1 in " +
                    "$KILL_CHANCE chance to drop one."
            )
            return
        }
        val held =
            ClueTier.entries
                .filter { counts[it.ordinal] > 0 }
                .joinToString { "${counts[it.ordinal]} ${it.name.lowercase()}" }
        mes("You are carrying clue keys: $held.")
    }

    private companion object {
        const val KILL_CHANCE = 10
    }
}
