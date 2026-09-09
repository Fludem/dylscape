package org.rsmod.content.skills.magic.utility.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

/**
 * The jewellery enchanting recipe book.
 *
 * Authored, not read: dbtable 86 `magic_enchant` exists in this cache with ninety named rows but no
 * columns and no data (`MagicDump` prints it), so the mapping lives here. The seven spells and
 * their levels, runes and xp still come from the spell objs.
 */
public enum class Gem(public val level: Int, public val sound: Gem?) {
    Sapphire(1, null),
    Opal(1, Sapphire),
    Emerald(2, null),
    Jade(2, Emerald),
    Ruby(3, null),
    Topaz(3, Ruby),
    Diamond(4, null),
    Dragonstone(5, null),
    Onyx(6, null),
    Zenyte(7, Onyx);

    /** Which gem's sound effect plays: the four "lesser" gems borrow their bigger sibling's. */
    public val soundGem: Gem
        get() = sound ?: this
}

public enum class JewelleryKind {
    Amulet,
    Necklace,
    Ring,
    Bracelet;

    /** Rings and bracelets share the ring animation; amulets and necklaces the amulet one. */
    public val ringLike: Boolean
        get() = this == Ring || this == Bracelet
}

public data class EnchantRow(
    val base: ObjType,
    val product: ObjType,
    val gem: Gem,
    val kind: JewelleryKind,
) {
    val level: Int
        get() = gem.level
}

public object EnchantTable {
    val spells: Map<Int, ObjType> =
        mapOf(
            1 to objs.spell_enchant_lvl_1,
            2 to objs.spell_enchant_lvl_2,
            3 to objs.spell_enchant_lvl_3,
            4 to objs.spell_enchant_lvl_4,
            5 to objs.spell_enchant_lvl_5,
            6 to objs.spell_enchant_lvl_6,
            7 to objs.spell_enchant_lvl_7,
        )

    val rows: List<EnchantRow> =
        listOf(
            row(
                utility_objs.sapphire_ring,
                utility_objs.ring_of_recoil,
                Gem.Sapphire,
                JewelleryKind.Ring,
            ),
            row(
                utility_objs.sapphire_necklace,
                utility_objs.games_necklace,
                Gem.Sapphire,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.sapphire_amulet,
                utility_objs.amulet_of_magic,
                Gem.Sapphire,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.sapphire_bracelet,
                utility_objs.bracelet_of_clay,
                Gem.Sapphire,
                JewelleryKind.Bracelet,
            ),
            row(utility_objs.opal_ring, utility_objs.ring_of_pursuit, Gem.Opal, JewelleryKind.Ring),
            row(
                utility_objs.opal_necklace,
                utility_objs.dodgy_necklace,
                Gem.Opal,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.opal_amulet,
                utility_objs.amulet_of_bounty,
                Gem.Opal,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.opal_bracelet,
                utility_objs.expeditious_bracelet,
                Gem.Opal,
                JewelleryKind.Bracelet,
            ),
            row(
                utility_objs.emerald_ring,
                utility_objs.ring_of_dueling,
                Gem.Emerald,
                JewelleryKind.Ring,
            ),
            row(
                utility_objs.emerald_necklace,
                utility_objs.binding_necklace,
                Gem.Emerald,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.emerald_amulet,
                utility_objs.amulet_of_defence,
                Gem.Emerald,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.emerald_bracelet,
                utility_objs.castle_wars_bracelet,
                Gem.Emerald,
                JewelleryKind.Bracelet,
            ),
            row(
                utility_objs.jade_ring,
                utility_objs.ring_of_returning,
                Gem.Jade,
                JewelleryKind.Ring,
            ),
            row(
                utility_objs.jade_necklace,
                utility_objs.necklace_of_passage,
                Gem.Jade,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.jade_amulet,
                utility_objs.amulet_of_chemistry,
                Gem.Jade,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.jade_bracelet,
                utility_objs.flamtaer_bracelet,
                Gem.Jade,
                JewelleryKind.Bracelet,
            ),
            row(utility_objs.ruby_ring, utility_objs.ring_of_forging, Gem.Ruby, JewelleryKind.Ring),
            row(
                utility_objs.ruby_necklace,
                utility_objs.digsite_pendant,
                Gem.Ruby,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.ruby_amulet,
                utility_objs.amulet_of_strength,
                Gem.Ruby,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.ruby_bracelet,
                utility_objs.inoculation_bracelet,
                Gem.Ruby,
                JewelleryKind.Bracelet,
            ),
            row(utility_objs.topaz_ring, utility_objs.efaritays_aid, Gem.Topaz, JewelleryKind.Ring),
            row(
                utility_objs.topaz_necklace,
                utility_objs.necklace_of_faith,
                Gem.Topaz,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.topaz_amulet,
                utility_objs.burning_amulet,
                Gem.Topaz,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.topaz_bracelet,
                utility_objs.bracelet_of_slaughter,
                Gem.Topaz,
                JewelleryKind.Bracelet,
            ),
            row(
                utility_objs.diamond_ring,
                utility_objs.ring_of_life,
                Gem.Diamond,
                JewelleryKind.Ring,
            ),
            row(
                utility_objs.diamond_necklace,
                utility_objs.phoenix_necklace,
                Gem.Diamond,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.diamond_amulet,
                utility_objs.amulet_of_power,
                Gem.Diamond,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.diamond_bracelet,
                utility_objs.abyssal_bracelet,
                Gem.Diamond,
                JewelleryKind.Bracelet,
            ),
            row(
                utility_objs.dragonstone_ring,
                utility_objs.ring_of_wealth,
                Gem.Dragonstone,
                JewelleryKind.Ring,
            ),
            row(
                utility_objs.dragonstone_necklace,
                utility_objs.skills_necklace,
                Gem.Dragonstone,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.dragonstone_amulet,
                utility_objs.amulet_of_glory,
                Gem.Dragonstone,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.dragonstone_bracelet,
                utility_objs.combat_bracelet,
                Gem.Dragonstone,
                JewelleryKind.Bracelet,
            ),
            row(utility_objs.onyx_ring, utility_objs.ring_of_stone, Gem.Onyx, JewelleryKind.Ring),
            row(
                utility_objs.onyx_necklace,
                utility_objs.berserker_necklace,
                Gem.Onyx,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.onyx_amulet,
                utility_objs.amulet_of_fury,
                Gem.Onyx,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.onyx_bracelet,
                utility_objs.regen_bracelet,
                Gem.Onyx,
                JewelleryKind.Bracelet,
            ),
            row(
                utility_objs.zenyte_ring,
                utility_objs.ring_of_suffering,
                Gem.Zenyte,
                JewelleryKind.Ring,
            ),
            row(
                utility_objs.zenyte_necklace,
                utility_objs.necklace_of_anguish,
                Gem.Zenyte,
                JewelleryKind.Necklace,
            ),
            row(
                utility_objs.zenyte_amulet,
                utility_objs.amulet_of_torture,
                Gem.Zenyte,
                JewelleryKind.Amulet,
            ),
            row(
                utility_objs.zenyte_bracelet,
                utility_objs.tormented_bracelet,
                Gem.Zenyte,
                JewelleryKind.Bracelet,
            ),
        )

