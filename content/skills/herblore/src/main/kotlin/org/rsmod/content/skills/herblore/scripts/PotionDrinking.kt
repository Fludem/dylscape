package org.rsmod.content.skills.herblore.scripts

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.skills.herblore.configs.EffectKind
import org.rsmod.content.skills.herblore.configs.HerbloreSeqs
import org.rsmod.content.skills.herblore.configs.HerbloreTimers
import org.rsmod.content.skills.herblore.configs.Potion
import org.rsmod.content.skills.herblore.configs.Potions
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Drinking a dosed potion: one script for every potion in the game.
 *
 * The binding is on the `potion` content group rather than on two hundred obj ids.
 * [org.rsmod.content.skills.herblore.configs.PotionEditor] stamps that group onto exactly the objs
 * in [Potions], `HeldInteractions.opHeld1` dispatches on it, and the bank reads the same group --
 * `BankInvScript` treats `content.potion` exactly as it treats `content.food` -- so tagging lights
 * up drinking from the bank interface for free.
 *
 * Timing is `Consume`'s, deliberately and not by accident: potions ride the same three-tick food
 * clock, so a player cannot sip and eat in the same tick, and rather than *waiting* on the weapon
 * cooldown they *push it back*. Sharing one field with combat would let a slow weapon block a
 * prayer restore, which is the only situation where a potion matters at all.
 *
 * Nothing here suspends. Every sip resolves inside the tick it started in and hands control
 * straight back, so a player can walk off, click again or die mid-stack without the script needing
 * to unwind -- the same reason `Consume` and `BuryBones` do not suspend either.
 */
class PotionDrinking @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(content.potion) { drink(it.type, it.slot) }

        onPlayerSoftTimer(HerbloreTimers.stamina_expire) {
            // A soft-timer hook hands back a plain `Player`, not `ProtectedAccess`, and
            // `VarPlayerIntMap` has no set operator - this is the documented way to write a var
            // from outside a protected block.
            VarPlayerIntMapSetter.set(player, varbits.stamina_active, 0)
            VarPlayerIntMapSetter.set(player, varbits.stamina_duration, 0)
        }
    }

    private fun ProtectedAccess.drink(type: UnpackedObjType, slot: Int) {
        val potion = Potions.byObjId[type.id]
        if (potion == null) {
            // Tagged into the potion group but missing from the table. That is a bug on our side
            // rather than something to invent behaviour for, so it falls back to what an untagged
            // obj does. `PotionCoverageTest` is what stops this ever being reachable.
            mes(constants.dm_default, ChatType.Engine)
            return
        }

        // Swallowed rather than messaged: the client fires an op per click, and live says nothing
        // about the clicks that land inside the cooldown either.
        if (foodDelay > mapClock) {
            return
        }

        // Consume first - nothing below can pay out on a transaction that did not happen. The next
        // dose goes into the *same* slot; `invReplace` would put it in the first free slot and
        // shuffle the inventory under the player between sips.
        val consumed = invReplaceSlot(inv, slot, count = 1, replacement = potion.next)
        if (!consumed.success) {
            return
        }

        val until = mapClock + DRINK_TICKS
        foodDelay = until
        // `max`, not assignment: a player who has just swung a godsword keeps the longer delay,
        // while an idle one picks up a fresh three ticks. Assigning would hand a free attack-speed
        // reset to anyone holding a potion.
        actionDelay = max(actionDelay, until)
        anim(HerbloreSeqs.drink)

        mes("You drink some of your ${type.name.lowercase()}.")
        applyEffects(potion)
        if (potion.heal > 0 || potion.healPercent > 0) {
            statHeal(stats.hitpoints, potion.heal, potion.healPercent)
        }
        restoreEnergy(potion.energy)
        applyStamina(potion)
        mes(dosesLeft(potion))
        potion.inertMessage?.let(::mes)
    }

    /** The line live prints under the drink message, and the reason [Potion.dose] is stored. */
    private fun dosesLeft(potion: Potion): String =
        when (val left = potion.dose - 1) {
            0 -> "You have finished your potion."
            1 -> "You have 1 dose of potion left."
            else -> "You have $left doses of potion left."
        }

    private fun ProtectedAccess.applyEffects(potion: Potion) {
        for (effect in potion.effects) {
            when (effect.kind) {
                EffectKind.Boost -> statBoost(effect.stat, effect.constant, effect.percent)
                EffectKind.Drain -> statDrain(effect.stat, effect.constant, effect.percent)
                // `statHeal` clamps at the base level, which is exactly what a restore means and
                // exactly what `statBoost` would get wrong.
                EffectKind.Restore -> statHeal(effect.stat, effect.constant, effect.percent)
            }
        }
    }

    /**
     * Run energy is stored in hundredths of a percent and, unlike a stat, is not flushed to the
     * client by the thing that changes it - `Consume.restoreEnergy` pushes it by hand for the same
     * reason.
     */
    private fun ProtectedAccess.restoreEnergy(percent: Int) {
        if (percent == 0) {
            return
        }
        val restored = min(constants.run_max_energy, player.runEnergy + percent * ENERGY_PERCENT)
        player.runEnergy = restored
        UpdateRun.energy(player, restored)
    }

    /**
     * Stamina, which is the one timed potion effect this engine can actually honour.
     *
     * `PlayerRunUpdateProcessor.decreaseRunEnergy` already consults `varbits.stamina_active` and
     * cuts the drain to 30%, so that half is the real mechanic rather than a cosmetic varbit.
     *
     * The remaining duration is kept in `varbits.stamina_duration`, which is **five bits wide** and
     * therefore cannot hold a tick count -- writing 200 into it throws `Varbit overflow`, which is
     * how this was found. It holds coarse units instead, and a soft timer converts back to ticks.
     * Storing it in the varbit rather than in a field is what makes doses stack correctly across a
     * relog, since the underlying varp is `Perm` scope.
     *
     * **The client's own reading of that unit is unverified.** Thirty-one units at
     * [STAMINA_TICKS_PER_UNIT] ticks each comes to just under the ten minutes live caps a stamina
     * at, which is a good sign and not a proof. If the buff timer in a real client counts wrong,
     * this constant is the thing to change; the server-side expiry does not depend on it.
     */
    private fun ProtectedAccess.applyStamina(potion: Potion) {
        if (potion.staminaUnits == 0) {
            return
        }
        val remaining = if (vars[varbits.stamina_active] == 1) vars[varbits.stamina_duration] else 0
        val total = min(remaining + potion.staminaUnits, STAMINA_UNITS_MAX)
        vars[varbits.stamina_active] = 1
        vars[varbits.stamina_duration] = total
        softTimer(HerbloreTimers.stamina_expire, total * STAMINA_TICKS_PER_UNIT)
    }

    private companion object {
        /** The same three ticks food rides, so a potion and a shark cannot share a tick. */
        const val DRINK_TICKS = 3

        /** Run energy is stored in hundredths of a percent. */
        const val ENERGY_PERCENT = 100

        /**
         * `stamina_duration` is bits 8..12 of varp `dragonresist`, so it holds 0..31 and nothing
         * larger. Thirty-one units of [STAMINA_TICKS_PER_UNIT] ticks is 992 ticks, just under the
         * ten minutes live caps a stamina at.
         */
        const val STAMINA_UNITS_MAX = 31

        /** 32 ticks, a little over nineteen seconds. See [STAMINA_UNITS_MAX]. */
        const val STAMINA_TICKS_PER_UNIT = 32
    }
}
