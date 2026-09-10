package org.rsmod.content.skills.farming.scripts

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.inv.LocUOpScript
import org.rsmod.api.player.events.interact.LocTDefaultEvents
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.farming.configs.FarmingLocs
import org.rsmod.content.skills.farming.configs.FarmingObjs
import org.rsmod.content.skills.farming.data.FarmingCrop
import org.rsmod.content.skills.farming.data.FarmingCrops
import org.rsmod.content.skills.farming.data.FarmingPatches
import org.rsmod.content.skills.farming.data.FarmingRates
import org.rsmod.content.skills.farming.data.FarmingRegistry
import org.rsmod.content.skills.farming.data.PatchKind
import org.rsmod.content.skills.farming.data.PatchState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

/**
 * A farming run, end to end, against a real patch loc.
 *
 * The patch is placed into the empty test world rather than visited on the real map: it is the same
 * multiloc either way, and `placeMapLoc` puts it somewhere the player can be stood next to.
 *
 * Time is moved by backdating [PatchState.plantedAt] rather than by waiting. That is not a way of
 * dodging the clock, it *is* the design: growth is a subtraction of wall-clock timestamps, so a
 * crop planted five minutes ago and one planted before a server restart are the same thing to every
 * code path here.
 *
 * Anything the player does by dragging an item onto a patch goes through [FarmingPatchActions]
 * directly, because the harness can drive a loc op but has no "use this item on that loc". That the
 * ops themselves land in the right slots is asserted separately, off the cache, in
 * `FarmingCropTableTest`.
 *
 * Runs single-threaded on purpose: `integration-test-suite` runs methods concurrently against one
 * shared world, and these move items and place locs.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FarmingScriptTest {
    class Deps
    @Inject
    constructor(
        val registry: FarmingRegistry,
        val actions: FarmingPatchActions,
        val objTypes: ObjTypeList,
        val locT: LocTInteractions,
        val locU: LocUInteractions,
    )

    private val potato = FarmingCrops.all.first { it.cropName == "potato" }
    private val guam = FarmingCrops.all.first { it.cropName == "herb_guam_leaf" }

    @Test
    fun GameTestState.`raking an overgrown patch clears it a stage at a time`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(FarmingObjs.rake)

            val state = deps.registry[player][FarmingPatches.byLoc.getValue(patch.id)]
            state.clearedAt = 0L // long enough ago to be fully overgrown

            // Three stages of weeds, three ticks apiece, and the harness clears its message
            // buffer every tick -- so the "clear" line has to be caught on the tick it lands.
            player.opLoc1(patch)
            advance(ticks = 7)
            assertMessageSent("The allotment is now clear of weeds.")
            assertEquals(3, player.count(FarmingObjs.weeds)) { "One clump of weeds per rake." }
            assertEquals(
                PatchKind.Allotment.cleared,
                state.displayState(PatchKind.Allotment, null, System.currentTimeMillis()),
            )
        }

    @Test
    fun GameTestState.`a rake is required to weed`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()

            deps.registry[player][FarmingPatches.byLoc.getValue(patch.id)].clearedAt = 0L

            player.opLoc1(patch)
            advance(ticks = 1)
            assertMessageSent("You need a rake to clear this patch.")
        }

    @Test
    fun GameTestState.`a seed needs a dibber, the level, and a weeded patch`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            val registered = FarmingPatches.byLoc.getValue(patch.id)
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()
            player.setBaseLevel(stats.farming, 99)
            player.setCurrentLevel(stats.farming, 99)

            val state = deps.registry[player][registered]
            state.clear(System.currentTimeMillis())
            val seed = deps.objTypes[potato.seed]

            player.withProtectedAccess {
                with(deps.actions) { plant(registered, state, seed, System.currentTimeMillis()) }
            }
            assertMessageSent("You need seed dibber to plant that.")
            assertTrue(state.empty) { "Planting without a dibber must not take the seed." }

            player.inv[0] = InvObj(FarmingObjs.dibber)
            player.inv[1] = InvObj(potato.seed)
            player.withProtectedAccess {
                with(deps.actions) { plant(registered, state, seed, System.currentTimeMillis()) }
            }
            advance(ticks = 3)
            assertEquals(seed.id, state.seedId) { "The seed did not go in the ground." }
        }

    @Test
    fun GameTestState.`a crop below the player's level is refused`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            val registered = FarmingPatches.byLoc.getValue(patch.id)
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(FarmingObjs.dibber)
            player.setBaseLevel(stats.farming, 1)
            player.setCurrentLevel(stats.farming, 1)

            val watermelon = FarmingCrops.all.first { it.cropName == "watermelon" }
            val state = deps.registry[player][registered]
            state.clear(System.currentTimeMillis())

            player.withProtectedAccess {
                with(deps.actions) {
                    plant(
                        registered,
                        state,
                        deps.objTypes[watermelon.seed],
                        System.currentTimeMillis(),
                    )
                }
            }
            assertMessageSent("You need a Farming level of ${watermelon.level} to plant that.")
            assertTrue(state.empty)
        }

    @Test
    fun GameTestState.`a grown crop is harvested until its lives run out`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()
            player.setBaseLevel(stats.farming, 50)
            player.setCurrentLevel(stats.farming, 50)

            val state = deps.registry[player][FarmingPatches.byLoc.getValue(patch.id)]
            state.plant(deps.objTypes[potato.seed].id, grownAgo(potato))
            assertTrue(state.fullyGrown(potato, System.currentTimeMillis()))

            repeat(40) {
                if (state.empty) return@repeat
                player.opLoc1(patch)
                advance(ticks = 3)
            }
            val picked = player.count(potato.produce!!)
            assertTrue(picked >= 3) {
                "A patch should yield at least its three lives; got $picked."
            }
            assertTrue(state.empty) { "The patch empties itself once its lives are gone." }
        }

    @Test
    fun GameTestState.`a herb patch grows on the herb clock, not the allotment one`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_herb_patch_1])
            player.teleport(patch.coords.translateX(-1))

            val state = deps.registry[player][FarmingPatches.byLoc.getValue(patch.id)]
            val stage = FarmingRates.stageMillis(guam.cycleMinutes)
            state.plant(deps.objTypes[guam.seed].id, System.currentTimeMillis() - stage * 3)

            assertEquals(3, state.stagesGrown(guam, System.currentTimeMillis()))
            assertTrue(!state.fullyGrown(guam, System.currentTimeMillis()))
            assertEquals(10L * 60_000, guam.growthStages * stage) {
                "A guam should take ten real minutes: eighty vanilla minutes over eight."
            }
        }

    @Test
    fun GameTestState.`clearing a patch needs a spade and empties it`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            val registered = FarmingPatches.byLoc.getValue(patch.id)
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()

            val state = deps.registry[player][registered]
            state.plant(deps.objTypes[potato.seed].id, grownAgo(potato))

            player.withProtectedAccess {
                with(deps.actions) { clear(registered, state, potato, System.currentTimeMillis()) }
            }
            assertMessageSent("You need a spade to clear this patch.")
            assertTrue(!state.empty)

            player.inv[0] = InvObj(FarmingObjs.spade)
            player.withProtectedAccess {
                with(deps.actions) { clear(registered, state, potato, System.currentTimeMillis()) }
            }
            advance(ticks = 3)
            assertTrue(state.empty) { "A dug-up patch should be empty." }
        }

    @Test
    fun GameTestState.`compost buys extra harvests and leaves the bucket behind`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_veg_patch_1])
            val registered = FarmingPatches.byLoc.getValue(patch.id)
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(FarmingObjs.bucket_supercompost)

            val state = deps.registry[player][registered]
            state.clear(System.currentTimeMillis())

            player.withProtectedAccess {
                with(deps.actions) {
                    applyCompost(
                        registered,
                        state,
                        deps.objTypes[FarmingObjs.bucket_supercompost],
                        System.currentTimeMillis(),
                    )
                }
            }
            advance(ticks = 2)
            assertTrue(player.count(FarmingObjs.bucket_empty) > 0) {
                "Emptying a compost bucket should leave the bucket."
            }
            state.plant(deps.objTypes[potato.seed].id, grownAgo(potato))
            assertEquals(5, state.livesLeft(potato)) { "Supercompost is worth two extra lives." }
        }

    /**
     * The bug this pins: a patch is a multiloc, and by the time "use item on loc" reaches
     * `LocUInteractions` the loc has already been resolved down to the child face the player can
     * see. A handler registered against the parent - the loc that is actually on the map, and the
     * only one [FarmingScript] knows about - was never consulted, so every seed, compost bucket and
     * watering can used on any patch answered "Nothing interesting happens."
     *
     * Driven through both halves of the real dispatch: `LocTInteractions` decides which script owns
     * the click and resolves the face, then `LocUInteractions` is handed exactly what
     * `LocUOpScript` hands it. The `OpLocT` packet itself is not replayed because the handler ahead
     * of it wants the inventory interface open with `TgtLoc` armed, which is the client's side of
     * the conversation and not what was broken.
     */
    @Test
    fun GameTestState.`a seed is planted by using it on the patch`() =
        runInjectedGameTest(Deps::class, null, FarmingScript::class, LocUOpScript::class) { deps ->
            val patch = placeMapLoc(TILE, locTypes[FarmingLocs.farming_herb_patch_1])
            player.teleport(patch.coords.translateX(-1))
            player.clearInv()
            player.setBaseLevel(stats.farming, 99)
            player.setCurrentLevel(stats.farming, 99)
            player.inv[0] = InvObj(guam.seed)
            player.inv[1] = InvObj(FarmingObjs.dibber)

            val state = deps.registry[player][FarmingPatches.byLoc.getValue(patch.id)]
            state.clear(System.currentTimeMillis())

            val seed = deps.objTypes[guam.seed]
            val click = deps.locT.opTrigger(player, patch, seed, components.inv_items, comsub = 0)
            assertTrue(click is LocTDefaultEvents.Op) {
                "The engine's inventory-on-loc script should own this click."
            }
            check(click is LocTDefaultEvents.Op)
            assertTrue(click.vis.id != click.loc.id) {
                "A patch must resolve to a multiloc face, or this test proves nothing."
            }

            player.withProtectedAccess {
                deps.locU.interactOp(this, click.vis, click.loc, click.type, seed, inv, invSlot = 0)
            }
            advance(ticks = 3)

            assertEquals(seed.id, state.seedId) {
                "Using a seed on a weeded patch should plant it, not answer with the engine default."
            }
            assertEquals(0, player.count(guam.seed)) { "Planting should take the seed." }
        }

    private fun grownAgo(crop: FarmingCrop): Long =
        System.currentTimeMillis() -
            FarmingRates.stageMillis(crop.cycleMinutes) * crop.growthStages -
            1_000L

    private companion object {
        val TILE: CoordGrid = CoordGrid(0, 50, 50, 40, 40)
    }
}
