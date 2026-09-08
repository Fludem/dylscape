package org.rsmod.content.skills.thieving.configs

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

private typealias pouches = ThievingObjs

private typealias npcs = ThievingTargetNpcs

/**
 * Every npc that can be pickpocketed, and what it is worth.
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
 * while `farmer1` reads `[null, Attack, Pickpocket]`, so the slot is stable even where the earlier
 * options are not.
 *
 * Deliberately excluded after the dump: `thug` (525) carries no `Pickpocket` op at all, and
 * `knight_of_ardougne_west` (4267) and `paladin_west` (4262) are nameless placeholders with an
 * entirely null op array. Adding them would mean editing ops onto npcs the cache never intended to
 * be robbed.
 */
object ThievingTargetNpcs : NpcReferences() {
    val man = find("man")
    val man2 = find("man2")
    val man3 = find("man3")
    val man_indoor = find("man_indoor")
    val woman = find("woman")
    val woman2 = find("woman2")
    val woman3 = find("woman3")
    val farmer1 = find("farmer1")
    val farmer2 = find("farmer2")
    val farmer3 = find("farmer3")
    val farmer4 = find("farmer4")
    val warrior_woman = find("warrior_woman")
    val rogue = find("rogue")
    val master_farmer_1 = find("master_farmer_1")
    val master_farmer_2 = find("master_farmer_2")
    val master_farmer_1_f = find("master_farmer_1_f")
    val master_farmer_2_f = find("master_farmer_2_f")
    val guard1 = find("guard1")
    val guard1_f = find("guard1_f")
    val knight_of_ardougne = find("knight_of_ardougne")
    val knight_of_ardougne2 = find("knight_of_ardougne2")
    val yanille_watchman = find("yanille_watchman")
    val paladin = find("paladin")
    val paladin2 = find("paladin2")
    val hero = find("hero")
}

/**
 * One rung of the pickpocket ladder.
 *
 * [baseXp] and [coins] are the real OSRS values; [ThievingRates.XP_RATE] and
 * [ThievingRates.LOOT_RATE] are applied when they are paid out, so this table stays directly
 * comparable to the wiki.
 */
data class PickpocketTarget(
    val level: Int,
    val baseXp: Double,
    val pouch: ObjType,
    val coins: IntRange,
    val stunDamage: IntRange,
    val stunTicks: Int,
) {
    val rateLow: Int = ThievingRates.rateLow(level)
    val rateHigh: Int = ThievingRates.rateHigh()
}

object ThievingTargets {
    val all: Map<NpcType, PickpocketTarget> = buildMap {
        tier(
            listOf(
                npcs.man,
                npcs.man2,
                npcs.man3,
                npcs.man_indoor,
                npcs.woman,
                npcs.woman2,
                npcs.woman3,
            ),
            level = 1,
            xp = 8.0,
            pouch = pouches.pouch_citizen,
            coins = 1..3,
            damage = 1..1,
        )
        tier(
            listOf(npcs.farmer1, npcs.farmer2, npcs.farmer3, npcs.farmer4),
            level = 10,
            xp = 14.5,
            pouch = pouches.pouch_farmer,
            coins = 5..9,
            damage = 1..1,
        )
        tier(
            listOf(npcs.warrior_woman),
            level = 25,
            xp = 26.0,
            pouch = pouches.pouch_warrior,
            coins = 12..18,
            damage = 1..2,
        )
        tier(
            listOf(npcs.rogue),
            level = 32,
            xp = 35.5,
            pouch = pouches.pouch_rogue,
            coins = 18..25,
            damage = 1..2,
        )
        // Master farmers pay seeds in OSRS. Seeds are a table this module has no business owning,
        // so they pay a fat purse instead and stay on the ladder for the level range they cover.
        tier(
            listOf(
                npcs.master_farmer_1,
                npcs.master_farmer_2,
                npcs.master_farmer_1_f,
                npcs.master_farmer_2_f,
            ),
            level = 38,
            xp = 43.0,
            pouch = pouches.pouch_farmer,
            coins = 20..30,
            damage = 2..3,
        )
        tier(
            listOf(npcs.guard1, npcs.guard1_f),
            level = 40,
            xp = 46.8,
            pouch = pouches.pouch_guard,
            coins = 20..30,
            damage = 2..2,
        )
        tier(
            listOf(npcs.knight_of_ardougne, npcs.knight_of_ardougne2),
            level = 55,
            xp = 84.3,
            pouch = pouches.pouch_knight,
            coins = 35..50,
            damage = 2..3,
        )
        tier(
            listOf(npcs.yanille_watchman),
            level = 65,
            xp = 137.5,
            pouch = pouches.pouch_watchman,
            coins = 45..60,
            damage = 2..3,
        )
        tier(
            listOf(npcs.paladin, npcs.paladin2),
            level = 70,
            xp = 151.75,
            pouch = pouches.pouch_paladin,
            coins = 60..80,
            damage = 3..3,
        )
        tier(
            listOf(npcs.hero),
            level = 80,
            xp = 273.3,
            pouch = pouches.pouch_hero,
            coins = 150..200,
            damage = 3..4,
        )
    }

    /** The lowest requirement on the ladder, for the "you cannot pickpocket anything yet" case. */
    val lowestLevel: Int = all.values.minOf { it.level }

    private fun MutableMap<NpcType, PickpocketTarget>.tier(
        targets: List<NpcType>,
        level: Int,
        xp: Double,
        pouch: ObjType,
        coins: IntRange,
        damage: IntRange,
        stunTicks: Int = DEFAULT_STUN_TICKS,
    ) {
        val target = PickpocketTarget(level, xp, pouch, coins, damage, stunTicks)
        for (npc in targets) {
            put(npc, target)
        }
    }

    /**
     * How long a caught thief is frozen for. OSRS stuns for five ticks on most targets; this is
     * deliberately shorter because the loop resumes on its own afterwards and a long stun in an
     * unattended session is just dead time.
     */
    private const val DEFAULT_STUN_TICKS = 3
}
