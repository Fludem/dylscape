package org.rsmod.content.custom.skillingtasks

import org.rsmod.content.custom.skillingtasks.TaskKind.Chop
import org.rsmod.content.custom.skillingtasks.TaskKind.Cook
import org.rsmod.content.custom.skillingtasks.TaskKind.Craft
import org.rsmod.content.custom.skillingtasks.TaskKind.Fish
import org.rsmod.content.custom.skillingtasks.TaskKind.Fletch
import org.rsmod.content.custom.skillingtasks.TaskKind.Mine
import org.rsmod.content.custom.skillingtasks.TaskKind.Potion
import org.rsmod.content.custom.skillingtasks.TaskKind.Smelt
import org.rsmod.content.custom.skillingtasks.TaskKind.Smith
import org.rsmod.content.custom.skillingtasks.configs.skilling_task_objs
import org.rsmod.content.skills.smithing.configs.SmithingProductObjs
import org.rsmod.game.type.obj.ObjType

/**
 * Every job the Taskmaster can hand out.
 *
 * Levels and per-item experience are the live OSRS values. They are not what gates the action -
 * each skill enforces its own requirements when the player actually cuts, cooks or smelts - they
 * only decide who can be *given* the task and how much the bonus is worth.
 *
 * Anvil rows count any item of a bar tier, so "smith 30 steel items" is satisfied by whatever the
 * player finds worth making; arrowheads and dart tips are excluded because they come out fifteen
 * and ten at a time and would trivialise the count.
 */
object SkillingTasks {
    private val objs
        get() = skilling_task_objs

    private val smithing
        get() = SmithingProductObjs

