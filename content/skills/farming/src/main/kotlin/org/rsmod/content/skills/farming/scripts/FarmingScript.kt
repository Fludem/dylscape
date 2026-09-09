package org.rsmod.content.skills.farming.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.skills.farming.configs.FarmingObjs
import org.rsmod.content.skills.farming.data.FarmingPatch
import org.rsmod.content.skills.farming.data.FarmingPatches
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Which op on which patch runs what. The doing itself lives in [FarmingPatchActions].
 *
 * **Why one handler for all five ops.** A patch is a multiloc with up to 256 faces, and the op on
 * any given face is whatever that face's child loc says: `Rake` on weeds, `Pick` on a grown herb,
 * `Check-health` on a new tree, `Clear` on a stump. Rather than hard-code which op slot means what
 * -- which is not uniform, spirit trees put `Inspect` on op3 while everything else uses op2 -- this
 * registers all five ops on every patch and then reads the op *text* back out of the cache for the
 * face the player is actually looking at. Adding a patch means adding a row to [FarmingPatches];
 * nothing here needs to know about it.
 *
 * The engine does the multiloc resolution for both the menu (`LocInteractions.hasOp` refuses an op
 * the visible face does not carry) and dispatch, so a handler registered against the *parent* loc
 * fires whatever the patch currently looks like.
 *
 * Disease is deliberately not implemented on this server: a planted crop always reaches maturity,
 * so there is no `Cure`, no plant cure, no dead crops and no paying a farmer to watch a patch. The
 * `Prune` and `Cure` faces are therefore unreachable, and the handler says so rather than
 * pretending otherwise.
 */
class FarmingScript
@Inject
constructor(private val actions: FarmingPatchActions, private val objTypes: ObjTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (patch in FarmingPatches.all) {
            for (loc in patch.locs) {
                onOpLoc1(loc) { patchOp(patch, opIndex = 0) }
                onOpLoc2(loc) { patchOp(patch, opIndex = 1) }
                onOpLoc3(loc) { patchOp(patch, opIndex = 2) }
                onOpLoc4(loc) { patchOp(patch, opIndex = 3) }
                onOpLoc5(loc) { patchOp(patch, opIndex = 4) }
                onOpLocU(loc) { useOnPatch(patch, it.objType) }
            }
        }
    }

    private suspend fun ProtectedAccess.patchOp(patch: FarmingPatch, opIndex: Int) {
        val state = actions.state(this, patch)
        val crop = actions.cropOf(state)
        val now = System.currentTimeMillis()
        val display = state.displayState(patch.kind, crop, now)

        with(actions) {
            when (visibleOp(patch, display, opIndex)) {
                "Rake" -> rake(patch, state, now)
                "Dig" -> mes("There is nothing to dig up.")
                "Inspect" -> inspect(patch, state, crop, now)
                "Guide" -> mes("Check the Farming tab of the skill guide for what grows here.")
                "Check-health" -> checkHealth(patch, state, crop, now)
                "Clear" -> clear(patch, state, crop, now)
                "Chop down",
                "Chop-down",
                "Chop" -> chopDown(patch, state, crop, now)
                "Prune",
                "Cure" -> mes("Crops don't get diseased here, so there's nothing to treat.")
                "Travel",
                "Talk-to" -> mes("This spirit tree hasn't found its voice yet.")
                null -> return
                else -> harvest(patch, state, crop, now)
            }
        }
    }

    private suspend fun ProtectedAccess.useOnPatch(patch: FarmingPatch, used: UnpackedObjType) {
        val state = actions.state(this, patch)
        val crop = actions.cropOf(state)
        val now = System.currentTimeMillis()

        with(actions) {
            when {
                used.id == objTypes[FarmingObjs.rake].id -> rake(patch, state, now)
                used.id == objTypes[FarmingObjs.spade].id -> clear(patch, state, crop, now)
                actions.isCompost(used) -> applyCompost(patch, state, used, now)
                actions.isWateringCan(used) -> water(patch, state, crop, now)
                used.id == objTypes[FarmingObjs.plantpot_empty].id -> fillPlantPots()
                else -> plant(patch, state, used, now)
            }
        }
    }
}
