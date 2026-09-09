package org.rsmod.content.skills.slayer.scripts

import com.github.michaelbull.logging.InlineLogger
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.slayer.SlayerRequirement
import org.rsmod.api.combat.commons.slayer.SlayerRequirementRegistry
import org.rsmod.api.config.refs.params
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
     * Every slayer helmet in the cache, found by the param rather than by id.
     *
     * `slayer_helm` and `slayer_helm_imbued` are the same params the melee formula already uses to
     * spot a black mask, so this picks up the recoloured and imbued variants for free and cannot
     * drift from a hand-written list.
     */
    private fun slayerHelmets(): IntSet {
        val helmets = IntOpenHashSet()
        for ((id, type) in objTypes) {
            if (type.param(params.slayer_helm) != 0 || type.param(params.slayer_helm_imbued) != 0) {
                helmets.add(id)
            }
        }
        return helmets
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
