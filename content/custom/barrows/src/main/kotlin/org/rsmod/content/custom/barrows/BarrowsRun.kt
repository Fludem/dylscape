package org.rsmod.content.custom.barrows

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.custom.barrows.BarrowsProgress.brothersAlive
import org.rsmod.content.custom.barrows.BarrowsProgress.hasKilled
import org.rsmod.content.custom.barrows.BarrowsProgress.markKilled
import org.rsmod.content.custom.barrows.BarrowsProgress.rewardPotential
import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.content.custom.barrows.configs.barrows_npcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * The rules of a Barrows run, kept apart from the scripts so they can be tested on their own.
 *
 * The scripts are bindings and messages; this is the part with decisions in it - who is raised,
 * what a kill is worth, and whether a second player may raise a brother that is already up.
 */
@Singleton
class BarrowsRun
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val random: GameRandom,
) {
    /**
     * Raises [brother] from his sarcophagus, if there is anything to raise.
     *
     * Returns why nothing happened, or [Raise.Raised] on success. The refusals matter because
     * Barrows is shared-world here: see [findLiveBrother].
     */
    fun raise(access: ProtectedAccess, brother: Brother): Raise {
        if (access.player.hasKilled(brother)) {
            return Raise.AlreadyKilled
        }
        val spawn = BarrowsMap.brotherSpawn(brother)
        if (findLiveBrother(brother, spawn) != null) {
            return Raise.AlreadyRaised
        }
        spawn(brother, spawn)
        return Raise.Raised
    }

    /** Puts [brother] on [coords] with a lifetime long enough that no fight outlasts it. */
    fun spawn(brother: Brother, coords: CoordGrid) {
        val type = npcTypes[checkNotNull(barrows_npcs.brothers[brother])]
        npcRepo.add(Npc(type, coords), duration = BROTHER_LIFETIME)
    }

    /**
     * A brother of this type already standing near his sarcophagus.
     *
     * RSMod has no per-player npc visibility, so a raised brother is a world npc that everybody in
     * the crypt can see and hit. On a small server that is livable, but two players searching one
     * sarcophagus must not stack two copies of the same brother on one tile, so a search is refused
     * while one is up. The kill still credits whoever actually did the damage, through `findHero`.
     */
    fun findLiveBrother(brother: Brother, near: CoordGrid): Npc? {
        val type = checkNotNull(barrows_npcs.brothers[brother])
        return npcRepo.findAll(ZoneKey.from(near), zoneRadius = 1).firstOrNull {
            it.id == type.id && it.coords.chebyshevDistance(near) <= RAISE_RADIUS
        }
    }

    /** Records a brother's death and pays the reward potential for it. */
    fun brotherDefeated(access: ProtectedAccess, brother: Brother) {
        val player = access.player
        if (player.hasKilled(brother)) {
            return
        }
        player.markKilled(brother)
        player.rewardPotential += BarrowsProgress.BROTHER_POTENTIAL
        val remaining = player.brothersAlive.size
        if (remaining == 0) {
            access.mes("You have defeated all six brothers. The chest is yours to open.")
        } else {
            val plural = if (remaining == 1) "brother" else "brothers"
            access.mes("${brother.displayName} falls. $remaining $plural still rest.")
        }
    }

    /** Records a crypt monster kill. Worth far less than a brother, but it adds up. */
    fun monsterDefeated(player: Player) {
        player.rewardPotential += BarrowsProgress.MONSTER_POTENTIAL
    }

    /**
     * Picks the brother who ambushes a player looting the chest early, or `null` when all six are
     * already dead and the loot is unguarded.
     */
    fun pickAmbush(player: Player): Brother? = player.brothersAlive.randomOrNull()

    private fun <T> List<T>.randomOrNull(): T? = if (isEmpty()) null else this[random.of(size)]

    enum class Raise {
        Raised,
        AlreadyKilled,
        AlreadyRaised,
    }

    private companion object {
        /**
         * Long enough that no fight outlasts it, short enough that a brother abandoned mid-fight
         * cleans himself up rather than standing in the crypt forever. Thirty minutes of ticks.
         */
        const val BROTHER_LIFETIME = 3000

        /**
         * A sarcophagus is 2x3 and a brother is raised on its own tile, so anything within a couple
         * of tiles of it is "the brother from this sarcophagus".
         */
        const val RAISE_RADIUS = 3
    }
}
