package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.content.skills.magic.utility.configs.Conversion
import org.rsmod.content.skills.magic.utility.configs.LunarTables
import org.rsmod.content.skills.magic.utility.configs.utility_objs
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Lunar inventory spells: Humidify, Superglass Make, Spin Flax, String Jewellery, Tan Leather,
 * Plank Make, Hunter Kit, Recharge Dragonstone and Magic Imbue.
 *
 * All of them decide what they would change *before* the runes are paid (inside the cast's
 * validation), so an empty inventory costs nothing and gets the spell's own message. The
 * conversions themselves are one-for-one slot swaps from [LunarTables]. Plank Make is the one cast
 * on a specific item (its component carries `TgtCom`); the rest are self-casts.
 *
 * No Lunar spell has a named sound effect in this cache, so these are silent rather than guessed.
 */
class LunarSkillingScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val random: GameRandom,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val humidify = spellbooks.spellFor(objs.spell_humidify)
        onSelfSpell(humidify, protectedAccess, eventBus) {
            convertAll(
                spell = humidify,
                table = LunarTables.humidify,
                max = Int.MAX_VALUE,
                nothing = "You have nothing to humidify.",
                seq = utility_seqs.humidify,
                spotanim = utility_spotanims.humidify,
            )
        }

        val tanLeather = spellbooks.spellFor(objs.spell_tan_leather)
        onSelfSpell(tanLeather, protectedAccess, eventBus) {
            convertAll(
                spell = tanLeather,
                table = LunarTables.tanLeather,
                max = LunarTables.TAN_LEATHER_MAX,
                nothing = "You have no hides to tan.",
                seq = utility_seqs.lunar_cast,
                spotanim = utility_spotanims.tan_leather,
            )
        }

        val stringJewellery = spellbooks.spellFor(objs.spell_string_jewelry)
        onSelfSpell(stringJewellery, protectedAccess, eventBus) {
            val strung =
                convertAll(
                    spell = stringJewellery,
                    table = LunarTables.stringJewellery,
                    max = Int.MAX_VALUE,
                    nothing = "You have no unstrung amulets to string.",
                    seq = utility_seqs.lunar_cast,
                    spotanim = utility_spotanims.string_jewellery,
                )
            giveCraftingXp(strung * LunarTables.STRING_JEWELLERY_XP_PER_AMULET)
        }

        val rechargeDragonstone = spellbooks.spellFor(objs.spell_recharge_dragonstone)
        onSelfSpell(rechargeDragonstone, protectedAccess, eventBus) {
            convertAll(
                spell = rechargeDragonstone,
                table = LunarTables.rechargeDragonstone,
                max = Int.MAX_VALUE,
                nothing = "You have no dragonstone jewellery to recharge.",
                seq = utility_seqs.lunar_cast,
                spotanim = utility_spotanims.recharge_dragonstone,
            )
        }

        val spinFlax = spellbooks.spellFor(objs.spell_spin_flax)
        onSelfSpell(spinFlax, protectedAccess, eventBus) {
            val spun =
                convertAll(
                    spell = spinFlax,
                    table = listOf(Conversion(utility_objs.flax, utility_objs.bow_string)),
                    max = LunarTables.SPIN_FLAX_MAX,
                    nothing = "You need some flax to cast this spell.",
                    seq = utility_seqs.lunar_cast,
                    spotanim = utility_spotanims.spin_flax,
                )
            giveCraftingXp(spun * LunarTables.SPIN_FLAX_XP_PER_STRING)
        }

        val superglass = spellbooks.spellFor(objs.spell_superglass)
        onSelfSpell(superglass, protectedAccess, eventBus) { superglassMake(superglass) }

        val plankMake = spellbooks.spellFor(objs.spell_plank_make)
        onSpellOnInvObj(plankMake, protectedAccess, eventBus) { slot, obj ->
            plankMake(plankMake, slot, obj)
        }

        val hunterKit = spellbooks.spellFor(objs.spell_hunter_kit)
        onSelfSpell(hunterKit, protectedAccess, eventBus) {
            val cast =
                casting.attemptUtility(this, hunterKit) {
                    if (inv.isFull()) {
                        mes("You don't have enough inventory space for a hunter kit.")
                    }
                    !inv.isFull()
                }
            if (cast) {
                anim(utility_seqs.hunter_kit)
                invAdd(inv, utility_objs.hunter_kit)
            }
        }

        val magicImbue = spellbooks.spellFor(objs.spell_magic_imbue)
        onSelfSpell(magicImbue, protectedAccess, eventBus) {
            if (casting.attemptUtility(this, magicImbue)) {
                anim(utility_seqs.lunar_cast)
                spotanim(utility_spotanims.magic_imbue, height = 92)
                player.softTimer(magic_timers.imbue, IMBUE_TICKS)
                mes("You are charged to combine runes!")
            }
        }
    }

    /**
     * Swaps every slot matching [table] (at most [max] of them) after the runes are paid, and
     * returns how many changed. With nothing to swap the runes are kept and [nothing] is said.
     */
    private fun ProtectedAccess.convertAll(
        spell: MagicSpell,
        table: List<Conversion>,
        max: Int,
        nothing: String,
        seq: SeqType,
        spotanim: SpotanimType,
    ): Int {
        if (actionDelay > mapClock) {
            return 0
        }
        val byFrom = table.associateBy { it.from.id }
        val slots = inv.indices.filter { slot -> inv[slot]?.id in byFrom }.take(max)
        val cast =
            casting.attemptUtility(this, spell) {
                if (slots.isEmpty()) mes(nothing)
                slots.isNotEmpty()
            }
        if (!cast) {
            return 0
        }
        for (slot in slots) {
            val obj = inv[slot] ?: continue
            val into = byFrom.getValue(obj.id).into
            invReplaceSlot(inv, slot, obj.count, into)
        }
        anim(seq)
        spotanim(spotanim, height = 92)
        actionDelay = mapClock + CAST_DELAY
        return slots.size
    }

    private fun ProtectedAccess.superglassMake(spell: MagicSpell) {
        if (actionDelay > mapClock) {
            return
        }
        val seaweed = objTypes[utility_objs.seaweed]
        val sand = objTypes[utility_objs.bucket_of_sand]
        val pairs = minOf(inv.count(seaweed), inv.count(sand))
        val cast =
            casting.attemptUtility(this, spell) {
                if (pairs == 0) mes("You need seaweed and buckets of sand to cast this spell.")
                pairs > 0
            }
        if (!cast) {
            return
        }
        invDel(inv, utility_objs.seaweed, count = pairs)
        invDel(inv, utility_objs.bucket_of_sand, count = pairs)
        var glass = pairs
        repeat(pairs) { if (random.of(100) < LunarTables.SUPERGLASS_BONUS_PERCENT) glass++ }
        invAdd(inv, utility_objs.molten_glass, count = glass, strict = false)
        giveCraftingXp(glass * LunarTables.SUPERGLASS_XP_PER_GLASS)
        anim(utility_seqs.lunar_cast)
        spotanim(utility_spotanims.superglass, height = 92)
        actionDelay = mapClock + CAST_DELAY
    }

    private fun ProtectedAccess.plankMake(spell: MagicSpell, slot: Int, obj: UnpackedObjType) {
        if (actionDelay > mapClock || !slotHolds(slot, obj)) {
            return
        }
        val plank = LunarTables.plankMake.firstOrNull { it.logs.id == obj.id }
        val coins = objTypes[objs.coins]
        val cast =
            casting.attemptUtility(this, spell) {
                when {
                    plank == null -> {
                        mes("You can only cast this spell on logs.")
                        false
                    }
                    inv.count(coins) < plank.cost -> {
                        mes("You need ${plank.cost} coins to make that plank.")
                        false
                    }
                    else -> true
                }
            }
        if (!cast || plank == null) {
            return
        }
        invDel(inv, objs.coins, count = plank.cost)
        invReplaceSlot(inv, slot, count = 1, replacement = plank.plank)
        anim(utility_seqs.plank_make)
        spotanim(utility_spotanims.plank_make, height = 92)
        actionDelay = mapClock + CAST_DELAY
    }

    private fun ProtectedAccess.giveCraftingXp(xp: Double) {
        if (xp > 0) {
            statAdvance(stats.crafting, xp * xpMods.get(player, stats.crafting))
        }
    }

    private companion object {
        const val CAST_DELAY = 3
        /** Twelve and a half seconds, the official Magic Imbue window. */
        const val IMBUE_TICKS = 21
    }
}
