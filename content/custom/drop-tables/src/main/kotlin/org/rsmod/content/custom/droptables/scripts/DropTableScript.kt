package org.rsmod.content.custom.droptables.scripts

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onNpcQueue
import org.rsmod.content.custom.droptables.DropBoost
import org.rsmod.content.custom.droptables.DropTable
import org.rsmod.content.custom.droptables.DropTableRoller
import org.rsmod.content.custom.droptables.configs.LumbridgeDropTables
import org.rsmod.content.custom.droptables.data.DropTableResourceLoader
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Gives npcs a drop table on death.
 *
 * Npc queues resolve in `type -> content group -> default` order and stop at the first match, so
 * registering a type-specific `death` handler replaces [org.rsmod.api.death.plugin.NpcDeathScript]
 * for that npc. That means this script owns the whole death sequence for the npcs it covers, and
 * calls [NpcDeath.deathNoDrops] before spawning its own loot instead of the default single bone.
 */
class DropTableScript
@Inject
constructor(
    private val death: NpcDeath,
    private val roller: DropTableRoller,
    private val objRepo: ObjRepository,
    private val players: PlayerList,
    private val generated: DropTableResourceLoader,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Keyed on the raw npc id: `find()` hands back a `HashedNpcType` and the loader hands back
        // an `UnpackedNpcType`, and those never compare equal.
        val claimed = HashSet<Int>()

        // Hand-written tables register first so that they win. That ordering is load-bearing:
        // `EventBus.subscribeSuspend` throws on a duplicate key, so registering the same npc twice
        // is a boot failure rather than an override.
        for ((type, table) in LumbridgeDropTables.assignments) {
            claimed += type.id
            onNpcQueue(type, queues.death) { dropTableDeath(table) }
        }

        val loaded = generated.load()
        var registered = 0
        var conflicts = 0
        for ((type, table) in loaded.assignments) {
            if (!claimed.add(type.id)) {
                continue
            }
            // Another module may already own this npc's death - `KnightWavesScript` does, for its
            // knights - and script start-up order is ClassGraph scan order, so we cannot check
            // first. Losing the race is the right outcome anyway: bespoke content beats generated
            // data, exactly as a hand-written table does.
            val result = runCatching { onNpcQueue(type, queues.death) { dropTableDeath(table) } }
            if (result.isFailure) {
                conflicts++
                logger.warn {
                    "Npc '${type.internalName}' already has a death handler; " +
                        "keeping it over the generated table."
                }
                continue
            }
            registered++
        }

        for (error in loaded.errors.take(MAX_LOGGED_ERRORS)) {
            logger.warn { "Skipped generated drop table: $error" }
        }
        if (loaded.errors.size > MAX_LOGGED_ERRORS) {
            logger.warn { "... and ${loaded.errors.size - MAX_LOGGED_ERRORS} more." }
        }
        logger.info {
            "Registered $registered generated drop tables " +
                "(${loaded.errors.size} rows skipped, ${loaded.unattackable.size} unattackable, " +
                "$conflicts already owned, ${LumbridgeDropTables.assignments.size} hand-written)."
        }
    }

    private suspend fun StandardNpcAccess.dropTableDeath(table: DropTable) {
        // Both are captured before the death sequence runs: it walks the npc, plays the death
        // animation and then despawns it, by which point neither is reliable.
        val dropCoords = coords
        val hero = findHero(players)

        death.deathNoDrops(this)

        if (hero == null) {
            return
        }

        val duration = hero.lootDropDuration ?: constants.lootdrop_duration
        for (drop in roller.roll(table, DropBoost.forTier(hero.xpRateTier))) {
            objRepo.add(
                type = drop.obj,
                coords = dropCoords,
                duration = duration,
                receiver = hero,
                count = drop.count,
            )
        }
    }

    private companion object {
        private const val MAX_LOGGED_ERRORS = 25

        private val logger = InlineLogger()
    }
}
