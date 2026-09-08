package org.rsmod.content.skills.fletching.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.game.type.obj.ObjType

/**
 * Joins the authored tables to the installed cache.
 *
 * The tables themselves are checked by `FletchingRecipes.init`, which runs at class-load and covers
 * the things that need no cache: level ranges, positive experience, no duplicate ingredient pair.
 * What is left for here is whether every obj and seq the tables name actually exists, which only
 * the cache can answer.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FletchingConfigTest {
    @Test
    fun GameTestState.`every cut product resolves`() = runBasicGameTest {
        for (recipe in FletchingRecipes.cutting) {
            assertResolves(recipe.log)
            for (product in recipe.products) {
                assertResolves(product.product)
            }
        }
    }

    @Test
    fun GameTestState.`every stringing row resolves`() = runBasicGameTest {
        for (recipe in FletchingRecipes.stringing) {
            assertResolves(recipe.unstrung)
            assertResolves(recipe.string)
            assertResolves(recipe.strung)
        }
    }

    @Test
    fun GameTestState.`every attach row resolves`() = runBasicGameTest {
        for (recipe in FletchingRecipes.attaching) {
            assertResolves(recipe.base)
            assertResolves(recipe.tip)
            assertResolves(recipe.product)
            assertNotNull(cacheTypes.seqs[recipe.seq.id]) {
                "Unresolvable seq on ${recipe.product}: ${recipe.seq}"
            }
        }
    }

    @Test
    fun GameTestState.`every crossbow row resolves`() = runBasicGameTest {
        for (recipe in FletchingRecipes.crossbows) {
            assertResolves(recipe.limb)
            assertResolves(recipe.stock)
            assertResolves(recipe.unstrung)
        }
    }

    /**
     * Every stock a crossbow needs has to be cuttable, or that crossbow is unreachable. This is the
     * one join the tables cannot check themselves: the stock is a product of one table and an
     * ingredient of another.
     */
    @Test
    fun GameTestState.`every crossbow stock can be cut from logs`() = runBasicGameTest {
        val cuttable =
            FletchingRecipes.cutting.flatMap { it.products }.map { it.product.id }.toSet()
        for (recipe in FletchingRecipes.crossbows) {
            assertTrue(recipe.stock.id in cuttable) {
                "No log cuts into ${recipe.stock}, so ${recipe.unstrung} cannot be made."
            }
        }
    }

    /** Likewise: an unstrung bow nothing strings is a dead end. */
    @Test
    fun GameTestState.`every unstrung bow can be strung`() = runBasicGameTest {
        val strung = FletchingRecipes.stringing.map { it.unstrung.id }.toSet()
        val unstrungProducts =
            FletchingRecipes.cutting
                .flatMap { it.products }
                .map { it.product }
                .filter {
                    cacheTypes.objs.getValue(it.id).internalName?.startsWith("unstrung_") == true
                }
        assertTrue(unstrungProducts.isNotEmpty()) { "No unstrung bows are cut at all." }
        for (product in unstrungProducts) {
            assertTrue(product.id in strung) { "Nothing strings $product." }
        }
    }

    @Test
    fun GameTestState.`no cut menu exceeds the make-menu's slots`() = runBasicGameTest {
        for (recipe in FletchingRecipes.cutting) {
            assertTrue(recipe.products.size <= SkillMulti.MAX_SLOTS) {
                "${recipe.log} offers ${recipe.products.size} products; the menu draws at most " +
                    "${SkillMulti.MAX_SLOTS}."
            }
        }
    }

    @Test
    fun GameTestState.`arrow and dart batches match the table's constants`() = runBasicGameTest {
        for (recipe in FletchingRecipes.darts) {
            assertEquals(FletchingRecipes.DART_BATCH, recipe.batch) {
                "Wrong batch: ${recipe.product}"
            }
        }
        for (recipe in FletchingRecipes.arrows + FletchingRecipes.bolts) {
            assertEquals(FletchingRecipes.ARROW_BATCH, recipe.batch) {
                "Wrong batch: ${recipe.product}"
            }
        }
    }

    private fun GameTestState.assertResolves(obj: ObjType) {
        assertNotNull(cacheTypes.objs[obj.id]) { "Unresolvable obj: $obj" }
    }
}
