package org.rsmod.content.other.special.attacks

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackModule as SpecialAttackApiModule
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.skills.magic.commons.FreezeManager
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript

class SpecialAttackDeps
@Inject
constructor(val registry: SpecialAttackRegistry, val freezes: FreezeManager)

/**
 * Boots specials the way the server does: upstream's `SpecialAttackScript` loads every energy cost
 * from cache enum 906, then registers every bound `SpecialAttackMap`. A variant missing from the
 * enum, or registered twice, fails that startup, and so fails every test here.
 *
 * Handlers are then called directly, the way `PvNCombat` does once its range and delay checks pass.
 * Upstream's dispatch scripts are `internal`, so the click is not driven here.
 *
 * The harness random is a queue of raw values. `0` makes an accuracy roll land, a huge value makes
 * it miss, and a damage roll hands back whatever is queued.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SpecialAttacksTest {
    @Test
    fun GameTestState.`every new special weapon registers as the right kind of attack`() =
        runSpecialTest { deps ->
            for (obj in MELEE) {
                assertTrue(deps.registry[InvObj(obj, 1)] is SpecialAttack.Melee, "melee $obj")
            }
            for (obj in RANGED) {
                assertTrue(deps.registry[InvObj(obj, 1)] is SpecialAttack.Ranged, "ranged $obj")
            }
            for (obj in INSTANT) {
                assertTrue(deps.registry[InvObj(obj, 1)] is SpecialAttack.Instant, "instant $obj")
            }
        }

    @Test
    fun GameTestState.`dragon dagger strikes twice`() = runSpecialTest { deps ->
        val npc = prepare(special_objs.dragon_dagger)
        val before = npc.hitpoints

        land(5)
        land(7)
        assertTrue(melee(deps, special_objs.dragon_dagger, npc, MeleeAttackType.Stab))
        advance(3)

        assertEquals(before - 12, npc.hitpoints)
    }

    @Test
    fun GameTestState.`abyssal dagger's second hit rides on the first roll`() =
        runSpecialTest { deps ->
            val npc = prepare(special_objs.abyssal_dagger)
            val before = npc.hitpoints

            land(6)
            random.then = 4
            melee(deps, special_objs.abyssal_dagger, npc, MeleeAttackType.Stab)
            advance(3)
            assertEquals(before - 10, npc.hitpoints)

            miss()
            melee(deps, special_objs.abyssal_dagger, npc, MeleeAttackType.Stab)
            advance(3)
            assertEquals(before - 10, npc.hitpoints)
        }

    @Test
    fun GameTestState.`dragon claws cascade from a first hit`() = runSpecialTest { deps ->
        val npc = prepare(objs.dragon_claws)
        val before = npc.hitpoints

        // First roll lands for 20: 20, 10, 5, 6.
        land(20)
        melee(deps, objs.dragon_claws, npc)
        advance(4)

        assertEquals(before - 41, npc.hitpoints)
    }

    @Test
    fun GameTestState.`dragon claws that all miss can still chip two`() = runSpecialTest { deps ->
        val npc = prepare(objs.dragon_claws)
        val before = npc.hitpoints

        repeat(4) { miss() }
        random.then = 1
        melee(deps, objs.dragon_claws, npc)
        advance(4)

        assertEquals(before - 2, npc.hitpoints)
    }

    @Test
    fun GameTestState.`dragon warhammer takes 30 percent defence only on a hit`() =
        runSpecialTest { deps ->
            val npc = prepare(special_objs.dragon_warhammer)
            val defence = npc.defenceLvl

            miss()
            melee(deps, special_objs.dragon_warhammer, npc, MeleeAttackType.Crush)
            assertEquals(defence, npc.defenceLvl)

            land(10)
            melee(deps, special_objs.dragon_warhammer, npc, MeleeAttackType.Crush)
            assertEquals(defence - defence * 30 / 100, npc.defenceLvl)
        }

    @Test
    fun GameTestState.`bandos godsword drains defence then strength by the damage`() =
        runSpecialTest { deps ->
            val npc = prepare(special_objs.bandos_godsword)
            val defence = npc.defenceLvl
            val strength = npc.strengthLvl

            land(defence + 5)
            melee(deps, special_objs.bandos_godsword, npc)

            assertEquals(0, npc.defenceLvl)
            assertEquals(strength - 5, npc.strengthLvl)
        }

    @Test
    fun GameTestState.`saradomin godsword heals half the damage and a quarter in prayer`() =
        runSpecialTest { deps ->
            val npc = prepare(special_objs.saradomin_godsword)
            player.setCurrentLevel(stats.hitpoints, 50)
            player.setCurrentLevel(stats.prayer, 10)

            land(30)
            melee(deps, special_objs.saradomin_godsword, npc)

            assertEquals(65, player.stats[stats.hitpoints])
            assertEquals(17, player.stats[stats.prayer])
        }

    @Test
    fun GameTestState.`zamorak godsword freezes only on a hit`() = runSpecialTest { deps ->
        val npc = prepare(special_objs.zamorak_godsword)

        miss()
        melee(deps, special_objs.zamorak_godsword, npc)
        assertFalse(deps.freezes.isFrozen(npc))

        land(10)
        melee(deps, special_objs.zamorak_godsword, npc)
        assertTrue(deps.freezes.isFrozen(npc))
    }

    @Test
    fun GameTestState.`saradomin sword adds lightning to a landed hit`() = runSpecialTest { deps ->
        val npc = prepare(special_objs.saradomin_sword)
        val before = npc.hitpoints

        land(10)
        random.then = 7
        melee(deps, special_objs.saradomin_sword, npc)
        advance(3)

        assertEquals(before - 17, npc.hitpoints)
    }

    @Test
    fun GameTestState.`rampage trades four stats for strength`() = runSpecialTest { deps ->
        prepare(special_objs.dragon_battleaxe)

        assertTrue(instant(deps, special_objs.dragon_battleaxe))

        // 10% of 99 is 9 from each of four stats: 36 drained, so Strength gains 10 + 36 / 4.
        assertEquals(90, player.stats[stats.attack])
        assertEquals(90, player.stats[stats.defence])
        assertEquals(90, player.stats[stats.ranged])
        assertEquals(90, player.stats[stats.magic])
        assertEquals(118, player.stats[stats.strength])
    }

    @Test
    fun GameTestState.`granite maul does nothing without a target`() = runSpecialTest { deps ->
        prepare(special_objs.granite_maul)
        assertFalse(instant(deps, special_objs.granite_maul))
    }

    @Test
    fun GameTestState.`magic shortbow needs two arrows and spends two`() = runSpecialTest { deps ->
        val npc = prepare(objs.magic_shortbow)
        // A fired arrow can drop, and a floor obj is owned by the player's observer id. A real
        // login always has one; the harness player does not.
        player.observerUUID = 1L

        player.quiver = InvObj(objs.rune_arrow, 1)
        assertFalse(ranged(deps, objs.magic_shortbow, npc))
        assertMessageSent(
            "You need to have at least 2 arrows in your quiver for this special attack."
        )
        assertEquals(1, player.quiver?.count)

        player.quiver = InvObj(objs.rune_arrow, 10)
        land(3)
        land(4)
        assertTrue(ranged(deps, objs.magic_shortbow, npc))
        assertEquals(8, player.quiver?.count)
    }

    private fun GameTestState.runSpecialTest(body: GameTestScope.(SpecialAttackDeps) -> Unit) =
        runInjectedGameTest(
            SpecialAttackDeps::class,
            SpecialTestModule(),
            SPECIAL_ATTACK_SCRIPT,
            WeaponAttackStylesScript::class,
            NpcHitScript::class,
            testBody = body,
        )

    private class SpecialTestModule : AbstractModule() {
        override fun configure() {
            install(SpecialAttackApiModule())
            install(SpecialAttackModule())
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
        }
    }

    /** Equips [weapon] on a maxed player and puts a sturdy npc beside them. */
    private fun GameTestScope.prepare(weapon: ObjType): Npc {
        player.placeAt(START)
        player.righthand = InvObj(weapon, 1)
        for (stat in COMBAT_STATS) {
            player.stats[stat] = 99
        }
        return spawnNpc(player.coords.translateX(1), sturdyType())
    }

    private fun GameTestScope.melee(
        deps: SpecialAttackDeps,
        weapon: ObjType,
        target: Npc,
        type: MeleeAttackType = MeleeAttackType.Slash,
    ): Boolean {
        val special = deps.registry[InvObj(weapon, 1)] as SpecialAttack.Melee
        val attack =
            CombatAttack.Melee(
                weapon = InvObj(weapon, 1),
                type = type,
                style = MeleeAttackStyle.Accurate,
                stance = CombatStance.Stance1,
            )
        var result = false
        player.withProtectedAccess { result = special.attack(this, target, attack) }
        return result
    }

    private fun GameTestScope.ranged(
        deps: SpecialAttackDeps,
        weapon: ObjType,
        target: Npc,
    ): Boolean {
        val special = deps.registry[InvObj(weapon, 1)] as SpecialAttack.Ranged
        val attack =
            CombatAttack.Ranged(
                weapon = InvObj(weapon, 1),
                type = RangedAttackType.Standard,
                style = RangedAttackStyle.Accurate,
            )
        var result = false
        player.withProtectedAccess { result = special.attack(this, target, attack) }
        return result
    }

    private fun GameTestScope.instant(deps: SpecialAttackDeps, weapon: ObjType): Boolean {
        val special = deps.registry[InvObj(weapon, 1)] as SpecialAttack.Instant
        var result = false
        player.withProtectedAccess { result = special.activate(this) }
        return result
    }

    /** Queues a landed accuracy roll and then [damage] as the damage roll. */
    private fun GameTestScope.land(damage: Int) {
        random.next = 0
        random.then = damage
    }

    private fun GameTestScope.miss() {
        random.next = 1_000_000
    }

    private fun GameTestScope.sturdyType(): UnpackedNpcType =
        npcTypes.values.first {
            it.op[1] == "Attack" &&
                it.size == 1 &&
                it.hitpoints in 150..1000 &&
                it.defence in 20..150 &&
                it.strength >= 10
        }

    private companion object {
        val START = CoordGrid(3096, 3504, 0)

        val COMBAT_STATS =
            listOf(
                stats.attack,
                stats.strength,
                stats.defence,
                stats.hitpoints,
                stats.ranged,
                stats.magic,
                stats.prayer,
            )

        /** Upstream's startup script is `internal` to `api:specials`, so it is loaded by name. */
        @Suppress("UNCHECKED_CAST")
        val SPECIAL_ATTACK_SCRIPT: KClass<out PluginScript> =
            Class.forName("org.rsmod.api.specials.scripts.SpecialAttackScript").kotlin
                as KClass<out PluginScript>

        val MELEE: List<ObjType> =
            listOf(
                special_objs.dragon_dagger,
                special_objs.dragon_dagger_p,
                special_objs.dragon_dagger_p_plus,
                special_objs.dragon_dagger_p_plus_plus,
                special_objs.abyssal_dagger,
                special_objs.abyssal_dagger_p,
                special_objs.abyssal_dagger_p_plus,
                special_objs.abyssal_dagger_p_plus_plus,
                objs.abyssal_whip,
                special_objs.abyssal_whip_lava,
                special_objs.abyssal_whip_ice,
                special_objs.abyssal_tentacle,
                special_objs.dragon_mace,
                special_objs.dragon_scimitar,
                special_objs.dragon_scimitar_or,
                special_objs.dragon_warhammer,
                special_objs.dragon_warhammer_or,
                objs.dragon_claws,
                special_objs.dragon_claws_or,
                objs.armadyl_godsword,
                special_objs.armadyl_godsword_or,
                special_objs.bandos_godsword,
                special_objs.bandos_godsword_or,
                special_objs.saradomin_godsword,
                special_objs.saradomin_godsword_or,
                special_objs.zamorak_godsword,
                special_objs.zamorak_godsword_or,
                special_objs.abyssal_bludgeon,
                special_objs.saradomin_sword,
            )

        val RANGED: List<ObjType> =
            listOf(objs.magic_shortbow, special_objs.magic_shortbow_i, special_objs.magic_longbow)

        val INSTANT: List<ObjType> =
            listOf(
                special_objs.dragon_battleaxe,
                special_objs.granite_maul,
                special_objs.granite_maul_or,
                special_objs.granite_maul_plus,
                special_objs.granite_maul_or_plus,
            )
    }
}
