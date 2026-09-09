package org.rsmod.content.skills.smithing.configs

import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.content.ContentReferences
import org.rsmod.api.type.refs.enums.EnumReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjType

object SmithingContent : ContentReferences() {
    val smithing_anvil = find("smithing_anvil")
    val smithing_furnace = find("smithing_furnace")

    /** The six bars that can be worked at an anvil. Silver and gold are cast, not smithed. */
    val smithing_bar = find("smithing_bar")
}

object SmithingObjs : ObjReferences() {
    val bronze_bar = find("bronze_bar")
    val iron_bar = find("iron_bar")
    val steel_bar = find("steel_bar")
    val silver_bar = find("silver_bar")
    val gold_bar = find("gold_bar")
    val mithril_bar = find("mithril_bar")
    val adamantite_bar = find("adamantite_bar")
    val runite_bar = find("runite_bar")

    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val silver_ore = find("silver_ore")
    val coal = find("coal")
    val gold_ore = find("gold_ore")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")
}

object SmithingSeqs : SeqReferences() {
    val smith = find("human_smithing")
    val smelt = find("human_furnace")
}

object SmithingVarBits : VarBitReferences() {
    /**
     * The bar tier the smithing interface should draw.
     *
     * `proc,smithing_setup` (clientscript 430) runs from interface 312's own `onLoad` and switches
     * on this varbit *after* passing it through enum 1253, which maps `1..7` to `bronze_bar`,
     * `iron_bar`, `steel_bar`, `mithril_bar`, `adamantite_bar`, `runite_bar` and `lovakite_bar`. So
     * the value stored here is a small tier index, **not** the bar's obj id.
     *
     * The distinction matters because `smithing_bar_type` is bits 0..2 of the varp `smithbars`
     * (210). Writing a bar's obj id into the varp does not fail -- it silently truncates to the low
     * three bits, and `bronze_bar` (2349) lands on index 5, which is adamant. Every tier mis-drew
     * that way until the setup script was actually decoded.
     */
    val bar_type = find("smithing_bar_type")
}

object SmithingInterfaces : InterfaceReferences() {
    val smithing = find("smithing")
}

/**
 * The product buttons on interface 312.
 *
 * Every one of these carries a single `*` op in the cache; the client's setup proc relabels them
 * per quantity at runtime. [org.rsmod.content.skills.smithing.scripts.AnvilSmithing] enables ops
 * 1-5 on each and maps them to the makex quantities.
 *
 * `other_1`, `other_2` and `other_3` are deliberately absent: the client fills those slots with
 * tier-specific oddities (crossbow grapple tips, lanterns, spits, blurite and Shayzien gear) that
 * belong to quest and minigame content this module does not implement.
 */
object SmithingComponents : ComponentReferences() {
    val dagger = find("smithing:dagger")
    val sword = find("smithing:sword")
    val scimitar = find("smithing:scimitar")
    val longsword = find("smithing:longsword")
    val two_hand = find("smithing:2h")
    val axe = find("smithing:axe")
    val mace = find("smithing:mace")
    val warhammer = find("smithing:warhammer")
    val battleaxe = find("smithing:battleaxe")
    val claws = find("smithing:claws")
    val chainbody = find("smithing:chainbody")
    val platelegs = find("smithing:platelegs")
    val plateskirt = find("smithing:plateskirt")
    val platebody = find("smithing:platebody")
    val nails = find("smithing:nails")
    val med_helm = find("smithing:medhelm")
    val full_helm = find("smithing:fullhelm")
    val sq_shield = find("smithing:squareshield")
    val kiteshield = find("smithing:kiteshield")
    val dart_tips = find("smithing:darttips")
    val arrowheads = find("smithing:arrowheads")
    val knives = find("smithing:knives")
    val bolts = find("smithing:bolts")
    val limbs = find("smithing:limbs")
}

