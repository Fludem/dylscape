package org.rsmod.content.custom.dagannothkings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.maxhit.npc.NpcMagicMaxHit
import org.rsmod.api.combat.maxhit.npc.NpcMeleeMaxHit
import org.rsmod.api.combat.maxhit.npc.NpcRangedMaxHit
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.dagannothkings.configs.dk_npcs
import org.rsmod.content.custom.dagannothkings.configs.dk_seqs
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.TypeResolver
import org.rsmod.game.type.category.isType

/**
 * Guards the parts of the fight that live entirely in configuration.
 *
 * The module is mostly an `NpcEditor`, which means almost everything that could go wrong here goes
 * wrong silently: a missing ranged defence bonus makes a king trivially shot rather than throwing,
 * a stray strength bonus quietly doubles its max hit, and a missing animation makes a dagannoth
 * throw punches. None of that fails a build on its own, so it is asserted.
 */
@Execution(ExecutionMode.SAME_THREAD)
class DagannothKingsConfigTest {
    @Test
    fun GameTestState.`every king resolves and is attackable`() = runBasicGameTest {
        assertEquals(DagannothKing.all.size, dk_npcs.byKing.size)
        for (king in DagannothKing.all) {
            val ref = checkNotNull(dk_npcs.byKing[king]) { "No npc mapped for $king." }
            val type = cacheTypes.npcs.getValue(ref.id)
            // The exact predicate `DropTableResourceLoader` filters on. Without an `Attack` op the
            // generated drop table for this king is dropped at load, and since this module owns no
            // death handler, that would mean a boss with no loot and no error.
            assertTrue(type.op.any { it == "Attack" }, "${type.name} cannot be attacked.")
        }
    }

    @Test
    fun GameTestState.`the editor landed on all three kings`() = runBasicGameTest {
        for ((king, ref) in dk_npcs.byKing) {
            val type = cacheTypes.npcs.getValue(ref.id)
            assertEquals(0, type.wanderRange, "${type.name} would roam, or flee home mid-fight.")
            assertEquals(32, type.maxRange, "${type.name} would leash too early.")
            assertEquals(NpcMode.None, type.defaultMode, "${type.name} would idle-wander.")
            assertEquals(150, type.respawnRate, "${type.name} respawns at the wrong rate.")
            assertNotNull(type.huntMode, "${type.name} would never engage.")
            assertTrue(type.huntRange > 0, "${type.name} has no hunt range.")

            val expectedHunt =
                if (king.style.attacksAtRange) {
                    huntmodes.aggressive_ranged
                } else {
                    huntmodes.aggressive_melee
                }
            // `huntMode` is a raw id on the npc type, so resolve the reference to its id rather
            // than comparing the objects -- a hashed reference never equals an unpacked type.
            assertEquals(
                TypeResolver[expectedHunt],
                type.huntMode,
                "${type.name} engages through the wrong mode for a ${king.style} attacker.",
            )

            val expectedAttackRange = if (king.style.attacksAtRange) 10 else 1
            assertEquals(
                expectedAttackRange,
                type.attackRange,
                "${type.name} opens fire at the wrong distance.",
            )
            assertEquals(
                6,
                type.param(params.attackrate),
                "${type.name} attacks at the wrong speed.",
            )
        }
    }

    @Test
    fun GameTestState.`the kings keep the levels the cache already gave them`() = runBasicGameTest {
        // The editor deliberately declares none of these. If someone later "helpfully" adds them
        // and mistypes one, this is what catches it -- and it documents that they are
        // cache-sourced.
        for ((king, ref) in dk_npcs.byKing) {
            val type = cacheTypes.npcs.getValue(ref.id)
            assertEquals(255, type.hitpoints, "${type.name} hitpoints")
            assertEquals(255, type.attack, "${type.name} attack")
            assertEquals(255, type.strength, "${type.name} strength")
            assertEquals(3, type.size, "${type.name} size")

            val expectedDefence = if (king == DagannothKing.Supreme) 128 else 255
            assertEquals(expectedDefence, type.defence, "${type.name} defence")
        }
    }

    @Test
    fun GameTestState.`each king has exactly one weakness`() = runBasicGameTest {
        // The whole mechanic of the room, in one assertion. Each king is soft to precisely the
        // style the other two punish, which is what forces the gear switch.
        val rex = cacheTypes.npcs.getValue(dk_npcs.melee.id)
        assertEquals(10, rex.param(params.defence_magic), "Rex should fold to magic.")
        assertEquals(255, rex.param(params.defence_stab), "Rex should wall melee.")
        assertEquals(255, rex.param(params.defence_standard), "Rex should wall ranged.")

        val prime = cacheTypes.npcs.getValue(dk_npcs.magic.id)
        assertEquals(10, prime.param(params.defence_standard), "Prime should fold to ranged.")
        assertEquals(10, prime.param(params.defence_light), "Prime should fold to ranged.")
        assertEquals(10, prime.param(params.defence_heavy), "Prime should fold to ranged.")
        assertEquals(255, prime.param(params.defence_stab), "Prime should wall melee.")
        assertEquals(255, prime.param(params.defence_magic), "Prime should wall magic.")

        val supreme = cacheTypes.npcs.getValue(dk_npcs.supreme.id)
        assertEquals(10, supreme.param(params.defence_stab), "Supreme should fold to melee.")
        assertEquals(10, supreme.param(params.defence_slash), "Supreme should fold to melee.")
        assertEquals(10, supreme.param(params.defence_crush), "Supreme should fold to melee.")
        assertEquals(550, supreme.param(params.defence_standard), "Supreme should wall ranged.")
        assertEquals(255, supreme.param(params.defence_magic), "Supreme should wall magic.")
    }

