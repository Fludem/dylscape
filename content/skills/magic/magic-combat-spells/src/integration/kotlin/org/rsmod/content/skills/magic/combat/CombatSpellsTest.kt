package org.rsmod.content.skills.magic.combat

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.areas
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.api.spells.attack.attack
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.combat.spells.AncientSpells
import org.rsmod.content.skills.magic.combat.spells.StandardCombatSpells
import org.rsmod.content.skills.magic.commons.FreezeManager
import org.rsmod.content.skills.magic.commons.MagicRuneTestSupport
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.scripts.FrozenScript
import org.rsmod.content.skills.magic.commons.scripts.MagicCommonsScript
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class CombatDeps
@Inject
constructor(
    val manager: SpellAttackManager,
    val spellbooks: MagicSpellbooks,
    val freeze: FreezeManager,
    val standard: StandardCombatSpells,
    val ancients: AncientSpells,
    val areaIndex: AreaIndex,
)

/**
 * Exercises the registered `SpellAttack`s directly, the way upstream's `PvNCombat` does once its
 * range and target checks have passed. Upstream's dispatch scripts are `internal`, so the click
 * itself is not driven here; what is proved is that every spell registers without colliding and
 * that its damage, effect and refusal land on the right tick.
 *
 * The harness random is a queue: `next = 0` makes the accuracy roll succeed (a hit needs the roll
 * under the hit chance) and `then` is handed back as the damage roll.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CombatSpellsTest {
    @Test
    fun GameTestState.`every non-elemental combat spell registers exactly once`() =
        runCombatTest { deps ->
            val registry = register(deps)
            val combat =
                deps.spellbooks.all.filter {
                    it.type == org.rsmod.api.combat.commons.magic.MagicSpellType.Combat &&
                        it.spellbook != Spellbook.Arceuus
                }
            // Spells with a player target or a self-only effect are not SpellAttacks; the
            // elementals are upstream's. Everything else on the standard and Ancient books must
            // have registered.
            val notAttacks =
                setOf(
                        objs.spell_charge,
                        objs.spell_vengeance,
                        objs.spell_vengeance_other,
                        objs.spell_tele_block,
                        objs.spell_teleport_to_target,
                        objs.spell_cure_other,
                        objs.spell_cure_me,
                        objs.spell_cure_group,
                        objs.spell_stat_spy,
                        objs.spell_energy_transfer,
                        objs.spell_heal_other,
                        objs.spell_heal_group,
                        objs.spell_monster_examine,
                        objs.spell_dream,
                        objs.spell_res_pot_share,
                        objs.spell_statboost_pot_share,
                    )
                    .map { it.id }
            val elemental = listOf("Strike", "Bolt", "Blast", "Wave", "Surge")
            val missing =
                combat
                    .filter { registry[it.obj] == null }
                    .filterNot { it.obj.id in notAttacks }
                    .filterNot { spell -> elemental.any { spell.name.endsWith(it) } }
            assertEquals(emptyList<String>(), missing.map { it.name })
        }

    @Test
    fun GameTestState.`ice rush damages and freezes the target`() = runCombatTest { deps ->
        val registry = register(deps)
        val spell = prepare(deps, Spellbook.Ancients, objs.spell_ice_rush)
        val npc = spawnTarget(livingType())
        val before = npc.hitpoints
        val xp = player.statMap.getXP(stats.magic)

        hitFor(8)
        cast(registry, spell, npc)
        advance(4)

        assertEquals(before - 8, npc.hitpoints)
        assertTrue(deps.freeze.isFrozen(npc))
        assertTrue(player.statMap.getXP(stats.magic) > xp)
        assertEquals(0, player.count(objs.death_rune))
    }

    @Test
    fun GameTestState.`confuse lowers attack by five percent of base and never stacks`() =
        runCombatTest { deps ->
            val registry = register(deps)
            val spell = prepare(deps, Spellbook.Standard, objs.spell_confuse, runeSets = 2)
            val npc = spawnTarget(livingType())
            val base = npc.baseAttackLvl

            hitFor(0)
            cast(registry, spell, npc)
            advance(4)
            assertEquals(base - base * 5 / 100, npc.attackLvl)
            assertEquals(npc.baseHitpointsLvl, npc.hitpoints)

            hitFor(0)
            cast(registry, spell, npc)
            advance(4)
            assertEquals(base - base * 5 / 100, npc.attackLvl)
        }

    @Test
    fun GameTestState.`bind holds the target without damaging it`() = runCombatTest { deps ->
        val registry = register(deps)
        val spell = prepare(deps, Spellbook.Standard, objs.spell_bind)
        val npc = spawnTarget(livingType())
        val before = npc.hitpoints

        hitFor(0)
        cast(registry, spell, npc)
        advance(4)

        assertEquals(before, npc.hitpoints)
        assertTrue(deps.freeze.isFrozen(npc))
    }

    @Test
    fun GameTestState.`blood rush heals the caster a quarter of the damage`() =
        runCombatTest { deps ->
            val registry = register(deps)
            val spell = prepare(deps, Spellbook.Ancients, objs.spell_blood_rush)
            player.setCurrentLevel(stats.hitpoints, 50)
            val npc = spawnTarget(livingType())

            hitFor(12)
            cast(registry, spell, npc)
            advance(4)

            assertEquals(53, player.stats[stats.hitpoints])
        }

    @Test
    fun GameTestState.`crumble undead refuses the living and hits the dead`() =
        runCombatTest { deps ->
            val registry = register(deps)
            val spell = prepare(deps, Spellbook.Standard, objs.spell_crumble_undead, runeSets = 2)
            val living = spawnTarget(livingType())
            val before = living.hitpoints

            cast(registry, spell, living)
            assertMessageSent("This spell only affects skeletons, zombies, ghosts and shades.")
            advance(4)
            assertEquals(before, living.hitpoints)
            assertEquals(2, player.count(objs.chaos_rune))

            val undead = spawnTarget(undeadType(), offset = 2)
            val undeadBefore = undead.hitpoints
            hitFor(6)
            cast(registry, spell, undead)
            advance(4)
            assertEquals(undeadBefore - 6, undead.hitpoints)
            assertEquals(1, player.count(objs.chaos_rune))
        }

    @Test
    fun GameTestState.`magic dart needs the slayer staff`() = runCombatTest { deps ->
        val registry = register(deps)
        val spell = prepare(deps, Spellbook.Standard, objs.spell_magic_dart)
        player.clearInv()
        player.inv[20] = InvObj(objs.death_rune, 1)
        player.inv[21] = InvObj(objs.mind_rune, 4)
        val npc = spawnTarget(livingType())
        val before = npc.hitpoints

        cast(registry, spell, npc)
        assertMessageSent("You need to be wielding a suitable staff to cast this spell.")
        advance(4)
        assertEquals(before, npc.hitpoints)
    }

    @Test
    fun GameTestState.`ice burst strikes every neighbour of the target in multi-combat`() =
        runCombatTest { deps ->
            val registry = register(deps)
            val spell = prepare(deps, Spellbook.Ancients, objs.spell_ice_burst)
            // The harness binds an empty `AreaIndex`, so the square is made multi-combat by hand.
            val centre = MULTI_CENTRE
            for (dz in -1..1) {
                val zone = ZoneKey.from(centre.translateZ(dz * 8))
                deps.areaIndex.registerAll(zone, listOf(areas.multiway.id.toShort()).iterator())
            }
            player.placeAt(centre.translateZ(-2))
            val type = livingType()
            val target = spawnNpc(centre, type)
            val east = spawnNpc(centre.translateX(1), type)
            val north = spawnNpc(centre.translateZ(1), type)
            val far = spawnNpc(centre.translateX(3), type)
            val full = type.hitpoints

            // One accuracy roll and one damage roll per struck npc.
            random.next = 0
            random.then = 5
            random.then = 0
            random.then = 5
            random.then = 0
            random.then = 5
            cast(registry, spell, target)
            advance(4)

            assertEquals(full - 5, target.hitpoints)
            assertEquals(full - 5, east.hitpoints)
            assertEquals(full - 5, north.hitpoints)
            assertEquals(full, far.hitpoints)
            assertTrue(deps.freeze.isFrozen(east))
            assertFalse(deps.freeze.isFrozen(far))
        }

    private fun GameTestState.runCombatTest(body: GameTestScope.(CombatDeps) -> Unit) =
        runInjectedGameTest(
            CombatDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            NpcHitScript::class,
            FrozenScript::class,
            MagicCommonsScript::class,
            testBody = body,
        )

    private fun register(deps: CombatDeps): SpellAttackRegistry {
        val registry = SpellAttackRegistry()
        val repo = SpellAttackRepository(registry)
        with(deps.standard) { repo.register(deps.manager) }
        with(deps.ancients) { repo.register(deps.manager) }
        return registry
    }

    private fun GameTestScope.prepare(
        deps: CombatDeps,
        book: Spellbook,
        obj: org.rsmod.game.type.obj.ObjType,
        runeSets: Int = 1,
    ): MagicSpell {
        val spell = deps.spellbooks.spellFor(obj)
        player.placeAt(START)
        VarPlayerIntMapSetter.set(player, varbits.spellbook, book.varValue)
        player.stats[stats.magic] = 99
        player.stats[stats.hitpoints] = 99
        player.setCurrentLevel(stats.hitpoints, 99)
        player.clearInv()
        for ((index, req) in spell.objReqs.withIndex()) {
            player.inv[RUNE_SLOT + index] = InvObj(req.obj, req.count * runeSets)
        }
        return spell
    }

    private fun GameTestScope.cast(registry: SpellAttackRegistry, spell: MagicSpell, npc: Npc) {
        val attack = registry[spell.obj] ?: error("${spell.name} is not registered")
        player.withProtectedAccess {
            attack.attack(
                this,
                npc,
                CombatAttack.Spell(weapon = null, spell = spell, defensive = false),
            )
        }
    }

    /** Queues a successful accuracy roll and then [damage] as the max-hit roll. */
    private fun GameTestScope.hitFor(damage: Int) {
        random.next = 0
        random.then = damage
    }

    private fun GameTestScope.spawnTarget(type: UnpackedNpcType, offset: Int = 1): Npc =
        spawnNpc(player.coords.translateX(offset), type)

    private fun GameTestScope.livingType(): UnpackedNpcType =
        npcTypes.values.first {
            it.op[1] == "Attack" &&
                it.hitpoints in 30..200 &&
                it.size == 1 &&
                "skeleton" !in it.name.lowercase() &&
                "zombie" !in it.name.lowercase() &&
                "ghost" !in it.name.lowercase()
        }

    private fun GameTestScope.undeadType(): UnpackedNpcType =
        npcTypes.values.first {
            it.op[1] == "Attack" &&
                it.hitpoints in 20..200 &&
                it.size == 1 &&
                "skeleton" in it.name.lowercase()
        }

    private companion object {
        const val RUNE_SLOT = 20
        val START = CoordGrid(3096, 3504, 0)
        /** Open ground in the Edgeville square with a clear tile on every side. */
        val MULTI_CENTRE = CoordGrid(3099, 3503, 0)
    }
}
