package org.rsmod.content.skills.prayer.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.prayer.scripts.BuryBones.Companion.prayerXp
import org.rsmod.content.skills.prayer.scripts.PrayAtAltar.Companion.offerKeepPercent
import org.rsmod.content.skills.prayer.scripts.PrayAtAltar.Companion.offerXpPercent

/**
 * Guards the config against the failure mode this module is most exposed to: the cache, not this
 * plugin, decides which objs show `Bury` and `Scatter`. A bone we forget to tag still shows the
 * menu entry and simply does nothing when clicked, which no amount of testing the script itself
 * would catch.
 */
class PrayerConfigTest {
    @Test
    fun GameTestState.`every buriable obj in the cache is tagged`() = runBasicGameTest {
        val untagged =
            cacheTypes.objs.values
                .filter { it.iop.getOrNull(0) == "Bury" }
                .filterNot { it.isContentType(PrayerContent.prayer_bones) }
                .mapNotNull { it.internalName }
                .toSet()
        assertEquals(BURY_EXCLUSIONS, untagged) {
            "Objs the cache marks buriable but this module does not handle."
        }
    }

    @Test
    fun GameTestState.`every scatterable obj in the cache is tagged`() = runBasicGameTest {
        val untagged =
            cacheTypes.objs.values
                .filter { it.iop.getOrNull(0) == "Scatter" }
                .filterNot { it.isContentType(PrayerContent.prayer_ashes) }
                .mapNotNull { it.internalName }
                .toSet()
        assertTrue(untagged.isEmpty()) {
            "Objs the cache marks scatterable but this module does not handle: $untagged"
        }
    }

    @Test
    fun GameTestState.`tagged objs are all actually usable`() = runBasicGameTest {
        val tagged =
            cacheTypes.objs.values.filter {
                it.isContentType(PrayerContent.prayer_bones) ||
                    it.isContentType(PrayerContent.prayer_ashes)
            }
        assertTrue(tagged.isNotEmpty()) { "No objs were tagged into the prayer groups at all." }
        for (obj in tagged) {
            // A tag on an obj with no op is dead weight: nothing can ever fire the script.
            assertTrue(obj.iop.getOrNull(0) in setOf("Bury", "Scatter")) {
                "'${obj.internalName}' is tagged for prayer but has no Bury/Scatter op."
            }
            assertTrue(obj.prayerXp > 0.0) { "'${obj.internalName}' would grant no prayer xp." }
        }
    }

    @Test
    fun GameTestState.`altars can all be prayed at`() = runBasicGameTest {
        val altars = cacheTypes.locs.values.filter { it.isContentType(PrayerContent.prayer_altar) }
        assertTrue(altars.isNotEmpty()) { "No locs were tagged into the prayer_altar group." }
        for (altar in altars) {
            // The script binds op1, so an altar whose first option is something else would be
            // tagged but unreachable.
            assertTrue(altar.op.getOrNull(0) in setOf("Pray", "Pray-at")) {
                "'${altar.internalName}' is tagged as an altar but op1 is ${altar.op.getOrNull(0)}."
            }
        }
    }

    @Test
    fun GameTestState.`only chaos altars accept bone offerings`() = runBasicGameTest {
        val altars = cacheTypes.locs.values.filter { it.isContentType(PrayerContent.prayer_altar) }
        val offering = altars.filter { it.offerXpPercent > 100 }.mapNotNull { it.internalName }
        assertEquals(setOf("chaosaltar", "trappedchaosaltar", "thrantaxaltar"), offering.toSet()) {
            "The set of altars granting bonus offering xp changed."
        }
        for (altar in altars.filter { it.offerXpPercent > 100 }) {
            assertEquals(350, altar.offerXpPercent)
            assertEquals(50, altar.offerKeepPercent)
        }
        // Everything else must sit on the param default, which is what makes it refuse offerings.
        for (altar in altars.filter { it.internalName !in offering }) {
            assertEquals(100, altar.offerXpPercent) { "'${altar.internalName}' offers bonus xp." }
            assertEquals(0, altar.offerKeepPercent) { "'${altar.internalName}' saves bones." }
        }
    }

    private companion object {
        /**
         * Bones whose `Bury` is a quest step rather than prayer training, left for those quests to
         * bind: the Zadimus corpse (Shilo Village) and the five goblin high priests reburied during
         * Land of the Goblins. Listed here so the coverage check stays exact instead of merely
         * "mostly covered".
         */
        val BURY_EXCLUSIONS =
            setOf(
                "zqzadimusbones",
                "lotg_bone_highpriest1",
                "lotg_bone_highpriest2",
                "lotg_bone_highpriest3",
                "lotg_bone_highpriest4",
                "lotg_bone_highpriest5",
            )
    }
}
