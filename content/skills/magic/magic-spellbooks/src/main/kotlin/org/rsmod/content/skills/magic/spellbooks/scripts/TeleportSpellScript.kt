package org.rsmod.content.skills.magic.spellbooks.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.custom.teleports.MAX_WILDERNESS_LEVEL
import org.rsmod.content.custom.teleports.wildernessLevel
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.commons.launchSpell
import org.rsmod.content.skills.magic.spellbooks.configs.TeleportFx
import org.rsmod.content.skills.magic.spellbooks.configs.spellbook_components
import org.rsmod.content.skills.magic.spellbooks.configs.spellbook_synths
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Every teleport spell in the standard, Ancient and Lunar books, driven entirely by the cache.
 *
 * A spell qualifies when its type is `Teleport` and it carries a `spell_telecoord` (stamped by
 * upstream's `SpellObjEditor`). That leaves out the home teleports, the group teleports, the house
 * teleport and Teleport to Target on its own, which is right: they have no fixed destination. The
 * standard home teleport is bound by `content/custom/teleports` instead, whose component is skipped
 * here so the two never collide at boot.
 *
 * The teleport itself is the three-tick cast the official game plays: animation and graphic, the
 * cast sound, then the jump. Runes are paid up front by [SpellCasting.attemptUtility].
 */
class TeleportSpellScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spell in spellbooks.teleports()) {
            if (spell.spellbook == Spellbook.Arceuus) continue
            if (spell.component == spellbook_components.home_teleport_standard) continue
            onIfOverlayButton(spell.component) {
                protectedAccess.launchSpell(player, eventBus) { castTeleport(spell) }
            }
        }
    }

    private suspend fun ProtectedAccess.castTeleport(spell: MagicSpell) {
        val dest = spellbooks.telecoord(spell) ?: return
        val cast =
            casting.attemptUtility(this, spell) {
                if (wildernessLevel(player.coords) > MAX_WILDERNESS_LEVEL) {
                    mes("You can't teleport above level $MAX_WILDERNESS_LEVEL Wilderness.")
                    false
                } else {
                    true
                }
            }
        if (!cast) {
            return
        }
        val fx = TeleportFx.forBook(spell.spellbook)
        anim(fx.seq)
        spotanim(fx.spotanim, height = 92)
        soundSynth(spellbook_synths.teleport)
        delay(TELEPORT_DELAY)
        telejump(mapFindSquareLineOfWalk(dest, 0, 2) ?: dest)
        resetAnim()
    }

    private companion object {
        /** Ticks between the cast starting and the player arriving. */
        const val TELEPORT_DELAY = 3
    }
}
