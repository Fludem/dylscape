package org.rsmod.content.skills.magic.combat.spells

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.combat.configs.combat_objs
import org.rsmod.content.skills.magic.combat.configs.combat_seqs
import org.rsmod.content.skills.magic.combat.configs.combat_spotanims
import org.rsmod.content.skills.magic.combat.configs.combat_synths
import org.rsmod.content.skills.magic.commons.FreezeManager
import org.rsmod.content.skills.magic.commons.MagicStatus
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

/**
 * The standard book's non-elemental combat spells: the six curses, the three binds, the three god
 * spells, Iban Blast, Magic Dart and Crumble Undead.
 *
 * The cache gives these spells their level, runes and staff requirements but no `spell_maxhit` and
 * no `spell_drain_stat` (both are empty on every non-elemental spell in rev 233, as `MagicDump`
 * shows), so the numbers here are the wiki's: curses lower 5% or 10% of the base stat, binds hold
 * for 5, 10 and 15 seconds, the god spells hit 20 (30 with Charge and the matching cape), Iban
 * Blast 25, Crumble Undead 15, and Magic Dart 10 plus a tenth of the Magic level.
 */
class StandardCombatSpells
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val npcRepo: NpcRepository,
    private val casting: SpellCasting,
    private val freeze: FreezeManager,
) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        registerCurses(manager)
        registerBinds(manager)
        registerGodSpells(manager)
        registerStaffSpells(manager)
    }

    private fun SpellAttackRepository.registerCurses(manager: SpellAttackManager) {
        fun curse(
            spell: ObjType,
            stat: DrainStat,
            percent: Int,
            seq: SeqType,
            staffSeq: SeqType,
            casting: SpotanimType,
            travel: SpotanimType,
            impact: SpotanimType,
            castSound: SynthType,
            hitSound: SynthType,
        ) {
            register(
                spell = spell,
                attack =
                    attack(
                        manager,
                        SpellFx(staffSeq, seq, casting, travel, impact, castSound, hitSound),
                        maxHit = { _, _ -> null },
                        onHit = { _, target, _ -> SpellEffects.drain(target, stat, percent) },
                    ),
            )
        }
        curse(
            objs.spell_confuse,
            DrainStat.Attack,
            LESSER_DRAIN_PERCENT,
            combat_seqs.confuse,
            combat_seqs.confuse_staff,
            combat_spotanims.confuse_casting,
            combat_spotanims.confuse_travel,
            combat_spotanims.confuse_impact,
            combat_synths.confuse_cast,
            combat_synths.confuse_hit,
        )
        curse(
            objs.spell_weaken,
            DrainStat.Strength,
            LESSER_DRAIN_PERCENT,
            combat_seqs.weaken,
            combat_seqs.weaken_staff,
            combat_spotanims.weaken_casting,
            combat_spotanims.weaken_travel,
            combat_spotanims.weaken_impact,
            combat_synths.weaken_all,
            combat_synths.weaken_all,
        )
        curse(
            objs.spell_curse,
            DrainStat.Defence,
            LESSER_DRAIN_PERCENT,
            combat_seqs.curse,
            combat_seqs.curse_staff,
            combat_spotanims.curse_casting,
            combat_spotanims.curse_travel,
            combat_spotanims.curse_impact,
            combat_synths.curse_cast,
            combat_synths.curse_hit,
        )
        curse(
            objs.spell_vulnerability,
            DrainStat.Defence,
            GREATER_DRAIN_PERCENT,
            combat_seqs.stun,
            combat_seqs.stun_staff,
            combat_spotanims.vulnerability_casting,
            combat_spotanims.vulnerability_travel,
            combat_spotanims.vulnerability_impact,
            combat_synths.vulnerability_all,
            combat_synths.vulnerability_all,
        )
        curse(
            objs.spell_enfeeble,
            DrainStat.Strength,
            GREATER_DRAIN_PERCENT,
            combat_seqs.enfeeble,
            combat_seqs.enfeeble_staff,
            combat_spotanims.enfeeble_casting,
            combat_spotanims.enfeeble_travel,
            combat_spotanims.enfeeble_impact,
            combat_synths.enfeeble_cast,
            combat_synths.enfeeble_hit,
        )
        curse(
            objs.spell_stun,
            DrainStat.Attack,
            GREATER_DRAIN_PERCENT,
            combat_seqs.stun,
            combat_seqs.stun_staff,
            combat_spotanims.stun_casting,
            combat_spotanims.stun_travel,
            combat_spotanims.stun_impact,
            combat_synths.stun_all,
            combat_synths.stun_all,
        )
    }

    private fun SpellAttackRepository.registerBinds(manager: SpellAttackManager) {
        fun bind(
            spell: ObjType,
            ticks: Int,
            impact: SpotanimType,
            castSound: SynthType,
            hitSound: SynthType,
        ) {
            register(
                spell = spell,
                attack =
                    attack(
                        manager,
                        SpellFx(
                            staffSeq = combat_seqs.bind_staff,
                            unarmedSeq = combat_seqs.bind,
                            launch = combat_spotanims.bind_casting,
                            travel = combat_spotanims.bind_travel,
                            impact = impact,
                            castSound = castSound,
                            hitSound = hitSound,
                        ),
                        maxHit = { _, _ -> null },
                        onHit = { _, target, _ -> freeze.freeze(target, ticks) },
                    ),
            )
        }
        bind(
            objs.spell_bind,
            BIND_TICKS,
            combat_spotanims.bind_impact,
            combat_synths.bind_cast,
            combat_synths.bind_impact,
        )
        bind(
            objs.spell_snare,
            SNARE_TICKS,
            combat_spotanims.snare_impact,
            combat_synths.snare_all,
            combat_synths.snare_all,
        )
        bind(
            objs.spell_entangle,
            ENTANGLE_TICKS,
            combat_spotanims.entangle_impact,
            combat_synths.entangle_cast,
            combat_synths.entangle_hit,
        )
    }

    private fun SpellAttackRepository.registerGodSpells(manager: SpellAttackManager) {
        fun godSpell(spell: ObjType, cape: ObjType, impact: SpotanimType) {
            register(
                spell = spell,
                attack =
                    attack(
                        manager,
                        SpellFx(staffSeq = combat_seqs.god_spell, impact = impact),
                        maxHit = { caster, _ -> godSpellMaxHit(caster, cape) },
                    ),
            )
        }
        godSpell(
            objs.spell_saradomin_strike,
            combat_objs.saradomin_cape,
            combat_spotanims.saradomin_strike,
        )
        godSpell(
            objs.spell_claws_of_guthix,
            combat_objs.guthix_cape,
            combat_spotanims.claws_of_guthix,
        )
        godSpell(
            objs.spell_flames_of_zamorak,
            combat_objs.zamorak_cape,
            combat_spotanims.flames_of_zamorak,
        )
    }

    private fun SpellAttackRepository.registerStaffSpells(manager: SpellAttackManager) {
        register(
            spell = objs.spell_iban_blast,
            attack =
                attack(
                    manager,
                    SpellFx(
                        staffSeq = combat_seqs.iban_blast,
                        launch = combat_spotanims.iban_blast_casting,
                        travel = combat_spotanims.iban_blast_travel,
                        impact = combat_spotanims.iban_blast_impact,
                    ),
                    maxHit = { _, _ -> IBAN_BLAST_MAX_HIT },
                ),
        )
        register(
            spell = objs.spell_magic_dart,
            attack =
                attack(
                    manager,
                    SpellFx(
                        staffSeq = combat_seqs.magic_dart,
                        travel = combat_spotanims.magic_dart_travel,
                        impact = combat_spotanims.magic_dart_impact,
                        hitSound = combat_synths.magic_dart_hit,
                    ),
                    maxHit = { caster, _ -> MAGIC_DART_BASE + caster.player.magicLvl / 10 },
                ),
        )
        register(
            spell = objs.spell_crumble_undead,
            attack =
                attack(
                    manager,
                    SpellFx(
                        staffSeq = combat_seqs.crumble_undead_staff,
                        unarmedSeq = combat_seqs.crumble_undead,
                        launch = combat_spotanims.crumble_undead_casting,
                        travel = combat_spotanims.crumble_undead_travel,
                        impact = combat_spotanims.crumble_undead_impact,
                        castSound = combat_synths.crumble_cast,
                        hitSound = combat_synths.crumble_hit,
                    ),
                    maxHit = { _, _ -> CRUMBLE_UNDEAD_MAX_HIT },
                    canTarget = { _, target ->
                        if (target is Npc && !SpellEffects.isUndead(target)) {
                            "This spell only affects skeletons, zombies, ghosts and shades."
                        } else {
                            null
                        }
                    },
                ),
        )
    }

    private fun godSpellMaxHit(caster: ProtectedAccess, cape: ObjType): Int {
        val charged = MagicStatus.isCharged(caster.player) && caster.player.back.isType(cape)
        return if (charged) GOD_SPELL_CHARGED_MAX_HIT else GOD_SPELL_MAX_HIT
    }

    private fun attack(
        manager: SpellAttackManager,
        fx: SpellFx,
        maxHit: (ProtectedAccess, PathingEntity) -> Int?,
        canTarget: (ProtectedAccess, PathingEntity) -> String? = { _, _ -> null },
        onHit: (org.rsmod.game.entity.Player, PathingEntity, Int) -> Unit = { _, _, _ -> },
    ) = CombatSpellAttack(manager, casting, objTypes, npcRepo, fx, false, maxHit, canTarget, onHit)

    private companion object {
        const val LESSER_DRAIN_PERCENT = 5
        const val GREATER_DRAIN_PERCENT = 10
        const val BIND_TICKS = 8
        const val SNARE_TICKS = 16
        const val ENTANGLE_TICKS = 24
        const val GOD_SPELL_MAX_HIT = 20
        const val GOD_SPELL_CHARGED_MAX_HIT = 30
        const val IBAN_BLAST_MAX_HIT = 25
        const val MAGIC_DART_BASE = 10
        const val CRUMBLE_UNDEAD_MAX_HIT = 15
    }
}
