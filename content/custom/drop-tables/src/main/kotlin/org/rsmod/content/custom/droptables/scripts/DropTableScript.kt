package org.rsmod.content.custom.droptables.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onNpcQueue
import org.rsmod.content.custom.droptables.DropTable
import org.rsmod.content.custom.droptables.DropTableRoller
import org.rsmod.content.custom.droptables.configs.LumbridgeDropTables
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
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((type, table) in LumbridgeDropTables.assignments) {
            onNpcQueue(type, queues.death) { dropTableDeath(table) }
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
        for (drop in roller.roll(table)) {
            objRepo.add(
                type = drop.obj,
                coords = dropCoords,
                duration = duration,
                receiver = hero,
                count = drop.count,
            )
        }
    }
}
