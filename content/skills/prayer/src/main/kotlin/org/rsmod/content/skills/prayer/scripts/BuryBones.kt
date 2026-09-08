package org.rsmod.content.skills.prayer.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.objXpParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.prayer.configs.PrayerContent
import org.rsmod.content.skills.prayer.configs.PrayerSeqs
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Burying bones and scattering demonic ashes: the baseline way to train Prayer.
 *
 * Both are the same action with different flavour text, so they share one implementation. There is
 * no level requirement on either - every bone in the game is buriable at level 1 - so the only
 * gating is the two-tick cooldown, which is what caps burying at roughly 3,000 bones an hour the
 * way it does live.
 *
 * The cooldown rides `actionDelay` rather than suspending the coroutine. Burying is not an
 * interruptible skilling loop: each click buries exactly one bone and hands control straight back,
 * so a player can walk away mid-stack without the script needing to unwind anything.
 */
class BuryBones @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(PrayerContent.prayer_bones) {
            consume(it.type, it.slot, PrayerSeqs.bury, Flavour.Bury)
        }
        onOpHeld1(PrayerContent.prayer_ashes) {
            consume(it.type, it.slot, PrayerSeqs.scatter, Flavour.Scatter)
        }
    }

    private fun ProtectedAccess.consume(
        type: UnpackedObjType,
        slot: Int,
        seq: SeqType,
        flavour: Flavour,
    ) {
        // Swallowed rather than messaged: the client lets you spam-click a stack, and live gives no
        // feedback for the clicks that land inside the cooldown either.
        if (actionDelay > mapClock) {
            return
        }

        // The xp is only awarded once the obj is actually gone, so a failed delete cannot pay out.
        val deleted = invDel(inv, type, count = 1, slot = slot)
        if (!deleted.success) {
            return
        }

        actionDelay = mapClock + BURY_TICKS
        anim(seq)

        flavour.preamble?.let(::mes)
        mes(flavour.confirmation)

        statAdvance(stats.prayer, type.prayerXp * xpMods.get(player, stats.prayer))
    }

    private enum class Flavour(val preamble: String?, val confirmation: String) {
        Bury("You dig a hole in the ground.", "You bury the bones."),
        Scatter(null, "You scatter the ashes."),
    }

    companion object {
        /** Two ticks per bone, matching the ~1.2s live bury rate. */
        private const val BURY_TICKS = 2

        val UnpackedObjType.prayerXp: Double by objXpParam(params.skill_xp)
    }
}
