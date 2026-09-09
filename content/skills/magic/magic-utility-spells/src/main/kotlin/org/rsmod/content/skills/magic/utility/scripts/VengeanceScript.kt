package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.magic.commons.MagicBuffRegistry
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.MagicStatus
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.events.EventBus
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Vengeance: the next hit taken rebounds three quarters of its damage onto whoever dealt it.
 *
 * The rebound hangs off `PlayerHitEvents.Impact`, which `StandardPlayerHitProcessor` publishes once
 * a hit has landed. It is an unbound event on purpose: the hit queue itself takes exactly one
 * handler, and that one is upstream's. Only sourced hits count, so poison and other typeless damage
 * never trigger it. The thirty-second cooldown is a soft timer.
 */
class VengeanceScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val buffs: MagicBuffRegistry,
    private val players: PlayerList,
    private val npcs: NpcList,
    private val npcHitModifier: NpcHitModifier,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spell = spellbooks.spellFor(objs.spell_vengeance)
        onSelfSpell(spell, protectedAccess, eventBus) {
            val cast =
                casting.attemptUtility(this, spell) {
                    when {
                        buffs[player].vengeance -> {
                            mes("You already have Vengeance cast.")
                            false
                        }
                        MagicStatus.onVengeanceCooldown(player) -> {
                            mes("You can only cast Vengeance spells every 30 seconds.")
                            false
                        }
                        else -> true
                    }
                }
            if (cast) {
                anim(utility_seqs.vengeance)
                spotanim(utility_spotanims.vengeance, height = 92)
                buffs[player].vengeance = true
                player.softTimer(magic_timers.vengeance_cooldown, COOLDOWN_TICKS)
            }
        }
        onEvent<PlayerHitEvents.Impact> { rebound(hit) }
    }

    private fun PlayerHitEvents.Impact.rebound(hit: Hit) {
        val state = buffs[player]
        if (!state.vengeance || hit.damage <= 0) {
            return
        }
        if (!hit.isFromNpc && !hit.isFromPlayer) {
            return
        }
        state.vengeance = false
        player.say("Taste vengeance!")
        val recoil = hit.damage * REBOUND_PERCENT / 100
        if (recoil <= 0) {
            return
        }
        if (hit.isFromNpc) {
            val npc = hit.resolveNpcSource(npcs) ?: return
            npc.queueHit(
                source = player,
                delay = 1,
                type = HitType.Typeless,
                damage = recoil,
                modifier = npcHitModifier,
            )
        } else {
            val attacker = hit.resolvePlayerSource(players) ?: return
            attacker.queueHit(source = player, delay = 1, type = HitType.Typeless, damage = recoil)
        }
    }

    private companion object {
        const val COOLDOWN_TICKS = 50
        const val REBOUND_PERCENT = 75
    }
}
