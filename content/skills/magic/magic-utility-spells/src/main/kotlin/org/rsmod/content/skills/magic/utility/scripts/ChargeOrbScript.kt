package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLocT
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.utility_locs
import org.rsmod.content.skills.magic.utility.configs.utility_objs
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.content.skills.magic.utility.configs.utility_synths
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Charge Air/Water/Earth/Fire Orb, cast on the matching obelisk.
 *
 * The obelisks carry no ops of their own; the spell's component carries `TgtLoc`, so the cast
 * arrives as `OpLocT` and the player walks up to the obelisk first. The unpowered orb is one of the
 * spell's cache requirements (`spell_runetype_3 = stafforb`), so `MagicRuneManager` takes it along
 * with the runes and this script only hands the charged orb back.
 */
class ChargeOrbScript
@Inject
constructor(private val spellbooks: MagicSpellbooks, private val casting: SpellCasting) :
    PluginScript() {
    private class Orb(
        val loc: LocType,
        val spell: ObjType,
        val orb: ObjType,
        val spotanim: SpotanimType,
        val sound: SynthType,
    )

    override fun ScriptContext.startup() {
        val orbs =
            listOf(
                Orb(
                    utility_locs.obelisk_air,
                    objs.spell_chargeorb_air,
                    utility_objs.air_orb,
                    utility_spotanims.charge_air_orb,
                    utility_synths.charge_air_orb,
                ),
                Orb(
                    utility_locs.obelisk_water,
                    objs.spell_chargeorb_water,
                    utility_objs.water_orb,
                    utility_spotanims.charge_water_orb,
                    utility_synths.charge_water_orb,
                ),
                Orb(
                    utility_locs.obelisk_earth,
                    objs.spell_chargeorb_earth,
                    utility_objs.earth_orb,
                    utility_spotanims.charge_earth_orb,
                    utility_synths.charge_earth_orb,
                ),
                Orb(
                    utility_locs.obelisk_fire,
                    objs.spell_chargeorb_fire,
                    utility_objs.fire_orb,
                    utility_spotanims.charge_fire_orb,
                    utility_synths.charge_fire_orb,
                ),
            )
        for (orb in orbs) {
            val spell = spellbooks.spellFor(orb.spell)
            onOpLocT(orb.loc, spell.component) { charge(spell, orb, it.loc) }
        }
    }

    private suspend fun ProtectedAccess.charge(spell: MagicSpell, orb: Orb, loc: BoundLocInfo) {
        faceLoc(loc)
        if (!casting.attemptUtility(this, spell)) {
            return
        }
        anim(utility_seqs.charge_orb)
        spotanim(orb.spotanim, height = 92)
        soundSynth(orb.sound)
        delay(CHARGE_DELAY)
        invAdd(inv, orb.orb)
    }

    private companion object {
        const val CHARGE_DELAY = 3
    }
}
