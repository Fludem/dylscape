package org.rsmod.content.skills.fletching.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences

/**
 * Every obj Fletching consumes or produces.
 *
 * Nothing here is edited or tagged: the skill is entirely relational -- a pair of objs makes a
 * third -- so the recipe tables in [FletchingRecipes] carry the data and the cache is left alone.
 * That is deliberate. Content-group edits are additive once packed, and removing one later does not
 * restore the obj's original group, so a skill that does not need them should not create them. It
 * would not be an option anyway for logs, which `FiremakingLogsEditor` already claims.
 */
object FletchingObjs : ObjReferences() {
    // Tools and universal materials.
    val bow_string = find("bow_string")
    val feather = find("feather")
    val arrow_shaft = find("arrow_shaft")
    val headless_arrow = find("headless_arrow")

    // Logs.
    val logs = find("logs")
    val oak_logs = find("oak_logs")
    val willow_logs = find("willow_logs")
    val maple_logs = find("maple_logs")
    val yew_logs = find("yew_logs")
    val magic_logs = find("magic_logs")
    val redwood_logs = find("redwood_logs")
    val teak_logs = find("teak_logs")
    val mahogany_logs = find("mahogany_logs")

    // Unstrung bows.
    val unstrung_shortbow = find("unstrung_shortbow")
    val unstrung_longbow = find("unstrung_longbow")
    val unstrung_oak_shortbow = find("unstrung_oak_shortbow")
    val unstrung_oak_longbow = find("unstrung_oak_longbow")
    val unstrung_willow_shortbow = find("unstrung_willow_shortbow")
    val unstrung_willow_longbow = find("unstrung_willow_longbow")
    val unstrung_maple_shortbow = find("unstrung_maple_shortbow")
    val unstrung_maple_longbow = find("unstrung_maple_longbow")
    val unstrung_yew_shortbow = find("unstrung_yew_shortbow")
    val unstrung_yew_longbow = find("unstrung_yew_longbow")
    val unstrung_magic_shortbow = find("unstrung_magic_shortbow")
    val unstrung_magic_longbow = find("unstrung_magic_longbow")

    // Strung bows.
    val shortbow = find("shortbow")
    val longbow = find("longbow")
    val oak_shortbow = find("oak_shortbow")
    val oak_longbow = find("oak_longbow")
    val willow_shortbow = find("willow_shortbow")
    val willow_longbow = find("willow_longbow")
    val maple_shortbow = find("maple_shortbow")
    val maple_longbow = find("maple_longbow")
    val yew_shortbow = find("yew_shortbow")
    val yew_longbow = find("yew_longbow")
    val magic_shortbow = find("magic_shortbow")
    val magic_longbow = find("magic_longbow")

    // Wooden shields.
    val oak_shield = find("oak_shield")
    val willow_shield = find("willow_shield")
    val maple_shield = find("maple_shield")
    val yew_shield = find("yew_shield")
    val magic_shield = find("magic_shield")
    val redwood_shield = find("redwood_shield")

    // Arrowheads and arrows.
    val bronze_arrowheads = find("bronze_arrowheads")
    val iron_arrowheads = find("iron_arrowheads")
    val steel_arrowheads = find("steel_arrowheads")
    val mithril_arrowheads = find("mithril_arrowheads")
    val adamant_arrowheads = find("adamant_arrowheads")
    val rune_arrowheads = find("rune_arrowheads")
    val amethyst_arrowheads = find("amethyst_arrowheads")
    val dragon_arrowheads = find("dragon_arrowheads")

    val bronze_arrow = find("bronze_arrow")
    val iron_arrow = find("iron_arrow")
    val steel_arrow = find("steel_arrow")
    val mithril_arrow = find("mithril_arrow")
    val adamant_arrow = find("adamant_arrow")
    val rune_arrow = find("rune_arrow")
    val amethyst_arrow = find("amethyst_arrow")
    val dragon_arrow = find("dragon_arrow")

