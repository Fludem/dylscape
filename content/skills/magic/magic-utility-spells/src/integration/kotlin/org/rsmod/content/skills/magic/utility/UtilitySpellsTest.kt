package org.rsmod.content.skills.magic.utility

import jakarta.inject.Inject
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.inv.InvOpenScript
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.commons.MagicBuffRegistry
import org.rsmod.content.skills.magic.commons.MagicRuneTestSupport
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.utility.configs.EnchantTable
import org.rsmod.content.skills.magic.utility.configs.utility_objs
import org.rsmod.content.skills.magic.utility.scripts.AlchemyScript
import org.rsmod.content.skills.magic.utility.scripts.BakePieScript
import org.rsmod.content.skills.magic.utility.scripts.BonesToFoodScript
import org.rsmod.content.skills.magic.utility.scripts.EnchantScript
import org.rsmod.content.skills.magic.utility.scripts.LunarSkillingScript
import org.rsmod.content.skills.magic.utility.scripts.SuperheatScript
import org.rsmod.content.skills.magic.utility.scripts.VengeanceScript
import org.rsmod.content.skills.smithing.configs.SmithingObjs
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript

class UtilityDeps
@Inject
constructor(val spellbooks: MagicSpellbooks, val buffs: MagicBuffRegistry)

/**
 * Item-targeted casts go through `ifButtonT`, which needs both the spellbook and the inventory
 * overlays open: the inventory's `Target` event is granted by `InvOpenScript` when it opens, and
 * the handler drops the packet otherwise. Runes sit from slot 20 up so the items under test keep
 * the low slots.
 */
