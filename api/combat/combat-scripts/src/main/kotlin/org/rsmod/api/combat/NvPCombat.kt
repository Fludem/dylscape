package org.rsmod.api.combat

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.combat.npc.attackingPlayer
import org.rsmod.api.combat.npc.lastAttack
import org.rsmod.api.combat.player.aggressiveNpc
import org.rsmod.api.combat.player.lastCombat
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.obj.ObjTypeList

/**
 * The npc side of combat: everything an npc does when it attacks a player.
 *
 * Public because integration suites need to start [org.rsmod.api.combat.scripts.NvPCombatScript],
 * which takes this as a constructor dependency, and a test source set is a separate compilation
 * unit that cannot see internals.
 */
public class NvPCombat
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
    private val objTypes: ObjTypeList,
    private val worldRepo: WorldRepository,
) {
    public fun attack(access: StandardNpcAccess, target: Player, attack: CombatAttack.NpcAttack) {
        when (attack) {
            is CombatAttack.NpcMelee -> access.attackMelee(target, attack)
            is CombatAttack.NpcRanged -> access.attackRanged(target, attack)
            is CombatAttack.NpcMagic -> access.attackMagic(target, attack)
        }
    }

    /**
     * Runs the checks and side effects every npc attack shares, in the order the melee path
     * established, and starts the attack fx.
     *
     * @return `false` when the npc must not attack this cycle. The caller returns immediately; any
     *   mode reset has already been applied.
     */
    private fun StandardNpcAccess.beginAttack(target: Player): Boolean {
        if (!canAttack(target)) {
            resetMode()
            return false
        }

        // Note: We do not need to explicitly call `opplayer2` because npcs will automatically
        // repeat their last interaction until it is canceled (e.g., by changing their `npcmode`).
        if (actionDelay > mapClock) {
            return false
        }

        if (!npc.isInCombat()) {
            resetMode()
            return false
        }

        val attackRate = npc.attackRate()
        actionDelay = mapClock + attackRate

        val attackAnim = npc.visType.param(params.attack_anim)
        val attackSound = npc.visType.paramOrNull(params.attack_sound)

        anim(attackAnim)
        attackSound?.let(target::soundSynth)
        return true
    }

    private fun StandardNpcAccess.attackMelee(target: Player, attack: CombatAttack.NpcMelee) {
        if (!beginAttack(target)) {
            return
        }

        val successfulHit = accuracy.rollMeleeAccuracy(npc, target, attack.type, random)

        val damage =
            if (successfulHit) {
                val maxHit = maxHits.getMeleeMaxHit(npc, target, attack.type)
                random.of(0..maxHit)
            } else {
                0
            }

        setAttackVars(target)

        // Note: Retaliation must be queued _before_ the hit. If queued after, every hit would
        // trigger the "speed-up" death mechanic, since the hit queues would no longer be the
        // last entries in the queue list at the time of processing.
        target.queueCombatRetaliate(npc)

        target.queueHit(npc, 1, HitType.Melee, damage)
        target.combatPlayDefendAnim(objTypes)
    }

    private fun StandardNpcAccess.attackRanged(target: Player, attack: CombatAttack.NpcRanged) {
        if (!beginAttack(target)) {
            return
        }

        // Unlike melee, a projectile attack has no sane fallback: the hit delay is read back off
        // the projectile, so an npc that declares no flight profile has no defensible tick to land
        // its damage on. Rather than invent one, stop and let the npc fall back to its default
        // mode.
        val projanim = spawnAttackProjectile(target)
        if (projanim == null) {
            resetMode()
            return
        }
        val (serverDelay, clientDelay) = projanim.durations

        // Note: `attack.type` does not reach the formulae. `NvPRangedAccuracy` rolls against the
        // player's aggregate ranged defence bonus and `NvPRangedMaxHit` against
        // `params.ranged_strength`; neither reads light/standard/heavy for an npc source. The
        // field is kept because it is the right shape, not because it selects anything today.
        val successfulHit = accuracy.rollRangedAccuracy(npc, target, random)

        val damage =
            if (successfulHit) {
                val maxHit = maxHits.getRangedMaxHit(npc, target)
                random.of(0..maxHit)
            } else {
                0
            }

        setAttackVars(target)

        target.queueCombatRetaliate(npc, serverDelay)

        target.queueHit(npc, serverDelay, HitType.Ranged, damage)
        target.combatPlayDefendAnim(objTypes, clientDelay)
    }

    private fun StandardNpcAccess.attackMagic(target: Player, attack: CombatAttack.NpcMagic) {
        if (!beginAttack(target)) {
            return
        }

        val projanim = spawnAttackProjectile(target)
        if (projanim == null) {
            resetMode()
            return
        }
        val (serverDelay, clientDelay) = projanim.durations

        val successfulHit = accuracy.rollMagicAccuracy(npc, target, random)

        setAttackVars(target)

        if (!successfulHit) {
            // Splash. Mirrors `PlayerAttackManager.playMagicSplashFx`: the failed-spell impact and
            // its sound land with the projectile, but retaliation is pulled forward to the next
            // cycle rather than waiting out the flight, and a 0 is still queued so the player sees
            // a hitsplat.
            target.spotanim(spotanims.failedspell_impact, delay = clientDelay, height = 124)
            worldRepo.soundArea(target.coords, synths.spellfail, delay = clientDelay, radius = 10)
            target.queueCombatRetaliate(npc)
            target.queueHit(npc, serverDelay, HitType.Magic, damage = 0)
            return
        }

        // `attack.maxHit` overrides the formula for npcs whose magic damage is authored rather
        // than derived; `0` means "use the formula". `NvPMagicMaxHit` reads the npc's magic level
        // and `params.npc_magic_damage_bonus`.
        val maxHit =
            if (attack.maxHit > 0) {
                attack.maxHit
            } else {
                maxHits.getMagicMaxHit(npc, target)
            }
        val damage = random.of(0..maxHit)

        target.queueCombatRetaliate(npc, serverDelay)

        target.queueHit(npc, serverDelay, HitType.Magic, damage)
        target.combatPlayDefendAnim(objTypes, clientDelay)
    }

    /**
     * Spawns the npc's declared attack projectile at [target], returning it so the caller can read
     * its flight time back out.
     *
     * Both params are required: `proj_travel` is the spotanim that flies and `proj_type` is the
     * flight profile the hit delay is derived from. This is the same discipline the player ranged
     * path applies to ammunition -- without both, there is no projectile and no honest delay.
     */
    private fun StandardNpcAccess.spawnAttackProjectile(target: Player): ProjAnim? {
        val travelSpotanim = npc.visType.paramOrNull(params.proj_travel) ?: return null
        val projanimType = npc.visType.paramOrNull(params.proj_type) ?: return null

        // Unlike the player path, which sends a "null" (-1) spotanim when a weapon has no
        // `proj_launch`, an npc with none simply skips the launch fx.
        val launchSpotanim = npc.visType.paramOrNull(params.proj_launch)
        if (launchSpotanim != null) {
            spotanim(launchSpotanim, height = 96, slot = constants.spotanim_slot_combat)
        }

        return worldRepo.projAnim(npc, target, travelSpotanim, projanimType)
    }

    private fun canAttack(target: Player): Boolean {
        return target.isValidTarget()
    }

    private fun StandardNpcAccess.setAttackVars(target: Player) {
        npc.lastAttack = mapClock
        npc.attackingPlayer = target.uid
        target.lastCombat = mapClock
        target.aggressiveNpc = npc.uid
    }
}
