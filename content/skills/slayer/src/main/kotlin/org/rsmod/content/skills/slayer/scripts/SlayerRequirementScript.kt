package org.rsmod.content.skills.slayer.scripts

import com.github.michaelbull.logging.InlineLogger
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.slayer.SlayerRequirement
import org.rsmod.api.combat.commons.slayer.SlayerRequirementRegistry
import org.rsmod.content.skills.slayer.configs.SlayerGear
import org.rsmod.content.skills.slayer.data.SlayerTaskRepository
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Hands combat the slayer level and equipment gates.
 *
 * Combat enforces them but cannot look them up: the level belongs to the task, in a dbtable only
 * this module reads, and the equipment is authored in [SlayerGear]. Pushing a flat npc-keyed table
 * into the registry at boot keeps the per-attack check to one map lookup, and keeps api free of any
 * dependency on content.
 */
class SlayerRequirementScript
@Inject
constructor(
    private val repo: SlayerTaskRepository,
    private val registry: SlayerRequirementRegistry,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Depends on the repository being loaded. `SlayerRepositoryScript` does that in its own
        // startup and script order is not fixed, so the load is repeated here rather than assumed;
        // it is idempotent and reading the tables twice at boot is cheaper than a boot-order rule
        // nobody can see.
        repo.load()

        val helmets = slayerHelmets()
        val requirements = buildRequirements(helmets)
        registry.populate(requirements)

        logger.info {
            "Slayer: gated ${requirements.size} npcs " +
                "(${helmets.size} slayer helmet variants count as head protection)."
        }
    }

    private fun buildRequirements(helmets: IntSet): Map<Int, SlayerRequirement> {
        val requirements = HashMap<Int, SlayerRequirement>()
        for ((npcId, taskId) in repo.allNpcTasks()) {
            val task = repo.task(taskId) ?: continue
            val gear = SlayerGear.byTaskId[taskId]
            val level = task.slayerLevel
            if (level <= 1 && gear == null) {
                continue
            }
            val accepted =
                if (gear == null) {
                    IntSets.emptySet()
                } else {
                    val ids = IntOpenHashSet(gear.objs.map { it.id })
                    if (gear.helmSubstitutes) {
                        ids.addAll(helmets)
                    }
                    ids
                }
            requirements[npcId] =
                SlayerRequirement(level = level, gear = accepted, gearMessage = gear?.message ?: "")
        }
        return requirements
    }

    /**
     * Every slayer helmet in the cache, found by internal name rather than by id.
     *
     * The obvious approach - reading the `slayer_helm` and `slayer_helm_imbued` params, which is
     * what the melee formula uses to spot a black mask - does not work: **no obj in this cache
     * carries either param**. They are RSMod-authored params that were never populated, the same
     * dead end as `slayer_levelrequire` and `slayer_category`. The first cut of this shipped with
     * them and reported "0 slayer helmet variants", which would have meant a player in a slayer
     * helmet being told to go and fetch earmuffs.
     *
     * The names, by contrast, are the cache's own and are perfectly uniform across all 24 variants,
     * so this cannot drift from a hand-written id list either.
     */
    private fun slayerHelmets(): IntSet {
        val helmets = IntOpenHashSet()
        for ((id, type) in objTypes) {
            if (type.internalName?.startsWith(HELMET_PREFIX) == true) {
                helmets.add(id)
            }
        }
        return helmets
    }

    private companion object {
        /**
         * Matches all 24 helmet variants - plain, imbued, and every recolour from black through
         * araxyte - because the cache names them uniformly. Noted and placeholder copies are
         * `cert_`/`placeholder_` prefixed, so they fall outside this and should: neither can be
         * worn.
         */
        private const val HELMET_PREFIX = "slayer_helm"

        private val logger = InlineLogger()
    }
}
