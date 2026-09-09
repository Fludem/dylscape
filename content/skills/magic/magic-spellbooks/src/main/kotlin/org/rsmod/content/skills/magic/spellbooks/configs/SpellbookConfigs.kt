package org.rsmod.content.skills.magic.spellbooks.configs

import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.synth.SynthReferences
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.map.CoordGrid

public typealias spellbook_locs = SpellbookLocs

public typealias spellbook_seqs = SpellbookSeqs

public typealias spellbook_spotanims = SpellbookSpotanims

public typealias spellbook_synths = SpellbookSynths

public typealias spellbook_components = SpellbookComponents

/**
 * The two altars that change a player's spellbook. Both exist in the cache (the Ancient one in the
 * Jaldraocht pyramid, the astral one on Lunar Isle); [EdgevilleAltars] copies them next to the
 * Edgeville bank so nobody has to walk there. Ops from `MagicDump`: the ancient altar carries only
 * `Pray-at` on op1; the astral altar carries `Craft-rune` on op1 and `Pray` on op2.
 */
public object SpellbookLocs : LocReferences() {
    val ancient_altar = find("dt_zaros_altar")
    val astral_altar = find("astral_altar")
}

public object SpellbookSeqs : SeqReferences() {
    val teleport_standard = find("human_castteleport")
    val teleport_ancient = find("zaros_teleport")
    val teleport_lunar = find("lunar_teleport")
    val spellbook_swap = find("dream_player_spellbook_swap")
}

public object SpellbookSpotanims : SpotanimReferences() {
    val teleport_standard = find("teleport_casting")
    val teleport_ancient = find("zaros_teleport")
    val teleport_lunar = find("lunar_teleport_spotanim")
}

public object SpellbookSynths : SynthReferences() {
    val teleport = find("teleport_all")
}

public object SpellbookComponents : ComponentReferences() {
    /** Owned by `content/custom/teleports`, which turns it into a destination picker. */
    val home_teleport_standard = find("magic_spellbook:teleport_home_standard")
}

/** The cast animation and graphic for a teleport from each book. */
public data class TeleportFx(val seq: SeqType, val spotanim: SpotanimType) {
    public companion object {
        public fun forBook(book: Spellbook?): TeleportFx =
            when (book) {
                Spellbook.Ancients ->
                    TeleportFx(
                        spellbook_seqs.teleport_ancient,
                        spellbook_spotanims.teleport_ancient,
                    )
                Spellbook.Lunars ->
                    TeleportFx(spellbook_seqs.teleport_lunar, spellbook_spotanims.teleport_lunar)
                else ->
                    TeleportFx(
                        spellbook_seqs.teleport_standard,
                        spellbook_spotanims.teleport_standard,
                    )
            }
    }
}

/**
 * Where the altars stand in Edgeville: the open square directly north of the bank, which
 * `MagicDump`'s Edgeville render shows clear of locs and walls from x 3083 to 3103 between z 3501
 * and 3505 (the ground south of the bank looks open on a walkability map but is a graveyard full of
 * fences). Both are placed with the default angle, so a 3x1 altar runs along x from its coordinate
 * and the 3x3 astral altar fills a square from its south-west corner. Row 3501 is left free as the
 * walkway along the bank's north wall. `EdgevilleAltarsTest` re-checks the footprints.
 */
public data class AltarPlacement(
    val loc: LocType,
    val book: Spellbook,
    val coords: CoordGrid,
    val width: Int,
    val length: Int,
) {
    val angle: LocAngle = LocAngle.West
    val shape: LocShape = LocShape.CentrepieceStraight

    /** Every tile the altar occupies. */
    val footprint: List<CoordGrid>
        get() = buildList {
            for (dx in 0 until width) for (dz in 0 until length) {
                add(CoordGrid(coords.x + dx, coords.z + dz, coords.level))
            }
        }
}

public object EdgevilleAltars {
    val ancient =
        AltarPlacement(
            loc = spellbook_locs.ancient_altar,
            book = Spellbook.Ancients,
            coords = CoordGrid(3093, 3503, 0),
            width = 3,
            length = 1,
        )
    val astral =
        AltarPlacement(
            loc = spellbook_locs.astral_altar,
            book = Spellbook.Lunars,
            coords = CoordGrid(3099, 3502, 0),
            width = 3,
            length = 3,
        )
    val all: List<AltarPlacement> = listOf(ancient, astral)
}
