package org.rsmod.content.other.consumables.scripts

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.other.consumables.configs.ConsumableQueues
import org.rsmod.content.other.consumables.configs.ConsumableSeqs
import org.rsmod.content.other.consumables.configs.Consumables
import org.rsmod.content.other.consumables.configs.Edible
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Eating and drinking: one script for every consumable in the game.
 *
 * The main binding is on the `food` content group rather than on several hundred obj ids.
 * `ConsumableEditor` stamps that group onto exactly the objs in [Consumables],
 * `HeldInteractions.opHeld1` dispatches on it, and the bank's bankside consumable op already routes
 * to `opHeld1` for anything wearing it - so tagging the group lights up eating from the bank
 * interface for free.
 *
 * The group binding only covers op1, and 36 consumables do not put their verb there: pineapples,
 * watermelons, dwellberries and the Hosidius food all carry `Eat` on op4, and a handful sit on op2,
 * op3 or op5. Those are bound per obj, from the same table, so the two cannot drift. They are bound
 * *per obj* rather than per group on purpose - `onOpHeld2(content)` would replace the default wield
 * handling for every food at once, and `onOpHeld5(content)` the default drop handling.
 *
 * The timing is the OSRS one, and it is deliberately not [ProtectedAccess.actionDelay]. Eating runs
 * on its own three-tick clock, and rather than *waiting* on the weapon cooldown it *pushes it
 * back*: a player mid-fight eats on the tick they click and loses the next three ticks of attacks.
 * Sharing one field with combat would let a slow weapon block a heal, which is the only situation
 * where food matters at all.
 *
 * Nothing here suspends. Every consumable resolves inside the tick it started in and hands control
 * straight back, so a player can walk off, click again or die mid-stack without the script needing
 * to unwind - the same reason `BuryBones` rides a delay rather than a coroutine. The one exception
 * is the hunter meats' second helping, which goes on a queue.
 */
