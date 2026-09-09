package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * Prints everything the Herblore module needs to decode from the cache rather than assume.
 *
 * Herblore has more of itself already in the cache than most skills, and the naming is inconsistent
 * enough that guessing is not an option: grimy herbs are `unidentified_*` rather than `grimy_*`,
 * the unfinished marrentill potion has two r's where the herb has one, and the four-dose strength
 * potion is `strength4` while every one of its siblings is `4dose<family>`. Six questions are
 * answered here, in this order:
 * 1. The ops, params and content group on every herb, vial and unfinished potion. `Clean` has to be
 *    real op text or the cleaning binding is dead - see the op-name rule in `brief-guide.md`.
 * 2. The full universe of dosed potions, found the same way `ConsumableCoverageTest` *excludes*
 *    them: a `Drink` op and a `(1)`-`(4)` name suffix. That set is what the potion table has to
 *    cover, and what its coverage test asserts against.
 * 3. Whether anything already wears the `potion` content group, which decides whether the module's
 *    obj editor is a tag or a no-op.
 * 4. Whether potions carry `levelrequire` / `skill_xp` params, which would make the level and
 *    experience tables cache-derived instead of hand-authored.
 * 5. The skill guide's own herb and potion inventories, which are ordered by level.
 * 6. The make-menu verb table, for the verb potion mixing should use.
 */
@Execution(ExecutionMode.SAME_THREAD)
class HerbloreDump {
    @Test
    fun GameTestState.`dump herbs, vials and secondaries`() = runBasicGameTest {
        val herbs =
            cacheTypes.objs.values
                .filter { it.internalName.orEmpty().startsWith("unidentified_") }
                .sortedBy { it.id }
        for (obj in herbs) {
            println(
                "GRIMY ${obj.id} sym=${obj.internalName} name='${obj.name}' " +
                    "ops=${obj.iop.toList()} group=${obj.contentGroup} " +
                    "params=${obj.paramMap?.primitiveMap}"
            )
        }
        println("GRIMY_TOTAL=${herbs.size}")

        val named =
            listOf(
                "guam_leaf",
                "marentill",
                "tarromin",
                "harralander",
                "ranarr_weed",
                "toadflax",
                "irit_leaf",
                "avantoe",
                "kwuarm",
                "snapdragon",
                "huasca",
                "cadantine",
                "lantadyme",
                "dwarf_weed",
                "torstol",
                "guamvial",
                "marrentillvial",
                "tarrominvial",
                "harralandervial",
                "ranarrvial",
                "toadflaxvial",
                "iritvial",
                "avantoevial",
                "kwuarmvial",
                "snapdragonvial",
                "huascavial",
                "cadantinevial",
                "lantadymevial",
                "dwarfweedvial",
                "torstolvial",
                "vial_water",
                "vial_empty",
                "pestle_and_mortar",
                "eye_of_newt",
                "red_spiders_eggs",
                "limpwurt_root",
                "snape_grass",
                "white_berries",
                "wine_of_zamorak",
                "jangerberries",
                "toads_legs",
                "cactus_potato",
                "mortmyremushroom",
                "poisonivy_berries",
                "unicorn_horn",
                "unicorn_horn_dust",
                "blue_dragon_scale",
                "dragon_scale_dust",
                "chocolate_bar",
                "chocolate_dust",
                "desert_goat_horn",
                "ground_desert_goat_horn",
                "huntingbeast_sabreteeth",
                "huntingbeast_sabreteeth_dust",
                "crushed_bird_nest",
                "yew_roots",
                "amylase",
                "sote_crystal_dust",
                "ashes",
                "garlic",
                "ground_guam",
                "strength4",
            )
        for (name in named) {
            val obj = cacheTypes.objs.values.firstOrNull { it.internalName == name }
            println(
                "OBJ $name -> id=${obj?.id} name='${obj?.name}' ops=${obj?.iop?.toList()} " +
                    "group=${obj?.contentGroup} params=${obj?.paramMap?.primitiveMap}"
            )
        }
    }