@Execution(ExecutionMode.SAME_THREAD)
class UtilitySpellsTest {
    @Test
    fun GameTestState.`high alchemy turns an item into coins and consumes the runes`() =
        runMagicTest(AlchemyScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_high_alchemy)
            prepare(Spellbook.Standard, spell)
            player.inv[0] = InvObj(objs.rune_platebody)
            val value = objTypes[objs.rune_platebody].cost * 60 / 100
            val xp = player.statMap.getXP(stats.magic)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = objs.rune_platebody,
            )
            advance(1)

            assertEquals(0, player.count(objs.rune_platebody))
            assertEquals(value, player.count(objs.coins))
            assertEquals(0, player.count(objs.nature_rune))
            assertTrue(player.statMap.getXP(stats.magic) > xp)
        }

    @Test
    fun GameTestState.`alchemy refuses coins and keeps the runes`() =
        runMagicTest(AlchemyScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_low_alchemy)
            prepare(Spellbook.Standard, spell)
            player.inv[0] = InvObj(objs.coins, 100)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = objs.coins,
            )
            advance(1)

            assertMessageSent("Coins are already made of gold.")
            assertEquals(100, player.count(objs.coins))
            assertEquals(1, player.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`a second alchemy inside the cast delay is ignored`() =
        runMagicTest(AlchemyScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_high_alchemy)
            prepare(Spellbook.Standard, spell, runeSets = 2)
            player.inv[0] = InvObj(objs.rune_platebody)
            player.inv[1] = InvObj(objs.rune_platebody)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = objs.rune_platebody,
            )
            advance(1)
            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 1,
                targetObj = objs.rune_platebody,
            )
            advance(1)
            assertEquals(1, player.count(objs.rune_platebody))

            advance(5)
            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 1,
                targetObj = objs.rune_platebody,
            )
            advance(1)
            assertEquals(0, player.count(objs.rune_platebody))
        }

    @Test
    fun GameTestState.`lvl-1 enchant turns a sapphire ring into a ring of recoil`() =
        runMagicTest(EnchantScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_enchant_lvl_1)
            prepare(Spellbook.Standard, spell)
            player.inv[0] = InvObj(utility_objs.sapphire_ring)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = utility_objs.sapphire_ring,
            )
            advance(1)

            assertEquals(1, player.count(utility_objs.ring_of_recoil))
            assertEquals(0, player.count(utility_objs.sapphire_ring))
            assertEquals(0, player.count(utility_objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`the wrong enchant level is refused with the spell's own description`() =
        runMagicTest(EnchantScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_enchant_lvl_2)
            prepare(Spellbook.Standard, spell)
            player.inv[0] = InvObj(utility_objs.sapphire_ring)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = utility_objs.sapphire_ring,
            )
            advance(1)

            assertMessageSent("This spell can only be cast on emerald and jade jewellery.")
            assertEquals(1, player.count(utility_objs.sapphire_ring))
            assertEquals(1, player.count(utility_objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`every enchant row resolves and every level has four kinds`() =
        runMagicTest(EnchantScript::class) { _ ->
            for (row in EnchantTable.rows) {
                assertTrue(objTypes[row.base].name.isNotBlank())
                assertTrue(objTypes[row.product].name.isNotBlank())
            }
            for (level in 1..7) {
                val kinds = EnchantTable.rows.filter { it.level == level }.map { it.kind }.toSet()
                assertEquals(4, kinds.size) { "level $level" }
            }
        }

    @Test
    fun GameTestState.`superheat makes steel from iron ore and two coal`() =
        runMagicTest(SuperheatScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_superheat)
            prepare(Spellbook.Standard, spell)
            player.stats[stats.smithing] = 99
            player.inv[0] = InvObj(SmithingObjs.iron_ore)
            player.inv[1] = InvObj(SmithingObjs.coal)
            player.inv[2] = InvObj(SmithingObjs.coal)
            val xp = player.statMap.getXP(stats.smithing)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = SmithingObjs.iron_ore,
            )
            advance(1)

            assertEquals(1, player.count(SmithingObjs.steel_bar))
            assertEquals(0, player.count(SmithingObjs.coal))
            assertEquals(0, player.count(SmithingObjs.iron_ore))
            assertTrue(player.statMap.getXP(stats.smithing) > xp)
        }

    @Test
    fun GameTestState.`bones to bananas converts every bone`() =
        runMagicTest(BonesToFoodScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_bones_to_bananas)
            prepare(Spellbook.Standard, spell)
            repeat(3) { player.inv[it] = InvObj(objs.bones) }

            player.ifButton(spell.component)
            advance(1)

            assertEquals(3, player.count(utility_objs.banana))
            assertEquals(0, player.count(objs.bones))
        }

    @Test
    fun GameTestState.`vengeance rebounds three quarters of the next hit`() =
        runMagicTest(VengeanceScript::class, PlayerHitScript::class, NpcHitScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_vengeance)
            prepare(Spellbook.Lunars, spell, runeSets = 2)
            player.stats[stats.hitpoints] = 99
            player.setCurrentLevel(stats.hitpoints, 99)

            player.ifButton(spell.component)
            advance(1)
            assertTrue(deps.buffs[player].vengeance)

            // A second cast straight away is on cooldown.
            player.ifButton(spell.component)
            advance(1)
            assertMessageSent("You already have Vengeance cast.")

            val type = npcTypes.values.first { it.op[1] != null && it.hitpoints >= 20 }
            val npc = spawnNpc(player.coords.translateX(1), type)
            val before = npc.hitpoints
            player.queueHit(source = npc, delay = 1, type = HitType.Melee, damage = 10)
            advance(3)

            assertEquals(before - 7, npc.hitpoints)
            assertTrue(!deps.buffs[player].vengeance)
        }

    @Test
    fun GameTestState.`humidify fills every empty vessel`() =
        runMagicTest(LunarSkillingScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_humidify)
            prepare(Spellbook.Lunars, spell)
            player.inv[0] = InvObj(utility_objs.bucket)
            player.inv[1] = InvObj(utility_objs.watering_can_3)

            player.ifButton(spell.component)
            advance(1)

            assertEquals(1, player.count(utility_objs.bucket_of_water))
            assertEquals(1, player.count(utility_objs.watering_can_full))
        }

    @Test
    fun GameTestState.`plank make charges coins for one plank`() =
        runMagicTest(LunarSkillingScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_plank_make)
            prepare(Spellbook.Lunars, spell)
            player.inv[0] = InvObj(utility_objs.oak_logs)
            player.inv[1] = InvObj(objs.coins, 200)

            player.ifButtonT(
                spell.component,
                components.inv_items,
                targetSub = 0,
                targetObj = utility_objs.oak_logs,
            )
            advance(1)

            assertEquals(1, player.count(utility_objs.oak_plank))
            assertEquals(25, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`bake pie cooks each pie in turn and pays runes each time`() =
        runMagicTest(BakePieScript::class) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_bake_pie)
            prepare(Spellbook.Lunars, spell, runeSets = 2)
            player.stats[stats.cooking] = 99
            player.inv[0] = InvObj(utility_objs.uncooked_meat_pie)
            player.inv[1] = InvObj(utility_objs.uncooked_meat_pie)

            player.ifButton(spell.component)
            advance(1)
            assertEquals(1, player.count(utility_objs.meat_pie))
            advance(3)
            assertEquals(2, player.count(utility_objs.meat_pie))
            assertEquals(0, player.count(utility_objs.astral_rune))
        }

    private fun GameTestState.runMagicTest(
        vararg scripts: KClass<out PluginScript>,
        body: GameTestScope.(UtilityDeps) -> Unit,
    ) =
        runInjectedGameTest(
            UtilityDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            InvOpenScript::class,
            *scripts,
            testBody = body,
        )

    private fun GameTestScope.prepare(book: Spellbook, spell: MagicSpell, runeSets: Int = 1) {
        player.placeAt(START)
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
        player.ifOpenOverlay(interfaces.inventory, components.toplevel_target_side3)
        VarPlayerIntMapSetter.set(player, varbits.spellbook, book.varValue)
        player.stats[stats.magic] = 99
        player.clearInv()
        for ((index, req) in spell.objReqs.withIndex()) {
            player.inv[RUNE_SLOT + index] = InvObj(req.obj, req.count * runeSets)
        }
    }

    private fun GameTestScope.count(obj: ObjType): Int = player.count(obj)

    private companion object {
        const val RUNE_SLOT = 20
        val START = CoordGrid(3096, 3504, 0)
    }
}
