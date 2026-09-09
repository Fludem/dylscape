package org.rsmod.content.areas.misc.tutorial.configs

import org.rsmod.api.type.refs.loc.LocReferences

/**
 * Tutorial Island's own scenery: the doors and gate a player walks through, the ladders between the
 * surface and the cave, and the bank booth.
 *
 * Every one of these carries its op in the cache but sits in **no content group**, so nothing in
 * the base game binds any of them -- a fresh player could not leave the first room, could not reach
 * the mining cave, and could not open the bank. `newbie_door4`/`5` are the double doors, each half
 * a separate loc. [org.rsmod.content.areas.misc.tutorial.TutorialDoors] and
 * [org.rsmod.content.areas.misc.tutorial.TutorialScenery] bind them.
 */
object TutorialLocs : LocReferences() {
    val door1 = find("newbie_door1")
    val door2 = find("newbie_door2")
    val door3 = find("newbie_door3")
    val door4 = find("newbie_door4")
    val door4_left = find("newbiedoor4l")
    val door4_right = find("newbiedoor4r")
    val door5_left = find("newbiedoor5_l")
    val door5_right = find("newbiedoor5_r")
    val door6 = find("newbie_door6")
    val door7 = find("newbie_door7")
    val door8 = find("newbie_door8")
    val door9 = find("newbie_door9")

    /**
     * The fence gate between the Survival Expert's clearing and the Master Chef's, at 3089,3091-2.
     * Named as a gate rather than a door, so it was missed the first time round -- but it is the
     * only way west out of the survival area, and it decodes as the same single-model wall (shape
     * `WallStraight`, 1x1, `Open` on op1) as every door above, so it swings the same way.
     */
    val gate_left = find("newbiegateclosedl2")
    val gate_right = find("newbiegateclosedr2")

    val allDoors =
        listOf(
            door1,
            door2,
            door3,
            door4,
            door4_left,
            door4_right,
            door5_left,
            door5_right,
            door6,
            door7,
            door8,
            door9,
            gate_left,
            gate_right,
        )

    /**
     * The island's two ladders, each as a `Climb-down` top and a `Climb-up` bottom.
     *
     * `newbieladdertop1` (3088,3119, in the Quest Guide's house) drops into the mining cave at
     * 3088,9519, and `newbieladdertop2` (3111,3126) is the way back up out of the combat cave at
     * 3111,9526 -- the standard `+6400` dungeon offset. None of the four carry a content group, so
     * nothing in `generic-locs` binds them and the whole underground half of the tutorial --
     * mining, smelting, smithing and combat -- was unreachable.
     */
    val ladder_down_quest = find("newbieladdertop1")
    val ladder_up_mining = find("newbieladder1")
    val ladder_down_bank = find("newbieladdertop2")
    val ladder_up_combat = find("newbieladder2")

    val laddersDown = listOf(ladder_down_quest, ladder_down_bank)
    val laddersUp = listOf(ladder_up_mining, ladder_up_combat)

    /**
     * Tutorial Island's bank booth. The generic [org.rsmod.content.generic.locs.banks.BankBooth]
     * script binds `content.bank_booth` on **op2**, which every ordinary booth carries as "Bank" --
     * this one carries `Use` on **op1** and nothing else, so it cannot be tagged into that group
     * and is bound directly instead.
     */
    val bank_booth = find("newbiebankbooth")
}
