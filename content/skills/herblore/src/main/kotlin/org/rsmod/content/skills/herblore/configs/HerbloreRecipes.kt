package org.rsmod.content.skills.herblore.configs

import org.rsmod.content.skills.herblore.configs.HerbloreObjs as obj
import org.rsmod.game.type.obj.ObjType

/** Rubbing the dirt off a grimy herb. One click, one herb, instant. */
data class CleanRecipe(val grimy: ObjType, val clean: ObjType, val levelReq: Int, val xp: Double)

/**
 * A clean herb into a base liquid.
 *
 * [base] is a vial of water for thirteen of the fifteen herbs, a vial of blood for the cadantine
 * that starts bastion and battlemage, and coconut milk for the two antidotes. Carrying it per row
 * rather than assuming water is what lets those three share this table instead of needing their
 * own.
 *
 * There is no `xp` field and that is not an omission: the live game pays nothing for an unfinished
 * potion, and the whole reward lands on the mixing step. The "every recipe pays experience" rule in
 * [HerbloreRecipes]'s `init` therefore skips this table by construction.
 */
data class UnfRecipe(val base: ObjType, val herb: ObjType, val unf: ObjType, val levelReq: Int)

/**
 * Something plus a secondary makes a potion.
 *
 * [primary] is usually an unfinished potion, but the top of the skill is built out of finished
 * potions -- crystal dust on a super attack, lava shards on an antifire -- and those are the same
 * shape, so they share this table rather than getting a parallel one.
 *
 * [product] is looked up rather than stored so that the recipe table and the dose ladder cannot
 * name different objs. [family] is the ladder's stem, and [Potions] owns the mapping from a stem to
 * its four-dose head; if a stem here has no ladder, `getValue` throws at startup naming it.
 */
data class PotionRecipe(
    val primary: ObjType,
    val secondary: ObjType,
    val family: String,
    val levelReq: Int,
    val xp: Double,
) {
    val product: ObjType
        get() = Potions.heads.getValue(family).obj
}

/** Pestle and mortar. Pays no experience and needs no level, in live and here. */
data class GrindRecipe(val input: ObjType, val output: ObjType)

/**
 * Every Herblore recipe in the game, in four tables.
 *
 * **Where the numbers come from.** The level requirements are the cache's own: clientscript 7886
 * `[proc,skill_guide_data_herblore]` carries a level for every potion the vanilla skill guide
 * lists, and `HerbloreDump` prints them. The experience values are not in the cache -- no potion
 * obj carries `skill_xp`, which the same dump proves -- so they come from the OSRS Wiki via
 * `tools/herblore/generate.py`, which cross-checks its own level column against the cache's and
 * fails on a disagreement. The two sources currently agree on all 38 potions they share.
 *
 * This is a plain `object`, not an `ObjReferences` subclass, and it must stay that way: it reads
 * `ObjType.id` to build its lookup maps, and `id` throws on a reference the loader has not resolved
 * yet. A plain object is first touched from a `PluginScript.startup()`, long after the loader runs.
 */
object HerbloreRecipes {
    val cleaning: List<CleanRecipe> =
        listOf(
            CleanRecipe(obj.unidentified_guam, obj.guam_leaf, 3, 2.5),
            CleanRecipe(obj.unidentified_marentill, obj.marentill, 5, 3.8),
            CleanRecipe(obj.unidentified_tarromin, obj.tarromin, 11, 5.0),
            CleanRecipe(obj.unidentified_harralander, obj.harralander, 20, 6.3),
            CleanRecipe(obj.unidentified_ranarr, obj.ranarr_weed, 25, 7.5),
            CleanRecipe(obj.unidentified_toadflax, obj.toadflax, 30, 8.0),
            CleanRecipe(obj.unidentified_irit, obj.irit_leaf, 40, 8.8),
            CleanRecipe(obj.unidentified_avantoe, obj.avantoe, 48, 10.0),
            CleanRecipe(obj.unidentified_kwuarm, obj.kwuarm, 54, 11.3),
            CleanRecipe(obj.unidentified_huasca, obj.huasca, 58, 11.8),
            CleanRecipe(obj.unidentified_snapdragon, obj.snapdragon, 59, 11.8),
            CleanRecipe(obj.unidentified_cadantine, obj.cadantine, 65, 12.5),
            CleanRecipe(obj.unidentified_lantadyme, obj.lantadyme, 67, 13.1),
            CleanRecipe(obj.unidentified_dwarf_weed, obj.dwarf_weed, 70, 13.8),
            CleanRecipe(obj.unidentified_torstol, obj.torstol, 75, 15.0),
        )

