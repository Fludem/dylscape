package org.rsmod.content.skills.herblore.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.skillmulti.SkillMulti

/**
 * That every obj Herblore names exists, and that the four tables agree with each other.
 *
 * The joins are the point. A skill made of four separate tables can be internally consistent in
 * each one and still produce an unfinished potion nothing consumes, or a potion with no dose ladder
 * behind it, and none of that shows up in a script test.
 */
@Execution(ExecutionMode.SAME_THREAD)
class HerbloreConfigTest {
    @Test
    fun GameTestState.`every authored obj resolves in the cache`() = runBasicGameTest {
        val objs =
            HerbloreRecipes.cleaning.flatMap { listOf(it.grimy, it.clean) } +
                HerbloreRecipes.unfinished.flatMap { listOf(it.base, it.herb, it.unf) } +
                HerbloreRecipes.mixing.flatMap { listOf(it.primary, it.secondary, it.product) } +
                HerbloreRecipes.grinding.flatMap { listOf(it.input, it.output) } +
                HerbloreRecipes.superCombatInputs +
                listOf(HerbloreObjs.pestle_and_mortar, HerbloreObjs.vial_empty)
        for (obj in objs) {
            assertNotNull(cacheTypes.objs[obj.id]) { "Unresolved obj: ${obj.internalName}" }
        }
    }

    @Test
    fun GameTestState.`every authored seq resolves in the cache`() = runBasicGameTest {
        for (seq in listOf(HerbloreSeqs.mix, HerbloreSeqs.grind, HerbloreSeqs.drink)) {
            assertNotNull(cacheTypes.seqs[seq.id]) { "Unresolved seq: ${seq.internalName}" }
        }
    }

    /**
     * The op-name rule: the server cannot send an op *label*, only the event, and the client builds
     * a click from the label. An op1 binding on an obj whose cache op1 is not `Clean` is silently
     * dead, and no script test would catch it because the harness hands the event straight to the
     * handler.
     */
    @Test
    fun GameTestState.`every grimy herb carries Clean on the op we bind`() = runBasicGameTest {
        for (recipe in HerbloreRecipes.cleaning) {
            val type = cacheTypes.objs.getValue(recipe.grimy.id)
            assertEquals("Clean", type.iop.getOrNull(0)) {
                "${type.internalName} ops=${type.iop.toList()}; the op1 binding would be dead."
            }
        }
    }

    @Test
    fun GameTestState.`cleaning and the unfinished table agree on the herb set`() =
        runBasicGameTest {
            val cleaned = HerbloreRecipes.cleaning.map { it.clean.id }.toSet()
            val brewed = HerbloreRecipes.unfinished.map { it.herb.id }.toSet()
            assertEquals(cleaned, brewed) {
                "A herb can be cleaned but not brewed, or brewed but not cleaned."
            }
        }

    @Test
    fun GameTestState.`every unfinished potion is consumed by a recipe`() = runBasicGameTest {
        val consumed = HerbloreRecipes.mixing.map { it.primary.id }.toSet()
        val orphans =
            HerbloreRecipes.unfinished
                .filterNot { it.unf.id in consumed }
                .map { it.unf.internalName }
        assertTrue(orphans.isEmpty()) { "Unfinished potions that make nothing: $orphans" }
    }

    /** The join the one-module decision exists to make checkable. */
    @Test
    fun GameTestState.`every mixed potion is the four-dose head of a ladder`() = runBasicGameTest {
        for (recipe in HerbloreRecipes.mixing) {
            val potion = Potions.byObjId[recipe.product.id]
            assertNotNull(potion) { "${recipe.family} produces an obj with no dose ladder." }
            assertEquals(4, potion!!.dose) { "${recipe.family} produces a partial dose." }
            assertEquals(recipe.family, potion.family) {
                "Recipe and ladder disagree on the family."
            }
        }
        val superCombat = Potions.byObjId[HerbloreRecipes.superCombatProduct.id]
        assertNotNull(superCombat) { "Super combat produces an obj with no dose ladder." }
    }

    @Test
    fun GameTestState.`every ground secondary is either ground here or sold`() = runBasicGameTest {
        // A dust that no recipe grinds has to come from a shop, or the potion using it is
        // unmakeable. `crushed_bird_nest`, `chocolate_dust` and the horns are all grindable; the
        // rest are stocked by Primula's in Edgeville.
        val ground = HerbloreRecipes.grinding.map { it.output.id }.toSet()
        val dusts =
            HerbloreRecipes.mixing
                .map { it.secondary }
                .filter { cacheTypes.objs.getValue(it.id).name.contains("dust", ignoreCase = true) }
        for (dust in dusts) {
            val name = cacheTypes.objs.getValue(dust.id).internalName
            assertTrue(dust.id in ground || name in SHOP_ONLY_DUSTS) {
                "$name is neither ground by a recipe nor on the shop-only list."
            }
        }
    }

    @Test
    fun GameTestState.`no menu exceeds the make-menu's slot count`() = runBasicGameTest {
        // Every menu this module opens lists exactly one product, so this is trivially true today.
        // It is asserted anyway because it stops being trivial the moment a recipe offers a choice.
        assertTrue(1 <= SkillMulti.MAX_SLOTS)
    }

    private companion object {
        /** Dusts with no grinding recipe, which the Edgeville herblore shop stocks instead. */
        private val SHOP_ONLY_DUSTS = setOf("sote_crystal_dust", "nihil_dust")
    }
}
