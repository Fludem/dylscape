package org.rsmod.content.custom.cluechest.scripts

import jakarta.inject.Inject
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.custom.cluechest.ClueKeyOpener
import org.rsmod.content.custom.cluechest.configs.ClueChestInterfaces
import org.rsmod.content.custom.cluechest.configs.ClueChestLocs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Edgeville clue chest: a clue key goes in, that tier's reward casket comes out.
 *
 * Nothing implements treasure trails, so the drop tables hand out keys where vanilla hands out clue
 * scrolls (`tools/drop-tables/generate.py`, `CLUE_KEYS`) and this chest skips the trail. The
 * opening itself lives in [ClueKeyOpener], which the Clue Compass league relic shares.
 *
 * The close hook covers every reward screen, whichever opened it.
 */
class ClueChestScript @Inject constructor(private val opener: ClueKeyOpener) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ClueChestLocs.loot_chest) { opener.loot(this) }
        onOpLocU(ClueChestLocs.loot_chest) { opener.useKey(this, it.objType, it.invSlot) }
        onIfClose(ClueChestInterfaces.reward) { opener.collectReward(player) }
    }
}
