package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.righthand
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.MagicStatus
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.content.skills.magic.utility.configs.utility_objs
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.events.EventBus
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Charge: seven minutes of stronger god spells, for a player wielding a god staff with the matching
 * cape on. The combat module reads [MagicStatus.isCharged].
 */
class ChargeScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
) : PluginScript() {
    private val godSets: List<Pair<ObjType, ObjType>> =
        listOf(
            objs.saradomin_staff to utility_objs.saradomin_cape,
            objs.guthix_staff to utility_objs.guthix_cape,
            objs.zamorak_staff to utility_objs.zamorak_cape,
        )

    override fun ScriptContext.startup() {
        val spell = spellbooks.spellFor(objs.spell_charge)
        onSelfSpell(spell, protectedAccess, eventBus) {
            val cast =
                casting.attemptUtility(this, spell) {
                    when {
                        MagicStatus.isCharged(player) -> {
                            mes("You have already charged yourself.")
                            false
                        }
                        !wearingGodSet() -> {
                            mes(
                                "You need to be wielding a god staff and wearing the matching " +
                                    "cape to cast this spell."
                            )
                            false
                        }
                        else -> true
                    }
                }
            if (cast) {
                anim(utility_seqs.charge)
                player.softTimer(magic_timers.charge, CHARGE_TICKS)
                mes("You feel charged with a magic power.")
            }
        }
    }

    private fun ProtectedAccess.wearingGodSet(): Boolean =
        godSets.any { (staff, cape) -> player.righthand.isType(staff) && player.back.isType(cape) }

    private companion object {
        /** Seven minutes. */
        const val CHARGE_TICKS = 700
    }
}
