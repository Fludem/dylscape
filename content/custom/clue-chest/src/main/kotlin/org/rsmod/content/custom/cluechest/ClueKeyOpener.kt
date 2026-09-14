package org.rsmod.content.custom.cluechest

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.custom.cluechest.configs.ClueChestInterfaces
import org.rsmod.content.custom.cluechest.configs.ClueChestInvs
import org.rsmod.content.custom.cluechest.configs.ClueChestSeqs
import org.rsmod.content.custom.cluechest.configs.ClueKeys
import org.rsmod.content.custom.droptables.RolledDrop
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * Turns clue keys into reward caskets: the Edgeville chest opens one key at a time, and the Clue
 * Compass league relic opens every key the player carries at once, from anywhere.
 *
 * Three perks change what a key pays:
 * - [Perk.ClueChestAnyTier] (Fairy's Flight) asks which tier to roll, whatever the key.
 * - [Perk.CasketUpgrade] gives each key a 1 in [UPGRADE_CHANCE] chance at the next tier's casket.
 * - [Perk.DoubleCaskets] rolls every casket twice.
 *
 * ### Driving the reward screen
 *
 * `trail_rewardscreen` (73) component 0 carries `onLoad=[trail_rewardscreen_init, ...]`, and that
 * script pushes inv **141** (`trail_rewardinv`), draws from it and registers `if_setoninvtransmit`
 * on it - the same arrangement as `barrows_reward`. So: fill the inv, `startInvTransmit`, open 73.
 * The loot moves into the player's inventory on close, and whatever does not fit stays in the
 * reward inv until a key is next opened.
 *
 * A doubled elite casket, or a stack of keys opened at once, can roll more than inv 141 holds. The
 * surplus goes to the bank instead - or to the floor, for the player alone, if that is full too -
 * so no key is ever spent on loot that has nowhere to go.
 */
@Singleton
class ClueKeyOpener
@Inject
constructor(
    private val casket: ClueCasket,
    private val invTypes: InvTypeList,
    private val objTypes: ObjTypeList,
    private val objRepo: ObjRepository,
    private val perks: Perks,
    // Not `random`: that would be shadowed by `ProtectedAccess.random` in every extension below.
    private val rolls: GameRandom,
) {
    private val Player.rewardInv: Inventory
        get() = invMap.getOrPut(invTypes[ClueChestInvs.reward])

    private val Player.bank: Inventory
        get() = invMap.getOrPut(invTypes[invs.bank])

    private val ClueTier.label: String
        get() = name.lowercase()

    /** The chest's `Loot`: spends the highest-tier key the player holds. */
    suspend fun loot(access: ProtectedAccess): Unit = access.lootChest()

    /** A key used on the chest. */
    suspend fun useKey(access: ProtectedAccess, obj: UnpackedObjType, slot: Int): Unit =
        access.useOnChest(obj, slot)

    /** Opens every clue key in the inventory, highest tier first, into one reward screen. */
    suspend fun openAll(access: ProtectedAccess): Unit = access.openEveryKey()

    /** The reward screen closed: moves what fits into the inventory. */
    fun collectReward(player: Player): Unit = player.collect()

    private suspend fun ProtectedAccess.lootChest() {
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

    private suspend fun ProtectedAccess.useOnChest(obj: UnpackedObjType, slot: Int) {
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
     * Reopens the reward screen over a reward inv that still holds loot, rather than rolling more
     * into it. Barrows borrows the same inv, so the leftovers may be its own.
     *
     * Every entry point runs this first, so a player never picks a tier from the
     * [Perk.ClueChestAnyTier] menu only to be handed their old loot.
     */
    private fun ProtectedAccess.handBackLeftovers(): Boolean {
        if (player.rewardInv.isEmpty()) {
            return false
        }
        mes("You have not collected your last reward.")
        showReward()
        return true
    }

    private suspend fun ProtectedAccess.openWith(slot: Int) {
        val key = inv[slot] ?: return
        val keyTier = ClueKeys.tiers[key.id] ?: return
        val chosen =
            if (perks.has(player, Perk.ClueChestAnyTier)) {
                val picked = chooseTier()
                // The menu gave the player time to move, drop or bank the key.
                if (inv[slot]?.id != key.id) {
                    mes("You need to keep hold of your clue key to open the chest.")
                    return
                }
                picked
            } else {
                keyTier
            }
        val tier = upgrade(chosen)

        // Rolled before the key goes, so a casket table that failed to load costs nothing.
        val drops = rollCaskets(tier)
        if (drops == null) {
            mes("The chest will not open.")
            return
        }
        if (!invDel(inv, objTypes[key], count = 1, slot = slot).success) {
            return
        }

        anim(ClueChestSeqs.open_chest)
        val overflowed = deposit(drops)
        mes("You unlock the chest with your key.")
        if (tier != chosen) {
            mes("Luck is on your side: the chest holds a ${tier.label} casket!")
        }
        if (overflowed) {
            mes(OVERFLOW)
        }
        showReward()
    }

    private suspend fun ProtectedAccess.openEveryKey() {
        if (handBackLeftovers()) {
            return
        }
        if (highestKeySlot() == null) {
            mes("You are not carrying any clue keys.")
            return
        }
        // Asked once, for the whole stack: a menu per key would be a chore, not a reward.
        val chosen = if (perks.has(player, Perk.ClueChestAnyTier)) chooseTier() else null

        var opened = 0
        var upgrades = 0
        var overflowed = false
        // Re-reads the inventory every pass, so keys moved during the menu are simply not found.
        while (true) {
            val slot = highestKeySlot() ?: break
            val key = inv[slot] ?: break
            val keyTier = chosen ?: ClueKeys.tiers[key.id] ?: break
            val tier = upgrade(keyTier)
            val drops = rollCaskets(tier)
            if (drops == null) {
                mes("The ${tier.label} casket will not open.")
                break
            }
            if (!invDel(inv, objTypes[key], count = 1, slot = slot).success) {
                break
            }
            overflowed = deposit(drops) || overflowed
            opened++
            if (tier != keyTier) {
                upgrades++
            }
        }
        if (opened == 0) {
            return
        }

        mes(if (opened == 1) "You open a reward casket." else "You open $opened reward caskets.")
        when {
            upgrades == 0 -> {}
            opened == 1 -> mes("Luck is on your side: it was a tier better than its key!")
            upgrades == 1 -> mes("Luck is on your side: one was a tier better than its key!")
            else -> mes("Luck is on your side: $upgrades were a tier better than their keys!")
        }
        if (overflowed) {
            mes(OVERFLOW)
        }
        showReward()
    }

    /** [Perk.ClueChestAnyTier]'s menu: every tier, lowest first. */
    private suspend fun ProtectedAccess.chooseTier(): ClueTier =
        choice5(
            "Beginner rewards",
            ClueTier.Beginner,
            "Easy rewards",
            ClueTier.Easy,
            "Medium rewards",
            ClueTier.Medium,
            "Hard rewards",
            ClueTier.Hard,
            "Elite rewards",
            ClueTier.Elite,
            "Which rewards would you like?",
        )

    private fun ProtectedAccess.upgrade(tier: ClueTier): ClueTier =
        if (perks.has(player, Perk.CasketUpgrade) && rolls.of(UPGRADE_CHANCE) == 0) {
            tier.next()
        } else {
            tier
        }

    /** One casket of [tier], or two under [Perk.DoubleCaskets]; null if its table did not load. */
    private fun ProtectedAccess.rollCaskets(tier: ClueTier): List<RolledDrop>? {
        val first = casket.roll(tier) ?: return null
        if (!perks.has(player, Perk.DoubleCaskets)) {
            return first
        }
        return first + (casket.roll(tier) ?: emptyList())
    }

    /** Fills the reward inv; returns whether anything had to go to the bank or floor instead. */
    private fun ProtectedAccess.deposit(drops: List<RolledDrop>): Boolean {
        var overflowed = false
        for (drop in drops) {
            if (player.invAdd(player.rewardInv, drop.obj, drop.count).success) {
                continue
            }
            player.invAddOrDrop(objRepo, drop.obj, drop.count, inv = player.bank)
            overflowed = true
        }
        return overflowed
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
    private fun Player.collect() {
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
            mes(
                "You do not have room for the rest of your reward. It waits for you: open a " +
                    "clue key again to collect it."
            )
        }
    }

    companion object {
        /** [Perk.CasketUpgrade]'s odds: 1 in this many keys opens the next tier's casket. */
        const val UPGRADE_CHANCE = 4

        private const val OVERFLOW =
            "Your reward was too big to hold, so the rest has gone to your bank (or the floor, " +
                "if your bank is full)."
    }
}
