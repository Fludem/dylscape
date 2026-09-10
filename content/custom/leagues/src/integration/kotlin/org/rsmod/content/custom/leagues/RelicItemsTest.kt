package org.rsmod.content.custom.leagues

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicUnlocked
import org.rsmod.content.custom.leagues.relics.effects.GuardianNpcs
import org.rsmod.content.custom.leagues.relics.effects.GuardianScript
import org.rsmod.content.custom.leagues.relics.effects.PocketKingdomScript
import org.rsmod.events.EventBus
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList

/** The relics whose effect hangs off an item op or a timer, driven the way the client would. */
@Execution(ExecutionMode.SAME_THREAD)
class RelicItemsTest {
    @Test
    fun GameTestState.`the guardian horn summons a guardian and dismiss sends it away`() =
        runInjectedGameTest(ItemDeps::class, null, GuardianScript::class) { deps ->
            player.setVarBit(league_varbits.relic_selection[7], Relic.Guardian.slot)
            player.withProtectedAccess { invAdd(inv, league_objs.guardian_horn) }

            opHeld(deps, 1)
            advance(1)
            assertEquals(1, deps.guardians())

            opHeld(deps, 2)
            advance(1)
            assertEquals(0, deps.guardians())
        }

    @Test
    fun GameTestState.`the horn does nothing without the relic`() =
        runInjectedGameTest(ItemDeps::class, null, GuardianScript::class) { deps ->
            player.withProtectedAccess { invAdd(inv, league_objs.guardian_horn) }
            opHeld(deps, 1)
            advance(1)
            assertEquals(0, deps.guardians())
        }

    @Test
    fun GameTestState.`the pocket kingdom delivers tribute to the bank`() =
        runInjectedGameTest(ItemDeps::class, null, PocketKingdomScript::class) { deps ->
            player.setVarBit(league_varbits.relic_selection[6], Relic.PocketKingdom.slot)
            deps.eventBus.publish(RelicUnlocked(player, Relic.PocketKingdom, null))
            val bank = deps.bank(player)
            assertFalse(objs.coins in bank)

            advance(DELIVERY_TICKS + 1)
            assertTrue(objs.coins in bank)
            assertEquals(COINS, bank.count(objs.coins))
        }

    private fun GameTestScope.opHeld(deps: ItemDeps, op: Int) {
        val slot = player.inv.indexOfFirst { it != null }
        val obj = checkNotNull(player.inv[slot])
        val type = deps.objTypes[obj]
        player.withProtectedAccess {
            val event =
                when (op) {
                    1 -> HeldObjEvents.Op1(slot, obj, type, player.inv)
                    2 -> HeldObjEvents.Op2(slot, obj, type, player.inv)
                    else -> error("op$op")
                }
            deps.eventBus.publish(this, event)
        }
    }

    class ItemDeps
    @Inject
    constructor(
        val eventBus: EventBus,
        val npcs: NpcList,
        val invTypes: InvTypeList,
        val objTypes: ObjTypeList,
    ) {
        fun guardians(): Int = npcs.count { it.type.id == GuardianNpcs.guardian.id }

        fun bank(player: Player): Inventory = player.invMap.getOrPut(invTypes[invs.bank])
    }

    private companion object {
        const val DELIVERY_TICKS = 1000
        const val COINS = 25_000
    }
}
