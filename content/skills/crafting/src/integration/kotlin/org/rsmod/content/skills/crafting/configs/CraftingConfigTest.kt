package org.rsmod.content.skills.crafting.configs

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
 * The range checks that need no cache -- levels, experience, duplicate rows -- run in
 * `CraftingRecipes.init` at class-load. What is left for here is whether the cache actually
 * supplies every obj, component and interface the tables name, and whether the two jewellery panels
 * still look the way `CraftingInterfaceDump` found them.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CraftingConfigTest {
    @Test
    fun GameTestState.`every gem row resolves`() = runBasicGameTest {
        for (recipe in CraftingRecipes.gems) {
            assertResolves(recipe.uncut)
            assertResolves(recipe.cut)
            assertTrue(recipe.uncut.id != recipe.cut.id) { "${recipe.uncut} cuts into itself." }
        }
    }

    @Test
    fun GameTestState.`every leather row resolves`() = runBasicGameTest {
        for (recipe in CraftingRecipes.leather) {
            assertResolves(recipe.hide)
            for (product in recipe.products) {
                assertResolves(product.product)
            }
            assertTrue(recipe.products.size <= SkillMulti.MAX_SLOTS) {
                "${recipe.hide} offers more products than the menu can draw."
            }
        }
    }

    @Test
    fun GameTestState.`every jewellery row resolves`() = runBasicGameTest {
        for (recipe in CraftingRecipes.jewellery) {
            assertResolves(recipe.product)
            assertResolves(recipe.bar)
            assertResolves(recipe.mould)
            recipe.gem?.let { assertResolves(it) }
            assertNotNull(cacheTypes.components[recipe.component.packed]) {
                "Unresolvable component for ${recipe.product}."
            }
        }
    }

    /**
     * The panels are keyed by bar: gold rows must sit on interface 446 and silver rows on 6, or a
     * click would arrive on a panel that never draws that row.
     */
    @Test
    fun GameTestState.`jewellery rows sit on the panel their bar belongs to`() = runBasicGameTest {
        for (recipe in CraftingRecipes.goldJewellery) {
            assertEquals(CraftingInterfaces.crafting_gold.id, recipe.component.interfaceId) {
                "${recipe.product} is a gold row but not on the gold panel."
            }
            assertEquals(CraftingObjs.gold_bar.id, recipe.bar.id) {
                "${recipe.product} is not cast in gold."
            }
        }
        for (recipe in CraftingRecipes.silverJewellery) {
            assertEquals(CraftingInterfaces.silver_crafting.id, recipe.component.interfaceId) {
                "${recipe.product} is a silver row but not on the silver panel."
            }
            assertEquals(CraftingObjs.silver_bar.id, recipe.bar.id) {
                "${recipe.product} is not cast in silver."
            }
        }
    }

    /**
     * Every unstrung amulet the panels cast must have a stringing row, or it is a dead end.
     *
     * The gold panel casts amulets unstrung -- `unstrung_gold_amulet`, not `strung_gold_amulet` --
     * which is the real behaviour, and a ball of wool finishes them.
     */
    @Test
    fun GameTestState.`every unstrung amulet can be strung`() = runBasicGameTest {
        val strung = CraftingRecipes.amuletStringing.map { it.first.id }.toSet()
        val unstrung =
            CraftingRecipes.jewellery
                .map { it.product }
                .filter {
                    cacheTypes.objs.getValue(it.id).internalName?.startsWith("unstrung_") == true
                }
        assertTrue(unstrung.isNotEmpty()) { "No unstrung amulets are cast at all." }
        for (product in unstrung) {
            assertTrue(product.id in strung) { "Nothing strings $product." }
        }
        for ((from, to) in CraftingRecipes.amuletStringing) {
            assertResolves(from)
            assertResolves(to)
        }
    }

    @Test
    fun GameTestState.`every spinning row resolves`() = runBasicGameTest {
        for (recipe in CraftingRecipes.spinning) {
            assertResolves(recipe.material)
            assertResolves(recipe.product)
        }
    }

    @Test
    fun GameTestState.`every tanning row resolves and fits the panel`() = runBasicGameTest {
        assertTrue(CraftingRecipes.tanning.size <= TannerComponents.ROWS) {
            "More tanning rows than the panel has."
        }
        for (recipe in CraftingRecipes.tanning) {
            assertResolves(recipe.hide)
            assertResolves(recipe.leather)
            assertTrue(recipe.name.isNotBlank()) { "${recipe.hide} has no label." }
            assertTrue(recipe.hide.id != recipe.leather.id) { "${recipe.hide} tans into itself." }
        }
    }

    /** All eight rows are addressed, whether or not the table fills them. */
    @Test
    fun GameTestState.`the tanner panel's components all resolve`() = runBasicGameTest {
        val components =
            TannerComponents.models +
                TannerComponents.names +
                TannerComponents.prices +
                TannerComponents.buttons1 +
                TannerComponents.buttons5 +
                TannerComponents.buttonsX +
                TannerComponents.buttonsAll
        assertEquals(TannerComponents.ROWS * 7, components.size) {
            "A tanner row is missing a part."
        }
        for (component in components) {
            assertNotNull(cacheTypes.components[component.packed]) { "Unresolvable: $component" }
        }
    }

    /** The shatter curve has to reach zero, or a maxed player would still crush gems forever. */
    @Test
    fun GameTestState.`the gem shatter chance tapers to nothing`() = runBasicGameTest {
        for (recipe in CraftingRecipes.gems) {
            val atRequirement = CraftingRecipes.crushChance(recipe, recipe.levelReq)
            val atMax = CraftingRecipes.crushChance(recipe, 99)
            if (recipe.crushes) {
                assertTrue(atRequirement > 0) { "${recipe.uncut} never shatters." }
                assertEquals(0, atMax) { "${recipe.uncut} still shatters at 99." }
            } else {
                assertEquals(0, atRequirement) { "${recipe.uncut} should never shatter." }
            }
        }
    }

    private fun GameTestState.assertResolves(obj: ObjType) {
        assertNotNull(cacheTypes.objs[obj.id]) { "Unresolvable obj: $obj" }
    }
}
