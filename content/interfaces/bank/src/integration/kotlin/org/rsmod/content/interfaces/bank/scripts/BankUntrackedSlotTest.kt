package org.rsmod.content.interfaces.bank.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.util.UncheckedType

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(UncheckedType::class)
/*
 * Tab sizes are bookkeeping kept alongside the bank inv rather than derived from it, so anything
 * that puts an obj into the bank without growing a tab leaves a slot no tab claims - `::bankadd`
 * being the one that actually bit. Emptying such a slot used to throw inside the input cycle,
 * which drops the player's connection, and the desync is in the save, so relogging did not help.
 */
class BankUntrackedSlotTest {
    @Test
    fun GameTestState.`withdrawing an obj from a slot no tab claims does not throw`() =
        runGameTest(BankInvScript::class) {
            val stackable = firstObjType { it.isStackable }
            val bank = openBank(player)
            // A bank the tab sizes know nothing about, the way `::bankadd` leaves one.
            bank[0] = InvObj(stackable, 10)
            player.bankTabSizeMain = 0

            player.clearInv()
            player.ifButton(bank_components.main_inventory, comsub = 0, op = MAIN_INV_WITHDRAW_ALL)
            advance()

            assertEquals(InvObj(stackable, 10), player.inv[0])
            assertTrue(bank[0] == null) { "The withdrawn slot should be empty." }
        }

    @Test
    fun GameTestState.`an untracked slot past the tracked end is adopted by the main tab`() =
        runGameTest(BankInvScript::class) {
            val tracked = firstObjType { it.isStackable }
            val untracked = firstObjType { it.isStackable && it.id != tracked.id }
            val bank = openBank(player)
            // Slot 0 is tracked, slot 1 is not: one obj was banked normally and the next was
            // spawned straight into the inv, which is what `::bankadd` does.
            bank[0] = InvObj(tracked, 5)
            bank[1] = InvObj(untracked, 7)
            player.bankTabSizeMain = 1

            player.clearInv()
            player.ifButton(bank_components.main_inventory, comsub = 1, op = MAIN_INV_WITHDRAW_ALL)
            advance()

            assertEquals(InvObj(untracked, 7), player.inv[0])
            assertTrue(bank[1] == null) { "The untracked slot should have been emptied." }
            assertTrue(BankTab.Main.occupiedSpace(player) >= 1) {
                "The main tab should still claim the slot it started with."
            }
        }
}
