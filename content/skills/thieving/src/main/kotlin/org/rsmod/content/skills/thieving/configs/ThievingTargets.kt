package org.rsmod.content.skills.thieving.configs

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

private typealias pouches = ThievingObjs

private typealias npcs = ThievingTargetNpcs

/**
 * Every npc that can be pickpocketed, and what it is worth.
 *
 * ### How the lists were chosen
 *
 * Not by hand. `ThievingOpsDump` decodes every npc in the cache; a type belongs here when its
 * **display name** is one of the ladder's names (`Man`, `Woman`, `Drunken man`, `Farmer`,
 * `Warrior`, `Al Kharid warrior`, `Rogue`, `Master Farmer`, `Guard`, `Head Guard`, `Knight of
 * Ardougne`, `Watchman`, `Paladin`, `Hero`) *and* it carries `Pickpocket` on op3.
 * `ThievingConfigTest` asserts that rule in both directions, so a target the cache offers can no
 * longer be missed the way `al_kharid_man` was — the ladder used to list four of the cache's
 * eighteen `Man` types and quietly skip the rest, `al_kharid_man` among them.
 *
 * Deliberately outside the rule, and therefore outside this table:
 * - `thug` (525) and the west-Ardougne placeholders `knight_of_ardougne_west` (4267) /
 *   `paladin_west` (4262) carry no `Pickpocket` op at all. Their real, op-carrying twins are the
 *   `_vis` types, which *are* listed.
 * - `dttd_ham_guard_*_postquest` (4522-4526) put `Pickpocket` on **op1**, not op3, so `onOpNpc3`
 *   would never fire for them. They are post-quest H.A.M. content that nothing spawns yet.
 * - Everything with its own loot table rather than a coin pouch: H.A.M. members, cave goblins,
 *   bandits, Menaphite thugs, Tai Bwo Wannai villagers, TzHaar-Hur, Prifddinas elves, Darkmeyer
 *   vampyres, gnomes, priests and Varlamore's `Citizen` / `Tourist` / `Salvager` / `Wealthy
 *   citizen` families. Those are separate content, not extra rungs of this ladder.
 *
 * ### Why these are not a content group
 *
 * `contentGroup` is a single field on the type, and upstream's `GenericPersonConfig` already tags
 * `man`, `man2`, `man3`, `man_indoor`, `woman`, `woman2` and `woman3` into `content.person` to give
 * them Talk-to dialogue. `TypeUpdaterConfigs.merge` folds every editor's edits for the same id in
 * ClassGraph scan order, and that scan runs in parallel — so a second editor setting a thieving
 * group on `man` would win or lose the fold at random and silently break the dialogue. Registering
 * per npc type sidesteps it entirely, and costs only a loop in `startup()`.
 *
 * ### Why the numbers live here rather than in cache params
 *
 * Mining keeps its tuning in loc params; fishing keeps it in a Kotlin table. This follows fishing.
 * The two multipliers in [ThievingRates] are server policy rather than cache data and belong
 * somewhere visible, and putting per-target values in params would mean an `NpcEditor` edit for
 * every target — which is the exact clobber hazard described above.
 *
 * ### The ops
 *
 * `Pickpocket` sits on **op3** for every npc here, decoded from the cache rather than assumed
 * (`ThievingOpsDump`). Note that is op3 and not op2: `man` reads `[Talk-to, Attack, Pickpocket]`
 * while `farmer1` reads `[null, Attack, Pickpocket]` and `al_kharid_man` reads `[null, Attack,
 * Pickpocket]` too, so the slot is stable even where the earlier options are not.
 */
object ThievingTargetNpcs : NpcReferences() {
    /** Level-1 fodder: every `Man`, `Woman` and `Drunken man` in the cache. */
    val citizens: List<NpcType> =
        refs(
            "varrock_man1",
            "varrock_woman1",
            "man",
            "man2",
            "man3",
            "man4",
            "man4_for_musa_point",
            "woman",
            "woman2",
            "woman3",
            "al_kharid_man",
            "falador_man1",
            "falador_man2",
            "falador_man3",
            "falador_woman2",
            "ardougnian_male1",
            "ardougnian_female1",
            "karamja_man",
            "death_man_indoors1",
            "man_indoor",
            "zeah_man",
            "zeah_man2",
            "zeah_man3",
            "zeah_woman",
            "zeah_woman2",
            "zeah_woman3",
            "zeah_woman_outside",
            "shayzien_woman_1",
            "shayzien_woman_2",
            "shayzien_man_1",
            "shayzien_man_2",
        )