    val unfinished: List<UnfRecipe> =
        listOf(
            UnfRecipe(obj.vial_water, obj.guam_leaf, obj.guamvial, 3),
            UnfRecipe(obj.vial_water, obj.marentill, obj.marrentillvial, 5),
            UnfRecipe(obj.vial_water, obj.tarromin, obj.tarrominvial, 12),
            UnfRecipe(obj.vial_water, obj.harralander, obj.harralandervial, 22),
            UnfRecipe(obj.vial_water, obj.ranarr_weed, obj.ranarrvial, 30),
            UnfRecipe(obj.vial_water, obj.toadflax, obj.toadflaxvial, 34),
            UnfRecipe(obj.vial_water, obj.irit_leaf, obj.iritvial, 45),
            UnfRecipe(obj.vial_water, obj.avantoe, obj.avantoevial, 50),
            UnfRecipe(obj.vial_water, obj.kwuarm, obj.kwuarmvial, 55),
            UnfRecipe(obj.vial_water, obj.huasca, obj.huascavial, 58),
            UnfRecipe(obj.vial_water, obj.snapdragon, obj.snapdragonvial, 63),
            UnfRecipe(obj.vial_water, obj.cadantine, obj.cadantinevial, 66),
            UnfRecipe(obj.vial_water, obj.lantadyme, obj.lantadymevial, 69),
            UnfRecipe(obj.vial_water, obj.dwarf_weed, obj.dwarfweedvial, 72),
            UnfRecipe(obj.vial_water, obj.torstol, obj.torstolvial, 78),
            // The three that do not start from water.
            UnfRecipe(obj.vial_coconut_milk, obj.toadflax, obj.unfinished_antidote_plus, 68),
            UnfRecipe(obj.vial_coconut_milk, obj.irit_leaf, obj.unfinished_antidote_plusplus, 79),
            UnfRecipe(obj.vial_blood, obj.cadantine, obj.cadantine_bloodvial, 80),
        )

