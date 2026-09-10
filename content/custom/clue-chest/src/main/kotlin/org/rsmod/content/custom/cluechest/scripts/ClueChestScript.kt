package org.rsmod.content.custom.cluechest.scripts

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.custom.cluechest.ClueCasket
import org.rsmod.content.custom.cluechest.ClueTier
import org.rsmod.content.custom.cluechest.configs.ClueChestInterfaces
import org.rsmod.content.custom.cluechest.configs.ClueChestInvs
import org.rsmod.content.custom.cluechest.configs.ClueChestLocs
import org.rsmod.content.custom.cluechest.configs.ClueChestSeqs
import org.rsmod.content.custom.cluechest.configs.ClueKeys
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Edgeville clue chest: a clue key goes in, that tier's reward casket comes out.
 *
 * Nothing implements treasure trails, so the drop tables hand out keys where vanilla hands out clue
 * scrolls (`tools/drop-tables/generate.py`, `CLUE_KEYS`) and this chest skips the trail.
 *
 * ### Driving the reward screen
 *
 * `trail_rewardscreen` (73) component 0 carries `onLoad=[trail_rewardscreen_init, ...]`, and that
 * script pushes inv **141** (`trail_rewardinv`), draws from it and registers `if_setoninvtransmit`
 * on it - the same arrangement as `barrows_reward`. So: fill the inv, `startInvTransmit`, open 73.
 * The loot moves into the player's inventory on close, and whatever does not fit stays in the
 * reward inv until the chest is next opened.
 */
class ClueChestScript
@Inject
constructor(
    private val casket: ClueCasket,
    private val invTypes: InvTypeList,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    private val Player.rewardInv: Inventory
        get() = invMap.getOrPut(invTypes[ClueChestInvs.reward])

    override fun ScriptContext.startup() {
        onOpLoc1(ClueChestLocs.loot_chest) { lootChest() }
        onOpLocU(ClueChestLocs.loot_chest) { useOnChest(it.objType, it.invSlot) }
        onIfClose(ClueChestInterfaces.reward) { player.collectReward() }
    }

    /** `Loot`: spends the highest-tier key the player holds. */
    private fun ProtectedAccess.lootChest() {
        if (handBackLeftovers()) {
            return
        }
        val slot = highestKeySlot()
        if (slot == null) {
            mes("You need a clue key to open this chest.")
            return
        }
        openWith(slot)
    }

    private fun ProtectedAccess.useOnChest(obj: UnpackedObjType, slot: Int) {
        if (obj.id !in ClueKeys.tiers) {
            mes("Nothing interesting happens.")
            return
        }
        if (handBackLeftovers()) {
            return
        }
        openWith(slot)
    }

    /**
     * Reopens the reward screen over a reward inv that still holds loot, rather than rolling a
     * second casket into it. Barrows borrows the same inv, so the leftovers may be its own.
     */
    private fun ProtectedAccess.handBackLeftovers(): Boolean {
        if (player.rewardInv.isEmpty()) {
            return false
        }
        mes("You have not collected your last reward.")
        showReward()
        return true
    }

    private fun ProtectedAccess.openWith(slot: Int) {
        val key = inv[slot] ?: return
        val tier = ClueKeys.tiers[key.id] ?: return

        // Rolled before the key goes, so a casket table that failed to load costs nothing.
        val drops = casket.roll(tier)
        if (drops == null) {
            mes("The chest will not open.")
            return
        }
        if (!invDel(inv, objTypes[key], count = 1, slot = slot).success) {
            return
        }

        anim(ClueChestSeqs.open_chest)
        for (drop in drops) {
            player.invAdd(player.rewardInv, drop.obj, drop.count)
        }
        mes("You unlock the chest with your key.")
        showReward()
    }

    private fun ProtectedAccess.showReward() {
        player.startInvTransmit(player.rewardInv)
        ifOpenMainModal(ClueChestInterfaces.reward)
    }

    private fun ProtectedAccess.highestKeySlot(): Int? {
        var best: Int? = null
        var bestTier: ClueTier? = null
        for (slot in inv.indices) {
            val tier = inv[slot]?.let { ClueKeys.tiers[it.id] } ?: continue
            if (bestTier == null || tier > bestTier) {
                best = slot
                bestTier = tier
            }
        }
        return best
    }

    /** As `BarrowsChestScript.collectReward`: what does not fit waits for the next open. */
    private fun Player.collectReward() {
        stopInvTransmit(rewardInv)
        var left = false
        for (slot in rewardInv.indices) {
            val obj = rewardInv[slot] ?: continue
            if (invAdd(inv, obj.id, obj.count).success) {
                rewardInv[slot] = null
            } else {
                left = true
            }
        }
        if (left) {
            mes("You do not have room for the rest of your reward. It waits in the chest.")
        }
    }
}
