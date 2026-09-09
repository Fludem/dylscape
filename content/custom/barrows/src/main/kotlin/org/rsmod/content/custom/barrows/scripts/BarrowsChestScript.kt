package org.rsmod.content.custom.barrows.scripts

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.custom.barrows.BarrowsChest
import org.rsmod.content.custom.barrows.BarrowsProgress.chestOpen
import org.rsmod.content.custom.barrows.BarrowsProgress.resetRun
import org.rsmod.content.custom.barrows.BarrowsRun
import org.rsmod.content.custom.barrows.configs.BarrowsInterfaces
import org.rsmod.content.custom.barrows.configs.BarrowsInvs
import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.content.custom.barrows.configs.barrows_locs
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The reward chest: opening it, looting it, and the brother who objects.
 *
 * The chest is a multiloc over `barrows_chest_open`, so its two faces are two loc types and the
 * server never touches the loc itself - writing the varbit both records the state and redraws it,
 * per player. `barrows_stone_chest_closed` carries `Open` on op1; `barrows_stone_chest_open`
 * carries `Search` on op1 and `Close` on op2.
 *
 * ### Driving the reward panel
 *
 * `barrows_reward` (155) component 3 carries `onLoad=[clientscript,barrows_reward_init]`, and that
 * script names inv **141** (`trail_rewardinv`, shared with clue scrolls), draws the panel from it
 * with `inv_getobj` and registers `if_setoninvtransmit` on it. So the panel is driven exactly like
 * a shop: fill that inv, `startInvTransmit`, open the interface. Nothing is bound to a component,
 * because the cache already wires them.
 *
 * The loot is moved out of that inv and into the player's own inventory on close, so a reward inv
 * is never left holding items a player could not carry.
 */
class BarrowsChestScript
@Inject
constructor(
    private val chest: BarrowsChest,
    private val run: BarrowsRun,
    private val invTypes: InvTypeList,
) : PluginScript() {
    private val Player.rewardInv: Inventory
        get() = invMap.getOrPut(invTypes[BarrowsInvs.reward])

    override fun ScriptContext.startup() {
        onOpLoc1(barrows_locs.stone_chest_closed) { openChest() }
        onOpLoc1(barrows_locs.stone_chest_open) { searchChest() }
        onOpLoc2(barrows_locs.stone_chest_open) { player.chestOpen = false }
        onIfClose(BarrowsInterfaces.reward) { player.collectReward() }
    }

    private fun ProtectedAccess.openChest() {
        player.chestOpen = true
        mes("The lid grinds aside.")
    }

    /**
     * Loots the chest.
     *
     * The ambush is spawned *before* the loot is handed over, so the brother is already on his feet
     * while the reward panel is open - which is the whole point of him.
     */
    private suspend fun ProtectedAccess.searchChest() {
        if (player.rewardInv.isNotEmpty()) {
            mes("You are still holding the last of your reward.")
            return
        }

        val ambush = run.pickAmbush(player)
        if (ambush != null) {
            run.spawn(ambush, BarrowsMap.chestAmbushSpawn)
            mes("${ambush.displayName} bursts from the shadows!")
        }

        val drops = chest.roll(player)
        for (drop in drops) {
            player.invAdd(player.rewardInv, drop.obj, drop.count)
        }

        // Only now is the run spent: a chest that failed to roll anything would otherwise have
        // eaten six brothers for nothing.
        player.resetRun()
        player.chestOpen = false

        player.startInvTransmit(player.rewardInv)
        ifOpenMainModal(BarrowsInterfaces.reward)
    }

    /**
     * Moves the reward into the player's inventory when they close the panel, and keeps whatever
     * will not fit in the reward inv so the next `Search` hands it back rather than destroying it.
     */
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
