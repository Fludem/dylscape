package org.rsmod.content.skills.magic.spellbooks.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.custom.teleports.MAX_WILDERNESS_LEVEL
import org.rsmod.content.custom.teleports.wildernessLevel
import org.rsmod.content.skills.magic.spellbooks.configs.Teletab
import org.rsmod.content.skills.magic.spellbooks.configs.Teletabs
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * `Break` (op1) on every teleport tablet in [Teletabs].
 *
 * A tablet goes where its spell goes and asks nothing the spell would: no magic level, no runes and
 * no particular spellbook, which is why an Ancient or Arceuus tablet works on the standard book.
 * The Wilderness limit is the one rule it shares with a cast teleport.
 *
 * The tablet is gone the moment it is broken. Then the smash plays, the absorb animation and
 * graphic follow a tick later, and the jump lands two ticks after that: three ticks in all, the
 * same as a cast teleport. [ProtectedAccess.delay] holds the player throughout, and a delayed
 * player's held ops are dropped, so a second click cannot break a second tablet mid-flight.
 *
 * The extra ops some tablets carry (`Varrock`/`Grand Exchange`/`Toggle`, `Configure`) choose an
 * alternative destination in OSRS and are not bound; `Break` always goes to the default one.
 */
public class TeletabScript @Inject constructor(private val objTypes: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (teletab in Teletabs.all) {
            onOpHeld1(teletab.tab) { breakTablet(teletab, it.slot) }
        }
    }

    private suspend fun ProtectedAccess.breakTablet(teletab: Teletab, slot: Int) {
        val dest = teletab.destination(objTypes) ?: return
        if (wildernessLevel(player.coords) > MAX_WILDERNESS_LEVEL) {
            mes("You can't teleport above level $MAX_WILDERNESS_LEVEL Wilderness.")
            return
        }
        if (invDel(inv, teletab.tab, count = 1, slot = slot).failure) {
            return
        }
        anim(seqs.poh_smash_magic_tablet)
        delay(1)
        anim(seqs.poh_absorb_tablet_teleport)
        spotanim(spotanims.poh_absorb_tablet_magic)
        delay(2)
        telejump(mapFindSquareLineOfWalk(dest, 0, 2) ?: dest)
        resetAnim()
    }
}
