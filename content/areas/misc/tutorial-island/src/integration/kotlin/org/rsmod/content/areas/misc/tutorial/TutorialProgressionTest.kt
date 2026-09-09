package org.rsmod.content.areas.misc.tutorial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.areas.misc.tutorial.configs.TutorialConstants
import org.rsmod.content.areas.misc.tutorial.configs.TutorialNpcs
import org.rsmod.content.areas.misc.tutorial.configs.TutorialObjs
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class TutorialProgressionTest {
    private var Player.newAccount by boolVarBit(varbits.new_player_account)

    @Test
    fun GameTestState.`progress persists on the tutorial varp and never moves backwards`() =
        runGameTest(TutorialIsland::class) {
            player.tutorialStage = TutorialStage.NOT_STARTED
            player.advanceTutorial(TutorialStage.SURVIVAL_FISH)
            assertEquals(TutorialStage.SURVIVAL_FISH, player.tutorialStage)

            // A later re-trigger of an earlier step must not rewind saved progress.
            player.advanceTutorial(TutorialStage.GUIDE)
            assertEquals(TutorialStage.SURVIVAL_FISH, player.tutorialStage)

            // The raw varp is what a save would persist.
            assertEquals(TutorialStage.SURVIVAL_FISH.value, player.tutorialValue)
        }

    @Test
    fun GameTestState.`a new account is put on the island at login`() =
        runGameTest(TutorialIsland::class) {
            // The whole point of the feature, and the thing that was silently off: a brand-new
            // account logs in at the realm's spawn and must be moved onto the island instead.
            player.newAccount = true
            player.tutorialStage = TutorialStage.NOT_STARTED
            player.coords = CoordGrid(0, 50, 50, 21, 18)

            eventBus.publish(SessionStateEvent.Login(player))

            assertEquals(TutorialStage.GUIDE, player.tutorialStage) {
                "A new account logged in without the tutorial starting."
            }
            assertEquals(TutorialConstants.START_COORD, player.coords) {
                "A new account was left at the realm spawn instead of the island."
            }
        }

    @Test
    fun GameTestState.`an existing account is left alone at login`() =
        runGameTest(TutorialIsland::class) {
            player.newAccount = false
            player.tutorialStage = TutorialStage.NOT_STARTED
            val where = CoordGrid(0, 50, 50, 30, 30)
            player.coords = where

            eventBus.publish(SessionStateEvent.Login(player))

            assertEquals(TutorialStage.NOT_STARTED, player.tutorialStage) {
                "A returning player was pushed into the tutorial."
            }
            assertEquals(where, player.coords) { "A returning player was teleported." }
        }

    @Test
    fun GameTestState.`a new account mid-tutorial is not sent back to the start on login`() =
        runGameTest(TutorialIsland::class) {
            player.newAccount = true
            player.tutorialStage = TutorialStage.MINING_MINE
            val where = CoordGrid(0, 48, 48, 20, 25)
            player.coords = where

            eventBus.publish(SessionStateEvent.Login(player))

            assertEquals(TutorialStage.MINING_MINE, player.tutorialStage) { "Progress was reset." }
            assertEquals(where, player.coords) {
                "The player was yanked back to the island entrance."
            }
        }

    @Test
    fun GameTestState.`every instructor, obj and the progress varp resolve in the cache`() =
        runBasicGameTest {
            for (npc in TutorialNpcs.all) {
                assertNotNull(cacheTypes.npcs[npc.id]) { "Instructor npc $npc does not resolve." }
            }
            val objs =
                listOf(
                    TutorialObjs.bronze_axe,
                    TutorialObjs.tinderbox,
                    TutorialObjs.small_net,
                    TutorialObjs.newbie_raw_shrimp,
                    TutorialObjs.cooked_shrimp,
                    TutorialObjs.bronze_pickaxe,
                    TutorialObjs.hammer,
                    TutorialObjs.copper_ore,
                    TutorialObjs.tin_ore,
                    TutorialObjs.bronze_bar,
                    TutorialObjs.bronze_dagger,
                    TutorialObjs.bronze_sword,
                    TutorialObjs.wooden_shield,
                    TutorialObjs.shortbow,
                    TutorialObjs.bronze_arrow,
                    TutorialObjs.air_rune,
                    TutorialObjs.mind_rune,
                )
            for (obj in objs) {
                assertNotNull(cacheTypes.objs[obj.id]) { "Tutorial obj $obj does not resolve." }
            }
            // The nine instructors are all distinct npc types.
            assertEquals(9, TutorialNpcs.all.map { it.id }.toSet().size)
            assertNotEquals(0, TutorialConstants.START_COORD.packed)
        }
}
