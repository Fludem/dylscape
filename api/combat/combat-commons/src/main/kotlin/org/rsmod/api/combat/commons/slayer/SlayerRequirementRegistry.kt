package org.rsmod.api.combat.commons.slayer

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntSet
import jakarta.inject.Singleton
import org.rsmod.game.type.npc.UnpackedNpcType

/**
 * What a monster demands before it can be attacked: a slayer level, and sometimes a piece of
 * protective equipment.
 *
 * [gear] is a set of obj ids of which the player needs *any one*, already expanded to include every
 * substitute - so the slayer helmet variants are in the banshee entry rather than being a special
 * case here. An empty set means no equipment is needed. [gearMessage] is what the player is told
 * when they lack it, phrased per monster.
 */
public data class SlayerRequirement(
    public val level: Int,
    public val gear: IntSet,
    public val gearMessage: String,
)

/**
 * The slayer requirements combat enforces, keyed by npc type id.
 *
 * Combat has to enforce these but cannot know them: the level is a property of the *task* and lives
 * in the `slayer_task` dbtable, and the equipment is authored by the slayer module. Reading either
 * is content's job, and api must not depend on content - so the module fills this at start-up and
 * combat reads it. Same shape as the special-attack, weapon and spell registries.
 *
 * Empty until populated, and an empty registry gates nothing. That is the right way round: with the
 * slayer module absent every monster stays attackable, rather than every monster becoming
 * unattackable.
 */
@Singleton
public class SlayerRequirementRegistry {
    private var requirements: Int2ObjectMap<SlayerRequirement> = Int2ObjectOpenHashMap()

    /** Replaces the table wholesale. Called once, from the slayer module's start-up. */
    public fun populate(byNpc: Map<Int, SlayerRequirement>) {
        requirements = Int2ObjectOpenHashMap(byNpc)
    }

    /** What [type] demands, or null when it demands nothing. */
    public fun get(type: UnpackedNpcType): SlayerRequirement? = requirements[type.id]
}