    // Dart tips and darts.
    val bronze_dart_tip = find("bronze_dart_tip")
    val iron_dart_tip = find("iron_dart_tip")
    val steel_dart_tip = find("steel_dart_tip")
    val mithril_dart_tip = find("mithril_dart_tip")
    val adamant_dart_tip = find("adamant_dart_tip")
    val rune_dart_tip = find("rune_dart_tip")
    val amethyst_dart_tip = find("amethyst_dart_tip")
    val dragon_dart_tip = find("dragon_dart_tip")

    val bronze_dart = find("bronze_dart")
    val iron_dart = find("iron_dart")
    val steel_dart = find("steel_dart")
    val mithril_dart = find("mithril_dart")
    val adamant_dart = find("adamant_dart")
    val rune_dart = find("rune_dart")
    val amethyst_dart = find("amethyst_dart")
    val dragon_dart = find("dragon_dart")

    // Javelin heads and javelins.
    val javelin_shaft = find("javelin_shaft")
    val bronze_javelin_head = find("bronze_javelin_head")
    val iron_javelin_head = find("iron_javelin_head")
    val steel_javelin_head = find("steel_javelin_head")
    val mithril_javelin_head = find("mithril_javelin_head")
    val adamant_javelin_head = find("adamant_javelin_head")
    val rune_javelin_head = find("rune_javelin_head")
    val amethyst_javelin_head = find("amethyst_javelin_head")
    val dragon_javelin_head = find("dragon_javelin_head")

    val bronze_javelin = find("bronze_javelin")
    val iron_javelin = find("iron_javelin")
    val steel_javelin = find("steel_javelin")
    val mithril_javelin = find("mithril_javelin")
    val adamant_javelin = find("adamant_javelin")
    val rune_javelin = find("rune_javelin")
    val amethyst_javelin = find("amethyst_javelin")
    val dragon_javelin = find("dragon_javelin")

    // Unfeathered bolts and the bolts they become.
    val bronze_bolts_unf = find("xbows_crossbow_bolts_bronze_unfeathered")
    val blurite_bolts_unf = find("xbows_crossbow_bolts_blurite_unfeathered")
    val iron_bolts_unf = find("xbows_crossbow_bolts_iron_unfeathered")
    val silver_bolts_unf = find("xbows_crossbow_bolts_silver_unfeathered")
    val steel_bolts_unf = find("xbows_crossbow_bolts_steel_unfeathered")
    val mithril_bolts_unf = find("xbows_crossbow_bolts_mithril_unfeathered")
    val adamantite_bolts_unf = find("xbows_crossbow_bolts_adamantite_unfeathered")
    val runite_bolts_unf = find("xbows_crossbow_bolts_runite_unfeathered")
    val dragon_bolts_unf = find("dragon_bolts_unfeathered")

    val bronze_bolts = find("bolts")
    val blurite_bolts = find("xbows_crossbow_bolts_blurite")
    val iron_bolts = find("xbows_crossbow_bolts_iron")
    val silver_bolts = find("xbows_crossbow_bolts_silver")
    val steel_bolts = find("xbows_crossbow_bolts_steel")
    val mithril_bolts = find("xbows_crossbow_bolts_mithril")
    val adamantite_bolts = find("xbows_crossbow_bolts_adamantite")
    val runite_bolts = find("xbows_crossbow_bolts_runite")
    val dragon_bolts = find("dragon_bolts")

    // Crossbow parts.
    val crossbow_string = find("xbows_crossbow_string")

    val stock_wood = find("xbows_crossbow_stock_wood")
    val stock_oak = find("xbows_crossbow_stock_oak")
    val stock_willow = find("xbows_crossbow_stock_willow")
    val stock_teak = find("xbows_crossbow_stock_teak")
    val stock_maple = find("xbows_crossbow_stock_maple")
    val stock_mahogany = find("xbows_crossbow_stock_mahogany")
    val stock_yew = find("xbows_crossbow_stock_yew")
    val stock_magic = find("xbows_crossbow_stock_magic")

    val limbs_bronze = find("xbows_crossbow_limbs_bronze")
    val limbs_blurite = find("xbows_crossbow_limbs_blurite")
    val limbs_iron = find("xbows_crossbow_limbs_iron")
    val limbs_steel = find("xbows_crossbow_limbs_steel")
    val limbs_mithril = find("xbows_crossbow_limbs_mithril")
    val limbs_adamantite = find("xbows_crossbow_limbs_adamantite")
    val limbs_runite = find("xbows_crossbow_limbs_runite")
    val limbs_dragon = find("xbows_crossbow_limbs_dragon")

