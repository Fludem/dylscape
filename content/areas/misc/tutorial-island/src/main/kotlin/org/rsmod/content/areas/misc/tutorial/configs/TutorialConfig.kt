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

    /** Brother Brace, the prayer instructor in the chapel. The cache calls him `brother_noob`. */
    val prayer = find("brother_noob")

    /** The banker behind the tutorial bank's counter. Not an instructor; spawned for flavour. */
    val banker = find("noobbanker")

    /** The level-3 tutorial rats, not `rat_boss_giant_rat` (Scurrius, level 46). */
    val giant_rat = find("newbiegiantrat")
    val giant_rat2 = find("newbiegiantrat2")
    val giant_rat3 = find("newbiegiantrat3")

    val all = listOf(guide, survival, chef, quest, mining, combat, account, prayer, magic)

    /** Everything this module pins in place, instructors plus the banker. */
    val stationary = all + banker
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

    /**
     * The Master Chef's bread lesson. `newbie_pot_flour` is the island's own flour, which the
     * cooking module already pairs with any water container to make bread dough directly rather
     * than offering the three-dough choice.
     */
    val newbie_pot_flour = find("newbie_pot_flour")
    val bucket_water = find("bucket_water")
    val bread_dough = find("bread_dough")
    val bread = find("bread")
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
     * **On.** Every tile the tutorial puts anything on is now read out of the cache and asserted
     * standable by `TutorialMapTest`, and the island is traversable end to end: the doors and the
     * survival-to-kitchen gate swing, both ladders reach the mining cave, and the trees, ore rocks,
     * fishing spots, range, furnace, anvils and bank booth are all bound to live skill scripts.
     * That last part is what this flag really waits on — finishing the tutorial is the only way off
     * the island, so routing new accounts onto it while a step could not be completed would strand
     * them.
     */
    const val ROUTE_NEW_ACCOUNTS: Boolean = true

    /**
     * Where a brand-new account is placed to begin the tutorial: 3094,3107, on the floor of the
     * Gielinor Guide's room, a few tiles inside the door at 3098,3107 and south of the Guide
     * himself.
     *
     * Confirmed against the cache's own collision map rather than guessed, and re-checked by
     * `TutorialMapTest` on every build.
     */
    val START_COORD: CoordGrid = CoordGrid(0, 48, 48, 22, 35)

    /** Where the finished tutorial deposits the player: Lumbridge, the realm's normal spawn. */
    val LUMBRIDGE: CoordGrid = CoordGrid(0, 50, 50, 21, 18)

    /**
     * Where each stage's hint arrow points, as a tile.
     *
     * A **tile**, not an npc, on purpose. The arrow used to be placed by looking for the next
     * instructor within three zones of the player, which is the one thing that is never true at the
     * moment it matters: an instructor sends you to the *next* room, which is always further away
     * than that. The lookup returned nothing, the call quietly did nothing, and the arrow stayed
     * stuck on the instructor you had just finished with. Pointing at a fixed tile always works,
     * whether or not the npc there is loaded yet.
     *
     * Two of these are not instructors at all but the ladders between the island and the cave --
     * the step the Quest Guide and the Combat Instructor send you to is the ladder, not a person.
     */
    val HINT_TILES: Map<String, CoordGrid> =
        mapOf(
            // Instructors, matching `npcs.toml`.
            "survival" to CoordGrid(0, 48, 48, 31, 23), // 3103,3095
            "chef" to CoordGrid(0, 48, 48, 3, 11), // 3075,3083
            "quest" to CoordGrid(0, 48, 48, 14, 51), // 3086,3123
            "mining" to CoordGrid(0, 48, 148, 9, 33), // 3081,9505
            "combat" to CoordGrid(0, 48, 148, 33, 37), // 3105,9509
            "account" to CoordGrid(0, 48, 48, 55, 51), // 3127,3123
            "prayer" to CoordGrid(0, 48, 48, 50, 35), // 3122,3107
            "magic" to CoordGrid(0, 49, 48, 4, 17), // 3140,3089
            // Ladders.
            "ladder_down" to CoordGrid(0, 48, 48, 16, 47), // 3088,3119, Quest Guide's house
            "ladder_up" to CoordGrid(0, 48, 148, 39, 54), // 3111,9526, out of the combat cave
        )
}
