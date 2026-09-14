package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onEvent
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.content.skills.thieving.configs.ThievingTargets
import org.rsmod.content.skills.thieving.scripts.Pickpocketing
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Dodgy Deals' pocketed clue keys: every successful pickpocket has a 1 in [KEY_CHANCE] chance to
 * find a key, tiered by the Thieving level the target needs.
 *
 * `Pickpocketed` is published once per npc robbed, so the crowd robs roll too. The relic's other
 * effects live in the thieving module behind perks, and its double experience in `DodgyDealsXp`.
 */
class DodgyDealsScript @Inject constructor(private val rewards: ClueKeyRewards) : PluginScript() {
    /** Raw npc id to the Thieving level its rung needs. */
    private val levels: Map<Int, Int> =
        ThievingTargets.all.entries.associate { (npc, target) -> npc.id to target.level }

    override fun ScriptContext.startup() {
        onEvent<Pickpocketing.Pickpocketed> {
            if (!player.hasRelic(Relic.DodgyDeals)) {
                return@onEvent
            }
            val level = levels[npc.id] ?: return@onEvent
            val tier = ClueKeyRewards.tierForThievingLevel(level)
            if (rewards.roll(player, KEY_CHANCE, tier, npc.coords)) {
                player.mes(
                    "You find a ${tier.name.lowercase()} clue key in the ${npc.name}'s pocket!"
                )
            }
        }
    }

    private companion object {
        const val KEY_CHANCE = 50
    }
}
