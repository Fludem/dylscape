package org.rsmod.content.custom.cityshops

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.back
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.shops.cost.StandardGpCostCalculations
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.cityshops.configs.ShopInvs
import org.rsmod.content.custom.cityshops.configs.ShopNpcs
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj

/**
 * The two promises Jack's counter makes: 100,000gp a cape, and no cape worn under 99.
 *
 * Neither is enforced by a script of ours. The price is `obj.cost * sell%` with the per-purchase
 * drift switched off, and the level gate is the cache's own `statreq1_skill`/`statreq1_level`
 * params read by [HeldEquipOp] — so both are the kind of thing that breaks silently, from an edit
 * made somewhere else entirely, with nothing to show for it until a player notices.
 *
 * The equip cases use the Herblore cape rather than sweeping all 23: the param sweep already proves
 * every cape carries the same gate, and what these add is that the gate is actually *read* on the
 * way into the cape slot.
 */
/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class SkillcapeShopTest {
    @Test
    fun GameTestState.`every cape Jack sells is gated on 99 in its own skill`() = runBasicGameTest {
        val stocked = cacheTypes.invs[ShopInvs.omnishop_inv_temp].stock?.filterNotNull().orEmpty()
        assertEquals(23, stocked.size, "Jack should stock one cape per skill.")

        for (stock in stocked) {
            val cape = cacheTypes.objs[stock.obj]
            assertNotNull(cape, "Jack stocks an obj the cache does not know: ${stock.obj}")
            checkNotNull(cape)

            assertNotNull(
                cape.paramOrNull(params.statreq1_skill),
                "'${cape.name}' can be worn by anyone: it has no statreq1_skill.",
            )
            assertEquals(
                99,
                cape.paramOrNull(params.statreq1_level),
                "'${cape.name}' is not gated at 99.",
            )
            assertNull(
                cape.paramOrNull(params.statreq2_skill),
                "'${cape.name}' carries a second requirement — it is a trimmed cape.",
            )
        }
    }

    /**
     * Every other shop in this module drifts its price as stock is bought out. Jack's must not: a
     * cape is a fixed 100,000gp whether it is the first off the shelf or the last.
     */
    @Test
    fun GameTestState.`a cape costs a flat 100,000gp however many are left`() = runBasicGameTest {
        val stocked = cacheTypes.invs[ShopInvs.omnishop_inv_temp].stock?.filterNotNull().orEmpty()
        val jack = cacheTypes.npcs[ShopNpcs.jack]
        val sell = jack.param(ShopParams.shop_sell_percentage) / 10.0
        val change = jack.param(ShopParams.shop_change_percentage) / 10.0

        for (stock in stocked) {
            val cape = checkNotNull(cacheTypes.objs[stock.obj])
            for (remaining in listOf(stock.count, 1)) {
                val price =
                    StandardGpCostCalculations.calculateShopSellSingleValue(
                        initialStock = stock.count,
                        currentStock = remaining,
                        baseCost = cape.cost,
                        sellPercentage = sell,
                        changePercentage = change,
                    )
                assertEquals(100_000, price) {
                    "'${cape.name}' costs $price with $remaining left on the shelf."
                }
            }
        }
    }

    /** A boost does not put the cape on: [HeldEquipOp] reads the base level. */
    @Test
    fun GameTestState.`a cape will not go on at a boosted 98`() = runBasicGameTest {
        val cape = cacheTypes.objs.values.first { it.internalName == "skillcape_herblore" }
        val herblore = cacheTypes.stats.values.first { it.internalName == "herblore" }
        withPlayerInit {
            inv[0] = InvObj(cape)
            statMap.setBaseLevel(herblore, 98)
            statMap.setCurrentLevel(herblore, 99)

            val result = HeldEquipOp(cacheTypes.objs, EventBus()).equip(this, 0, inv)

            assertInstanceOf<HeldEquipResult.Fail.StatRequirements>(result)
            assertEquals(InvObj(cape), inv[0], "The cape left the inventory anyway.")
            assertNull(back, "The cape went into the cape slot.")
        }
    }

    @Test
    fun GameTestState.`a cape goes on at 99`() = runBasicGameTest {
        val cape = cacheTypes.objs.values.first { it.internalName == "skillcape_herblore" }
        val herblore = cacheTypes.stats.values.first { it.internalName == "herblore" }
        withPlayerInit {
            inv[0] = InvObj(cape)
            statMap.setBaseLevel(herblore, 99)
            statMap.setCurrentLevel(herblore, 99)

            val result = HeldEquipOp(cacheTypes.objs, EventBus()).equip(this, 0, inv)

            assertInstanceOf<HeldEquipResult.Success>(result)
            assertEquals(InvObj(cape), back, "The cape is not in the cape slot.")
            assertNull(inv[0], "The cape is still in the inventory.")
        }
    }
}
