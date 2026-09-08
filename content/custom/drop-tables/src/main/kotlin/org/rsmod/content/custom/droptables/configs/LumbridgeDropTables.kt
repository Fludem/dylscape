package org.rsmod.content.custom.droptables.configs

import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.content.custom.droptables.DropTable
import org.rsmod.content.custom.droptables.dropTable
import org.rsmod.game.type.npc.NpcType

/**
 * Drop tables for the npcs a new character meets around Lumbridge.
 *
 * Weights are the official `/128` tables. The builder asserts that every table's slots sum to its
 * denominator, so a mistyped weight fails at start-up rather than skewing drops silently.
 *
 * Known gaps, deliberately left as `nothing` weight so the denominators stay honest:
 * - The shared herb sub-table (man/woman roll it at `23/128`).
 * - Clue scrolls and other tertiary drops.
 * - Goblin book, which has no obj under that name in the revision 233 cache.
 */
object LumbridgeDropTables {
    val chicken: DropTable = dropTable {
        always(objs.bones)
        always(drop_objs.raw_chicken)
        table(outOf = 128) {
            drop(64, drop_objs.feather, 5)
            drop(32, drop_objs.feather, 15)
            nothing(32)
        }
    }

    val cow: DropTable = dropTable {
        always(objs.bones)
        always(drop_objs.cow_hide)
        always(drop_objs.raw_beef)
    }

    val giant_rat: DropTable = dropTable {
        always(objs.bones)
        always(drop_objs.raw_rat_meat)
    }

    val rat: DropTable = dropTable { always(objs.bones) }

    val man: DropTable = dropTable {
        always(objs.bones)
        table(outOf = 128) {
            drop(2, drop_objs.bronze_med_helm)
            drop(1, drop_objs.iron_dagger)
            drop(22, drop_objs.bronze_bolts, 2..12)
            drop(3, objs.bronze_arrow, 7)
            drop(2, objs.earth_rune, 4)
            drop(2, objs.fire_rune, 6)
            drop(2, objs.mind_rune, 9)
            drop(1, objs.chaos_rune, 2)
            drop(38, objs.coins, 3)
            drop(9, objs.coins, 5)
            drop(4, objs.coins, 15)
            drop(1, objs.coins, 25)
            drop(5, drop_objs.fishing_bait)
            drop(2, drop_objs.copper_ore)
            drop(2, drop_objs.earth_talisman)
            drop(1, objs.cabbage)
            nothing(23) // Herb sub-table.
            nothing(8)
        }
    }

    val goblin: DropTable = dropTable {
        always(objs.bones)
        table(outOf = 128) {
            drop(3, drop_objs.bronze_sq_shield)
            drop(4, drop_objs.bronze_spear)
            drop(5, drop_objs.bodyrune, 7)
            drop(6, objs.water_rune, 6)
            drop(3, objs.earth_rune, 4)
            drop(3, drop_objs.bronze_bolts, 8)
            drop(28, objs.coins, 5)
            drop(3, objs.coins, 9)
            drop(3, objs.coins, 15)
            drop(2, objs.coins, 20)
            drop(1, objs.coins, 1)
            drop(15, objs.hammer)
            drop(5, drop_objs.goblin_mail)
            drop(3, drop_objs.chefs_hat)
            drop(2, objs.beer)
            drop(1, drop_objs.brass_necklace)
            drop(1, drop_objs.air_talisman)
            nothing(38)
            nothing(2) // Goblin book.
        }
    }

    /**
     * Every npc that has a table, paired with it. [org.rsmod.content.custom.droptables.scripts]
     * registers one death handler per entry.
     */
    val assignments: List<Pair<NpcType, DropTable>> = buildList {
        assign(chicken, drop_npcs.chicken, drop_npcs.chicken_brown)
        assign(
            cow,
            drop_npcs.cow,
            drop_npcs.cow2,
            drop_npcs.cow3,
            drop_npcs.cow_beef,
            drop_npcs.cow2_calf,
            drop_npcs.cow3_calf,
        )
        assign(
            giant_rat,
            drop_npcs.giantrat,
            drop_npcs.giantrat1,
            drop_npcs.giantrat2,
            drop_npcs.giantrat3,
            drop_npcs.giantrat1_2,
            drop_npcs.giantrat1_3,
        )
        assign(rat, drop_npcs.rat)
        assign(
            man,
            npcs.man,
            npcs.man2,
            npcs.man3,
            drop_npcs.man4,
            npcs.man_indoor,
            npcs.woman,
            npcs.woman2,
            npcs.woman3,
        )
        assign(
            goblin,
            drop_npcs.goblin,
            drop_npcs.goblin_unarmed_melee_1,
            drop_npcs.goblin_unarmed_melee_2,
            drop_npcs.goblin_unarmed_melee_3,
            drop_npcs.goblin_unarmed_melee_4,
            drop_npcs.goblin_unarmed_melee_5,
            drop_npcs.goblin_unarmed_melee_6,
            drop_npcs.goblin_unarmed_melee_7,
            drop_npcs.goblin_unarmed_melee_8,
            drop_npcs.goblin_armed_melee_1,
            drop_npcs.goblin_armed_melee_2,
            drop_npcs.goblin_armed_melee_3,
            drop_npcs.goblin_armed_melee_4,
        )
    }

    private fun MutableList<Pair<NpcType, DropTable>>.assign(
        table: DropTable,
        vararg types: NpcType,
    ) {
        types.forEach { add(it to table) }
    }
}
