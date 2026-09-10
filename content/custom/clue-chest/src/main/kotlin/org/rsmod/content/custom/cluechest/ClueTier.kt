package org.rsmod.content.custom.cluechest

/**
 * A clue key's tier, and what its casket pays.
 *
 * [casket] names the table in `caskets.toml`, which `tools/drop-tables/caskets.py` generates from
 * the wiki's per-roll casket rows. [rolls] is how many times that table is rolled per casket, which
 * the data does not carry: these are the vanilla ranges.
 *
 * Declared lowest first; the chest's `Loot` op spends the highest tier a player holds.
 */
enum class ClueTier(val casket: String, val rolls: IntRange) {
    Beginner("Reward casket (beginner)", 1..3),
    Easy("Reward casket (easy)", 2..4),
    Medium("Reward casket (medium)", 3..5),
    Hard("Reward casket (hard)", 4..6),
    Elite("Reward casket (elite)", 4..6),
}
