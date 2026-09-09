package org.rsmod.content.custom.barrows.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.custom.barrows.BarrowsRun
import org.rsmod.content.custom.barrows.Brother
import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.content.custom.barrows.configs.barrows_locs
import org.rsmod.content.custom.barrows.configs.barrows_npcs
import org.rsmod.events.EventBus
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Getting into Barrows, raising the brothers, and getting out again.
 *
 * ### Why digging is an obj op and not a loc op
 *
 * The mounds carry no loc at all - `BarrowsDump` confirms the six dig spots are bare grass - so
 * there is nothing to right-click. OSRS handles this the same way: you use a spade on the mound
 * itself. Here that is `onOpHeld1(spade)` plus a coordinate check, and the coordinates come from
 * [BarrowsMap] which a map test keeps honest.
 *
 * The binding works because the spade carries `Dig` on iop1 in the cache. That matters: the client
 * builds a click from the op's *name* and the server cannot send one, so an op the cache does not
 * name would never reach us however the event was granted.
 *
 * ### Why the fight is not one coroutine
 *
 * The obvious shape - `launch { raise(); awaitDeath() }` - cannot work, for the reason
 * `KnightWavesScript` sets out at length: protected access locks the player out of acting for as
 * long as the block runs, so they would stand in the crypt unable to fight back. Coroutines here
 * only cover the moments the player is a passenger. The fight is driven by each brother's own
 * `queues.death` handler.
 *
 * Registering `onNpcQueue(type, queues.death)` **replaces** the default death script for that type,
 * since npc queues resolve type -> content group -> default and stop at the first match, which is
 * why [NpcDeath.deathNoDrops] is called by hand. Registering per type rather than tagging a shared
 * content group is deliberate: `contentGroup` holds a single value and two editors claiming one npc
 * conflict silently. `DropTableScript` wraps its *generated* registrations in `runCatching` and
 * yields to a handler that is already there, and the brothers are in its generated set, so this one
 * wins cleanly - but the ordering is worth knowing about.
 */
class BarrowsScript
@Inject
constructor(
    private val run: BarrowsRun,
    private val protectedAccess: ProtectedAccessLauncher,
    private val players: PlayerList,
    private val death: NpcDeath,
    private val locRepo: LocRepository,
    private val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // The chest is not in the map - a whole-map scan finds no `barrows_stone_chest` placed
        // anywhere - so we place it ourselves, permanently, at the centre of the crypt maze. Doing
        // it here rather than through a `MapLocSpawnBuilder` is deliberate: that builder replaces a
        // whole mapsquare's loc list, which would wipe the 3,990 locs that make up the crypts.
        onGameStartup { placeRewardChest() }

        onOpHeld1(objs.spade) { digMound() }

        for (brother in Brother.all) {
            onOpLoc1(checkNotNull(barrows_locs.sarcophagi[brother])) { searchSarcophagus(brother) }
            onOpLoc1(checkNotNull(barrows_locs.staircases[brother])) { climbOut(brother) }
            onNpcQueue(checkNotNull(barrows_npcs.brothers[brother]), queues.death) {
                brotherDefeated(brother)
            }
        }

        for (monster in barrows_npcs.monsters) {
            onNpcQueue(monster, queues.death) { monsterDefeated() }
        }
    }

    private fun placeRewardChest() {
        locRepo.add(
            coords = BarrowsMap.chest,
            type = barrows_locs.stone_chest,
            duration = Int.MAX_VALUE,
            angle = LocAngle.West,
            shape = LocShape.CentrepieceStraight,
        )
    }

    /**
     * A spade dig. Only means anything while standing on one of the six mounds; everywhere else it
     * falls through to the same refusal the rest of the world gives, so this binding costs nothing
     * to players digging elsewhere.
     */
    private suspend fun ProtectedAccess.digMound() {
        val brother = BarrowsMap.mounds.entries.firstOrNull { it.value == player.coords }?.key
        if (brother == null) {
            mes("You find nothing but earth.")
            return
        }
        mes("You dig through the mound...")
        delay(DIG_TICKS)
        telejump(checkNotNull(BarrowsMap.cryptEntrances[brother]))
        rebuildAppearance()
        player.startCryptTick(eventBus)
        mes("...and drop into the crypt of ${brother.displayName}.")
    }

    private suspend fun ProtectedAccess.searchSarcophagus(brother: Brother) {
        when (run.raise(this, brother)) {
            BarrowsRun.Raise.Raised -> {
                mes("${brother.displayName} rises from his rest.")
            }
            BarrowsRun.Raise.AlreadyKilled -> {
                mes("You search the sarcophagus, but its occupant is already at rest.")
            }
            // Shared world: somebody else has this brother up. Saying so is better than a silent
            // no-op, and better than stacking a second copy on the same tile.
            BarrowsRun.Raise.AlreadyRaised -> {
                mes("${brother.displayName} already walks. Deal with him first.")
            }
        }
    }

    private fun ProtectedAccess.climbOut(brother: Brother) {
        telejump(checkNotNull(BarrowsMap.mounds[brother]))
        rebuildAppearance()
        player.leaveCrypt(eventBus)
    }

    private suspend fun StandardNpcAccess.brotherDefeated(brother: Brother) {
        // Captured before the death sequence runs: it walks the npc, plays the death animation and
        // then despawns it, by which point neither is reliable.
        val deathCoords = npc.coords
        val hero = findHero(players)

        death.deathNoDrops(this)

        val player = hero ?: return
        // A brother spawned loose in the world by an admin is nobody's run. Proximity to where he
        // fell is what says this kill happened in the crypt.
        if (!player.isWithinDistance(deathCoords, SAME_CRYPT_DISTANCE)) {
            return
        }
        protectedAccess.launch(player) { run.brotherDefeated(this, brother) }
    }

    private suspend fun StandardNpcAccess.monsterDefeated() {
        val deathCoords = npc.coords
        val hero = findHero(players)

        death.deathNoDrops(this)

        val player = hero ?: return
        if (!player.isWithinDistance(deathCoords, SAME_CRYPT_DISTANCE)) {
            return
        }
        run.monsterDefeated(player)
    }

    private companion object {
        /** Long enough to read as digging, short enough not to be a chore six times a run. */
        const val DIG_TICKS = 2

        /** The crypt maze is 56 tiles across, so anything further out is not in it. */
        const val SAME_CRYPT_DISTANCE = 64
    }
}
