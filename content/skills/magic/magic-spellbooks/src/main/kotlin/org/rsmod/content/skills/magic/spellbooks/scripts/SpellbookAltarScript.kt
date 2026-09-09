package org.rsmod.content.skills.magic.spellbooks.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.skills.magic.commons.SpellbookSwitcher
import org.rsmod.content.skills.magic.spellbooks.configs.EdgevilleAltars
import org.rsmod.content.skills.magic.spellbooks.configs.spellbook_locs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Switching spellbooks at the Ancient and astral altars, and putting both altars in Edgeville.
 *
 * ### Placement
 *
 * The altars are added at boot with an infinite duration, the way the Barrows reward chest is,
 * rather than through a `MapLocSpawnBuilder`: that builder replaces a whole mapsquare's loc list
 * and would need `packCache`, while `locRepo.add` needs neither and survives `::reboot`. The
 * originals in the pyramid and on Lunar Isle keep working too, since the binding is by type.
 *
 * ### Ops
 *
 * The Ancient altar is also tagged `prayer_altar` by the prayer module. That is a content-group
 * binding, and `LocInteractions` resolves a type binding first, so the per-type `onOpLoc1` here
 * wins without editing anything in prayer. Prayer points are still restored so nothing is lost
 * compared with a plain altar. The astral altar's op1 is `Craft-rune` (Runecrafting, not built);
 * its `Pray` is op2.
 *
 * No quest gates: Desert Treasure and Lunar Diplomacy do not exist on this server.
 */
class SpellbookAltarScript
@Inject
constructor(private val locRepo: LocRepository, private val switcher: SpellbookSwitcher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup { placeEdgevilleAltars() }
        onOpLoc1(spellbook_locs.ancient_altar) { prayAt(Spellbook.Ancients) }
        onOpLoc2(spellbook_locs.astral_altar) { prayAt(Spellbook.Lunars) }
    }

    private fun placeEdgevilleAltars() {
        for (altar in EdgevilleAltars.all) {
            locRepo.add(altar.coords, altar.loc, Int.MAX_VALUE, altar.angle, altar.shape)
        }
    }

    private fun ProtectedAccess.prayAt(book: Spellbook) {
        statRestore(stats.prayer)
        val current = switcher.current(player)
        val next = if (current == book) Spellbook.Standard else book
        switcher.switch(player, next)
    }
}