    @Test
    fun GameTestState.`dump every dosed potion`() = runBasicGameTest {
        // The same test `ConsumableCoverageTest` uses to decide an obj is Herblore's rather than
        // the food table's, run the other way round. The suffix alone is not enough: the Chambers
        // of Xeric fish are named "Pysk fish (0)" through "Kyren fish (6)" and are eaten.
        val doseSuffix = Regex("""\([1-4]\)$""")
        val dosed =
            cacheTypes.objs.values
                .filter { it.iop.any { op -> op == "Drink" } }
                .filter { doseSuffix.containsMatchIn(it.name) }
                .sortedBy { it.id }
        for (obj in dosed) {
            println(
                "DOSED ${obj.id} sym=${obj.internalName} name='${obj.name}' " +
                    "ops=${obj.iop.toList()} group=${obj.contentGroup} " +
                    "params=${obj.paramMap?.primitiveMap}"
            )
        }
        println("DOSED_TOTAL=${dosed.size}")

        // Anything named like a dose but *not* carrying `Drink`. If this is non-empty the naming
        // scheme alone cannot be trusted to enumerate the ladder.
        val suffixOnly =
            cacheTypes.objs.values
                .filter { doseSuffix.containsMatchIn(it.name) }
                .filter { it.iop.none { op -> op == "Drink" } }
                .sortedBy { it.id }
        for (obj in suffixOnly) {
            println(
                "DOSELIKE ${obj.id} sym=${obj.internalName} name='${obj.name}' " +
                    "ops=${obj.iop.toList()}"
            )
        }
        println("DOSELIKE_TOTAL=${suffixOnly.size}")
    }

    @Test
    fun GameTestState.`dump the potion content group and skill params`() = runBasicGameTest {
        // Content group 39 is `potion` in `.data/symbols/content.sym`. The consumables module
        // deliberately left it unclaimed for Herblore; this says whether the cache agrees.
        val tagged = cacheTypes.objs.values.filter { it.contentGroup == POTION_GROUP }
        for (obj in tagged.sortedBy { it.id }) {
            println("POTIONGROUP ${obj.id} sym=${obj.internalName} name='${obj.name}'")
        }
        println("POTIONGROUP_TOTAL=${tagged.size}")

        // If upstream already populates these on potions, the level and xp tables are free. Hunter
        // reads `skill_xp` straight off the cache this way; cooking and prayer have to write it.
        for (param in SKILL_PARAMS) {
            val carriers =
                cacheTypes.objs.values.filter {
                    it.paramMap?.primitiveMap?.containsKey(param) == true
                }
            val potions = carriers.filter { Regex("""\([1-4]\)$""").containsMatchIn(it.name) }
            println("PARAM $param carriers=${carriers.size} onPotions=${potions.size}")
            for (obj in potions.sortedBy { it.id }.take(20)) {
                println(
                    "    PARAMROW $param ${obj.id} sym=${obj.internalName} " +
                        "value=${obj.paramMap?.primitiveMap?.get(param)}"
                )
            }
        }
    }

    @Test
    fun GameTestState.`dump the skill guide inventories and script`() = runBasicGameTest {
        for (name in listOf("skill_guide_herblore_herbs", "skill_guide_herblore_potions")) {
            val inv = cacheTypes.invs.values.firstOrNull { it.internalName == name }
            if (inv == null) {
                println("GUIDEINV $name -> MISSING")
                continue
            }
            println("GUIDEINV $name id=${inv.id} size=${inv.size} stock=${inv.stock?.size}")
            inv.stock?.forEachIndexed { index, stock ->
                if (stock != null) {
                    val obj = cacheTypes.objs[stock.obj]
                    println(
                        "    GUIDEROW $name $index obj=${stock.obj} sym=${obj?.internalName} " +
                            "name='${obj?.name}' count=${stock.count}"
                    )
                }
            }
        }

        // `[proc,skill_guide_data_herblore]`. The guide pairs each obj above with a level; the
        // levels are int constants in this script's operand stream.
        for (id in listOf(7885, 7886)) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== SCRIPT $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount} ops=${script.commands.size}"
            )
            for (i in script.commands.indices) {
                val str = script.stringOperands[i]
                if (str != null) {
                    println("  $i: cmd=${script.commands[i]} str=\"$str\"")
                } else {
                    println("  $i: cmd=${script.commands[i]} op=${script.intOperands[i]}")
                }
            }
        }
    }

    @Test
    fun GameTestState.`dump the make-menu verbs`() = runBasicGameTest {
        for ((id, purpose) in
            listOf(
                1809 to "skillmulti verb",
                5178 to "no quantity buttons",
                1810 to "no X button",
            )) {
            val enum = cacheTypes.enums[id] ?: continue
            println("ENUM $id ($purpose)")
            enum.primitiveMap.toSortedMap(compareBy { it as Int }).forEach { (k, v) ->
                println("    ENUMROW $id $k -> $v")
            }
        }
    }

    private companion object {
        /** `content.sym` 39 `potion`, reserved by the consumables module for this one. */
        private const val POTION_GROUP: Int = 39

        /**
         * `levelrequire`, `skill_xp`, `skill_anim`, `skill_productitem`, `statreq1_skill/level`.
         */
        private val SKILL_PARAMS = listOf(23, 65493, 65494, 65492, 434, 436)
    }
}
