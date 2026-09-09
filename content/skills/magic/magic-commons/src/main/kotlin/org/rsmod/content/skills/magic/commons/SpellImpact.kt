package org.rsmod.content.skills.magic.commons

import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.magic.commons.configs.magic_queues
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.entity.player.PlayerUid

/**
 * A spell side-effect (freeze, stat drain, heal) delayed to the tick its hit lands on.
 *
 * Carried through the caster's `magic_spell_impact` soft queue rather than applied at cast time, so
 * a barrage that freezes lands its freeze together with its hitsplat instead of a projectile flight
 * ahead of it. The target is held by uid and looked up again on arrival: an npc that died or
 * despawned in between is skipped rather than resurrected by a stale reference.
 */
public class SpellImpact(private val target: Target, public val effect: (PathingEntity) -> Unit) {
    public fun resolve(players: PlayerList, npcs: NpcList): PathingEntity? =
        when (target) {
            is Target.NpcTarget -> target.uid.resolve(npcs)?.takeIf { it.isValidTarget() }
            is Target.PlayerTarget -> target.uid.resolve(players)
        }

    public sealed class Target {
        public data class NpcTarget(val uid: NpcUid) : Target()

        public data class PlayerTarget(val uid: PlayerUid) : Target()
    }

    public companion object {
        public fun of(target: PathingEntity, effect: (PathingEntity) -> Unit): SpellImpact {
            val key =
                when (target) {
                    is Npc -> Target.NpcTarget(target.uid)
                    is Player -> Target.PlayerTarget(target.uid)
                }
            return SpellImpact(key, effect)
        }

        /** Schedules [effect] on [target] to run [delay] ticks from now, from [caster]'s queue. */
        public fun ProtectedAccess.queueSpellImpact(
            target: PathingEntity,
            delay: Int,
            effect: (PathingEntity) -> Unit,
        ) {
            softQueue(magic_queues.spell_impact, maxOf(1, delay), of(target, effect))
        }
    }
}
