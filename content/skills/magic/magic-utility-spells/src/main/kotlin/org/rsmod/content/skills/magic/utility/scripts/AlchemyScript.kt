package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.content.skills.magic.utility.configs.utility_synths
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.isType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Low and High Level Alchemy: one item into coins, at 40% and 60% of its shop value.
 *
 * `params.no_alchemy` is the cache's own "can't alchemise" tag; coins are refused by name. One item
 * is converted per cast even from a stack, and the cast blocks for the official three and five
 * ticks through `actionDelay`.
 */
class AlchemyScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val perks: Perks,
) : PluginScript() {
    private class Alchemy(
        val percent: Int,
        val delay: Int,
        val seq: SeqType,
        val spotanim: SpotanimType,
        val sound: SynthType,
    )

    override fun ScriptContext.startup() {
        val low = spellbooks.spellFor(objs.spell_low_alchemy)
        val high = spellbooks.spellFor(objs.spell_high_alchemy)
        val lowAlchemy =
            Alchemy(
                40,
                3,
                utility_seqs.low_alchemy,
                utility_spotanims.low_alchemy,
                utility_synths.low_alchemy,
            )
        val highAlchemy =
            Alchemy(
                60,
                5,
                utility_seqs.high_alchemy,
                utility_spotanims.high_alchemy,
                utility_synths.high_alchemy,
            )
        onSpellOnInvObj(low, protectedAccess, eventBus) { slot, obj ->
            alch(low, lowAlchemy, slot, obj)
        }
        onSpellOnInvObj(high, protectedAccess, eventBus) { slot, obj ->
            alch(high, highAlchemy, slot, obj)
        }
    }

    private fun ProtectedAccess.alch(
        spell: MagicSpell,
        alchemy: Alchemy,
        slot: Int,
        obj: UnpackedObjType,
    ) {
        if (actionDelay > mapClock) {
            return
        }
        if (!slotHolds(slot, obj)) {
            return
        }
        // The Golden God league relic: no runes or level, 15% more coins, and a 65% chance the
        // item survives the cast.
        val golden = perks.has(player, Perk.GoldenAlchemy)
        val cast =
            casting.attemptUtility(this, spell, free = golden) {
                when {
                    obj.isType(objs.coins) -> {
                        mes("Coins are already made of gold.")
                        false
                    }
                    (obj.paramMap?.getOrNull(params.no_alchemy) ?: 0) != 0 -> {
                        mes("You can't alchemise that item.")
                        false
                    }
                    !hasRoomForCoins(obj, slot) -> {
                        mes("You don't have enough inventory space to do that.")
                        false
                    }
                    else -> true
                }
            }
        if (!cast) {
            return
        }
        val percent = if (golden) alchemy.percent * GOLDEN_BONUS_PERCENT / 100 else alchemy.percent
        val value = obj.cost * percent / 100
        val keepItem = golden && random.of(100) < GOLDEN_KEEP_CHANCE
        if (!keepItem) {
            invDel(inv, obj, count = 1, slot = slot)
        }
        if (value > 0) {
            invAdd(inv, objs.coins, count = value)
        }
        anim(alchemy.seq)
        spotanim(alchemy.spotanim, height = 92)
        soundSynth(alchemy.sound)
        actionDelay = mapClock + alchemy.delay
    }

    /** A stack of two or more leaves its slot occupied, so the coins need a slot of their own. */
    private fun ProtectedAccess.hasRoomForCoins(obj: UnpackedObjType, slot: Int): Boolean {
        val count = inv[slot]?.count ?: 0
        if (count <= 1 || !obj.stackable) {
            return true
        }
        return !inv.isFull() || inv.count(objTypes[objs.coins]) > 0
    }

    private companion object {
        /** Golden God pays this percentage of the spell's usual value: 15% more. */
        const val GOLDEN_BONUS_PERCENT = 115

        /** Golden God's chance, out of 100, that the item survives the cast. */
        const val GOLDEN_KEEP_CHANCE = 65
    }
}