    val mixing: List<PotionRecipe> =
        listOf(
            PotionRecipe(obj.guamvial, obj.eye_of_newt, "1attack", 3, 25.0),
            PotionRecipe(obj.marrentillvial, obj.unicorn_horn_dust, "antipoison", 5, 37.5),
            PotionRecipe(obj.tarrominvial, obj.limpwurt_root, "1strength", 12, 50.0),
            PotionRecipe(obj.harralandervial, obj.red_spiders_eggs, "statrestore", 22, 62.5),
            PotionRecipe(obj.harralandervial, obj.chocolate_dust, "1energy", 26, 67.5),
            PotionRecipe(obj.ranarrvial, obj.white_berries, "1defense", 30, 75.0),
            PotionRecipe(obj.toadflaxvial, obj.toads_legs, "1agility", 34, 80.0),
            PotionRecipe(obj.harralandervial, obj.ground_desert_goat_horn, "combat", 36, 84.0),
            PotionRecipe(obj.ranarrvial, obj.snape_grass, "prayerrestore", 38, 87.5),
            PotionRecipe(obj.iritvial, obj.eye_of_newt, "2attack", 45, 100.0),
            PotionRecipe(obj.iritvial, obj.unicorn_horn_dust, "2antipoison", 48, 106.3),
            PotionRecipe(obj.avantoevial, obj.snape_grass, "fisherspotion", 50, 112.5),
            PotionRecipe(obj.avantoevial, obj.mortmyremushroom, "2energy", 52, 117.5),
            PotionRecipe(obj.avantoevial, obj.huntingbeast_sabreteeth_dust, "hunting", 53, 120.0),
            PotionRecipe(obj.harralandervial, obj.aldarium, "goading", 54, 132.0),
            PotionRecipe(obj.kwuarmvial, obj.limpwurt_root, "2strength", 55, 125.0),
            PotionRecipe(obj.huascavial, obj.aldarium, "1prayer_regeneration", 58, 132.0),
            PotionRecipe(obj.snapdragonvial, obj.red_spiders_eggs, "2restore", 63, 142.5),
            PotionRecipe(obj.cadantinevial, obj.white_berries, "2defense", 66, 150.0),
            PotionRecipe(obj.unfinished_antidote_plus, obj.yew_roots, "antidote+", 68, 155.0),
            PotionRecipe(obj.lantadymevial, obj.dragon_scale_dust, "1antidragon", 69, 157.5),
            PotionRecipe(obj.dwarfweedvial, obj.wine_of_zamorak, "rangerspotion", 72, 162.5),
            PotionRecipe(obj.lantadymevial, obj.cactus_potato, "1magic", 76, 172.5),
            PotionRecipe(obj.torstolvial, obj.jangerberries, "potionofzamorak", 78, 175.0),
            PotionRecipe(
                obj.unfinished_antidote_plusplus,
                obj.magic_roots,
                "antidote++",
                79,
                177.5,
            ),
            PotionRecipe(obj.cadantine_bloodvial, obj.wine_of_zamorak, "bastion", 80, 155.0),
            PotionRecipe(obj.cadantine_bloodvial, obj.cactus_potato, "battlemage", 80, 155.0),
            PotionRecipe(obj.toadflaxvial, obj.crushed_bird_nest, "potionofsaradomin", 81, 180.0),
            PotionRecipe(obj.torstolvial, obj.demonic_tallow, "surge", 81, 185.0),
            PotionRecipe(obj.dwarfweedvial, obj.nihil_dust, "ancientbrew", 85, 190.0),
            // Potions made out of other potions. Same shape, so the same table.
            PotionRecipe(head("2energy"), obj.amylase, "stamina", 77, 102.0),
            PotionRecipe(head("1antidragon"), obj.lava_shard, "2antidragon", 84, 110.0),
            PotionRecipe(head("antidote++"), obj.snakeboss_scale, "antivenom", 87, 120.0),
            PotionRecipe(head("ancientbrew"), obj.ancient_essence, "forgottenbrew", 91, 145.0),
            PotionRecipe(head("1antidragon"), obj.crushed_dragon_bones, "3antidragon", 92, 130.0),
            PotionRecipe(head("antivenom"), obj.torstol, "antivenom+", 94, 125.0),
            PotionRecipe(
                head("antivenom+"),
                obj.araxyte_venom_sack,
                "extended_antivenom+",
                94,
                80.0,
            ),
            PotionRecipe(head("3antidragon"), obj.lava_shard, "4antidragon", 98, 160.0),
            // The divine family. Crystal dust on a finished potion; a flat 2xp each, which is not a
            // transcription slip -- the reward is the potion, not the experience.
            PotionRecipe(head("2attack"), obj.sote_crystal_dust, "divineattack", 70, 2.0),
            PotionRecipe(head("2strength"), obj.sote_crystal_dust, "divinestrength", 70, 2.0),
            PotionRecipe(head("2defense"), obj.sote_crystal_dust, "divinedefence", 70, 2.0),
            PotionRecipe(head("rangerspotion"), obj.sote_crystal_dust, "divinerange", 74, 2.0),
            PotionRecipe(head("1magic"), obj.sote_crystal_dust, "divinemagic", 78, 2.0),
            PotionRecipe(head("bastion"), obj.sote_crystal_dust, "divinebastion", 86, 2.0),
            PotionRecipe(head("battlemage"), obj.sote_crystal_dust, "divinebattlemage", 86, 2.0),
            PotionRecipe(head("2combat"), obj.sote_crystal_dust, "divinecombat", 97, 2.0),
        )

