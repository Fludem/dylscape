package org.rsmod.content.areas.misc.tutorial.configs

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid

/**
 * The classic Tutorial Island instructors, as they are named in the rev 233 cache (the `newbie_*`
 * set, npc ids 3305-8503). These are types, not spawns; [org.rsmod.content.areas.misc.tutorial.map]
 * places them.
 */
object TutorialNpcs : NpcReferences() {
    val guide = find("newbie_basics_instructor")
    val survival = find("newbie_survival_instructor")
    val chef = find("newbie_cook_instructor")
    val quest = find("newbie_quest_instructor")
    val mining = find("newbie_mining_instructor")
    val combat = find("newbie_combat_instructor")
    val account = find("newbie_account_instructor")
    val magic = find("newbie_magic_instructor")

    val all = listOf(guide, survival, chef, quest, mining, combat, account, magic)
}

/**
 * Items the instructors hand out and check for. Names are cache symbols; the tutorial's own
 * `newbie*` variants are used where they exist so nothing given here can be sold off the island.
 */
object TutorialObjs : ObjReferences() {
    val bronze_axe = find("bronze_axe")
    val tinderbox = find("tinderbox")
    val small_net = find("net")
    val newbie_raw_shrimp = find("newbieraw_shrimp")
    val cooked_shrimp = find("shrimp")
    val bronze_pickaxe = find("bronze_pickaxe")
    val hammer = find("hammer")
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val bronze_bar = find("bronze_bar")
    val bronze_dagger = find("bronze_dagger")
    val bronze_sword = find("bronze_sword")
    val wooden_shield = find("wooden_shield")
    val shortbow = find("shortbow")
    val bronze_arrow = find("bronze_arrow")
    val air_rune = find("airrune")
    val mind_rune = find("mindrune")
}

/**
 * The tutorial's progress is stored in the cache's own tutorial varp (281, `tutorial`), which the
 * character save pipeline persists like any other player varp, so a half-finished tutorial resumes
 * on the player's next login.
 */
object TutorialVarps : VarpReferences() {
    val tutorial = find("tutorial")
}

object TutorialConstants {
    /**
     * Whether a brand-new account is routed onto the island at login.
     *
     * **Off until the island coordinates are verified in a running client.** While off, new
     * accounts spawn at the realm's normal point (Lumbridge) exactly as before, and the tutorial
     * simply never triggers — nothing drags a fresh login to a guessed tile. Flip this to `true`
     * once [START_COORD] and the tiles in `npcs.toml` are confirmed to be real, walkable spots.
     */
    const val ROUTE_NEW_ACCOUNTS: Boolean = false

    /**
     * Where a brand-new account is placed to begin the tutorial.
     *
     * **APPROXIMATE — verify in game.** The classic island sits in mapsquare 48_48, but the cache
     * carries no npc spawns (they are server-authored), so the exact tiles of each room are not
     * readable from it. This coord, and every one in `npcs.toml`, is a best-effort starting point
     * in the Gielinor Guide's room and should be nudged once seen in a running client. The tutorial
     * *logic* does not depend on it; only where the player physically stands does.
     */
    val START_COORD: CoordGrid = CoordGrid(0, 48, 48, 22, 35)

    /** Where the finished tutorial deposits the player: Lumbridge, the realm's normal spawn. */
    val LUMBRIDGE: CoordGrid = CoordGrid(0, 50, 50, 21, 18)
}