    val all: List<SkillingTask> =
        listOf(
            // Woodcutting.
            task("chop_logs", Chop, "logs", objs.logs, 1, 25.0),
            task("chop_oak_logs", Chop, "oak logs", objs.oak_logs, 15, 37.5),
            task("chop_willow_logs", Chop, "willow logs", objs.willow_logs, 30, 67.5),
            task("chop_maple_logs", Chop, "maple logs", objs.maple_logs, 45, 100.0),
            task("chop_yew_logs", Chop, "yew logs", objs.yew_logs, 60, 175.0),
            task("chop_magic_logs", Chop, "magic logs", objs.magic_logs, 75, 250.0),
            // Mining.
            task("mine_copper_ore", Mine, "copper ore", objs.copper_ore, 1, 17.5),
            task("mine_tin_ore", Mine, "tin ore", objs.tin_ore, 1, 17.5),
            task("mine_iron_ore", Mine, "iron ore", objs.iron_ore, 15, 35.0),
            task("mine_silver_ore", Mine, "silver ore", objs.silver_ore, 20, 40.0),
            task("mine_coal", Mine, "coal", objs.coal, 30, 50.0),
            task("mine_gold_ore", Mine, "gold ore", objs.gold_ore, 40, 65.0),
            task("mine_mithril_ore", Mine, "mithril ore", objs.mithril_ore, 55, 80.0),
            task("mine_adamantite_ore", Mine, "adamantite ore", objs.adamantite_ore, 70, 95.0),
            task("mine_runite_ore", Mine, "runite ore", objs.runite_ore, 85, 125.0),
            // Fishing.
            task("fish_shrimp", Fish, "shrimps", objs.raw_shrimp, 1, 10.0),
            task("fish_sardine", Fish, "sardines", objs.raw_sardine, 5, 20.0),
            task("fish_herring", Fish, "herring", objs.raw_herring, 10, 30.0),
            task("fish_trout", Fish, "trout", objs.raw_trout, 20, 50.0),
            task("fish_salmon", Fish, "salmon", objs.raw_salmon, 30, 70.0),
            task("fish_tuna", Fish, "tuna", objs.raw_tuna, 35, 80.0),
            task("fish_lobster", Fish, "lobsters", objs.raw_lobster, 40, 90.0),
            task("fish_swordfish", Fish, "swordfish", objs.raw_swordfish, 50, 100.0),
            task("fish_monkfish", Fish, "monkfish", objs.raw_monkfish, 62, 120.0),
            task("fish_shark", Fish, "sharks", objs.raw_shark, 76, 110.0),
            // Cooking.
            task("cook_shrimp", Cook, "shrimps", objs.shrimp, 1, 30.0),
            task("cook_trout", Cook, "trout", objs.trout, 15, 70.0),
            task("cook_salmon", Cook, "salmon", objs.salmon, 25, 90.0),
            task("cook_tuna", Cook, "tuna", objs.tuna, 30, 100.0),
            task("cook_lobster", Cook, "lobsters", objs.lobster, 40, 120.0),
            task("cook_swordfish", Cook, "swordfish", objs.swordfish, 45, 140.0),
            task("cook_monkfish", Cook, "monkfish", objs.monkfish, 62, 150.0),
            task("cook_shark", Cook, "sharks", objs.shark, 80, 210.0),
            // Smelting.
            task("smelt_bronze_bars", Smelt, "bronze bars", objs.bronze_bar, 1, 6.2),
            task("smelt_iron_bars", Smelt, "iron bars", objs.iron_bar, 15, 12.5),
            task("smelt_steel_bars", Smelt, "steel bars", objs.steel_bar, 30, 17.5),
            task("smelt_mithril_bars", Smelt, "mithril bars", objs.mithril_bar, 50, 30.0),
            task("smelt_adamantite_bars", Smelt, "adamantite bars", objs.adamantite_bar, 70, 37.5),
            task("smelt_runite_bars", Smelt, "runite bars", objs.runite_bar, 85, 50.0),
            // Anvil work, any item of the tier.
            SkillingTask("smith_bronze_items", Smith, "bronze items", BRONZE_ITEMS, 1, 12.5),
            SkillingTask("smith_iron_items", Smith, "iron items", IRON_ITEMS, 15, 25.0),
            SkillingTask("smith_steel_items", Smith, "steel items", STEEL_ITEMS, 30, 37.5),
            SkillingTask("smith_mithril_items", Smith, "mithril items", MITHRIL_ITEMS, 50, 50.0),
            SkillingTask("smith_adamant_items", Smith, "adamant items", ADAMANT_ITEMS, 70, 62.5),
            // Fletching. Arrow shafts come fifteen to a cut, so the level-1 row is over quickly.
            task("fletch_arrow_shafts", Fletch, "arrow shafts", objs.arrow_shaft, 1, 0.33),
            task("fletch_shortbows", Fletch, "shortbows", objs.unstrung_shortbow, 5, 5.0),
            task("fletch_longbows", Fletch, "longbows", objs.unstrung_longbow, 10, 10.0),
            task(
                "fletch_oak_shortbows",
                Fletch,
                "oak shortbows",
                objs.unstrung_oak_shortbow,
                20,
                16.5,
            ),
            task(
                "fletch_oak_longbows",
                Fletch,
                "oak longbows",
                objs.unstrung_oak_longbow,
                25,
                25.0,
            ),
            task(
                "fletch_willow_shortbows",
                Fletch,
                "willow shortbows",
                objs.unstrung_willow_shortbow,
                35,
                33.3,
            ),
            task(
                "fletch_willow_longbows",
                Fletch,
                "willow longbows",
                objs.unstrung_willow_longbow,
                40,
                41.5,
            ),
            task(
                "fletch_maple_shortbows",
                Fletch,
                "maple shortbows",
                objs.unstrung_maple_shortbow,
                50,
                50.0,
            ),
            task(
                "fletch_maple_longbows",
                Fletch,
                "maple longbows",
                objs.unstrung_maple_longbow,
                55,
                58.3,
            ),
            task(
                "fletch_yew_shortbows",
                Fletch,
                "yew shortbows",
                objs.unstrung_yew_shortbow,
                65,
                67.5,
            ),
            task(
                "fletch_yew_longbows",
                Fletch,
                "yew longbows",
                objs.unstrung_yew_longbow,
                70,
                75.0,
            ),
            task(
                "fletch_magic_shortbows",
                Fletch,
                "magic shortbows",
                objs.unstrung_magic_shortbow,
                80,
                83.3,
            ),
            task(
                "fletch_magic_longbows",
                Fletch,
                "magic longbows",
                objs.unstrung_magic_longbow,
                85,
                91.5,
            ),
            // Crafting.
            task("craft_leather_gloves", Craft, "leather gloves", objs.leather_gloves, 1, 13.8),
            task("craft_leather_boots", Craft, "leather boots", objs.leather_boots, 7, 16.25),
            task("craft_leather_cowls", Craft, "leather cowls", objs.leather_cowl, 9, 18.5),
            task(
                "craft_leather_vambraces",
                Craft,
                "leather vambraces",
                objs.leather_vambraces,
                11,
                22.0,
            ),
            task("craft_leather_bodies", Craft, "leather bodies", objs.leather_armour, 14, 25.0),
            task("craft_leather_chaps", Craft, "leather chaps", objs.leather_chaps, 18, 27.0),
            task("craft_bow_strings", Craft, "bow strings", objs.bow_string, 10, 15.0),
            task("cut_sapphires", Craft, "sapphires", objs.sapphire, 20, 50.0),
            task("cut_emeralds", Craft, "emeralds", objs.emerald, 27, 67.5),
            task("cut_rubies", Craft, "rubies", objs.ruby, 34, 85.0),
            task("cut_diamonds", Craft, "diamonds", objs.diamond, 43, 107.5),
            // Herblore. Products are the four-dose heads `PotionMixing` hands out.
            task("mix_attack_potions", Potion, "attack potions", objs.attack_potion, 3, 25.0),
            task("mix_antipoisons", Potion, "antipoisons", objs.antipoison, 5, 37.5),
            task(
                "mix_strength_potions",
                Potion,
                "strength potions",
                objs.strength_potion,
                12,
                50.0,
            ),
            task("mix_restore_potions", Potion, "restore potions", objs.restore_potion, 22, 62.5),
            task("mix_energy_potions", Potion, "energy potions", objs.energy_potion, 26, 67.5),
            task("mix_defence_potions", Potion, "defence potions", objs.defence_potion, 30, 75.0),
            task("mix_prayer_potions", Potion, "prayer potions", objs.prayer_potion, 38, 87.5),
            task(
                "mix_super_attack_potions",
                Potion,
                "super attack potions",
                objs.super_attack_potion,
                45,
                100.0,
            ),
            task(
                "mix_super_energy_potions",
                Potion,
                "super energy potions",
                objs.super_energy_potion,
                52,
                117.5,
            ),
            task(
                "mix_super_strength_potions",
                Potion,
                "super strength potions",
                objs.super_strength_potion,
                55,
                125.0,
            ),
            task(
                "mix_super_restore_potions",
                Potion,
                "super restore potions",
                objs.super_restore_potion,
                63,
                142.5,
            ),
            task(
                "mix_super_defence_potions",
                Potion,
                "super defence potions",
                objs.super_defence_potion,
                66,
                150.0,
            ),
            task("mix_ranging_potions", Potion, "ranging potions", objs.ranging_potion, 72, 162.5),
            task("mix_magic_potions", Potion, "magic potions", objs.magic_potion, 76, 172.5),
        )

