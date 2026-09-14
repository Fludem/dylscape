package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.custom.cluechest.ClueTier
import org.rsmod.content.custom.cluechest.configs.ClueKeys
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * The bonus clue keys two relics hand out: Clue Compass on kills, Dodgy Deals on pickpockets. They
 * are the same key objs the drop tables drop, so they open at the chest or the compass alike.
 */
@Singleton
class ClueKeyRewards
@Inject
constructor(private val rolls: GameRandom, private val objRepo: ObjRepository) {
    /**
     * A 1 in [chance] roll for a [tier] key. A won key goes into [player]'s inventory, or onto
     * [coords] for them alone when that is full. Returns whether the roll was won.
     */
    fun roll(player: Player, chance: Int, tier: ClueTier, coords: CoordGrid): Boolean {
        if (rolls.of(chance) != 0) {
            return false
        }
        player.invAddOrDrop(objRepo, ClueKeys.keyFor(tier), coords = coords)
        return true
    }

    companion object {
        /** The key a monster of combat [level] drops for Clue Compass. */
        fun tierForCombatLevel(level: Int): ClueTier =
            when {
                level < 20 -> ClueTier.Beginner
                level < 50 -> ClueTier.Easy
                level < 100 -> ClueTier.Medium
                level < 200 -> ClueTier.Hard
                else -> ClueTier.Elite
            }

        /** The key a pickpocket target needing Thieving [level] holds for Dodgy Deals. */
        fun tierForThievingLevel(level: Int): ClueTier =
            when {
                level < 25 -> ClueTier.Beginner
                level < 40 -> ClueTier.Easy
                level < 65 -> ClueTier.Medium
                level < 80 -> ClueTier.Hard
                else -> ClueTier.Elite
            }
    }
}
