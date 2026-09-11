package org.rsmod.api.toxins

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/**
 * Poison and venom on players.
 *
 * The whole state is one int in varp 102 (`poison`), encoded exactly as the client's HP orb reads
 * it, so there is nothing else to keep in step:
 * - `0`: clean.
 * - `1 until 1_000_000`: poisoned, where the value is the *severity*. Each hit deals `ceil(severity
 *   / 5)` and takes one off the severity, so a severity-30 poison hits 6 five times, then 5, and
 *   wears off at zero.
 * - `>= 1_000_000`: envenomed. The first hit deals 6, and each hit after it two more, up to 20. The
 *   value goes up by one per hit and the damage is read back from it.
 * - `< 0`: immune. The value counts back up to zero, one per hit interval. Below
 *   [VENOM_IMMUNITY_FLOOR] it also stops venom, which is how an antidote++ protects from venom for
 *   the last two intervals of its twelve minutes and no longer.
 *
 * Every hit is typeless and sourceless, so protection prayers, armour and vengeance all ignore it,
 * as on live.
 */
public class Toxins @Inject constructor(private val hitProcessor: InstantPlayerHitProcessor) {
    public fun isPoisoned(player: Player): Boolean = state(player) in 1 until VENOM_THRESHOLD

    public fun isVenomed(player: Player): Boolean = state(player) >= VENOM_THRESHOLD

    public fun isPoisonImmune(player: Player): Boolean = state(player) < 0

    public fun isVenomImmune(player: Player): Boolean = state(player) < VENOM_IMMUNITY_FLOOR

    /**
     * Poisons [player] at [severity]. Does nothing, and returns `false`, if they are immune,
     * already envenomed, or already poisoned at least as hard.
     */
    public fun poison(player: Player, severity: Int): Boolean {
        require(severity in 1 until VENOM_THRESHOLD) { "Severity out of range: $severity" }
        val current = state(player)
        if (current < 0 || current >= severity) {
            return false
        }
        write(player, severity)
        player.mes("You have been poisoned!")
        startHitting(player)
        return true
    }

    /**
     * Envenoms [player]. Poison immunity does not stop venom - only venom immunity does - and a
     * poisoned player is upgraded rather than left alone. Returns `false` if nothing changed.
     */
    public fun envenom(player: Player): Boolean {
        val current = state(player)
        if (current < VENOM_IMMUNITY_FLOOR || current >= VENOM_THRESHOLD) {
            return false
        }
        write(player, VENOM_THRESHOLD)
        player.mes("You have been envenomed!")
        startHitting(player)
        return true
    }

    /**
     * What drinking one dose of [cure] does.
     *
     * A cure that cannot touch venom *converts* it instead: the venom becomes a poison that hits as
     * hard as the venom was about to, and no immunity is granted until a second dose cures that.
     * Otherwise the toxin is cleared and the immunity set, keeping whichever is longer if the
     * player was already immune.
     */
    public fun cure(player: Player, cure: ToxinCure) {
        val current = state(player)
        if (current >= VENOM_THRESHOLD && !cure.curesVenom) {
            write(player, venomDamage(current) * POISON_SEVERITY_PER_DAMAGE)
            return
        }
        val immunity = -cure.immunityIntervals
        write(player, if (current < 0) min(current, immunity) else immunity)
        player.softTimer(timers.toxins, HIT_INTERVAL)
    }

    /** Clears poison, venom and immunity alike. Death does this. */
    public fun clear(player: Player) {
        write(player, 0)
        player.clearSoftTimer(timers.toxins)
    }

    /** Picks the timer back up after a login; timers are not part of the save, the varp is. */
    public fun resume(player: Player) {
        if (state(player) != 0) {
            player.softTimer(timers.toxins, HIT_INTERVAL)
        }
    }