    /** Level-10 `Farmer`s, including the Varlamore and Kastori sets. */
    val farmers: List<NpcType> =
        refs(
            "farmer1",
            "farmer2",
            "farmer3",
            "farmer1_f",
            "farmer2_f",
            "farmer3_f",
            "farmer4",
            "varlamore_farmer_m_1",
            "varlamore_farmer_m_2",
            "varlamore_farmer_m_3",
            "varlamore_farmer_m_4",
            "varlamore_farmer_f_1",
            "varlamore_farmer_f_2",
            "varlamore_farmer_f_3",
            "varlamore_farmer_f_4",
            "kastori_farmer_m_1",
            "kastori_farmer_m_2",
            "kastori_farmer_f_1",
            "kastori_farmer_f_2",
            "tal_teklan_farmer",
        )

    /** `Warrior`, `Warrior woman` and the `Al Kharid warrior` that shares their rung. */
    val warriors: List<NpcType> =
        refs(
            "warrior_woman",
            "al_kharid_warrior",
            "warrior_woman_variant01",
            "warrior_woman_variant02",
            "warrior_man",
            "warrior_man_variant01",
            "warrior_man_variant02",
        )

    /** The lone `Rogue`. */
    val rogues: List<NpcType> = refs("rogue")

    /**
     * `Master Farmer`s. They pay seeds rather than a coin pouch; see [MasterFarmerSeeds].
     *
     * `martin_the_master_farmer` is here despite reading `Martin the Master Gardener` rather than
     * `Master Farmer`, which is why the display-name rule alone would miss him: he is the Draynor
     * master farmer, requires the same level 38, pays the same 43xp and rolls the same seed table.
     * He is also the first one most accounts ever meet, so leaving him off meant clicking
     * Pickpocket on the obvious target did nothing at all.
     */
    val masterFarmers: List<NpcType> =
        refs(
            "martin_the_master_farmer",
            "master_farmer_1",
            "master_farmer_2",
            "master_farmer_1_f",
            "master_farmer_2_f",
            "varlamore_master_farmer_m_1",
            "varlamore_master_farmer_m_2",
            "varlamore_master_farmer_m_3",
            "varlamore_master_farmer_m_4",
            "varlamore_master_farmer_f_1",
            "varlamore_master_farmer_f_2",
            "varlamore_master_farmer_f_3",
            "varlamore_master_farmer_f_4",
            "kastori_master_farmer_m_1",
            "kastori_master_farmer_m_2",
            "kastori_master_farmer_f_1",
            "kastori_master_farmer_f_2",
        )

    /**
     * Level-75 `Gnome`s. Women and children are the same rung wearing a different model, and OSRS
     * gives all three one table.
     *
     * `grim_gnome_incage_1` is the caged gnome from Grim Tales. It reads `Gnome` and carries the op
     * like the rest, so the module's own rule puts it here rather than making it a special case.
     */
    val gnomes: List<NpcType> =
        refs(
            "gnome",
            "browclothedgnome",
            "darkskinned_gnome",
            "grim_gnome_incage_1",
            "gnomefemale",
            "gnomefemale_dskinned",
            "gnomechild",
            "gnomechildblue",
            "gnomechildgreen",
        )