    val unstrung_bronze_crossbow = find("xbows_crossbow_unstrung_bronze")
    val unstrung_blurite_crossbow = find("xbows_crossbow_unstrung_blurite")
    val unstrung_iron_crossbow = find("xbows_crossbow_unstrung_iron")
    val unstrung_steel_crossbow = find("xbows_crossbow_unstrung_steel")
    val unstrung_mithril_crossbow = find("xbows_crossbow_unstrung_mithril")
    val unstrung_adamantite_crossbow = find("xbows_crossbow_unstrung_adamantite")
    val unstrung_runite_crossbow = find("xbows_crossbow_unstrung_runite")
    val unstrung_dragon_crossbow = find("xbows_crossbow_unstrung_dragon")

    val bronze_crossbow = find("xbows_crossbow_bronze")
    val blurite_crossbow = find("xbows_crossbow_blurite")
    val iron_crossbow = find("xbows_crossbow_iron")
    val steel_crossbow = find("xbows_crossbow_steel")
    val mithril_crossbow = find("xbows_crossbow_mithril")
    val adamantite_crossbow = find("xbows_crossbow_adamantite")
    val runite_crossbow = find("xbows_crossbow_runite")
    val dragon_crossbow = find("xbows_crossbow_dragon")
}

/**
 * Fletching's animations, all of which the cache already names.
 *
 * The per-tier bolt and dart seqs are not decoration: each shows the metal being worked, and the
 * live game plays the matching one. [FletchingRecipes] carries the seq on the recipe row for that
 * reason rather than picking one generic animation for everything.
 */
object FletchingSeqs : SeqReferences() {
    val cut_logs = find("human_fletching")
    val cut_single = find("human_fletching_single")
    val string_bow = find("human_fletching")

    val add_arrow_tips = find("human_fletching_add_arrow_tips")
    val add_feather = find("human_fletching_add_feather")

    val dart_feathers_bronze = find("human_fletching_add_dart_feathers_bronze")
    val dart_feathers_iron = find("human_fletching_add_dart_feathers_iron")
    val dart_feathers_steel = find("human_fletching_add_dart_feathers_steel")
    val dart_feathers_mithril = find("human_fletching_add_dart_feathers_mithril")
    val dart_feathers_adamant = find("human_fletching_add_dart_feathers_adamant")
    val dart_feathers_rune = find("human_fletching_add_dart_feathers_rune")
    val dart_feathers_dragon = find("human_fletching_add_dart_feathers_dragon")
    val dart_feathers_amethyst = find("human_fletching_add_dart_feathers_amethyst")

    val bolt_feathers_bronze = find("human_fletching_add_bolt_feathers_bronze")
    val bolt_feathers_blurite = find("human_fletching_add_bolt_feathers_blurite")
    val bolt_feathers_iron = find("human_fletching_add_bolt_feathers_iron")
    val bolt_feathers_silver = find("human_fletching_add_bolt_feathers_silver")
    val bolt_feathers_steel = find("human_fletching_add_bolt_feathers_steel")
    val bolt_feathers_mithril = find("human_fletching_add_bolt_feathers_mithril")
    val bolt_feathers_adamant = find("human_fletching_add_bolt_feathers_adamant")
    val bolt_feathers_rune = find("human_fletching_add_bolt_feathers_rune")
    val bolt_feathers_dragon = find("human_fletching_add_bolt_feathers_dragon")

    val crossbow_bronze = find("xbows_fletching_wood_bronze")
    val crossbow_blurite = find("xbows_fletching_oak_blurite")
    val crossbow_iron = find("xbows_fletching_willow_iron")
    val crossbow_steel = find("xbows_fletching_teak_steel")
    val crossbow_mithril = find("xbows_fletching_maple_mithril")
    val crossbow_adamantite = find("xbows_fletching_mahogany_adamantite")
    val crossbow_runite = find("xbows_fletching_yew_runite")
}
