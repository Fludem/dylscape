package org.rsmod.api.perks

/**
 * A single gameplay effect a skill or system can be asked about, without knowing where it comes
 * from.
 *
 * Perks are deliberately fine-grained - "failed mining rolls get a second chance", not "Power
 * Miner" - so the code that applies one reads as the effect itself. Whatever grants them (today
 * only leagues relics) binds a [PerkSource]; see [Perks].
 */
enum class Perk {
    /** The echo pickaxe acts as a crystal pickaxe with no level requirement. */
    EchoPickaxe,
    /** A failed mining roll gets a separate 50% chance to succeed. */
    MiningSecondChance,
    /** Ore rocks give four ores before depleting. */
    RockHoldsFourOres,
    /** Mined ore goes to the bank when it has room. */
    MiningToBank,

    /** The echo axe acts as a crystal axe with no level requirement. */
    EchoAxe,
    /** A failed woodcutting roll gets a separate 50% chance to succeed. */
    WoodcuttingSecondChance,
    /** Cut logs go to the bank when it has room. */
    WoodcuttingToBank,
    /** Lighting logs never fails. */
    NeverFailFire,

    /** The echo harpoon stands in for every fishing tool. */
    EchoHarpoon,
    /** A failed fishing roll gets a separate 50% chance to succeed. */
    FishingSecondChance,
    /** Fishing attempts come one tick sooner. */
    FishingFaster,
    /** Caught fish go to the bank when it has room. */
    FishingToBank,
    /** Cooking never burns food. */
    NeverBurnFood,

    /** Every Mark of grace a rooftop course spawns comes with 10,000 coins. */
    AgilityMarkCoins,

    /** Every npc that is somebody's slayer task pays slayer experience, assigned or not. */
    SlayerAlwaysOnTask,

    /** Alchemy needs no runes or level, pays 15% more and has a 65% chance to keep the item. */
    GoldenAlchemy,
}