    /** `Guard` and Kourend's `Head Guard`, which is the same rung wearing a bigger model. */
    val guards: List<NpcType> =
        refs(
            "hos_town_guard_01",
            "hos_town_guard_02",
            "hos_town_guard_03",
            "hos_town_guard_04",
            "jail_guard_1",
            "jail_guard_2",
            "jail_guard_3",
            "jail_guard_4",
            "jail_guard_5",
            "fai_varrock_guard",
            "fai_varrock_guard_captain",
            "guard1",
            "fai_falador_guard1",
            "fai_falador_guard2",
            "fai_falador_guard3",
            "fai_falador_guard4",
            "fai_falador_guard5",
            "fai_falador_guard6",
            "falador_doric_area_guard",
            "ardougne_guard",
            "kourend_guard_m1",
            "kourend_guard_m1_big",
            "kourend_guard_m2",
            "kourend_guard_m2_big",
            "kourend_guard_m3",
            "kourend_guard_m3_big",
            "kourend_guard_m4",
            "kourend_guard_m4_big",
            "kourend_guard_f1",
            "kourend_guard_f1_big",
            "kourend_guard_f2",
            "kourend_guard_f2_big",
            "kourend_guard_f3",
            "kourend_guard_f3_big",
            "kourend_guard_f4",
            "kourend_guard_f4_big",
            "fai_varrock_guard02",
            "fai_varrock_guard02_variant01",
            "fai_varrock_guard02_variant02",
            "fai_varrock_guard02_f",
            "fai_varrock_guard02_f_variant01",
            "fai_varrock_guard02_f_variant02",
            "fai_varrock_guard_captain02",
            "guard1_variant01",
            "guard1_f",
            "guard1_f_variant01",
            "ardougne_guard_variant01",
            "ardougne_guard_f",
            "ardougne_guard_f_variant01",
            "fai_falador_guard1_variant01",
            "fai_falador_guard1_f",
            "fai_falador_guard1_variant02",
            "fai_falador_guard2_f",
            "fai_falador_guard3_f",
            "fai_falador_guard4_f",
            "varlamore_guard_m_1",
            "varlamore_guard_m_2",
            "varlamore_guard_m_3",
            "varlamore_guard_m_4",
            "varlamore_guard_m_5",
            "varlamore_guard_f_1",
            "varlamore_guard_f_2",
            "varlamore_guard_f_3",
            "varlamore_guard_f_4",
            "varlamore_guard_f_5",
            "aldarin_guard_m_1",
            "aldarin_guard_m_2",
            "aldarin_guard_m_3",
            "aldarin_guard_m_4",
            "aldarin_guard_m_5",
            "aldarin_guard_f_1",
            "aldarin_guard_f_2",
            "aldarin_guard_f_3",
            "aldarin_guard_f_4",
            "aldarin_guard_f_5",
            "auburnvale_guard_m_1",
            "auburnvale_guard_m_2",
            "auburnvale_guard_m_3",
            "auburnvale_guard_m_4",
            "auburnvale_guard_f_1",
            "auburnvale_guard_f_2",
            "auburnvale_guard_f_3",
            "auburnvale_guard_f_4",
            "tlati_guard_m_1",
            "tlati_guard_m_2",
            "tlati_guard_m_3",
            "tlati_guard_m_4",
            "tlati_guard_f_1",
            "tlati_guard_f_2",
            "tlati_guard_f_3",
            "tlati_guard_f_4",
        )

    /** `Knight of Ardougne`, including the `_vis` twins of the op-less west placeholders. */
    val knights: List<NpcType> =
        refs(
            "knight_of_ardougne",
            "knight_of_ardougne2",
            "knight_of_ardougne_west_vis",
            "knight_of_ardougne_f_west_vis",
            "knight_of_ardougne_f",
        )

    /** The Yanille `Watchman`. */
    val watchmen: List<NpcType> = refs("yanille_watchman")

    /** `Paladin`, likewise including the `_vis` twins. */
    val paladins: List<NpcType> =
        refs(
            "paladin",
            "paladin2",
            "paladin_west_vis",
            "paladin_west_f_vis",
            "paladin_variant01",
            "paladin_variant02",
            "paladin_f",
            "paladin_f_variant01",
        )

    /** `Hero`. */
    val heroes: List<NpcType> = refs("hero", "hero_variant01", "hero_f")

    private fun refs(vararg internal: String): List<NpcType> = internal.map { find(it) }
}

