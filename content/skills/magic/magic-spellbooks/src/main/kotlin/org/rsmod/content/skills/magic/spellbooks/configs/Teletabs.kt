@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.skills.magic.spellbooks.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

public typealias teletab_objs = TeletabObjs

/**
 * The teleport tablets. [org.rsmod.api.config.refs.BaseObjs] exposes none of them.
 *
 * Three families, one per spellbook they copy: `poh_tablet_*` (standard), `tablet_*` (Ancient) and
 * `teletab_*` (Arceuus). The cache names are historical and do not always match what a player
 * reads: `teletab_lumbridge` is the *Arceuus library* teleport today.
 */
public object TeletabObjs : ObjReferences() {
    val poh_tablet_varrockteleport = find("poh_tablet_varrockteleport")
    val poh_tablet_lumbridgeteleport = find("poh_tablet_lumbridgeteleport")
    val poh_tablet_faladorteleport = find("poh_tablet_faladorteleport")
    val poh_tablet_camelotteleport = find("poh_tablet_camelotteleport")
    val poh_tablet_ardougneteleport = find("poh_tablet_ardougneteleport")
    val poh_tablet_watchtowerteleport = find("poh_tablet_watchtowerteleport")
    val poh_tablet_kourendteleport = find("poh_tablet_kourendteleport")
    val poh_tablet_fortisteleport = find("poh_tablet_fortisteleport")

    val tablet_paddewa = find("tablet_paddewa")
    val tablet_senntisten = find("tablet_senntisten")
    val tablet_kharyll = find("tablet_kharyll")
    val tablet_lassar = find("tablet_lassar")
    val tablet_dareeyak = find("tablet_dareeyak")
    val tablet_carrallangar = find("tablet_carrallangar")
    val tablet_annakarl = find("tablet_annakarl")
    val tablet_ghorrock = find("tablet_ghorrock")

    val teletab_lumbridge = find("teletab_lumbridge")
    val teletab_draynor = find("teletab_draynor")
    val teletab_battlefront = find("teletab_battlefront")
    val teletab_mind_altar = find("teletab_mind_altar")
    val teletab_salve = find("teletab_salve")
    val teletab_fenk = find("teletab_fenk")
    val teletab_westardy = find("teletab_westardy")
    val teletab_harmony = find("teletab_harmony")
    val teletab_cemetery = find("teletab_cemetery")
    val teletab_barrows = find("teletab_barrows")
    val teletab_ape = find("teletab_ape")
}

/**
 * A tablet and the spell whose destination it borrows.
 *
 * The destination is the spell's own `spell_telecoord`, stamped by upstream's `SpellObjEditor` in
 * `api/spells/.../configs/SpellObjs.kt`, so a tablet and its spell can never disagree about where
 * they go.
 */
public data class Teletab(val tab: ObjType, val spell: ObjType) {
    /** Read through `paramMap` because `paramOrNull` answers a missing param with its default. */
    public fun destination(objTypes: ObjTypeList): CoordGrid? =
        objTypes[spell].paramMap?.getOrNull(params.spell_telecoord)
}

/**
 * Every tablet `Break` works on, and therefore everything Akutha's counter in Edgeville sells.
 *
 * Left out on purpose:
 * - `poh_tablet_teleporttohouse`: there are no player-owned houses.
 * - the `nzone_teletab_*` house-portal redirects (Rimmington, Taverley, Pollnivneach, ...): they
 *   copy no spell, so there is no vetted coordinate to borrow.
 * - `tablet_target`, which follows a Bounty Hunter target, and `tablet_wildycrabs`, which copies no
 *   spell.
 * - the enchant, bones-to-fruit and telegrab tablets, which are not teleports.
 */
public object Teletabs {
    public val all: List<Teletab> =
        listOf(
            // Standard spellbook.
            Teletab(teletab_objs.poh_tablet_varrockteleport, objs.spell_varrock_teleport),
            Teletab(teletab_objs.poh_tablet_lumbridgeteleport, objs.spell_lumbridge_teleport),
            Teletab(teletab_objs.poh_tablet_faladorteleport, objs.spell_falador_teleport),
            Teletab(teletab_objs.poh_tablet_camelotteleport, objs.spell_camelot_teleport),
            Teletab(teletab_objs.poh_tablet_ardougneteleport, objs.spell_ardougne_teleport),
            Teletab(teletab_objs.poh_tablet_watchtowerteleport, objs.spell_watchtower_teleport),
            Teletab(teletab_objs.poh_tablet_kourendteleport, objs.spell_kourendcastle_teleport),
            Teletab(teletab_objs.poh_tablet_fortisteleport, objs.spell_civitas_fortis_teleport),

            // Ancient Magicks.
            Teletab(teletab_objs.tablet_paddewa, objs.spell_paddewwa_teleport),
            Teletab(teletab_objs.tablet_senntisten, objs.spell_senntisten_teleport),
            Teletab(teletab_objs.tablet_kharyll, objs.spell_kharyllyl_teleport),
            Teletab(teletab_objs.tablet_lassar, objs.spell_lassar_teleport),
            Teletab(teletab_objs.tablet_dareeyak, objs.spell_dareeyak_teleport),
            Teletab(teletab_objs.tablet_carrallangar, objs.spell_carrallagar_teleport),
            Teletab(teletab_objs.tablet_annakarl, objs.spell_annakarl_teleport),
            Teletab(teletab_objs.tablet_ghorrock, objs.spell_ghorrock_teleport),

            // Arceuus. The book itself is not castable here, but its tablets still work.
            Teletab(teletab_objs.teletab_lumbridge, objs.spell_arceuuslibrary_teleport),
            Teletab(teletab_objs.teletab_draynor, objs.spell_draynormanor_teleport),
            Teletab(teletab_objs.teletab_battlefront, objs.spell_battlefront_teleport),
            Teletab(teletab_objs.teletab_mind_altar, objs.spell_mindaltar_teleport),
            Teletab(teletab_objs.teletab_salve, objs.spell_salvegrave_teleport),
            Teletab(teletab_objs.teletab_fenk, objs.spell_fenkenstrainscastle_teleport),
            Teletab(teletab_objs.teletab_westardy, objs.spell_westardougne_teleport),
            Teletab(teletab_objs.teletab_harmony, objs.spell_harmonyisland_teleport),
            Teletab(teletab_objs.teletab_cemetery, objs.spell_cemetery_teleport),
            Teletab(teletab_objs.teletab_barrows, objs.spell_barrows_teleport),
            Teletab(teletab_objs.teletab_ape, objs.spell_arceuusapeatoll_teleport),
        )
}
