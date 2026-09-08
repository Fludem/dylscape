package org.rsmod.content.skills.cooking.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.skillmulti.SkillMultiType

/**
 * Guards the join between what this module authors and what the cache supplies: a food whose
 * product does not resolve, a range whose op is not the one we bind, or a verb id that has moved.
 */
class CookingConfigTest {
    @Test
    fun GameTestState.`every raw food is fully configured`() = runBasicGameTest {
        val foods = cacheTypes.objs.values.filter { it.isContentType(CookingContent.cooking_raw) }
        assertTrue(foods.isNotEmpty()) { "No objs were tagged into cooking_raw." }
        for (food in foods) {
            val name = food.internalName
            assertTrue(food.cookingLevel in 1..99) { "'$name' has an out-of-range level." }
            assertTrue(food.cookingXp > 0.0) { "'$name' awards no experience." }
            assertNotNull(cacheTypes.objs[food.cookedProduct.id]) {
                "'$name' cooks into an unresolvable obj."
            }
            assertNotNull(cacheTypes.objs[food.burntProduct.id]) {
                "'$name' burns into an unresolvable obj."
            }
            assertTrue(food.cookedProduct.id != food.id) { "'$name' cooks into itself." }
            assertTrue(food.burntProduct.id != food.cookedProduct.id) {
                "'$name' burns into its own cooked form."
            }
            // A stop-burn level below the requirement would mean the food never burns at all, and
            // a range that is harsher than a fire is backwards.
            assertTrue(food.stopBurnFire >= food.cookingLevel) {
                "'$name' stops burning on a fire before it can be cooked."
            }
            assertTrue(food.stopBurnRange in food.cookingLevel..food.stopBurnFire) {
                "'$name' has a range stop-burn outside ${food.cookingLevel}..${food.stopBurnFire}."
            }
            assertTrue(food.cookingName.isNotBlank()) { "'$name' has no chat name." }
        }
        assertTrue(foods.any { it.cookingLevel == 1 }) { "Nothing is cookable at level 1." }

        // Tutorial Island depends on these two.
        assertTrue(foods.any { it.internalName == "newbieraw_shrimp" }) {
            "Tutorial Island's shrimps must stay cookable."
        }
        val dough = foods.single { it.internalName == "bread_dough" }
        assertTrue(dough.rangeOnly) { "Bread must not be bakeable over a fire." }
        assertTrue(foods.none { it.rangeOnly && it.internalName?.startsWith("raw_") == true }) {
            "A raw fish or meat is marked range-only."
        }
    }

    @Test
    fun GameTestState.`every tagged range carries Cook on op1 and both fires are tagged`() =
        runBasicGameTest {
            val ranges =
                cacheTypes.locs.values.filter { it.isContentType(CookingContent.cooking_range) }
            assertTrue(ranges.isNotEmpty()) { "No locs were tagged into cooking_range." }
            for (loc in ranges) {
                assertTrue(loc.op.getOrNull(0) == "Cook") {
                    "Range '${loc.internalName}' has ops ${loc.op.toList()}; op1 is not Cook, so " +
                        "the click this module binds would never fire."
                }
            }
            assertTrue(ranges.any { it.internalName == "range" }) { "The plain range is untagged." }
            assertTrue(ranges.any { it.internalName == "newbierange" }) {
                "Tutorial Island's range must stay tagged; the tutorial depends on it."
            }

            val fires =
                cacheTypes.locs.values.filter { it.isContentType(CookingContent.cooking_fire) }
            val lit = fires.singleOrNull { it.internalName == "fire" }
            assertNotNull(lit) { "Firemaking's fire is not cookable." }
            // It has no ops in this cache; if that changes, `openMenu` starts firing on it too,
            // which is fine but worth knowing about.
            assertTrue(lit!!.op.all { it == null }) {
                "The firemaking fire has grown ops: ${lit.op.toList()}"
            }
            assertTrue(
                fires.any { it.internalName == "fire_cook" && it.op.getOrNull(0) == "Cook" }
            ) {
                "fire_cook is no longer a Fire with Cook on op1."
            }
        }

    @Test
    fun GameTestState.`the whole cache's cookable locs are tagged or deliberately excluded`() =
        runBasicGameTest {
            // Tagging is by name, so a range added in a cache update would silently be dead. This
            // walks every loc with a Cook op and demands it be tagged or on the exclusion list,
            // which documents *why* each one is left out.
            val excluded =
                setOf(
                    "gauntlet_range",
                    "gauntlet_range_hm",
                    "poh_stove_7",
                    "poh_stove_7_kettle",
                    "poh_stove_7_pot",
                )
            val cookable = cacheTypes.locs.values.filter { loc -> loc.op.any { it == "Cook" } }
            val untagged =
                cookable.filterNot {
                    it.isContentType(CookingContent.cooking_range) ||
                        it.isContentType(CookingContent.cooking_fire) ||
                        excluded.contains(it.internalName ?: "")
                }
            assertTrue(untagged.isEmpty()) {
                "Locs carry a Cook op but are neither tagged nor excluded: " +
                    untagged.map { it.internalName }
            }
            assertEquals(
                CookingLocs.ranges.size,
                cacheTypes.locs.values.count { it.isContentType(CookingContent.cooking_range) },
            ) {
                "The range list and the cache disagree on how many ranges are tagged."
            }
        }

    @Test
    fun GameTestState.`the make-menu verb is Cook`() = runBasicGameTest {
        val id = SkillMultiType.Cook.id
        assertEquals("Cook", cacheTypes.enums[1809]?.primitiveMap?.get(id)) {
            "Skillmulti type $id no longer means 'Cook' in enum 1809."
        }
        // Outside both exclusion sets, or the quantity buttons vanish and every click arrives with
        // a subcomponent of zero.
        assertNull(cacheTypes.enums[5178]?.primitiveMap?.get(id)) {
            "Skillmulti type $id no longer draws quantity buttons."
        }
        assertNull(cacheTypes.enums[1810]?.primitiveMap?.get(id)) {
            "Skillmulti type $id has lost its 'X' quantity button."
        }
    }
}