/**
 * Anvils and furnaces.
 *
 * Only ordinary, freely usable ones are tagged. Quest-locked and minigame furnaces
 * (`furnace_legendsquest`, `regicide_furnace`, `elemental_workshop_furnace`, the Blast Furnace),
 * scenery lookalikes (`anvil_icon`, `fai_varrock_furnace_chimney`), the `_noop` anvil variants and
 * Tekton's raid anvil are all left alone -- tagging them would advertise an op the surrounding
 * content cannot honour.
 *
 * **Ops here are decoded, not assumed, and they are not uniform:**
 * - anvils carry `Smith` on **op1**
 * - furnaces carry `Smelt` on **op2**
 * - `newbiefurnace`, Tutorial Island's, carries `Use` on **op1** instead
 *
 * Several plausible-looking locs carry **no ops at all** -- `gim_anvil`, `gh_anvil` ("Giant
 * anvil"), `wint_anvil` ("Ornamental anvil"), `furnace2`, `bcs_furnace`, `viking_furnace2` and
 * `dwarf_keldagrim_furnace2`. They are decorative twins of the real thing and nothing this module
 * binds can ever fire on them.
 *
 * They are tagged anyway, deliberately. A content-group edit is *additive* once packed: deleting
 * the edit here does not restore the loc's original group in `.data/cache/game`, so dropping them
 * would leave this file disagreeing with every already-packed cache while a fresh clone got a
 * different world. Keeping them listed makes the packed cache reproducible. The tag is inert for
 * ops, and harmless for use-an-ore-on-it, which is a reasonable thing to allow on an anvil.
 *
 * `newbiefurnace` is included on purpose: it is Tutorial Island's furnace.
 */
object SmithingLocs : LocReferences() {
    val anvil = find("anvil")
    val dorics_anvil = find("dorics_anvil")
    val lumbridge_anvil = find("lumbridge_anvil")
    val sw_anvil = find("sw_anvil")
    val keldagrim_anvil = find("dwarf_keldagrim_anvil")
    val viking_anvil = find("viking_anvil")
    val darkm_anvil = find("darkm_anvil")
    val wint_anvil = find("wint_anvil")
    val gh_anvil = find("gh_anvil")
    val gim_anvil = find("gim_anvil")
    val dorgesh_anvil = find("dorgesh_blacksmith_anvil")

    val furnace = find("furnace")
    val furnace3 = find("furnace3")
    val keldagrim_furnace = find("dwarf_keldagrim_furnace")
    val viking_furnace = find("viking_furnace")
    val fairy_furnace = find("fairy_furnace")
    val swan_furnace = find("swan_furnace")
    val newbie_furnace = find("newbiefurnace")
    val furnace2 = find("furnace2")
    val keldagrim_furnace2 = find("dwarf_keldagrim_furnace2")
    val viking_furnace2 = find("viking_furnace2")
    val bcs_furnace = find("bcs_furnace")
}

object SmithingLocsEditor : LocEditor() {
    init {
        val anvils =
            listOf(
                SmithingLocs.anvil,
                SmithingLocs.dorics_anvil,
                SmithingLocs.lumbridge_anvil,
                SmithingLocs.sw_anvil,
                SmithingLocs.keldagrim_anvil,
                SmithingLocs.viking_anvil,
                SmithingLocs.darkm_anvil,
                SmithingLocs.dorgesh_anvil,
                SmithingLocs.wint_anvil,
                SmithingLocs.gh_anvil,
                SmithingLocs.gim_anvil,
            )
        for (type in anvils) {
            edit(type) { contentGroup = SmithingContent.smithing_anvil }
        }

        val furnaces =
            listOf(
                SmithingLocs.furnace,
                SmithingLocs.furnace3,
                SmithingLocs.keldagrim_furnace,
                SmithingLocs.viking_furnace,
                SmithingLocs.fairy_furnace,
                SmithingLocs.swan_furnace,
                SmithingLocs.newbie_furnace,
                SmithingLocs.furnace2,
                SmithingLocs.keldagrim_furnace2,
                SmithingLocs.viking_furnace2,
                SmithingLocs.bcs_furnace,
            )
        for (type in furnaces) {
            edit(type) { contentGroup = SmithingContent.smithing_furnace }
        }
    }
}

/**
 * Jagex's own anvil tables, shipped in the vanilla cache.
 *
 * These are the same three enums interface 312's client scripts read to draw the menu, so reading
 * them server-side keeps what the player is shown and what the server charges them in exact
 * agreement -- there is no second copy of the numbers to drift.
 */
object SmithingEnums : EnumReferences() {
    val product_to_quantity = find<ObjType, Int>("smithing_product_to_quantity")
    val product_to_bars_required = find<ObjType, Int>("smithing_product_to_bars_required")
    val product_to_requirement = find<ObjType, Int>("smithing_product_to_requirement")
}

object SmithingBarsEditor : ObjEditor() {
    init {
        val bars =
            listOf(
                SmithingObjs.bronze_bar,
                SmithingObjs.iron_bar,
                SmithingObjs.steel_bar,
                SmithingObjs.mithril_bar,
                SmithingObjs.adamantite_bar,
                SmithingObjs.runite_bar,
            )
        for (type in bars) {
            edit(type) { contentGroup = SmithingContent.smithing_bar }
        }
    }
}
