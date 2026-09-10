package org.rsmod.content.custom.cityshops

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.shops.cost.StandardGpCostCalculations
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.cityshops.towns.TeletabShopInvs
import org.rsmod.content.custom.cityshops.towns.TeletabShopNpcs
import org.rsmod.content.skills.magic.spellbooks.configs.Teletabs

/**
 * Akutha's counter sells what `Break` works on, at one fixed price.
 *
 * Both halves live in other places -- the tablets and their destinations in `magic-spellbooks`, the
 * price in an obj cost edit and an npc param -- so either could drift from here unnoticed.
 */
class TeletabShopTest {
    @Test
    fun GameTestState.`akutha stocks every tablet that breaks and nothing else`() =
        runBasicGameTest {
            val stocked =
                cacheTypes.invs[TeletabShopInvs.dueldisplay_dummy].stock?.filterNotNull().orEmpty()
            assertEquals(Teletabs.all.map { it.tab.id }, stocked.map { it.obj })
        }

    @Test
    fun GameTestState.`a tablet costs a flat 1,000gp however many are left`() = runBasicGameTest {
        val stocked =
            cacheTypes.invs[TeletabShopInvs.dueldisplay_dummy].stock?.filterNotNull().orEmpty()
        val akutha = cacheTypes.npcs[TeletabShopNpcs.wizard_akutha]
        val sell = akutha.param(ShopParams.shop_sell_percentage) / 10.0
        val change = akutha.param(ShopParams.shop_change_percentage) / 10.0

        for (stock in stocked) {
            val tab = checkNotNull(cacheTypes.objs[stock.obj])
            for (remaining in listOf(stock.count, 1)) {
                val price =
                    StandardGpCostCalculations.calculateShopSellSingleValue(
                        initialStock = stock.count,
                        currentStock = remaining,
                        baseCost = tab.cost,
                        sellPercentage = sell,
                        changePercentage = change,
                    )
                assertEquals(1_000, price) {
                    "'${tab.name}' costs $price with $remaining left on the shelf."
                }
            }
        }
    }
}
