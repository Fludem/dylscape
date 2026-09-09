package org.rsmod.content.custom.barrows

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.content.custom.barrows.BarrowsProgress.brothersKilled
import org.rsmod.content.custom.barrows.BarrowsProgress.rewardPotential
import org.rsmod.content.custom.barrows.configs.BarrowsRewards
import org.rsmod.content.custom.droptables.DropTableRoller
import org.rsmod.content.custom.droptables.RolledDrop
import org.rsmod.game.entity.Player

/**
 * Rolls a Barrows chest.
 *
 * Split out from the scripts and from [BarrowsRun] because this is the part with arithmetic in it,
 * and arithmetic is worth testing against a scripted [GameRandom] rather than against a live world.
 *
 * The two stages are OSRS's, and [BarrowsRewards] documents them: one roll per brother killed plus
 * one, and each roll independently tests the reward potential to decide whether it reaches the
 * equipment table or falls through to the consolation table.
 */
@Singleton
class BarrowsChest
@Inject
constructor(private val random: GameRandom, private val roller: DropTableRoller) {
    /**
     * Rolls the whole chest for [player] and returns everything it paid out, in roll order.
     *
     * Reads the player's state but never writes it: clearing the run is the caller's job, so that a
     * chest whose loot could not be handed over does not silently consume the run.
     */
    fun roll(player: Player): List<RolledDrop> = roll(player.brothersKilled, player.rewardPotential)

    /**
     * The same roll expressed in numbers rather than in a player, so it can be pinned against a
     * scripted [GameRandom] in a plain unit test instead of needing a world.
     */
    fun roll(brothersKilled: Int, potential: Int): List<RolledDrop> {
        val drops = mutableListOf<RolledDrop>()
        repeat(BarrowsRewards.rollCount(brothersKilled)) {
            if (rollsEquipment(potential)) {
                drops += RolledDrop(BarrowsRewards.equipment[random.of(EQUIPMENT_COUNT)], 1)
            } else {
                drops += roller.roll(BarrowsRewards.consolation)
            }
        }
        return drops
    }

    /**
     * Whether one roll reaches the equipment table.
     *
     * `potential / (MAX * RARE_SCALE)`, expressed as a single draw so a scripted-random test can
     * pin it exactly. At full potential that is `1 in 45`; at zero it can never hit, which is
     * correct - a chest opened having killed nobody and cleared nothing pays consolation only.
     */
    fun rollsEquipment(potential: Int): Boolean {
        val space = BarrowsProgress.MAX_REWARD_POTENTIAL * BarrowsRewards.RARE_SCALE
        return random.of(space) < potential
    }

    private companion object {
        val EQUIPMENT_COUNT = BarrowsRewards.equipment.size
    }
}
