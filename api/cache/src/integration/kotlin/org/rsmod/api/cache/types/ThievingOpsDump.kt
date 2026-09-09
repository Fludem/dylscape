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
    /**
     * Enumerated from the cache by **display name**, not from a hand-typed list of internal names.
     *
     * A list of internal names is what the thieving ladder used to be, and it is how
     * `al_kharid_man` -- an ordinary `Man` wearing `Pickpocket` on op3 -- ended up unclickable
     * along with most of its siblings. Asking the cache which types wear a ladder name is the only
     * way to see all of them, and it also surfaces the ones whose op sits in an unexpected slot.
     */
    @Test
    fun GameTestState.`dump pickpocket target ops`() = runBasicGameTest {
        val targets = cacheTypes.npcs.values.filter { it.name in LADDER_NAMES }.sortedBy { it.id }
        for (npc in targets) {
            val slot = npc.op.indexOf("Pickpocket")
            val marker =
                when (slot) {
                    2 -> ""
                    -1 -> "  <-- no Pickpocket op; not a target"
                    else -> "  <-- Pickpocket is op${slot + 1}, so onOpNpc3 would never fire"
                }
            println(
                "NPC ${npc.id} sym=${npc.internalName} name='${npc.name}' " +
                    "ops=${npc.op.toList()}$marker"
            )
        }
        println("TOTAL_LADDER_NAMED=${targets.size}")
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
        /**
         * The display names that make an npc a rung of the ladder. Kept in step with
         * `ThievingTargetNpcs`, which registers exactly the types wearing one of these and carrying
         * `Pickpocket` on op3.
         */
        val LADDER_NAMES =
            setOf(
                "Man",
                "Woman",
                "Drunken man",
                "Farmer",
                "Warrior",
                "Al Kharid warrior",
                "Rogue",
                "Master Farmer",
                "Guard",
                "Head Guard",
                "Knight of Ardougne",
                "Watchman",
                "Paladin",
                "Hero",
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
