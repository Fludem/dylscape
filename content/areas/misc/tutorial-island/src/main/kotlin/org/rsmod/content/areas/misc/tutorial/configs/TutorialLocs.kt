package org.rsmod.content.areas.misc.tutorial.configs

import org.rsmod.api.type.refs.loc.LocReferences

/**
 * Tutorial Island's doors. Every one of these carries `Open` on op1 (decoded from the cache) but
 * nothing in the base game binds them, so a fresh player could not leave the first room.
 * [org.rsmod.content.areas.misc.tutorial.TutorialDoors] gives them the standard swing-open
 * behaviour. `newbie_door4`/`5` are the double doors, each half a separate loc.
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
        )
}