    /** Keyed lookups touch `ObjType.id` nowhere, but the map is still built after the loader. */
    val byKey: Map<String, SkillingTask> by lazy { all.associateBy { it.key } }

    fun find(key: String): SkillingTask? = byKey[key]

    init {
        val keys = all.map { it.key }
        require(keys.size == keys.toSet().size) {
            "Duplicate skilling task keys: ${keys.groupBy { it }.filter { it.value.size > 1 }.keys}"
        }
    }

    private fun task(
        key: String,
        kind: TaskKind,
        name: String,
        product: ObjType,
        level: Int,
        xpEach: Double,
    ): SkillingTask = SkillingTask(key, kind, name, setOf(product), level, xpEach)

    private val BRONZE_ITEMS: Set<ObjType>
        get() =
            setOf(
                smithing.bronze_dagger,
                smithing.bronze_sword,
                smithing.bronze_scimitar,
                smithing.bronze_longsword,
                smithing.bronze_2h_sword,
                smithing.bronze_axe,
                smithing.bronze_mace,
                smithing.bronze_warhammer,
                smithing.bronze_battleaxe,
                smithing.bronze_claws,
                smithing.bronze_chainbody,
                smithing.bronze_platelegs,
                smithing.bronze_plateskirt,
                smithing.bronze_platebody,
                smithing.bronze_med_helm,
                smithing.bronze_full_helm,
                smithing.bronze_sq_shield,
                smithing.bronze_kiteshield,
            )