    val grinding: List<GrindRecipe> =
        listOf(
            GrindRecipe(obj.unicorn_horn, obj.unicorn_horn_dust),
            GrindRecipe(obj.blue_dragon_scale, obj.dragon_scale_dust),
            GrindRecipe(obj.desert_goat_horn, obj.ground_desert_goat_horn),
            GrindRecipe(obj.huntingbeast_sabreteeth, obj.huntingbeast_sabreteeth_dust),
            GrindRecipe(obj.chocolate_bar, obj.chocolate_dust),
            GrindRecipe(obj.bird_nest_empty, obj.crushed_bird_nest),
        )

    /**
     * Super combat, which is the one recipe that does not fit `use A on B`.
     *
     * It takes a torstol and all three super potions at once, so the torstol is used on any of the
     * three and the other two are checked in the inventory. Kept out of [mixing] because every row
     * there is exactly two objs, and bending that shape for one recipe would cost every other row a
     * nullable field.
     */
    const val SUPER_COMBAT_LEVEL: Int = 90
    const val SUPER_COMBAT_XP: Double = 150.0

    val superCombatInputs: List<ObjType>
        get() = listOf(head("2attack"), head("2strength"), head("2defense"))

    val superCombatProduct: ObjType
        get() = head("2combat")

    /** Keyed by raw `Int`: `find` yields a `HashedObjType`, the runtime an `UnpackedObjType`. */
    val cleaningByGrimy: Map<Int, CleanRecipe> = cleaning.associateBy { it.grimy.id }

    private fun head(family: String): ObjType = Potions.heads.getValue(family).obj

    init {
        require(cleaning.distinctBy { it.grimy.id }.size == cleaning.size) {
            "Duplicate grimy herb."
        }
        require(cleaning.distinctBy { it.clean.id }.size == cleaning.size) {
            "Two herbs clean alike."
        }
        require(unfinished.distinctBy { it.unf.id }.size == unfinished.size) {
            "Two recipes make the same unfinished potion."
        }

        // Every herb that can be cleaned can be brewed, and nothing can be brewed that cannot be
        // cleaned. The join that keeps the first two tables from drifting apart.
        val cleaned = cleaning.map { it.clean.id }.toSet()
        require(unfinished.all { it.herb.id in cleaned }) {
            "An unfinished potion uses no real herb."
        }
        require(cleaned.all { herb -> unfinished.any { it.herb.id == herb } }) {
            "A clean herb makes no unfinished potion."
        }

        // `onOpHeldU` throws at startup on a repeated ordered pair, and the shared harness runs
        // every script in the repo -- a duplicate here would fail every module's integration run
        // with an error that does not point at Herblore. Failing at class-load names the row.
        val pairs: List<Pair<Int, Int>> =
            unfinished.map { it.base.id to it.herb.id } +
                mixing.map { it.primary.id to it.secondary.id } +
                grinding.map { obj.pestle_and_mortar.id to it.input.id } +
                superCombatInputs.map { obj.torstol.id to it.id }
        require(pairs.distinct().size == pairs.size) {
            "Two recipes claim the same ingredient pair."
        }
        val seen = pairs.toSet()
        require(pairs.none { (first, second) -> (second to first) in seen }) {
            "A pair is registered in both orders; `onOpHeldU` refuses the reverse registration."
        }

        // Every unfinished potion is consumed by something. A dead end would be a potion the skill
        // can make and then do nothing with.
        val consumed = mixing.map { it.primary.id }.toSet()
        require(unfinished.all { it.unf.id in consumed }) { "An unfinished potion makes nothing." }

        require(mixing.distinctBy { it.family }.size == mixing.size) { "Two recipes, one potion." }

        val levels =
            cleaning.map { it.levelReq } +
                unfinished.map { it.levelReq } +
                mixing.map { it.levelReq }
        require(levels.all { it in 1..99 }) { "A recipe has a level outside 1..99." }

        // Grinding and the unfinished step are the deliberate exceptions: neither pays anything in
        // the live game, so neither carries an xp field to check.
        require((cleaning.map { it.xp } + mixing.map { it.xp }).all { it > 0.0 }) {
            "A recipe pays no experience."
        }
    }
}
