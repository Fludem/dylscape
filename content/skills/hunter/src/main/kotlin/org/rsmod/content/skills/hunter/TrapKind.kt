package org.rsmod.content.skills.hunter

/**
 * The four trapping methods.
 *
 * Stored as an int on both the trap item/loc and the creature, so a creature can only be caught by
 * the method that is meant to catch it, and one engine can drive all four.
 */
object TrapKind {
    const val SNARE: Int = 0
    const val BOX: Int = 1
    const val NET: Int = 2
    const val DEADFALL: Int = 3
}

/**
 * Where a laid trap is in its lifecycle.
 *
 * `ARMED` and the two resting states (`FULL`, `FAILED`) persist until the player acts or the trap
 * times out. `CATCHING` and `FAILING` are transient: the cache's locs for those states carry no ops
 * at all, so a player physically cannot interact while one is showing.
 */
object TrapState {
    const val ARMED: Int = 0
    const val CATCHING: Int = 1
    const val FULL: Int = 2
    const val FAILING: Int = 3
    const val FAILED: Int = 4
}