    private val IRON_ITEMS: Set<ObjType>
        get() =
            setOf(
                smithing.iron_dagger,
                smithing.iron_sword,
                smithing.iron_scimitar,
                smithing.iron_longsword,
                smithing.iron_2h_sword,
                smithing.iron_axe,
                smithing.iron_mace,
                smithing.iron_warhammer,
                smithing.iron_battleaxe,
                smithing.iron_claws,
                smithing.iron_chainbody,
                smithing.iron_platelegs,
                smithing.iron_plateskirt,
                smithing.iron_platebody,
                smithing.iron_med_helm,
                smithing.iron_full_helm,
                smithing.iron_sq_shield,
                smithing.iron_kiteshield,
            )

    private val STEEL_ITEMS: Set<ObjType>
        get() =
            setOf(
                smithing.steel_dagger,
                smithing.steel_sword,
                smithing.steel_scimitar,
                smithing.steel_longsword,
                smithing.steel_2h_sword,
                smithing.steel_axe,
                smithing.steel_mace,
                smithing.steel_warhammer,
                smithing.steel_battleaxe,
                smithing.steel_claws,
                smithing.steel_chainbody,
                smithing.steel_platelegs,
                smithing.steel_plateskirt,
                smithing.steel_platebody,
                smithing.steel_med_helm,
                smithing.steel_full_helm,
                smithing.steel_sq_shield,
                smithing.steel_kiteshield,
            )

    private val MITHRIL_ITEMS: Set<ObjType>
        get() =
            setOf(
                smithing.mithril_dagger,
                smithing.mithril_sword,
                smithing.mithril_scimitar,
                smithing.mithril_longsword,
                smithing.mithril_2h_sword,
                smithing.mithril_axe,
                smithing.mithril_mace,
                smithing.mithril_warhammer,
                smithing.mithril_battleaxe,
                smithing.mithril_claws,
                smithing.mithril_chainbody,
                smithing.mithril_platelegs,
                smithing.mithril_plateskirt,
                smithing.mithril_platebody,
                smithing.mithril_med_helm,
                smithing.mithril_full_helm,
                smithing.mithril_sq_shield,
                smithing.mithril_kiteshield,
            )

    private val ADAMANT_ITEMS: Set<ObjType>
        get() =
            setOf(
                smithing.adamant_dagger,
                smithing.adamant_sword,
                smithing.adamant_scimitar,
                smithing.adamant_longsword,
                smithing.adamant_2h_sword,
                smithing.adamant_axe,
                smithing.adamant_mace,
                smithing.adamnt_warhammer,
                smithing.adamant_battleaxe,
                smithing.adamant_claws,
                smithing.adamant_chainbody,
                smithing.adamant_platelegs,
                smithing.adamant_plateskirt,
                smithing.adamant_platebody,
                smithing.adamant_med_helm,
                smithing.adamant_full_helm,
                smithing.adamant_sq_shield,
                smithing.adamant_kiteshield,
            )
}
