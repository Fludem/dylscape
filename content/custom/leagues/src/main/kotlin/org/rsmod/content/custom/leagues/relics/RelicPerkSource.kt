package org.rsmod.content.custom.leagues.relics

import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.PerkSource
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.game.entity.Player
import org.rsmod.game.type.stat.StatType

/**
 * Which relic grants which [Perk].
 *
 * The three gathering relics split in two, the way their vanilla text does: the echo tool itself
 * works for anyone holding the relic, but the skilling benefits only apply "when this item is in
 * your inventory or equipped". Everything else comes with the relic alone.
 */
class RelicPerkSource : PerkSource {
    override fun Player.has(perk: Perk): Boolean =
        when (perk) {
            Perk.EchoPickaxe -> hasRelic(Relic.PowerMiner)
            Perk.MiningSecondChance,
            Perk.RockHoldsFourOres,
            Perk.MiningToBank -> hasRelic(Relic.PowerMiner) && carries(league_objs.echo_pickaxe)

            Perk.EchoAxe -> hasRelic(Relic.Lumberjack)
            Perk.WoodcuttingSecondChance,
            Perk.WoodcuttingToBank -> hasRelic(Relic.Lumberjack) && carries(league_objs.echo_axe)
            Perk.NeverFailFire -> hasRelic(Relic.Lumberjack)

            Perk.EchoHarpoon -> hasRelic(Relic.AnimalWrangler)
            Perk.FishingSecondChance,
            Perk.FishingFaster,
            Perk.FishingToBank ->
                hasRelic(Relic.AnimalWrangler) && carries(league_objs.echo_harpoon)
            Perk.NeverBurnFood -> hasRelic(Relic.AnimalWrangler)

            Perk.AgilityMarkCoins -> hasRelic(Relic.CornerCutter)
            Perk.SlayerAlwaysOnTask -> hasRelic(Relic.SlayerMaster)
            Perk.GoldenAlchemy -> hasRelic(Relic.GoldenGod)
        }
}

/** Corner Cutter's 25% bonus Agility experience. */
class CornerCutterXp : XpMod {
    override fun Player.modifier(stat: StatType): Double =
        if (stat.id == stats.agility.id && hasRelic(Relic.CornerCutter)) BONUS else 0.0

    private companion object {
        const val BONUS = 0.25
    }
}
