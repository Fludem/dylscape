package org.rsmod.content.skills.thieving.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

private typealias loot = ThievingObjs

/**
 * Market stalls, all of which carry `Steal-from` on **op2** (decoded, see `ThievingOpsDump`).
 *
 * These are map locs, so unlike the pickpocket targets they already stand in the world — the server
 * decodes the game map from the cache, and the six `*thiefstall` locs make up the East Ardougne
 * market square. No spawn authoring is needed for any of them.
 *
 * Two families are deliberately left untagged:
 * - `silkthiefstall_noop`, `gemthiefstall_noop`, `silverthiefstall_noop` and
 *   `seed_stall_nothieving` are the "already robbed" variants that stand in for a depleted stall.
 *   Tagging one would hand the player an infinitely stealable empty stall.
 * - `seed_stall` itself, because its payout is a weighted seed table — real farming content this
 *   module has no business inventing. Faking it with a placeholder product would be worse than
 *   leaving the stall alone. Master farmers hit the same problem on the pickpocket side and are
 *   resolved differently there -- they stay on the ladder and pay a purse instead of seeds.
 */
object ThievingStallLocs : LocReferences() {
    val bakery = find("cakethiefstall")
    val tea = find("tea_stall")
    val silk = find("silkthiefstall")
    val fur = find("furthiefstall")
    val silver = find("silverthiefstall")
    val spice = find("spicethiefstall")
    val gem = find("gemthiefstall")
}

/**
 * One stall.
 *
 * [baseXp] is the real OSRS value and [baseCount] the real OSRS haul; [ThievingRates] scales both
 * at payout.
 *
 * [loot] is a list because the bakery and the gem stall roll between several products, and repeated
 * entries act as weights — a singleton list is the common case rather than a special one.
 *
 * [ticks] is the throttle. Stealing from a stall never fails in OSRS; what limits stall income
 * there is the respawn timer, and this module has removed that on purpose. So the tick rate is the
 * *only* thing pacing a stall now, which makes it the single biggest economy lever here.
 */
data class Stall(
    val what: String,
    val level: Int,
    val baseXp: Double,
    val loot: List<ObjType>,
    val baseCount: Int = 1,
    val ticks: Int = 3,
)

object ThievingStalls {
    val all: Map<LocType, Stall> =
        mapOf(
            ThievingStallLocs.bakery to
                Stall(
                    what = "baker's stall",
                    level = 5,
                    baseXp = 16.0,
                    loot = listOf(loot.cake, loot.cake, loot.bread, loot.chocolate_slice),
                ),
            ThievingStallLocs.tea to
                Stall(what = "tea stall", level = 5, baseXp = 16.0, loot = listOf(loot.cup_of_tea)),
            ThievingStallLocs.silk to
                Stall(what = "silk stall", level = 20, baseXp = 24.0, loot = listOf(loot.silk)),
            ThievingStallLocs.fur to
                Stall(
                    what = "fur stall",
                    level = 35,
                    baseXp = 36.0,
                    loot = listOf(loot.grey_wolf_fur),
                    ticks = 4,
                ),
            ThievingStallLocs.silver to
                Stall(
                    what = "silver stall",
                    level = 50,
                    baseXp = 54.0,
                    loot = listOf(loot.silver_ore),
                    ticks = 5,
                ),
            ThievingStallLocs.spice to
                Stall(
                    what = "spice stall",
                    level = 65,
                    baseXp = 81.0,
                    loot = listOf(loot.spice),
                    ticks = 6,
                ),
            ThievingStallLocs.gem to
                Stall(
                    what = "gem stall",
                    level = 75,
                    baseXp = 160.0,
                    // Repeats are the weighting: sapphires are common, diamonds are not.
                    loot =
                        listOf(
                            loot.uncut_sapphire,
                            loot.uncut_sapphire,
                            loot.uncut_sapphire,
                            loot.uncut_sapphire,
                            loot.uncut_emerald,
                            loot.uncut_emerald,
                            loot.uncut_ruby,
                            loot.uncut_diamond,
                        ),
                    ticks = 10,
                ),
        )

    val lowestLevel: Int = all.values.minOf { it.level }
}
