package org.rsmod.content.skills.farming

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.farming.data.FarmingCrop
import org.rsmod.content.skills.farming.data.FarmingCrops
import org.rsmod.content.skills.farming.data.FarmingPatches
import org.rsmod.content.skills.farming.data.HarvestModel
import org.rsmod.content.skills.farming.data.PatchKind
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType

/**
 * Checks the generated tables against the cache they were generated from.
 *
 * `FarmingCrops` is a table of raw varbit numbers, and a wrong number there is close to invisible
 * at runtime: the patch draws the wrong plant, or offers an op the script does not expect. Those
 * numbers were read out of the multiloc arrays in the loc archive, so the cache is the only thing
 * that can confirm them. Every assertion below re-reads the same arrays and checks that the face a
 * state selects carries the op this module is counting on.
 *
 * A failure here means the table is stale, not that the test is wrong.
 */
class FarmingCropTableTest {
    @Test
    fun GameTestState.`every patch loc is a multiloc driven by its transmit varbit`() =
        runBasicGameTest {
            for (patch in FarmingPatches.all) {
                for (loc in patch.locs) {
                    val type = cacheTypes.locs[loc.id]
                    assertNotNull(type) { "Patch loc ${loc.id} is missing from the cache." }
                    assertTrue(type!!.multiLoc.isNotEmpty()) {
                        "${type.internalName} has no multiloc faces, so nothing would ever draw."
                    }
                    assertEquals(patch.varbit.id, type.multiVarBit) {
                        "${type.internalName} is drawn by varbit ${type.multiVarBit}, not the " +
                            "${patch.varbit.id} this module writes for it."
                    }
                }
            }
        }

    @Test
    fun GameTestState.`weeds rake and bare soil does not`() = runBasicGameTest {
        val locs = cacheTypes.locs
        for (kind in PatchKind.entries) {
            val patch = FarmingPatches.of(kind).first()
            val type = locs[patch.primary.id]!!
            for (weedy in kind.weeds) {
                assertEquals("Rake", op1(locs, type, weedy)) {
                    "$kind state $weedy should be weeds waiting for a rake."
                }
            }
            // Bare soil offers nothing, except a vine trellis, whose empty face keeps the
            // "Dig" op it uses for clearing.
            val cleared = op1(locs, type, kind.cleared)
            assertTrue(cleared == null || cleared == "Dig") {
                "$kind state ${kind.cleared} should be bare soil, but offers '$cleared'."
            }
        }
    }

    /**
     * The growing stages have to be a run of faces that offer *nothing*: the moment one of them
     * carried a harvest op, a player could pick a crop early and the state machine would disagree
     * with what they were shown.
     */
    @Test
    fun GameTestState.`a crop shows nothing pickable until it is fully grown`() = runBasicGameTest {
        val locs = cacheTypes.locs
        for (crop in FarmingCrops.all) {
            val type = patchType(locs, crop)
            for (stage in 0 until crop.growthStages) {
                val state = crop.seedState + stage
                assertTrue(state in type.multiLoc.indices) {
                    "${crop.cropName} stage $stage falls outside its patch's face table."
                }
                val op = op1(locs, type, state)
                assertTrue(op == null || op == "Rake") {
                    "${crop.cropName} is still growing at state $state but offers '$op'."
                }
            }
        }
    }

    @Test
    fun GameTestState.`a finished crop never lands back on a weed face`() = runBasicGameTest {
        val locs = cacheTypes.locs
        for (crop in FarmingCrops.all) {
            val type = patchType(locs, crop)
            val finished = crop.fullyGrownState
            assertTrue(finished in type.multiLoc.indices) {
                "${crop.cropName} finishes on state $finished, which does not exist."
            }
            assertTrue(finished !in crop.kind.weeds.toList() && finished != crop.kind.cleared) {
                "${crop.cropName} finishes on state $finished, which is an empty patch."
            }
        }
    }

    @Test
    fun GameTestState.`harvest faces offer something to take`() = runBasicGameTest {
        val locs = cacheTypes.locs
        for (crop in FarmingCrops.all) {
            if (crop.model == HarvestModel.Checked) continue
            val type = patchType(locs, crop)
            assertTrue(crop.harvestStates.isNotEmpty()) {
                "${crop.cropName} pays out produce but lists no harvest faces."
            }
            for (state in crop.harvestStates) {
                val op = op1(locs, type, state)
                assertNotNull(op) { "${crop.cropName} face $state has no op1 to pick with." }
                assertTrue(op == "Harvest" || op!!.startsWith("Pick")) {
                    "${crop.cropName} face $state offers '$op', which is not a harvest."
                }
            }
        }
    }

    @Test
    fun GameTestState.`check-health faces really are check-health`() = runBasicGameTest {
        val locs = cacheTypes.locs
        for (crop in FarmingCrops.all) {
            if (crop.checkState == -1) continue
            assertEquals("Check-health", op1(locs, patchType(locs, crop), crop.checkState)) {
                "${crop.cropName} state ${crop.checkState} is not the check-health face."
            }
        }
    }