    /** Keyed by the raw base obj id: the runtime hands scripts `UnpackedObjType`s. */
    val byBase: Map<Int, EnchantRow> by lazy { rows.associateBy { it.base.id } }

    init {
        check(rows.map { it.base.id }.toSet().size == rows.size) { "Duplicate enchant base." }
    }

    private fun row(base: ObjType, product: ObjType, gem: Gem, kind: JewelleryKind) =
        EnchantRow(base, product, gem, kind)

    public fun anim(row: EnchantRow): SeqType =
        when {
            row.kind.ringLike -> utility_seqs.enchant_ring
            row.level == 1 -> utility_seqs.enchant_amulet_1
            row.level == 2 -> utility_seqs.enchant_amulet_2
            else -> utility_seqs.enchant_amulet_3
        }

    public fun spotanim(row: EnchantRow): SpotanimType =
        when {
            row.kind.ringLike -> utility_spotanims.enchant_ring
            row.level == 1 -> utility_spotanims.enchant_amulet_1
            row.level == 2 -> utility_spotanims.enchant_amulet_2
            row.level == 3 -> utility_spotanims.enchant_amulet_3
            row.level == 4 -> utility_spotanims.enchant_amulet_4
            row.level == 5 -> utility_spotanims.enchant_amulet_5
            else -> utility_spotanims.enchant_amulet_6
        }

    public fun sound(row: EnchantRow): SynthType =
        when (row.gem.soundGem) {
            Gem.Sapphire ->
                if (row.kind.ringLike) utility_synths.enchant_sapphire_ring
                else utility_synths.enchant_sapphire_amulet
            Gem.Emerald ->
                if (row.kind.ringLike) utility_synths.enchant_emerald_ring
                else utility_synths.enchant_emerald_amulet
            Gem.Ruby ->
                if (row.kind.ringLike) utility_synths.enchant_ruby_ring
                else utility_synths.enchant_ruby_amulet
            Gem.Diamond ->
                if (row.kind.ringLike) utility_synths.enchant_diamond_ring
                else utility_synths.enchant_diamond_amulet
            Gem.Dragonstone ->
                if (row.kind.ringLike) utility_synths.enchant_dragon_ring
                else utility_synths.enchant_dragon_amulet
            else ->
                if (row.kind.ringLike) utility_synths.enchant_onyx_ring
                else utility_synths.enchant_onyx_amulet
        }
}
