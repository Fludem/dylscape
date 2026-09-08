package org.rsmod.content.custom.tollgate

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.tollgate.configs.TollGateLocs
import org.rsmod.content.custom.tollgate.configs.TollGateNpcs
import org.rsmod.content.custom.tollgate.configs.TollGateVarps
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.map.CoordGrid

class TollGateTestDeps @Inject constructor(val resumePause: ResumePauseButtonHandler)

/**
 * Runs single-threaded on purpose: every test places the gate on the same two tiles of a shared
 * world, and `integration-test-suite` runs methods concurrently by default.
 *
 * Dialogues are clicked through with the real [ResumePauseButtonHandler]. Note the interface naming
 * is from the chat head's point of view: a player line opens `chat_right`, an npc line (with or
 * without a live npc) opens `chat_left`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TollGateScriptTest {
    @Test
    fun GameTestState
        .`pay-toll from Lumbridge takes ten coins and walks the player into Al Kharid`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(LUMBRIDGE_SIDE, coins = 25, princequest = 0)

            player.opLoc4(left)
            advance(1)
            assertDialogueText("You pay the guard.")
            assertEquals(25, player.count(objs.coins))

            continueMesbox(deps)
            advance(1)
            assertEquals(15, player.count(objs.coins))
            assertGateOpen(left, right)

            advance(2)
            assertEquals(AL_KHARID_ARRIVAL, player.coords)

            advance(4)
            assertGateClosed(left, right)
        }

    @Test
    fun GameTestState.`pay-toll from Al Kharid walks the player out to Lumbridge`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(left.coords, coins = 10, princequest = 0)

            player.opLoc4(left)
            advance(1)
            continueMesbox(deps)
            advance(1)
            assertEquals(0, player.count(objs.coins))
            assertGateOpen(left, right)

            advance(2)
            assertEquals(LUMBRIDGE_ARRIVAL, player.coords)
        }

    @Test
    fun GameTestState.`without ten coins the player is turned away and the gate stays shut`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(LUMBRIDGE_SIDE, coins = 9, princequest = 0)

            player.opLoc4(left)
            advance(1)
            assertDialogueText("Oh dear, I don't actually seem to have enough money.")

            continuePlayerChat(deps)
            advance(3)
            assertFalse(player.isBusy)
            assertEquals(9, player.count(objs.coins))
            assertEquals(LUMBRIDGE_SIDE, player.coords)
            assertGateClosed(left, right)
        }

    @Test
    fun GameTestState.`open asks the guard and agreeing to pay walks the player through`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(LUMBRIDGE_SIDE, coins = 10, princequest = 0)

            player.opLoc1(left)
            advance(1)
            assertDialogueText("Can I come through this gate?")

            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("You must pay a toll of 10 gold coins to pass.")

            continueGuardChat(deps)
            advance(1)
            assertDialogueText("No thank you, I'll walk around.|Who does my money go to?|Yes, ok.")

            chooseOption(deps, 3)
            advance(1)
            assertDialogueText("Yes, ok.")

            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("You pay the guard.")

            continueMesbox(deps)
            advance(1)
            assertEquals(0, player.count(objs.coins))
            assertGateOpen(left, right)

            advance(2)
            assertEquals(AL_KHARID_ARRIVAL, player.coords)
        }

    @Test
    fun GameTestState.`declining the toll leaves the player where they are`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(LUMBRIDGE_SIDE, coins = 10, princequest = 0)

            player.opLoc1(left)
            advance(1)
            continuePlayerChat(deps)
            advance(1)
            continueGuardChat(deps)
            advance(1)
            chooseOption(deps, 1)
            advance(1)
            assertDialogueText("No, thank you. I'll walk around.")

            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("Ok suit yourself.")

            continueGuardChat(deps)
            advance(3)
            assertFalse(player.isBusy)
            assertEquals(10, player.count(objs.coins))
            assertEquals(LUMBRIDGE_SIDE, player.coords)
            assertGateClosed(left, right)
        }

    @Test
    fun GameTestState.`asking where the money goes is answered without charging`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(LUMBRIDGE_SIDE, coins = 10, princequest = 0)

            player.opLoc1(left)
            advance(1)
            continuePlayerChat(deps)
            advance(1)
            continueGuardChat(deps)
            advance(1)
            chooseOption(deps, 2)
            advance(1)
            assertDialogueText("Who does my money go to?")

            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("The money goes to the city of Al-Kharid.")

            continueGuardChat(deps)
            advance(3)
            assertFalse(player.isBusy)
            assertEquals(10, player.count(objs.coins))
            assertGateClosed(left, right)
        }

    @Test
    fun GameTestState.`after Prince Ali Rescue the guard lets the player through for free`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            arriveWithCoins(LUMBRIDGE_SIDE, coins = 10, princequest = 110)

            // The multiloc now resolves to the `Open`-only variant, so Pay-toll is not an op.
            player.opLoc4(left)
            advance(2)
            assertGateClosed(left, right)
            assertEquals(LUMBRIDGE_SIDE, player.coords)

            player.opLoc1(left)
            advance(1)
            assertDialogueText("Can I come through this gate?")

            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("You may pass for free, you are a friend of Al-Kharid.")

            continueGuardChat(deps)
            advance(1)
            assertEquals(10, player.count(objs.coins))
            assertGateOpen(left, right)

            advance(2)
            assertEquals(AL_KHARID_ARRIVAL, player.coords)
        }

    @Test
    fun GameTestState
        .`talking to a border guard offers the same toll and walks through the gate`() =
        runInjectedGameTest(TollGateTestDeps::class, null, TollGateScript::class) { deps ->
            val (left, right) = placeGate()
            val guardType = npcTypes[TollGateNpcs.borderguard_lumbridge]
            val guard = spawnNpc(CoordGrid(0, 51, 50, 3, 26), guardType)
            arriveWithCoins(CoordGrid(0, 51, 50, 3, 25), coins = 10, princequest = 0)

            player.opNpc1(guard)
            advance(1)
            assertDialogueText("Can I come through this gate?")

            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("You must pay a toll of 10 gold coins to pass.")

            continueGuardChat(deps)
            advance(1)
            chooseOption(deps, 3)
            advance(1)
            continuePlayerChat(deps)
            advance(1)
            assertDialogueText("You pay the guard.")

            continueMesbox(deps)
            advance(1)
            assertEquals(0, player.count(objs.coins))

            // Beside the guard the player is not in front of the gate, so they walk up to it
            // first and only then does it open; the leaves would otherwise fence them in.
            assertGateClosed(left, right)
            val sawGateOpen = advanceUntilArrived(AL_KHARID_ARRIVAL, left)
            assertEquals(AL_KHARID_ARRIVAL, player.coords)
            assertTrue(sawGateOpen) { "The gate should have opened on the way through." }

            advance(2)
            assertGateClosed(left, right)
        }

    @Test
    fun GameTestState.`the border guard spawn file places two guards each side of the gate`() =
        runBasicGameTest {
            val text =
                checkNotNull(TollGateScript::class.java.getResource("npcs.toml")) {
                        "npcs.toml must sit next to TollGateScript on the classpath."
                    }
                    .readText()
            val spawns =
                Regex("npc = '([a-z0-9_]+)'\\s+coords = '([0-9_]+)'")
                    .findAll(text)
                    .map { it.groupValues[1] to it.groupValues[2] }
                    .toList()
            val expected =
                listOf(
                    "borderguard1" to "0_51_50_3_26",
                    "borderguard1" to "0_51_50_3_29",
                    "borderguard2" to "0_51_50_4_26",
                    "borderguard2" to "0_51_50_4_29",
                )
            Assertions.assertEquals(expected, spawns)
            for ((npc, _) in spawns) {
                Assertions.assertTrue(cacheTypes.npcs.values.any { it.internalName == npc }) {
                    "Unknown npc in npcs.toml: $npc"
                }
            }
        }

    private fun GameTestScope.placeGate(): Pair<BoundLocInfo, BoundLocInfo> {
        // The world and its loc-duration queue are shared across the methods in this class. A
        // previous test that ended with the gate open still has its "close" revert pending, and a
        // leaf placed on top of that pending deletion cannot be deleted again, which leaves the
        // wall collision in place. Let those reverts fire before placing a fresh gate.
        advance(GATE_SETTLE_TICKS)
        val left =
            placeMapLoc(LEFT_LEAF, TollGateLocs.closed_left, LocShape.WallStraight, LocAngle.West)
        val right =
            placeMapLoc(RIGHT_LEAF, TollGateLocs.closed_right, LocShape.WallStraight, LocAngle.West)
        return left to right
    }

    private fun GameTestScope.arriveWithCoins(at: CoordGrid, coins: Int, princequest: Int) {
        player.teleport(at)
        player.clearInv()
        player.inv[0] = InvObj(objs.coins, coins)
        player.withProtectedAccess { vars[TollGateVarps.princequest] = princequest }
    }

    /**
     * Advances a tick at a time until the player stands on [dest], reporting if the gate opened.
     */
    private fun GameTestScope.advanceUntilArrived(dest: CoordGrid, left: BoundLocInfo): Boolean {
        var sawOpen = false
        repeat(MAX_CROSSING_TICKS) {
            if (player.coords == dest) {
                return sawOpen
            }
            advance(1)
            sawOpen = sawOpen || !locExists(left)
        }
        return sawOpen
    }

    private fun GameTestScope.assertGateOpen(left: BoundLocInfo, right: BoundLocInfo) {
        assertDoesNotExist(left)
        assertDoesNotExist(right)
        assertTrue(locExists(LEFT_LEAF.translateX(-1), TollGateLocs.open_left)) {
            "Left leaf should have swung open onto the Lumbridge side."
        }
        assertTrue(locExists(RIGHT_LEAF.translateX(-1), TollGateLocs.open_right)) {
            "Right leaf should have swung open onto the Lumbridge side."
        }
    }

    private fun GameTestScope.assertGateClosed(left: BoundLocInfo, right: BoundLocInfo) {
        assertExists(left)
        assertExists(right)
        assertFalse(locExists(LEFT_LEAF.translateX(-1), TollGateLocs.open_left))
        assertFalse(locExists(RIGHT_LEAF.translateX(-1), TollGateLocs.open_right))
    }

    private fun GameTestScope.assertDialogueText(expected: String) {
        // Chat lines arrive as IfSetText; the option menu arrives as a clientscript argument.
        val texts = client.outgoingMessages.map { it.toString() }
        assertTrue(texts.any { it.contains(expected) }) {
            "Expected dialogue text `$expected` in this tick's messages: $texts"
        }
    }

    private fun GameTestScope.continuePlayerChat(deps: TollGateTestDeps) =
        pressPauseButton(deps, components.chat_right_pbutton)

    private fun GameTestScope.continueGuardChat(deps: TollGateTestDeps) =
        pressPauseButton(deps, components.chat_left_pbutton)

    private fun GameTestScope.continueMesbox(deps: TollGateTestDeps) =
        pressPauseButton(deps, components.messagebox_pbutton)

    private fun GameTestScope.chooseOption(deps: TollGateTestDeps, option: Int) =
        pressPauseButton(deps, components.chatmenu_pbutton, sub = option)

    private fun GameTestScope.pressPauseButton(
        deps: TollGateTestDeps,
        component: ComponentType,
        sub: Int = -1,
    ) {
        val combined = CombinedId(component.interfaceId, component.component)
        client.queue(deps.resumePause, ResumePauseButton(combined, sub))
    }

    private companion object {
        /** Longer than any open duration a test can produce. */
        const val GATE_SETTLE_TICKS = 8

        /** Upper bound on a walk-up-and-cross from anywhere these tests start. */
        const val MAX_CROSSING_TICKS = 10

        /** The two closed leaves as the map places them: straight walls facing west on x=3268. */
        val LEFT_LEAF = CoordGrid(0, 51, 50, 4, 27)
        val RIGHT_LEAF = CoordGrid(0, 51, 50, 4, 28)

        /** The tile directly west of the left leaf, on the Lumbridge side of the wall line. */
        val LUMBRIDGE_SIDE = CoordGrid(0, 51, 50, 3, 27)

        /** One tile clear of the gate on each side, where a crossing ends. */
        val AL_KHARID_ARRIVAL = CoordGrid(0, 51, 50, 5, 27)
        val LUMBRIDGE_ARRIVAL = CoordGrid(0, 51, 50, 2, 27)
    }
}