class Consume @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(content.food) { consume(it.type, it.slot) }

        for (row in Consumables.rows) {
            when (row.obj.internalName) {
                in OP2_OBJS -> onOpHeld2(row.obj) { consume(it.type, it.slot) }
                in OP3_OBJS -> onOpHeld3(row.obj) { consume(it.type, it.slot) }
                in OP4_OBJS -> onOpHeld4(row.obj) { consume(it.type, it.slot) }
                in OP5_OBJS -> onOpHeld5(row.obj) { consume(it.type, it.slot) }
            }
        }

        onPlayerQueueWithArgs<Int>(ConsumableQueues.delayed_heal) { event ->
            statHeal(stats.hitpoints, constant = event.args, percent = 0)
        }
    }

    private fun ProtectedAccess.consume(type: UnpackedObjType, slot: Int) {
        val edible = Consumables.byObjId[type.id]
        if (edible == null) {
            // Tagged into the food group but missing from the table. That is a bug on our side
            // rather than something to invent behaviour for, so it falls back to what an untagged
            // obj does. `ConsumableCoverageTest` is what stops this ever being reachable.
            mes(constants.dm_default, ChatType.Engine)
            return
        }

        // A refusal costs nothing: no item, no cooldown, no heal. Checked before the cooldown so
        // that spam-clicking something inedible keeps telling you why.
        if (edible.refuses) {
            edible.message?.let(::mes)
            return
        }

        // Swallowed rather than messaged: the client fires an op per click, and live says nothing
        // about the clicks that land inside the cooldown either.
        val cooldown = if (edible.combo) comboFoodDelay else foodDelay
        if (cooldown > mapClock) {
            return
        }

        // Consume first - nothing below can pay out on a transaction that did not happen. A
        // multi-bite food, or a drink handing back its vessel, goes into the *same* slot;
        // `invReplace` would put the half-eaten cake in the first free slot and shuffle the
        // inventory under the player between bites.
        val next = edible.next
        val consumed =
            if (next != null) {
                invReplaceSlot(inv, slot, count = 1, replacement = next)
            } else {
                invDel(inv, type, count = 1, slot = slot)
            }
        if (!consumed.success) {
            return
        }

        startCooldown(edible)
        anim(edible.seq)

        mes(edible.message ?: defaultMessage(edible, type))
        edible.extraMessage?.let(::mes)

        restore(edible)
        applyEffects(edible)
        restoreEnergy(edible.energy)
        applyDamage(edible)
    }

    /**
     * Combo food rides its own clock, and the two clocks are fully independent.
     *
     * That independence *is* combo-eating: a karambwan checks and sets only
     * [ProtectedAccess.comboFoodDelay], so it goes down in the same tick as a shark, while neither
     * can be chained with itself. Having ordinary food stamp the combo clock as well would block
     * the karambwan and quietly undo the whole mechanic.
     */
    private fun ProtectedAccess.startCooldown(edible: Edible) {
        val until = mapClock + CONSUME_TICKS
        if (edible.combo) {
            comboFoodDelay = until
        } else {
            foodDelay = until
        }
        // `max`, not assignment: a player who has just swung a godsword keeps the longer delay,
        // while an idle one picks up a fresh three ticks. Assigning would hand a free attack-speed
        // reset to anyone holding food.
        actionDelay = max(actionDelay, until)
    }

    /**
     * Hitpoints, and the one place the two healing primitives have to differ.
     *
     * `statHeal` clamps at the base level, which is what every ordinary food wants. Anglerfish and
     * the honey locust do not: they heal *past* it, up to a bonus that scales with the eater's
     * Hitpoints level. `statAdd` is the primitive that permits that, but it caps at 255 rather than
     * at the ceiling we actually want, so the gain is worked out here and handed over as a flat
     * constant.
     */
    private fun ProtectedAccess.restore(edible: Edible) {
        val range = edible.healRange
        val restored =
            when {
                range != null -> random.of(range.first, range.last)
                else -> edible.heal
            }
        if (restored == 0 && edible.healPercent == 0) {
            return
        }

        val base = statBase(stats.hitpoints)
        val bonus = edible.overheal(base)
        if (bonus == 0) {
            statHeal(stats.hitpoints, constant = restored, percent = edible.healPercent)
        } else {
            val current = stat(stats.hitpoints)
            val total = restored + (base * edible.healPercent) / 100
            val gain = (min(current + total, base + bonus) - current).coerceAtLeast(0)
            statAdd(stats.hitpoints, constant = gain, percent = 0)
        }

        if (edible.delayedHeal > 0) {
            queue(ConsumableQueues.delayed_heal, edible.delayedHealTicks, edible.delayedHeal)
        }
    }

    private fun ProtectedAccess.applyEffects(edible: Edible) {
        for (effect in edible.effects) {
            if (effect.boost) {
                statBoost(effect.stat, effect.constant, effect.percent)
            } else {
                statDrain(effect.stat, effect.constant, effect.percent)
            }
        }
    }

    /**
     * Run energy is stored in hundredths of a percent and, unlike a stat, is not flushed to the
     * client by the thing that changes it - `DevCommands` pushes it by hand for the same reason.
     */
    private fun ProtectedAccess.restoreEnergy(percent: Int) {
        if (percent == 0) {
            return
        }
        val restored = min(constants.run_max_energy, player.runEnergy + percent * ENERGY_PERCENT)
        player.runEnergy = restored
        UpdateRun.energy(player, restored)
    }

    private fun ProtectedAccess.applyDamage(edible: Edible) {
        val flat = edible.damage
        val scaled = stat(stats.hitpoints) * edible.damagePercent / 100
        val damage = flat + scaled
        if (damage > 0) {
            takeInstantHit(HitType.Typeless, damage)
        }
    }

    /**
     * "You eat the shrimps.", "You drink the beer."
     *
     * The verb comes off the animation because that is the thing that already distinguishes the
     * two: anything raising a vessel to the mouth is drunk, everything else is eaten. Rows with
     * their own wording override this entirely.
     */
    private fun defaultMessage(edible: Edible, type: UnpackedObjType): String {
        val verb = if (edible.seq == ConsumableSeqs.drink) "drink" else "eat"
        return "You $verb the ${type.name.lowercase()}."
    }

    private companion object {
        /** Three ticks between mouthfuls, which is live's 1.8-second cadence. */
        private const val CONSUME_TICKS = 3

        /** Run energy is stored in hundredths of a percent. */
        private const val ENERGY_PERCENT = 100

        /**
         * The consumables whose verb is not on op1.
         *
         * Read out of the cache by `EdibleObjDump`, not assumed: op slots are not uniform, and a
         * pineapple carrying `Eat` on op4 would simply never reach the group binding.
         */
        private val OP2_OBJS = setOf("cadava", "easter22_egg_melted")

        private val OP3_OBJS =
            setOf(
                "spoilt_cocktail",
                "spoilt_cocktail_fruity",
                "spoilt_cocktail_creamy",
                "mdaughter_white_pearl_fruit",
                "easter22_bucket_milk",
                "bowl_damiana_water",
                "bowl_damiana_tea",
                "bowl_damiana_tea_milky",
            )

        private val OP4_OBJS =
            setOf(
                "karamja_rum",
                "tentipineapple",
                "pineapple",
                "dwellberries",
                "nightshade",
                "watermelon",
                "village_rare_tuber",
                "grim_turnip",
                "snakeboss_eel",
                "hosidius_servery_meat_pie",
                "hosidius_servery_plain_pizza",
                "hosidius_servery_pineapple_pizza",
                "hosidius_servery_cooked_meat",
                "hosidius_servery_potato",
                "hosidius_servery_stew",
                "hosidius_tithe_fruit_a",
                "hosidius_tithe_fruit_b",
                "hosidius_tithe_fruit_c",
                "infernal_eel",
                "my2arm_potion",
                "easter22_hot_sauce",
                "easter22_special_kebab",
                "easter22_potion",
                "stackable_nightshade",
                "caerula_berries",
            )

        /** The joke `Orange` that is worn as a hat; the cache puts its `Eat` on the last slot. */
        private val OP5_OBJS = setOf("orange_hat")
    }
}
