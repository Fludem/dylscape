package org.rsmod.game.entity.player

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.map.CoordGrid

/**
 * Pins the ironman ground-obj rule, and in particular the two cases that are easy to get wrong: an
 * obj that has already revealed to the public still belongs to whoever dropped it, and an obj with
 * no owner at all is a server spawn that anyone may take.
 */
class AccountModeRulesTest {
    @Test
    fun `a standard account may take anything`() {
        val player = player(observer = ME, mode = AccountMode.Standard)

        assertTrue(AccountModeRules.canTakeObj(player, ownedBy(THEM)))
        assertTrue(AccountModeRules.canTakeObj(player, ownedBy(ME)))
        assertTrue(AccountModeRules.canTakeObj(player, serverSpawned()))
    }

    @Test
    fun `an ironman may take their own drop`() {
        val player = player(observer = ME, mode = AccountMode.Ironman)

        assertTrue(AccountModeRules.canTakeObj(player, ownedBy(ME)))
    }

    @Test
    fun `an ironman may take a server spawn`() {
        val player = player(observer = ME, mode = AccountMode.Ironman)

        assertTrue(AccountModeRules.canTakeObj(player, serverSpawned()))
    }

    @Test
    fun `an ironman may not take another player's drop`() {
        val player = player(observer = ME, mode = AccountMode.Ironman)

        assertFalse(AccountModeRules.canTakeObj(player, ownedBy(THEM)))
    }

    @Test
    fun `revealing another player's drop does not make it takeable`() {
        val player = player(observer = ME, mode = AccountMode.Ironman)
        val obj = ownedBy(THEM)

        // `reveal` only widens who can see the obj; ownership is what this rule turns on, and it
        // has to outlive the reveal or the restriction would lapse after a minute.
        obj.reveal()

        assertFalse(AccountModeRules.canTakeObj(player, obj))
    }

    @Test
    fun `every iron mode is restricted`() {
        val restricted =
            listOf(AccountMode.Ironman, AccountMode.HardcoreIronman, AccountMode.UltimateIronman)
        for (mode in restricted) {
            val player = player(observer = ME, mode = mode)
            assertFalse(AccountModeRules.canTakeObj(player, ownedBy(THEM)), "$mode may not take")
            assertFalse(AccountModeRules.canTradePlayers(player), "$mode may not trade")
            assertFalse(AccountModeRules.canUseSharedStorage(player), "$mode may not share storage")
        }
    }

    private fun player(observer: Long, mode: AccountMode): Player =
        Player().apply {
            observerUUID = observer
            accountMode = mode
        }

    private fun ownedBy(observer: Long): Obj =
        Obj(
            coords = COORDS,
            entity = ObjEntity(id = 1, count = 1, scope = ObjScope.Private.id),
            creationCycle = 0,
            receiverId = observer,
            ownerId = observer,
        )

    private fun serverSpawned(): Obj =
        Obj(
            coords = COORDS,
            entity = ObjEntity(id = 1, count = 1, scope = ObjScope.Temp.id),
            creationCycle = 0,
            receiverId = Obj.NULL_OBSERVER_ID,
        )

    private companion object {
        private const val ME = 1L
        private const val THEM = 2L
        private val COORDS = CoordGrid(0, 50, 50, 0, 0)
    }
}
