package org.rsmod.content.custom.leagues.relics

import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_structs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.struct.StructType

/**
 * Every relic the vanilla `league_relics` screen offers, in the order the client numbers them.
 *
 * **Declaration order is load-bearing.** `[proc,league_relics_draw_selections]` gives each relic's
 * clickzone a running child index, tier by tier and slot by slot, and that index is the `comsub` a
 * View click arrives with - so [forComsub] is simply `entries[comsub]`. Tier 6 has only two relics,
 * which is why a fixed three-per-tier formula cannot work.
 *
 * [tier] is 0-based (the selection varbit's index); players see it as `tier + 1`. [slot] is the
 * 1-based key into the tier's relic enum, and is exactly what `league_relic_selection_<tier>`
 * stores. `RelicTableTest` checks all of this against the cache.
 *
 * Only [implemented] relics can be picked; the rest keep their place in the grid so the numbering
 * holds, and their description says "Coming soon".
 */
enum class Relic(
    val tier: Int,
    val slot: Int,
    val struct: StructType,
    val displayName: String,
    val implemented: Boolean = false,
    val item: ObjType? = null,
) {
    PowerMiner(0, 1, league_structs.power_miner, "Power Miner", true, league_objs.echo_pickaxe),
    Lumberjack(0, 2, league_structs.lumberjack, "Lumberjack", true, league_objs.echo_axe),
    AnimalWrangler(
        0,
        3,
        league_structs.animal_wrangler,
        "Animal Wrangler",
        true,
        league_objs.echo_harpoon,
    ),
    CornerCutter(
        1,
        1,
        league_structs.corner_cutter,
        "Corner Cutter",
        true,
        league_objs.sage_greaves,
    ),
    FriendlyForager(1, 2, league_structs.friendly_forager, "Friendly Forager"),
    DodgyDeals(1, 3, league_structs.dodgy_deals, "Dodgy Deals"),
    ClueCompass(2, 1, league_structs.clue_compass, "Clue Compass"),
    BankHeist(2, 2, league_structs.bank_heist, "Bank Heist", true, league_objs.bankers_briefcase),
    FairysFlight(2, 3, league_structs.fairys_flight, "Fairy's Flight"),
    GoldenGod(3, 1, league_structs.golden_god, "Golden God", true),
    Reloaded(3, 2, league_structs.reloaded, "Reloaded"),
    Equilibrium(3, 3, league_structs.equilibrium, "Equilibrium"),
    TreasureArbiter(4, 1, league_structs.treasure_arbiter, "Treasure Arbiter"),
    ProductionMaster(4, 2, league_structs.production_master, "Production Master"),
    SlayerMaster(4, 3, league_structs.slayer_master, "Slayer Master", true),
    TotalRecall(
        5,
        1,
        league_structs.total_recall,
        "Total Recall",
        true,
        league_objs.crystal_of_echoes,
    ),
    BankersNote(5, 2, league_structs.bankers_note, "Banker's Note", true, league_objs.bankers_note),
    PocketKingdom(6, 1, league_structs.pocket_kingdom, "Pocket Kingdom"),
    Grimoire(6, 2, league_structs.grimoire, "Grimoire", true, league_objs.arcane_grimoire),
    Overgrown(6, 3, league_structs.overgrown, "Overgrown"),
    Specialist(7, 1, league_structs.specialist, "Specialist"),
    Guardian(7, 2, league_structs.guardian, "Guardian"),
    LastStand(7, 3, league_structs.last_stand, "Last Stand", true);

    companion object {
        const val TIER_COUNT: Int = 8

        /**
         * Each tier's League Points threshold, straight from the tier structs' param 877. The
         * client greys a tier out below it, so the server has to agree.
         */
        val TIER_POINTS: List<Int> = listOf(0, 750, 1500, 2500, 5000, 8000, 16000, 25000)

        /**
         * Coins to swap a tier's relic for another. The first pick in a tier is free; these are
         * what changing your mind costs afterwards.
         */
        val REPICK_COSTS: List<Int> =
            listOf(
                100_000,
                500_000,
                1_000_000,
                2_500_000,
                5_000_000,
                10_000_000,
                25_000_000,
                50_000_000,
            )

        fun forComsub(comsub: Int): Relic? = entries.getOrNull(comsub)

        fun of(tier: Int, slot: Int): Relic? =
            entries.firstOrNull { it.tier == tier && it.slot == slot }

        fun inTier(tier: Int): List<Relic> = entries.filter { it.tier == tier }
    }
}