    @Test
    fun GameTestState.`every king hits for twenty six`() = runBasicGameTest {
        // This is what makes "max hit is derived, not stored" safe to rely on. Max hit falls out of
        // level and strength bonus, and the editor sets no bonus at all, so the day anyone adds one
        // this fails instead of quietly buffing the fight.
        val rex = cacheTypes.npcs.getValue(dk_npcs.melee.id)
        val rexMaxHit =
            NpcMeleeMaxHit.calculateBaseDamage(
                NpcMeleeMaxHit.calculateEffectiveStrength(rex.strength),
                rex.param(params.attack_melee),
            )
        assertEquals(LIVE_MAX_HIT, rexMaxHit, "Dagannoth Rex max hit")

        val supreme = cacheTypes.npcs.getValue(dk_npcs.supreme.id)
        val supremeMaxHit =
            NpcRangedMaxHit.calculateBaseDamage(
                NpcRangedMaxHit.calculateEffectiveRanged(supreme.ranged),
                supreme.param(params.ranged_strength),
            )
        assertEquals(LIVE_MAX_HIT, supremeMaxHit, "Dagannoth Supreme max hit")

        val prime = cacheTypes.npcs.getValue(dk_npcs.magic.id)
        val primeMaxHit =
            NpcMagicMaxHit.calculateBaseDamage(
                NpcMagicMaxHit.calculateEffectiveMagic(prime.magic),
                prime.param(params.npc_magic_damage_bonus),
            )
        assertEquals(LIVE_MAX_HIT, primeMaxHit, "Dagannoth Prime max hit")
    }

    @Test
    fun GameTestState.`each king attacks and dies as a dagannoth`() = runBasicGameTest {
        for ((king, ref) in dk_npcs.byKing) {
            val type = cacheTypes.npcs.getValue(ref.id)
            val expectedAttack =
                when (king.style) {
                    DagannothStyle.Melee -> dk_seqs.attack_melee
                    DagannothStyle.Ranged -> dk_seqs.attack_range
                    DagannothStyle.Magic -> dk_seqs.attack_mage
                }
            assertEquals(
                expectedAttack.id,
                type.param(params.attack_anim).id,
                "${type.name} plays the wrong attack animation.",
            )
            assertEquals(
                dk_seqs.defend.id,
                type.param(params.defend_anim).id,
                "${type.name} blocks like an unarmed human.",
            )
            assertEquals(
                dk_seqs.death.id,
                type.param(params.death_anim).id,
                "${type.name} dies like an unarmed human.",
            )
        }
    }

    @Test
    fun GameTestState.`the ranged kings declare a projectile and the melee king does not`() =
        runBasicGameTest {
            for ((king, ref) in dk_npcs.byKing) {
                val type = cacheTypes.npcs.getValue(ref.id)
                val style = type.paramOrNull(params.npc_attack_type)
                if (!king.style.attacksAtRange) {
                    assertTrue(
                        style.isType(categories.attacktype_crush),
                        "${type.name} should bite, not shoot.",
                    )
                    continue
                }

                val expectedStyle =
                    if (king.style == DagannothStyle.Ranged) {
                        categories.attacktype_ranged
                    } else {
                        categories.attacktype_magic
                    }
                assertTrue(
                    style.isType(expectedStyle),
                    "${type.name} would be driven as a melee attacker.",
                )
                // Both halves are required. The driver refuses to fire without a flight profile,
                // so a style tag on its own leaves the king standing at range doing nothing.
                assertNotNull(
                    type.paramOrNull(params.proj_travel),
                    "${type.name} has nothing to fire.",
                )
                assertNotNull(
                    type.paramOrNull(params.proj_type),
                    "${type.name} has no flight time, so its hits could not be timed.",
                )
            }
        }

    @Test
    fun GameTestState.`the kings own no content group`() = runBasicGameTest {
        // `contentGroup` holds a single value, so a second editor tagging these types would
        // conflict nondeterministically. Nothing should be claiming them.
        for ((_, ref) in dk_npcs.byKing) {
            val type = cacheTypes.npcs.getValue(ref.id)
            // -1 is the unset sentinel; `contentGroup` is a plain Int on the packed type.
            assertEquals(-1, type.contentGroup, "${type.name} joined a content group.")
        }
    }

    private companion object {
        const val LIVE_MAX_HIT = 26
    }
}
