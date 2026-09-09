package org.rsmod.content.custom.dagannothkings.configs

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.content.custom.dagannothkings.DagannothKing
import org.rsmod.content.custom.dagannothkings.DagannothStyle

/**
 * What the cache does not already say about the three kings.
 *
 * It says a great deal, and `DagannothKingsDump` is what proves it: all three already carry 255
 * hitpoints, attack and strength, their defence levels (128 for Supreme, 255 for the other two),
 * the ranged and magic levels that make each one immune to a style, `size = 3`, their `Attack` op,
 * their ready and walk seqs, and their stab/slash/crush and magic defence bonuses. None of that is
 * repeated below. Re-declaring a value the cache already holds only creates a second place to be
 * wrong, and every type edit is additive once packed - deleting it later does not restore the
 * original.
 *
 * Five things are genuinely missing, and every one of them matters:
 * 1. **Ranged defence.** The cache sets stab, slash, crush and magic, but not
 *    `defence_light`/`_standard`/`_heavy`, which default to 0. `PvNRangedAccuracy` picks one of
 *    those three by the player's ranged attack type, so out of the box every king is trivially
 *    shot - including Prime, whose entire role in the room is that he is not.
 * 2. **AI.** All three ship `defaultMode = Wander` with no hunt mode, `wanderRange = 5` and
 *    `maxRange = 7`. Untouched, they would shuffle five tiles from their spawn and ignore you.
 * 3. **Attack speed.** `params.attackrate` is absent, so they would fall back to
 *    `constants.combat_default_attackrate` (4) and attack half again as fast as they should.
 * 4. **Animations.** No king has an `attack_anim`, `defend_anim` or `death_anim`. Those params
 *    default to the unarmed human set, so all three would throw punches and die like a man. The
 *    seqs exist in the cache and are named for them.
 * 5. **Respawn.** `respawnRate` is the cache default of 100 ticks; live is 90 seconds.
 *
 * Deliberately **not** set: any strength or attack bonus. Max hit is derived rather than stored -
 * `((level + 9) * (bonus + 64) + 320) / 640` - and at level 255 with a bonus of 0 that is exactly
 * 26, which is live for all three. The engine's own `NpcMeleeMaxHitTest` and `NpcRangedMaxHitTest`
 * already assert precisely that, with `strengthBonus = 0`. `DagannothKingsConfigTest` re-asserts
 * the 26 here so that adding a bonus later fails loudly rather than quietly buffing the fight.
 *
 * Also not set: `size`, which the cache puts at 3 and the client reads from its own copy of the
 * same type, so editing it would desync the two; and `params.respawn_time`, which despite the name
 * is a loc and obj param that nothing reads on an npc.
 */
internal object DagannothKingsNpcEditor : NpcEditor() {
    init {
        for (king in DagannothKing.all) {
            edit(checkNotNull(dk_npcs.byKing[king])) {
                // Two jobs, as in `KnightWavesNpcEditor`. It stops the king patrolling the lair,
                // and it disables the retreat branch in `NpcExtensions.retaliate`, which only fires
                // when `wanderRange > 0` and would otherwise send a king swimming home the moment a
                // fight drifted past `maxRange + attackRange` from its spawn.
                wanderRange = 0
                maxRange = 32
                defaultMode = none

                // Always aggressive, and - because the lair is multiway - all three at once.
                // `NpcPlayerHuntProcessor` skips its "already in combat" gate inside multi, so
                // being locked in combat with Rex does not stop the other two finding you. The
                // aggressive hunt modes also ignore combat level, so the fight never goes tolerant.
                huntMode =
                    when (king.style) {
                        // findNewMode = OpPlayer2: closes to melee range.
                        DagannothStyle.Melee -> huntmodes.aggressive_melee
                        // findNewMode = ApPlayer2: stops at `attackRange` and opens fire.
                        DagannothStyle.Ranged,
                        DagannothStyle.Magic -> huntmodes.aggressive_ranged
                    }
                huntRange = if (king.style.attacksAtRange) RANGED_HUNT_RANGE else MELEE_HUNT_RANGE

                // `AiPlayerInteractions.interactAp` reads this as `startApRange`. The cache leaves
                // it at the default of 1, which would make the two ranged kings walk into melee
                // range before firing.
                attackRange = if (king.style.attacksAtRange) RANGED_ATTACK_RANGE else 1

                respawnRate = RESPAWN_TICKS
                param[params.attackrate] = ATTACK_RATE

                param[params.attack_anim] =
                    when (king.style) {
                        DagannothStyle.Melee -> dk_seqs.attack_melee
                        DagannothStyle.Ranged -> dk_seqs.attack_range
                        DagannothStyle.Magic -> dk_seqs.attack_mage
                    }
                param[params.defend_anim] = dk_seqs.defend
                param[params.death_anim] = dk_seqs.death

                // How the king attacks, which is also what routes it through the ranged/magic
                // driver rather than the melee one. A style tag without both projectile params is
                // inert by design: the driver refuses to fire when it cannot read a flight time.
                when (king.style) {
                    DagannothStyle.Melee -> {
                        // Rex bites. Without this `resolveMeleeAttackType` falls through to crush
                        // anyway, so this is mostly documentation - but it does decide which of the
                        // player's three defensive bonuses the roll reads.
                        param[params.npc_attack_type] = categories.attacktype_crush
                    }
                    DagannothStyle.Ranged -> {
                        param[params.npc_attack_type] = categories.attacktype_ranged
                        param[params.proj_travel] = dk_spotanims.arrow_travel
                        param[params.proj_type] = projanims.arrow
                    }
                    DagannothStyle.Magic -> {
                        param[params.npc_attack_type] = categories.attacktype_magic
                        param[params.proj_travel] = dk_spotanims.spine_travel
                        param[params.proj_type] = projanims.magic_spell
                    }
                }

                val rangedDefence = rangedDefence(king)
                param[params.defence_light] = rangedDefence
                param[params.defence_standard] = rangedDefence
                param[params.defence_heavy] = rangedDefence
            }
        }
    }

    /**
     * The missing third of each king's defensive profile, from the reference block in
     * `api/combat/combat-formulas`' `FormulaTestNpcs`.
     *
     * Prime is the ranged weakness at 10 and Supreme the ranged wall at 550; Rex sits at his own
     * 255 melee band, because ranged is neither his weakness nor his speciality.
     */
    private fun rangedDefence(king: DagannothKing): Int =
        when (king) {
            DagannothKing.Prime -> 10
            DagannothKing.Supreme -> 550
            DagannothKing.Rex -> 255
        }

    /**
     * 3.6 seconds. This is the one figure here that is not read out of the cache or out of an
     * existing test fixture, so it is the first thing to check against the wiki if the fight feels
     * wrong.
     */
    private const val ATTACK_RATE = 6

    /** 90 seconds. `NpcDeath.death` hands `npc.type.respawnRate` straight to `npcRepo.despawn`. */
    private const val RESPAWN_TICKS = 150

    private const val MELEE_HUNT_RANGE = 10
    private const val RANGED_HUNT_RANGE = 12
    private const val RANGED_ATTACK_RANGE = 10
}