    @Test
    fun GameTestState.`stumps can be cleared`() = runBasicGameTest {
        val locs = cacheTypes.locs
        for (crop in FarmingCrops.all) {
            if (crop.stumpState == -1) continue
            assertEquals("Clear", op1(locs, patchType(locs, crop), crop.stumpState)) {
                "${crop.cropName} stump ${crop.stumpState} cannot be cleared."
            }
        }
    }

    /**
     * The growth rate is the one deliberate departure from the real game, so it is pinned here
     * rather than left to be discovered. Stage counts and cycle lengths are vanilla; only the
     * divisor is ours.
     */
    @Test
    fun GameTestState.`stage counts still reproduce the real growth times`() = runBasicGameTest {
        val examples =
            mapOf(
                "potato" to 40, // four ten-minute stages
                "herb_ranarr_weed" to 80, // four twenty-minute stages
                "redberry_bush" to 100, // five twenty-minute stages
                "magic_tree" to 480, // twelve forty-minute stages
                "apple_tree" to 960, // six one-hundred-and-sixty-minute stages
            )
        for ((name, vanillaMinutes) in examples) {
            val crop = FarmingCrops.all.first { it.cropName == name }
            assertEquals(vanillaMinutes, crop.growthStages * crop.cycleMinutes) {
                "$name takes $vanillaMinutes minutes in the real game."
            }
        }
    }

    @Test
    fun GameTestState.`every patch kind has something that can be planted in it`() =
        runBasicGameTest {
            for (kind in PatchKind.entries) {
                assertTrue(FarmingCrops.of(kind).isNotEmpty()) { "$kind has no crops." }
                assertTrue(FarmingPatches.of(kind).isNotEmpty()) { "$kind has no patches." }
            }
        }

    /**
     * The invariant `FarmingSyncScript` leans on.
     *
     * A transmit varbit belongs to whichever patch using it is nearest, so the patch a player is
     * standing at always wins -- but only while no two patches sharing a slot are close enough for
     * "nearest" to be a coin toss. A patch is at most about ten tiles across, so sixteen is the
     * margin that matters; the tightest pair Jagex actually ships is the Falador allotment and the
     * Draynor belladonna patch, 45 tiles apart.
     *
     * Patches that share a slot and are merely *loaded* together do overlap, and the further one
     * can briefly draw its neighbour's crop. That is not a bug here: the real client does the same
     * thing with the same fourteen varbits.
     */
    @Test
    fun GameTestState.`patches sharing a transmit varbit cannot both claim it`() =
        runBasicGameTest {
            for (sharing in FarmingPatches.byVarBit.values) {
                for ((index, a) in sharing.withIndex()) {
                    for (b in sharing.drop(index + 1)) {
                        val distance =
                            maxOf(abs(a.coords.x - b.coords.x), abs(a.coords.z - b.coords.z))
                        assertTrue(distance > 16) {
                            "Patch locs ${a.primary.id} and ${b.primary.id} share varbit " +
                                "${a.varbit.id} but are only $distance tiles apart."
                        }
                    }
                }
            }
        }

    /**
     * A state that will not fit its varbit is a crash, not a cosmetic slip: `VarPlayerIntMap`
     * refuses an out-of-range write outright, so a redwood needing six bits in a four-bit slot
     * would take the player's session down the moment they walked past the patch.
     */
    @Test
    fun GameTestState.`every state a patch can show fits in its transmit varbit`() =
        runBasicGameTest {
            for (patch in FarmingPatches.all) {
                val varbit = cacheTypes.varbits[patch.varbit.id]!!
                val max = (1 shl (varbit.msb - varbit.lsb + 1)) - 1
                val states = mutableListOf(patch.kind.cleared)
                states += patch.kind.weeds.toList()
                for (crop in FarmingCrops.of(patch.kind)) {
                    states += crop.seedState + crop.growthStages
                    states += crop.harvestStates.toList()
                    if (crop.checkState != -1) states += crop.checkState
                    if (crop.grownState != -1) states += crop.grownState
                    if (crop.stumpState != -1) states += crop.stumpState
                }
                for (state in states) {
                    assertTrue(state <= max) {
                        "Patch loc ${patch.primary.id} can show state $state, but varbit " +
                            "${patch.varbit.id} only holds 0..$max."
                    }
                }
            }
        }

    private fun patchType(locs: LocTypeList, crop: FarmingCrop): UnpackedLocType =
        locs[FarmingPatches.of(crop.kind).first().primary.id]!!

    private fun op1(locs: LocTypeList, type: UnpackedLocType, state: Int): String? {
        if (state !in type.multiLoc.indices) {
            return null
        }
        val childId = type.multiLoc[state].toInt() and 0xFFFF
        val child = locs[childId] ?: return null
        return child.op.getOrNull(0)
    }
}
