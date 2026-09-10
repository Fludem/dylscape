package org.rsmod.content.custom.leagues.relics

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.PerkSource
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.game.entity.Player
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.stat.StatTypeList

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

            Perk.CleanAllHerbs,
            Perk.SaveSecondary -> hasRelic(Relic.FriendlyForager)

            Perk.ThievingNeverFails,
            Perk.PickpocketCrowd,
            Perk.StallDoubleLoot -> hasRelic(Relic.DodgyDeals)

            Perk.ClueChestAnyTier -> hasRelic(Relic.FairysFlight)

            Perk.DoubleLoot,
            Perk.NotedLoot -> hasRelic(Relic.TreasureArbiter)

            Perk.InstantProduction -> hasRelic(Relic.ProductionMaster)

            Perk.SeedSaver,
            Perk.HalfGrownCrops,
            Perk.HarvestSaver -> hasRelic(Relic.Overgrown)

            Perk.CheapSpecials,
            Perk.AccurateSpecials,
            Perk.FastSpecRegen -> hasRelic(Relic.Specialist)
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

/**
 * Equilibrium's catch-up experience: skills below the player's average base level gain double, the
 * lowest skill (or skills, on a tie) triple, and everything else a tenth more.
 *
 * Levels are read fresh on every drop - 23 lookups - so a skill leaves the bonus the moment it
 * catches up.
 */
class EquilibriumXp @Inject constructor(private val statTypes: StatTypeList) : XpMod {
    override fun Player.modifier(stat: StatType): Double {
        if (!hasRelic(Relic.Equilibrium)) {
            return 0.0
        }
        val levels = statTypes.values.filterNot { it.unreleased }.map { statBase(it) }
        if (levels.isEmpty()) {
            return 0.0
        }
        val level = statBase(stat)
        return when {
            level <= levels.min() -> LOWEST_BONUS
            level < levels.average() -> BELOW_AVERAGE_BONUS
            else -> BASE_BONUS
        }
    }

    private companion object {
        const val LOWEST_BONUS = 2.0
        const val BELOW_AVERAGE_BONUS = 1.0
        const val BASE_BONUS = 0.1
    }
}
