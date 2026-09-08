package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Decodes the interaction options the Thieving content is going to hang off.
 *
 * `OpNpcHandler`/`OpLocHandler` refuse to dispatch an op whose text is absent from the cache type,
 * so which slot carries `Pickpocket` and `Steal-from` decides which `onOpNpcN`/`onOpLocN` the
 * scripts register against. Mining set the precedent of reading this out of the cache rather than
 * assuming it, and the same applies here: the option order is not guaranteed to be uniform across a
 * ladder that spans `man` to `hero`.
 *
 * Also dumps the coin pouch objs, because the whole "your inventory never fills" premise rests on
 * them being stackable, and that is a cache fact rather than a design choice.
 */
class ThievingOpsDump {
    @Test
    fun GameTestState.`dump pickpocket target ops`() = runBasicGameTest {
        for (name in TARGET_NPCS) {
            val npc = cacheTypes.npcs.values.firstOrNull { it.internalName == name }
            if (npc == null) {
                println("NPC $name -> MISSING")
                continue
            }
            println("NPC ${npc.id} sym=$name name='${npc.name}' ops=${npc.op.toList()}")
        }
    }

    @Test
    fun GameTestState.`dump stall ops`() = runBasicGameTest {
        for (name in STALL_LOCS) {
            val loc = cacheTypes.locs.values.firstOrNull { it.internalName == name }
            if (loc == null) {
                println("LOC $name -> MISSING")
                continue
            }
            println("LOC ${loc.id} sym=$name name='${loc.name}' ops=${loc.op.toList()}")
        }
    }

    @Test
    fun GameTestState.`dump coin pouch objs`() = runBasicGameTest {
        val pouches =
            cacheTypes.objs.values
                .filter { it.internalName?.startsWith("pickpocket_coin_pouch") == true }
                .sortedBy { it.id }
        for (pouch in pouches) {
            println(
                "OBJ ${pouch.id} sym=${pouch.internalName} name='${pouch.name}' " +
                    "stackable=${pouch.stackable} cert=${pouch.certlink} " +
                    "ops=${pouch.op.toList()} iops=${pouch.iop.toList()}"
            )
        }
        println("TOTAL_POUCHES=${pouches.size}")
    }

    @Test
    fun GameTestState.`dump stall loot cert links`() = runBasicGameTest {
        for (name in STALL_LOOT) {
            val obj = cacheTypes.objs.values.firstOrNull { it.internalName == name }
            if (obj == null) {
                println("OBJ $name -> MISSING")
                continue
            }
            println(
                "OBJ ${obj.id} sym=$name name='${obj.name}' stackable=${obj.stackable} " +
                    "certlink=${obj.certlink} certtemplate=${obj.certtemplate}"
            )
        }
    }

    private companion object {
        /** The pickpocket ladder, lowest tier first. */
        val TARGET_NPCS =
            listOf(
                "man",
                "man2",
                "man3",
                "man_indoor",
                "woman",
                "woman2",
                "woman3",
                "farmer1",
                "farmer2",
                "farmer3",
                "farmer4",
                "warrior_woman",
                "thug",
                "rogue",
                "master_farmer_1",
                "master_farmer_2",
                "master_farmer_1_f",
                "master_farmer_2_f",
                "guard1",
                "guard1_f",
                "knight_of_ardougne",
                "knight_of_ardougne2",
                "knight_of_ardougne_west",
                "yanille_watchman",
                "paladin",
                "paladin2",
                "paladin_west",
                "hero",
            )

        val STALL_LOCS =
            listOf(
                "cakethiefstall",
                "tea_stall",
                "silkthiefstall",
                "seed_stall",
                "furthiefstall",
                "silverthiefstall",
                "spicethiefstall",
                "gemthiefstall",
            )

        val STALL_LOOT =
            listOf(
                "cake",
                "bread",
                "chocolate_slice",
                "cup_of_tea",
                "silk",
                "grey_wolf_fur",
                "silver_ore",
                "uncut_sapphire",
                "uncut_emerald",
                "uncut_ruby",
                "uncut_diamond",
            )
    }
}
