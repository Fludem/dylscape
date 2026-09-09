package org.rsmod.content.skills.magic.combat.spells

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.combat.configs.combat_seqs
import org.rsmod.content.skills.magic.combat.configs.combat_spotanims
import org.rsmod.content.skills.magic.combat.configs.combat_synths
import org.rsmod.content.skills.magic.commons.FreezeManager
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

/**
 * The sixteen Ancient Magicks, as a four-by-four table of element and tier.
 *
 * Max hits are the wiki's (the cache carries no `spell_maxhit` for any of them). Ice freezes for 5,
 * 10, 15 and 20 seconds; shadow lowers Attack by 10% (rush, burst) or 15% (blitz, barrage); blood
 * heals the caster a quarter of every hit; smoke deals damage only, because this server has no
 * poison system yet. Bursts and barrages strike the 3x3 around the target in multi-combat.
 *
 * The cache names a travel graphic for every ice and smoke spell and for the blood and shadow rush
 * and blitz, but only an impact for the blood and shadow burst and barrage, which is why those four
 * land without a projectile.
 */
class AncientSpells
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val npcRepo: NpcRepository,
    private val casting: SpellCasting,
    private val freeze: FreezeManager,
) : SpellAttackMap {
    private enum class Element(val castSound: SynthType) {
        Smoke(combat_synths.smoke_cast),
        Shadow(combat_synths.shadow_cast),
        Blood(combat_synths.blood_cast),
        Ice(combat_synths.ice_cast),
    }

    private enum class Tier(val multi: Boolean, val freezeTicks: Int, val drainPercent: Int) {
        Rush(multi = false, freezeTicks = 8, drainPercent = 10),
        Burst(multi = true, freezeTicks = 16, drainPercent = 10),
        Blitz(multi = false, freezeTicks = 24, drainPercent = 15),
        Barrage(multi = true, freezeTicks = 32, drainPercent = 15),
    }

    private class Ancient(
        val spell: ObjType,
        val element: Element,
        val tier: Tier,
        val maxHit: Int,
        val travel: SpotanimType?,
        val impact: SpotanimType,
        val hitSound: SynthType,
    )

    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (ancient in table()) {
            register(
                spell = ancient.spell,
                attack =
                    CombatSpellAttack(
                        manager = manager,
                        casting = casting,
                        objTypes = objTypes,
                        npcRepo = npcRepo,
                        fx =
                            SpellFx(
                                staffSeq =
                                    if (ancient.tier.multi) combat_seqs.ancient_multi
                                    else combat_seqs.ancient_single,
                                travel = ancient.travel,
                                impact = ancient.impact,
                                castSound = ancient.element.castSound,
                                hitSound = ancient.hitSound,
                            ),
                        multi = ancient.tier.multi,
                        maxHit = { _, _ -> ancient.maxHit },
                        onHit = { caster, target, damage -> ancient.effect(caster, target, damage) },
                    ),
            )
        }
    }

    private fun Ancient.effect(caster: Player, target: PathingEntity, damage: Int) {
        if (damage <= 0) {
            return
        }
        when (element) {
            Element.Ice -> freeze.freeze(target, tier.freezeTicks)
            Element.Shadow -> SpellEffects.drain(target, DrainStat.Attack, tier.drainPercent)
            Element.Blood -> SpellEffects.healCaster(caster, damage, BLOOD_HEAL_PERCENT)
            Element.Smoke -> Unit // Poison: not implemented anywhere on this server yet.
        }
    }

    private fun table(): List<Ancient> =
        listOf(
            Ancient(
                objs.spell_smoke_rush,
                Element.Smoke,
                Tier.Rush,
                13,
                combat_spotanims.smoke_rush_travel,
                combat_spotanims.smoke_rush_impact,
                combat_synths.smoke_rush_impact,
            ),
            Ancient(
                objs.spell_shadow_rush,
                Element.Shadow,
                Tier.Rush,
                14,
                combat_spotanims.shadow_rush_travel,
                combat_spotanims.shadow_rush_impact,
                combat_synths.shadow_rush_impact,
            ),
            Ancient(
                objs.spell_blood_rush,
                Element.Blood,
                Tier.Rush,
                15,
                combat_spotanims.blood_rush_travel,
                combat_spotanims.blood_rush_impact,
                combat_synths.blood_rush_impact,
            ),
            Ancient(
                objs.spell_ice_rush,
                Element.Ice,
                Tier.Rush,
                16,
                combat_spotanims.ice_rush_travel,
                combat_spotanims.ice_rush_impact,
                combat_synths.ice_rush_impact,
            ),
            Ancient(
                objs.spell_smoke_burst,
                Element.Smoke,
                Tier.Burst,
                17,
                combat_spotanims.smoke_burst_travel,
                combat_spotanims.smoke_burst_impact,
                combat_synths.smoke_burst_impact,
            ),
            Ancient(
                objs.spell_shadow_burst,
                Element.Shadow,
                Tier.Burst,
                18,
                null,
                combat_spotanims.shadow_burst_impact,
                combat_synths.shadow_burst_impact,
            ),
            Ancient(
                objs.spell_blood_burst,
                Element.Blood,
                Tier.Burst,
                21,
                null,
                combat_spotanims.blood_burst_impact,
                combat_synths.blood_burst_impact,
            ),
            Ancient(
                objs.spell_ice_burst,
                Element.Ice,
                Tier.Burst,
                22,
                combat_spotanims.ice_burst_travel,
                combat_spotanims.ice_burst_impact,
                combat_synths.ice_burst_impact,
            ),
            Ancient(
                objs.spell_smoke_blitz,
                Element.Smoke,
                Tier.Blitz,
                23,
                combat_spotanims.smoke_blitz_travel,
                combat_spotanims.smoke_blitz_impact,
                combat_synths.smoke_blitz_impact,
            ),
            Ancient(
                objs.spell_shadow_blitz,
                Element.Shadow,
                Tier.Blitz,
                24,
                combat_spotanims.shadow_blitz_travel,
                combat_spotanims.shadow_blitz_impact,
                combat_synths.shadow_blitz_impact,
            ),
            Ancient(
                objs.spell_blood_blitz,
                Element.Blood,
                Tier.Blitz,
                25,
                combat_spotanims.blood_blitz_travel,
                combat_spotanims.blood_blitz_impact,
                combat_synths.blood_blitz_impact,
            ),
            Ancient(
                objs.spell_ice_blitz,
                Element.Ice,
                Tier.Blitz,
                26,
                combat_spotanims.ice_blitz_travel,
                combat_spotanims.ice_blitz_impact,
                combat_synths.ice_blitz_impact,
            ),
            Ancient(
                objs.spell_smoke_barrage,
                Element.Smoke,
                Tier.Barrage,
                27,
                combat_spotanims.smoke_barrage_travel,
                combat_spotanims.smoke_barrage_impact,
                combat_synths.smoke_barrage_impact,
            ),
            Ancient(
                objs.spell_shadow_barrage,
                Element.Shadow,
                Tier.Barrage,
                28,
                null,
                combat_spotanims.shadow_barrage_impact,
                combat_synths.shadow_barrage_impact,
            ),
            Ancient(
                objs.spell_blood_barrage,
                Element.Blood,
                Tier.Barrage,
                29,
                null,
                combat_spotanims.blood_barrage_impact,
                combat_synths.blood_barrage_impact,
            ),
            Ancient(
                objs.spell_ice_barrage,
                Element.Ice,
                Tier.Barrage,
                30,
                combat_spotanims.ice_barrage_travel,
                combat_spotanims.ice_barrage_impact,
                combat_synths.ice_barrage_impact,
            ),
        )

    private companion object {
        const val BLOOD_HEAL_PERCENT = 25
    }
}