/**
 * What one successful pickpocket pays.
 *
 * Two shapes rather than one because the master farmer genuinely is a different kind of target: the
 * rest of the ladder pays a stackable purse whose only content is coins, and he pays a seed off his
 * own table. Modelling that as a nullable extra field on [PickpocketTarget] would leave every
 * pouch-only consumer -- the inventory-space check, the auto-open, `CoinPouches.coinRangeFor` --
 * reading a pouch he does not drop.
 */
sealed interface PickpocketLoot {
    /**
     * The ladder's default: a stackable purse, optionally with items around it.
     *
     * [coins] is the real OSRS range for the tier; [ThievingRates.LOOT_RATE] is applied when the
     * pouch is opened. [table] and [extras] are the two ways OSRS pays items on top, and they are
     * genuinely different mechanics rather than one modelled twice — see each.
     */
    data class Pouch(
        val pouch: ObjType,
        val coins: IntRange,
        /**
         * Rows that **replace** the purse when they win the roll. Rogues, gnomes and heroes work
         * this way: one roll per success, and the purse is simply the rows' remainder.
         */
        val table: LootTable? = null,
        /**
         * Stacks paid **alongside** the purse on every single success. Watchmen (a loaf of bread)
         * and paladins (two chaos runes) work this way; the wiki prints both as `Always`.
         */
        val extras: List<LootStack> = emptyList(),
    ) : PickpocketLoot {
        /** True when a success can pay something other than the purse, which needs its own slot. */
        val paysItems: Boolean = table != null || extras.isNotEmpty()
    }

    /** Master farmers. See [MasterFarmerSeeds]. */
    data object Seeds : PickpocketLoot
}

/** A guaranteed stack, paid on every success. [count] is pre-[ThievingRates.LOOT_RATE]. */
data class LootStack(val obj: ObjType, val count: IntRange)

/** One weighted row: [weight] of its table's [LootTable.denominator] outcomes. */
data class LootRow(val obj: ObjType, val count: IntRange, val weight: Int)

/**
 * The item half of a rung's table, weighted out of [denominator].
 *
 * The purse is not a row. It takes every outcome the rows do not claim, which is exactly how the
 * wiki prints these tables — a rogue's `Coins 123/144` is the leftover of the four item rows, not a
 * number anyone has to keep in step by hand. [purseWeight] is therefore derived, and
 * `ThievingConfigTest` asserts it never goes negative.
 */
data class LootTable(val denominator: Int, val rows: List<LootRow>) {
    val purseWeight: Int = denominator - rows.sumOf(LootRow::weight)
}

/**
 * One rung of the pickpocket ladder.
 *
 * [baseXp] and the values inside [loot] are the real OSRS numbers; [ThievingRates.XP_RATE] and
 * [ThievingRates.LOOT_RATE] are applied when they are paid out, so this table stays directly
 * comparable to the wiki.
 */
data class PickpocketTarget(
    val level: Int,
    val baseXp: Double,
    val loot: PickpocketLoot,
    val stunDamage: IntRange,
    val stunTicks: Int,
) {
    val rateLow: Int = ThievingRates.rateLow(level)
    val rateHigh: Int = ThievingRates.rateHigh()
}

