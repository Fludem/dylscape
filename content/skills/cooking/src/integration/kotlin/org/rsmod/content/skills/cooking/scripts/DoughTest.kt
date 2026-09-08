package org.rsmod.content.skills.cooking.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.cooking.configs.CookingObjs
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class DoughTest {
    @Test
    fun GameTestState.`tutorial flour and water make bread dough without a menu`() =
        runGameTest(Dough::class) {
            player.clearInv()
            player.inv[0] = InvObj(CookingObjs.newbie_pot_flour)
            player.inv[1] = InvObj(CookingObjs.bucket_water)

            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    HeldUEvents.Type(
                        first = cacheTypes.objs.getValue(CookingObjs.newbie_pot_flour.id),
                        firstSlot = 0,
                        second = cacheTypes.objs.getValue(CookingObjs.bucket_water.id),
                        secondSlot = 1,
                    ),
                )
            }

            assertEquals(1, player.count(CookingObjs.bread_dough)) { "No dough was made." }
            assertEquals(1, player.count(CookingObjs.pot_empty)) { "The pot was not emptied." }
            assertEquals(1, player.count(CookingObjs.bucket_empty)) { "The bucket was kept full." }
            assertEquals(0, player.count(CookingObjs.newbie_pot_flour))
            assertEquals(0, player.count(CookingObjs.bucket_water))
        }

    @Test
    fun GameTestState.`dough needs a free inventory slot`() =
        runGameTest(Dough::class) {
            player.clearInv()
            player.inv[0] = InvObj(CookingObjs.newbie_pot_flour)
            player.inv[1] = InvObj(CookingObjs.bucket_water)
            for (slot in 2 until 28) {
                player.inv[slot] = InvObj(CookingObjs.shrimp)
            }

            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    HeldUEvents.Type(
                        first = cacheTypes.objs.getValue(CookingObjs.newbie_pot_flour.id),
                        firstSlot = 0,
                        second = cacheTypes.objs.getValue(CookingObjs.bucket_water.id),
                        secondSlot = 1,
                    ),
                )
            }

            assertMessageSent("You don't have enough inventory space to make dough.")
            assertEquals(0, player.count(CookingObjs.bread_dough)) { "Dough appeared anyway." }
            assertEquals(1, player.count(CookingObjs.newbie_pot_flour)) { "Flour was consumed." }
        }
}
