package org.rsmod.content.skills.smithing.configs

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents

/**
 * Guards the join between what this module authors and what the cache supplies.
 *
 * The anvil table maps buttons to products; every number attached to those products comes from
 * Jagex's enums. A typo in the mapping would otherwise show up only as one dead button in game.
 */
/**
 * Runs single-threaded on purpose.
 *
 * `integration-test-suite` sets `junit.jupiter.execution.parallel.mode.default=concurrent`, so test
 * *methods* in one class run at the same time while sharing a single game world. Any test that
 * places a loc or mutates a player's inventory then races the others -- which shows up as items
 * surviving a transaction that clearly ran, or as an outright `ConcurrentModificationException`,
 * and only on some runs.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SmithingConfigTest {
    @Test
    fun GameTestState.`every anvil product is one the cache agrees is smithable`() =
        runBasicGameTest {
            val requirements = cacheTypes.enums[SmithingEnums.product_to_requirement.id]
            val barsRequired = cacheTypes.enums[SmithingEnums.product_to_bars_required.id]
            val quantities = cacheTypes.enums[SmithingEnums.product_to_quantity.id]
            assertNotNull(requirements)
            assertNotNull(barsRequired)
            assertNotNull(quantities)

            var checked = 0
            for ((bar, component, product) in SmithingProducts.entries()) {
                val name = cacheTypes.objs[product.id]?.internalName
                assertNotNull(name) { "Product $product is not a real obj." }

                val level = requirements!!.primitiveMap[product.id] as? Int
                assertNotNull(level) {
                    "'$name' is not in smithing_product_to_requirement, so the game does not " +
                        "consider it smithable at all."
                }
                // Bronze daggers and axes are level 0 in the enum, meaning "no requirement".
                assertTrue(level!! in 0..99) { "'$name' has an out-of-range level: $level." }

                val bars = barsRequired!!.primitiveMap[product.id] as? Int
                assertNotNull(bars) { "'$name' has no bar cost." }
                assertTrue(bars!! in 1..5) { "'$name' costs an implausible $bars bars." }

                val made = quantities!!.primitiveMap[product.id] as? Int
                assertNotNull(made) { "'$name' has no output quantity." }
                assertTrue(made!! >= 1) { "'$name' would produce nothing." }

                assertTrue(SmithingProducts.find(bar.id, component)?.id == product.id) {
                    "Lookup by id disagrees with the declared table for '$name'."
                }
                checked++
            }
            // Six tiers times twenty-four shapes. A silently shrunk table would still pass above.
            assertTrue(checked == 144) { "Expected 144 anvil products, found $checked." }
        }

    @Test
    fun GameTestState.`anvils and furnaces are tagged and carry the op we bind`() =
        runBasicGameTest {
            val anvils =
                cacheTypes.locs.values.filter { it.isContentType(SmithingContent.smithing_anvil) }
            val furnaces =
                cacheTypes.locs.values.filter { it.isContentType(SmithingContent.smithing_furnace) }
            assertTrue(anvils.isNotEmpty()) { "No locs were tagged into smithing_anvil." }
            assertTrue(furnaces.isNotEmpty()) { "No locs were tagged into smithing_furnace." }

            // Ops are not uniform in this cache. A tagged loc must either carry the op the
            // scripts bind, or carry no ops at all -- the decorative twins. Anything else means
            // a loc was tagged whose right-click does something entirely different.
            for (loc in anvils) {
                val smithOnOp1 = loc.op.getOrNull(0) == "Smith"
                assertTrue(smithOnOp1 || loc.op.all { it == null }) {
                    "Anvil '${loc.internalName}' has ops ${loc.op.toList()}; op1 is not Smith " +
                        "and it is not an op-less decorative loc."
                }
            }
            for (loc in furnaces) {
                // Ordinary furnaces are `Smelt` on op2. Tutorial Island's is `Use` on op1.
                val smeltOnOp2 = loc.op.getOrNull(1) == "Smelt"
                val useOnOp1 = loc.op.getOrNull(0) == "Use"
                assertTrue(smeltOnOp2 || useOnOp1 || loc.op.all { it == null }) {
                    "Furnace '${loc.internalName}' has ops ${loc.op.toList()}; neither Smelt on " +
                        "op2 nor Use on op1, so nothing this module binds would ever fire."
                }
            }

            // ...and the usable ones must actually exist, or the skill is unreachable in game.
            assertTrue(anvils.any { it.op.getOrNull(0) == "Smith" }) { "No anvil is usable." }
            assertTrue(furnaces.any { it.op.getOrNull(1) == "Smelt" }) { "No furnace is usable." }
            assertTrue(furnaces.any { it.internalName == "newbiefurnace" }) {
                "Tutorial Island's furnace must stay tagged; the tutorial depends on it."
            }
        }

    @Test
    fun GameTestState.`every smelting recipe resolves and is reachable`() = runBasicGameTest {
        for (recipe in SmeltingRecipes.all) {
            val bar = cacheTypes.objs[recipe.bar.id]
            assertNotNull(bar) { "Recipe produces an unresolvable bar." }
            assertTrue(recipe.levelReq in 1..99) {
                "'${bar!!.internalName}' has an out-of-range level: ${recipe.levelReq}."
            }
            assertTrue(recipe.xp > 0.0) { "'${bar!!.internalName}' grants no xp." }
            assertTrue(recipe.ingredients.isNotEmpty()) {
                "'${bar!!.internalName}' costs nothing to make."
            }
            for (ingredient in recipe.ingredients) {
                assertNotNull(cacheTypes.objs[ingredient.obj.id]) {
                    "'${bar!!.internalName}' needs an unresolvable ingredient."
                }
                assertTrue(ingredient.count >= 1)
            }
            // Every recipe must be startable by putting its primary ore on a furnace.
            assertTrue(SmeltingRecipes.byOre[recipe.primaryOre.id]?.contains(recipe) == true) {
                "'${bar!!.internalName}' cannot be reached from any ore."
            }
        }
        // Bronze is the tutorial's bar and must be makeable from either half of the pair.
        assertTrue(SmeltingRecipes.byOre.containsKey(SmithingObjs.copper_ore.id))
        assertTrue(SmeltingRecipes.byOre.containsKey(SmithingObjs.tin_ore.id))
        // Iron ore is the one ambiguous case and must offer exactly two choices.
        assertTrue(SmeltingRecipes.byOre[SmithingObjs.iron_ore.id]?.size == 2)
    }

    @Test
    fun GameTestState.`the smelting menu fits the interface it is drawn on`() = runBasicGameTest {
        // The client draws at most ten buttons, and silently ignores anything past the tenth.
        assertTrue(SmeltingRecipes.all.size <= SkillMulti.MAX_SLOTS) {
            "${SmeltingRecipes.all.size} recipes will not fit in ${SkillMulti.MAX_SLOTS} slots."
        }
        assertTrue(SkillMultiComponents.slots.size == SkillMulti.MAX_SLOTS) {
            "The interface has ${SkillMultiComponents.slots.size} item buttons, not " +
                "${SkillMulti.MAX_SLOTS}."
        }
        for (slot in SkillMultiComponents.slots) {
            assertNotNull(cacheTypes.components[slot.packed]) { "Unresolvable menu slot: $slot." }
        }
        // The click carries a component and a quantity, nothing else, so the nth recipe *must*
        // stay the nth button - the menu is never filtered per player.
        assertTrue(SmeltingRecipes.all.first().bar.id == SmithingObjs.bronze_bar.id) {
            "Slot A is no longer bronze; the click handler indexes this list by slot."
        }
        // The verb the buttons offer comes from the client's own enum, keyed by this number.
        assertTrue(cacheTypes.enums[1809]?.primitiveMap?.get(SkillMultiType.Smelt.id) == "Smelt") {
            "Skillmulti type ${SkillMultiType.Smelt.id} no longer means 'Smelt' in enum 1809."
        }
        // Type 13 must stay outside enum 5178's exclusion set, or the quantity buttons vanish and
        // every click would arrive with a subcomponent of zero.
        assertTrue(cacheTypes.enums[5178]?.primitiveMap?.get(SkillMultiType.Smelt.id) == null) {
            "Skillmulti type ${SkillMultiType.Smelt.id} no longer draws quantity buttons."
        }
    }
}
