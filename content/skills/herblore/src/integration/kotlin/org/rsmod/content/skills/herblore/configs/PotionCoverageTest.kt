package org.rsmod.content.skills.herblore.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.other.consumables.configs.Consumables
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * That every potion in the game can actually be drunk, and that this module and `consumables` never
 * fight over the same obj.
 *
 * The denominator is the cache's own, re-derived here as the exact inverse of the predicate
 * `ConsumableCoverageTest.isConsumable` uses to *exclude* potions from the food table: a `Drink` op
 * **and** a `(1)`-`(4)` name suffix. Deriving it rather than listing it is what stops the set
 * rotting the moment either module moves.
 *
 * The op is required, not just the suffix, and that is not belt-and-braces: the cache holds 297
 * objs whose names end in a dose suffix and which carry no `Drink` op at all - the Chambers of
 * Xeric fish are named "Pysk fish (0)" through "Kyren fish (6)" and are eaten. Filtering on the
 * suffix alone would drag them in.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PotionCoverageTest {
    @Test
    fun GameTestState.`every dosed obj is covered or deliberately excluded`() = runBasicGameTest {
        val uncovered =
            cacheTypes.objs.values
                .filter(::isDosed)
                .filterNot { it.id in Potions.byObjId }
                .filterNot { obj -> EXCLUDED.any { obj.internalName.orEmpty().contains(it) } }
                .sortedBy { it.id }
        assertTrue(uncovered.isEmpty()) {
            "Objs the cache says are dosed potions, which neither the table covers nor the " +
                "exclusion list explains:\n" +
                uncovered.joinToString("\n") { "  ${it.id} ${it.internalName} '${it.name}'" }
        }
    }

    /**
     * The assertion that keeps the two modules from silently breaking each other.
     *
     * An obj carries exactly one content group, and a group edit is additive once packed, so if a
     * row appeared in both tables whichever editor ran last would win nondeterministically and the
     * loser's obj would become uneatable or undrinkable in production. This is a live risk rather
     * than a theoretical one: the ale kegs are named `Keg of beer (4)` and match the dose filter
     * exactly, and `ConsumableDrinks` has already claimed all forty of them for `food`.
     */
    @Test
    fun GameTestState.`the food and potion tables never claim the same obj`() = runBasicGameTest {
        val both = Potions.byObjId.keys intersect Consumables.byObjId.keys
        assertTrue(both.isEmpty()) {
            "Objs claimed by both `food` and `potion`: " +
                both.map { cacheTypes.objs[it]?.internalName }
        }
    }

    @Test
    fun GameTestState.`every row carries a Drink op on the slot we bind`() = runBasicGameTest {
        for (row in Potions.rows) {
            val type = cacheTypes.objs.getValue(row.obj.id)
            assertEquals("Drink", type.iop.getOrNull(0)) {
                "${type.internalName} ops=${type.iop.toList()}; the op1 binding would be dead."
            }
        }
    }

    @Test
    fun GameTestState.`every ladder walks four doses down to an empty vial`() = runBasicGameTest {
        val emptyVial = HerbloreObjs.vial_empty.id
        for (head in Potions.rows.filter { it.dose == 4 }) {
            var current = head
            val seen = mutableSetOf(current.obj.id)
            repeat(3) {
                val next = Potions.byObjId[current.next.id]
                assertNotNull(next) { "${current.obj.internalName} steps to a dose with no row." }
                assertEquals(current.dose - 1, next!!.dose) { "${head.family} skipped a dose." }
                assertTrue(seen.add(next.obj.id)) { "${head.family} loops back on itself." }
                current = next
            }
            assertEquals(1, current.dose) { "${head.family} does not end on a single dose." }
            assertEquals(emptyVial, current.next.id) {
                "${head.family} ends at ${current.next.internalName}, not an empty vial."
            }
        }
    }

    @Test
    fun GameTestState.`no obj is listed twice`() = runBasicGameTest {
        assertEquals(Potions.rows.size, Potions.byObjId.size) {
            "Two rows claim the same obj; one of them is unreachable."
        }
    }

    /**
     * A dose ladder replaces the obj in its own slot. On a stackable obj that would convert the
     * whole stack into a single lower dose, which is `ConsumableCoverageTest`'s "no stackable
     * consumable takes bites" applied to potions.
     */
    @Test
    fun GameTestState.`no dose is stackable`() = runBasicGameTest {
        val stackable =
            Potions.rows.mapNotNull { cacheTypes.objs[it.obj.id] }.filter { it.isStackable }
        assertTrue(stackable.isEmpty()) {
            "Stackable objs wired as a dose ladder: ${stackable.map { it.internalName }}"
        }
    }

    @Test
    fun GameTestState.`the editor tagged every row`() = runBasicGameTest {
        val untagged =
            Potions.rows
                .mapNotNull { cacheTypes.objs[it.obj.id] }
                .filterNot { it.isContentType(content.potion) }
        assertTrue(untagged.isEmpty()) {
            "Rows the obj editor did not tag: ${untagged.map { it.internalName }}"
        }
    }

    @Test
    fun GameTestState.`every row resolved against the cache`() = runBasicGameTest {
        val unresolved = Potions.rows.filter { it.obj.id < 0 }
        assertTrue(unresolved.isEmpty()) {
            "Rows whose obj never resolved: ${unresolved.map { it.obj.internalName }}"
        }
    }

    private companion object {
        private val DOSE_SUFFIX = Regex("""\([1-4]\)$""")

        private fun isDosed(type: UnpackedObjType): Boolean =
            type.iop.any { it == "Drink" } && DOSE_SUFFIX.containsMatchIn(type.name)

        /**
         * Dosed objs this module deliberately does not own, by internal-name fragment.
         *
         * Every one was read off `HerbloreDump` rather than guessed, and between them they account
         * for all 302 dosed objs the table does not cover - there is no unclassified remainder,
         * which is what makes the coverage assertion above exact rather than approximate.
         *
         * Three kinds:
         * - **Someone else's already.** `keg_` and `cup_guthix_rest_` are in `Consumables` and wear
         *   the `food` group; claiming them here would break eating them.
         * - **Instanced copies.** `raids_`, `brutal_`, `br_`, `nzone`, `deadman`, `blighted_`,
         *   `lotg_`, `gauntlet_`, `soul_wars_`, `toa_supply_`, `wint_` and `_castlewars` are
         *   minigame- and league-local objs handed out by their own content, never mixed by
         *   Herblore. Tagging them would be permanent and would claim objs this module does not
         *   own.
         * - **Not built yet.** `hunter_mix_` is the Varlamore hunter mixes, and barbarian herblore
         *   generally, which is its own mechanic and its own pass.
         */
        private val EXCLUDED =
            listOf(
                "keg_",
                "cup_guthix_rest_",
                "raids_",
                "brutal_",
                "br_",
                "nzone",
                "deadman",
                "blighted_",
                "lotg_",
                "gauntlet_",
                "soul_wars_",
                "toa_supply_",
                "wint_",
                "_castlewars",
                "hunter_mix_",
            )
    }
}
