package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLocT
import org.rsmod.content.skills.farming.data.CompostTier
import org.rsmod.content.skills.farming.data.FarmingPatch
import org.rsmod.content.skills.farming.data.FarmingPatches
import org.rsmod.content.skills.farming.scripts.FarmingPatchActions
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Fertile Soil and Cure Plant, cast on a farming patch.
 *
 * Fertile Soil treats an empty, weeded patch as if supercompost had been applied, reading and
 * writing the same `PatchState.compost` the bucket does. There is no plant disease on this server
 * (the farming module documents watering as a no-op for the same reason), so Cure Plant costs
 * nothing and only says so.
 */
class FarmingSpellScript
@Inject
constructor(
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val farming: FarmingPatchActions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val fertileSoil = spellbooks.spellFor(objs.spell_treat_soil)
        val curePlant = spellbooks.spellFor(objs.spell_cure_plant)
        for (patch in FarmingPatches.all) {
            for (loc in patch.locs) {
                onOpLocT(loc, fertileSoil.component) { fertileSoil(fertileSoil, patch, it.loc) }
                onOpLocT(loc, curePlant.component) {
                    faceLoc(it.loc)
                    mes("That plant doesn't need curing.")
                }
            }
        }
    }

    private fun ProtectedAccess.fertileSoil(
        spell: MagicSpell,
        patch: FarmingPatch,
        loc: BoundLocInfo,
    ) {
        faceLoc(loc)
        val state = farming.state(this, patch)
        val cast =
            casting.attemptUtility(this, spell) {
                when {
                    !state.empty -> {
                        mes("The ${patch.kind.label} has already been planted.")
                        false
                    }
                    state.compost.ordinal >= CompostTier.Supercompost.ordinal -> {
                        mes("This patch is already treated with something at least as good.")
                        false
                    }
                    else -> true
                }
            }
        if (!cast) {
            return
        }
        state.compost = CompostTier.Supercompost
        anim(utility_seqs.fertile_soil)
        spotanim(utility_spotanims.fertile_soil, height = 92)
        mes("You treat the ${patch.kind.label} with fertile soil.")
    }
}
