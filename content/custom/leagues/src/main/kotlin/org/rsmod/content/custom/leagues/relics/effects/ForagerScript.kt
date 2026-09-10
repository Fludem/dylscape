package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.carries
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.content.skills.fishing.scripts.Fishing
import org.rsmod.content.skills.herblore.configs.HerbloreRecipes
import org.rsmod.content.skills.hunter.scripts.HunterTrapScript
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.entity.Player
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Friendly Forager's herb finding: while the Forager's pouch is carried or worn, every gathered
 * ore, log, fish or hunter catch has a 1 in [FIND_CHANCE] chance to turn up a grimy herb.
 *
 * The herb is any the player's Herblore level can clean, picked evenly, and it goes to the bank so
 * it never costs an inventory slot mid-session - the inventory only when the bank is full, and the
 * floor after that. The relic's Herblore perks live in the herblore module.
 */
class ForagerScript
@Inject
constructor(
    private val random: GameRandom,
    private val invTypes: InvTypeList,
    private val objTypes: ObjTypeList,
    private val objRepo: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<Mining.MinedOre> { forage(player) }
        onEvent<Woodcutting.CutLogs> { forage(player) }
        onEvent<Fishing.CaughtFish> { forage(player) }
        onEvent<HunterTrapScript.CaughtCreature> { forage(player) }
    }

    private fun forage(player: Player) {
        if (
            !player.hasRelic(Relic.FriendlyForager) || !player.carries(league_objs.foragers_pouch)
        ) {
            return
        }
        if (random.of(FIND_CHANCE) != 0) {
            return
        }
        val cleanable = HerbloreRecipes.cleaning.filter { it.levelReq <= player.herbloreLvl }
        if (cleanable.isEmpty()) {
            return
        }
        val herb = random.pick(cleanable).grimy
        val bank = player.invMap.getOrPut(invTypes[invs.bank])
        val banked = !bank.isFull() && player.invAdd(bank, herb).success
        if (!banked) {
            player.invAddOrDrop(objRepo, herb)
        }
        val name = objTypes[herb].name.lowercase()
        player.spam("Your Forager's pouch finds a $name${if (banked) " for your bank" else ""}.")
    }

    private companion object {
        const val FIND_CHANCE = 3
    }
}