object ThievingTargets {
    val all: Map<NpcType, PickpocketTarget> = buildMap {
        tier(
            npcs.citizens,
            level = 1,
            xp = 8.0,
            loot = purse(pouches.pouch_citizen, coins = 1..3),
            damage = 1..1,
        )
        tier(
            npcs.farmers,
            level = 10,
            xp = 14.5,
            loot = purse(pouches.pouch_farmer, coins = 5..9),
            damage = 1..1,
        )
        tier(
            npcs.warriors,
            level = 25,
            xp = 26.0,
            loot = purse(pouches.pouch_warrior, coins = 12..18),
            damage = 1..2,
        )
        tier(
            npcs.rogues,
            level = 32,
            xp = 36.5,
            loot =
                purse(
                    pouches.pouch_rogue,
                    coins = 25..40,
                    table =
                        LootTable(
                            denominator = 144,
                            rows =
                                listOf(
                                    LootRow(pouches.airrune, 8..8, weight = 9),
                                    LootRow(pouches.jug_wine, 1..1, weight = 6),
                                    LootRow(pouches.lockpick, 1..1, weight = 5),
                                    LootRow(pouches.iron_dagger_p, 1..1, weight = 1),
                                ),
                        ),
                ),
            damage = 1..2,
        )
        // The one rung that pays loot rather than a purse; see `MasterFarmerSeeds`.
        tier(npcs.masterFarmers, level = 38, xp = 43.0, loot = PickpocketLoot.Seeds, damage = 2..3)
        tier(
            npcs.guards,
            level = 40,
            xp = 46.8,
            loot = purse(pouches.pouch_guard, coins = 20..30),
            damage = 2..2,
        )
        tier(
            npcs.knights,
            level = 55,
            xp = 84.3,
            loot = purse(pouches.pouch_knight, coins = 35..50),
            damage = 2..3,
        )
        tier(
            npcs.watchmen,
            level = 65,
            xp = 137.5,
            loot =
                purse(
                    pouches.pouch_watchman,
                    coins = 60..60,
                    extras = listOf(LootStack(pouches.bread, 1..1)),
                ),
            damage = 2..3,
        )
        tier(
            npcs.paladins,
            level = 70,
            xp = 131.8,
            loot =
                purse(
                    pouches.pouch_paladin,
                    coins = 80..80,
                    extras = listOf(LootStack(pouches.chaosrune, 2..2)),
                ),
            damage = 3..3,
        )
        tier(
            npcs.gnomes,
            level = 75,
            xp = 133.3,
            loot =
                purse(
                    pouches.pouch_gnome,
                    coins = 300..300,
                    table =
                        LootTable(
                            denominator = 128,
                            rows =
                                listOf(
                                    LootRow(pouches.arrow_shaft, 2..4, weight = 56),
                                    LootRow(pouches.swamp_toad, 1..1, weight = 24),
                                    LootRow(pouches.gold_ore, 1..1, weight = 8),
                                    LootRow(pouches.earthrune, 1..1, weight = 5),
                                    LootRow(pouches.king_worm, 1..1, weight = 3),
                                    LootRow(pouches.fire_orb, 1..1, weight = 2),
                                ),
                        ),
                ),
            damage = 1..1,
        )
        tier(
            npcs.heroes,
            level = 80,
            xp = 163.3,
            loot =
                purse(
                    pouches.pouch_hero,
                    coins = 200..300,
                    table =
                        LootTable(
                            denominator = 128,
                            rows =
                                listOf(
                                    LootRow(pouches.deathrune, 2..2, weight = 8),
                                    LootRow(pouches.jug_wine, 1..1, weight = 6),
                                    LootRow(pouches.bloodrune, 1..1, weight = 5),
                                    LootRow(pouches.fire_orb, 1..1, weight = 2),
                                    LootRow(pouches.diamond, 1..1, weight = 1),
                                    LootRow(pouches.gold_ore, 1..1, weight = 1),
                                ),
                        ),
                ),
            damage = 3..4,
        )
    }

    /** The lowest requirement on the ladder, for the "you cannot pickpocket anything yet" case. */
    val lowestLevel: Int = all.values.minOf { it.level }

    private fun MutableMap<NpcType, PickpocketTarget>.tier(
        targets: List<NpcType>,
        level: Int,
        xp: Double,
        loot: PickpocketLoot,
        damage: IntRange,
        stunTicks: Int = DEFAULT_STUN_TICKS,
    ) {
        val target = PickpocketTarget(level, xp, loot, damage, stunTicks)
        for (npc in targets) {
            put(npc, target)
        }
    }

    /** Shorthand for the pouch rungs, which are every rung but the master farmer. */
    private fun purse(
        pouch: ObjType,
        coins: IntRange,
        table: LootTable? = null,
        extras: List<LootStack> = emptyList(),
    ): PickpocketLoot = PickpocketLoot.Pouch(pouch, coins, table, extras)

    /**
     * How long a caught thief is frozen for. OSRS stuns for five ticks on most targets; this is
     * deliberately shorter because the loop resumes on its own afterwards and a long stun in an
     * unattended session is just dead time.
     */
    private const val DEFAULT_STUN_TICKS = 3
}
