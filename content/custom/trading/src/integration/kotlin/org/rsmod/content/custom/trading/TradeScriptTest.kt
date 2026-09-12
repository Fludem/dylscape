package org.rsmod.content.custom.trading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.PlayerEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.trading.configs.trade_components
import org.rsmod.content.custom.trading.scripts.TradeScript
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

/**
 * Two players share one world here, so these run single-threaded: methods are otherwise concurrent
 * and would trade with each other's players.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TradeScriptTest {
    @Test
    fun GameTestState.`one request alone does not open a trade`() =
        runGameTest(TradeScript::class) {
            val alpha = register("Alpha")
            val beta = register("Beta")
            alpha.inv[0] = InvObj(objs.logs)

            requestTrade(alpha, beta)
            advance(ticks = 1)

            // With no trade open the offer click is ignored, so the logs stay put.
            alpha.ifButton(trade_components.side_layer, comsub = 0)
            advance(ticks = 1)

            assertEquals(1, alpha.count(objs.logs)) {
                "Logs left the inventory with no trade open."
            }
        }

    @Test
    fun GameTestState.`requesting each other opens a trade and offers move both ways`() =
        runGameTest(TradeScript::class) {
            val alpha = register("Alpha")
            val beta = register("Beta")
            alpha.inv[0] = InvObj(objs.logs)

            requestTrade(alpha, beta)
            requestTrade(beta, alpha)
            advance(ticks = 1)

            alpha.ifButton(trade_components.side_layer, comsub = 0)
            advance(ticks = 1)
            assertEquals(0, alpha.count(objs.logs)) { "Offered logs were not taken from the inv." }

            // "Remove" on the player's own offer puts them back.
            alpha.ifButton(trade_components.your_offer, comsub = 0)
            advance(ticks = 1)
            assertEquals(1, alpha.count(objs.logs)) { "Removed logs did not return to the inv." }
        }

    @Test
    fun GameTestState.`accepting both screens swaps the offers`() =
        runGameTest(TradeScript::class) {
            val alpha = register("Alpha")
            val beta = register("Beta")
            alpha.inv[0] = InvObj(objs.logs)
            beta.inv[0] = InvObj(objs.coins, 100)

            requestTrade(alpha, beta)
            requestTrade(beta, alpha)
            advance(ticks = 1)

            alpha.ifButton(trade_components.side_layer, comsub = 0)
            // Offer-All, so the whole stack of coins moves rather than a single one.
            beta.ifButton(trade_components.side_layer, comsub = 0, op = IfButtonOp.Op4)
            advance(ticks = 1)

            alpha.ifButton(trade_components.accept)
            beta.ifButton(trade_components.accept)
            advance(ticks = 1)

            alpha.ifButton(trade_components.confirm_accept)
            beta.ifButton(trade_components.confirm_accept)
            advance(ticks = 1)

            assertEquals(100, alpha.count(objs.coins)) { "Alpha did not receive the coins." }
            assertEquals(1, beta.count(objs.logs)) { "Beta did not receive the logs." }
            assertEquals(0, alpha.count(objs.logs)) { "Alpha kept the logs they gave away." }
            assertEquals(0, beta.count(objs.coins)) { "Beta kept the coins they gave away." }
        }

    @Test
    fun GameTestState.`declining returns everything that was offered`() =
        runGameTest(TradeScript::class) {
            val alpha = register("Alpha")
            val beta = register("Beta")
            alpha.inv[0] = InvObj(objs.logs)
            beta.inv[0] = InvObj(objs.coins, 100)

            requestTrade(alpha, beta)
            requestTrade(beta, alpha)
            advance(ticks = 1)

            alpha.ifButton(trade_components.side_layer, comsub = 0)
            // Offer-All, so the whole stack of coins moves rather than a single one.
            beta.ifButton(trade_components.side_layer, comsub = 0, op = IfButtonOp.Op4)
            advance(ticks = 1)

            beta.ifButton(trade_components.decline)
            advance(ticks = 1)

            assertEquals(1, alpha.count(objs.logs)) { "Alpha's offer was not returned." }
            assertEquals(100, beta.count(objs.coins)) { "Beta's offer was not returned." }
        }

    private fun GameTestScope.register(name: String): Player =
        registerPlayer(
            coords = CoordGrid(0, 50, 50, 0, 0),
            player = Player().apply { displayName = name },
        )

    /** The same event `onOpPlayer4` binds: "Trade with" on another player. */
    private fun GameTestScope.requestTrade(from: Player, to: Player) {
        from.withProtectedAccess { eventBus.publish(this, PlayerEvents.Op4(to)) }
    }
}