    /** One interval: a hit if poisoned or envenomed, a step toward zero if immune. */
    internal fun tick(player: Player) {
        val current = state(player)
        when {
            current == 0 -> player.clearSoftTimer(timers.toxins)
            current < 0 -> {
                write(player, current + 1)
                if (current + 1 == 0) {
                    player.clearSoftTimer(timers.toxins)
                }
            }
            current >= VENOM_THRESHOLD -> {
                // The varp keeps climbing past the cap on live too; the cap is on the damage.
                write(player, min(current + 1, VENOM_THRESHOLD + VENOM_STATE_CAP))
                hit(player, venomDamage(current), venom = true)
                player.softTimer(timers.toxins, HIT_INTERVAL)
            }
            else -> {
                write(player, current - 1)
                hit(player, poisonDamage(current), venom = false)
                if (current - 1 == 0) {
                    player.clearSoftTimer(timers.toxins)
                } else {
                    player.softTimer(timers.toxins, HIT_INTERVAL)
                }
            }
        }
    }

    private fun hit(player: Player, damage: Int, venom: Boolean) {
        if (player.hitpoints <= 0) {
            return
        }
        player.takeInstantHit(
            type = HitType.Typeless,
            damage = min(damage, player.hitpoints),
            processor = hitProcessor,
            hitmark = if (venom) hitmark_groups.venom else hitmark_groups.poison_damage,
        )
    }

    /**
     * The first hit lands on the next tick rather than a full interval later, which is what makes a
     * fresh poison feel like it came from the attack that caused it.
     */
    private fun startHitting(player: Player) {
        player.softTimer(timers.toxins, 1)
    }

    private fun state(player: Player): Int = player.vars[ToxinVarps.poison]

    private fun write(player: Player, value: Int) {
        VarPlayerIntMapSetter.set(player, ToxinVarps.poison, value)
    }

    public companion object {
        /** Thirty ticks, eighteen seconds, for poison and venom alike. */
        public const val HIT_INTERVAL: Int = 30

        /** The orb's own threshold: `orbs_update_health` compares varp 102 against exactly this. */
        public const val VENOM_THRESHOLD: Int = 1_000_000

        /** Immunity below this stops venom as well as poison. */
        public const val VENOM_IMMUNITY_FLOOR: Int = -38

        private const val VENOM_FIRST_HIT = 6
        private const val VENOM_STEP = 2
        private const val VENOM_MAX_HIT = 20

        /** Enough for the damage to reach its cap and stay there; the rest is just a bigger int. */
        private const val VENOM_STATE_CAP = 100

        private const val POISON_SEVERITY_PER_DAMAGE = 5

        public fun venomDamage(state: Int): Int =
            min(VENOM_MAX_HIT, VENOM_FIRST_HIT + (state - VENOM_THRESHOLD) * VENOM_STEP)

        public fun poisonDamage(severity: Int): Int =
            (severity + POISON_SEVERITY_PER_DAMAGE - 1) / POISON_SEVERITY_PER_DAMAGE
    }
}

/**
 * The cures, by how long they protect, in [Toxins.HIT_INTERVAL]s of eighteen seconds.
 *
 * The antipoison tiers are the wiki's durations: 1.5, 6, 9 and 12 minutes, and six for sanfew. The
 * anti-venoms sit past [Toxins.VENOM_IMMUNITY_FLOOR], so they protect from venom for their first
 * few intervals (about 54 seconds, three minutes and six minutes) and from poison for the 38 after
 * that. That tail is the varp encoding speaking rather than a number anyone chose.
 */
public enum class ToxinCure(public val immunityIntervals: Int, public val curesVenom: Boolean) {
    Antipoison(immunityIntervals = 5, curesVenom = false),
    Superantipoison(immunityIntervals = 20, curesVenom = false),
    AntidotePlus(immunityIntervals = 30, curesVenom = false),
    AntidotePlusPlus(immunityIntervals = 40, curesVenom = false),
    Sanfew(immunityIntervals = 20, curesVenom = false),
    Antivenom(immunityIntervals = 41, curesVenom = true),
    AntivenomPlus(immunityIntervals = 48, curesVenom = true),
    ExtendedAntivenomPlus(immunityIntervals = 59, curesVenom = true),
}
