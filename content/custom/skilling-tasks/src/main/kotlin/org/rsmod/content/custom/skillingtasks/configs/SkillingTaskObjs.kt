@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.skillingtasks.configs

import org.rsmod.api.type.refs.obj.ObjReferences

internal typealias skilling_task_objs = SkillingTaskObjs

/**
 * Every product a task can count, as this module's own references so a task table typo fails the
 * boot by name. Anvil products come from `SmithingProductObjs` instead, which already names them.
 */
object SkillingTaskObjs : ObjReferences() {
    val logs = find("logs")
    val oak_logs = find("oak_logs")
    val willow_logs = find("willow_logs")
    val maple_logs = find("maple_logs")
    val yew_logs = find("yew_logs")
    val magic_logs = find("magic_logs")

    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val silver_ore = find("silver_ore")
    val coal = find("coal")
    val gold_ore = find("gold_ore")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")

    val raw_shrimp = find("raw_shrimp")
    val raw_sardine = find("raw_sardine")
    val raw_herring = find("raw_herring")
    val raw_trout = find("raw_trout")
    val raw_salmon = find("raw_salmon")
    val raw_tuna = find("raw_tuna")
    val raw_lobster = find("raw_lobster")
    val raw_swordfish = find("raw_swordfish")
    val raw_monkfish = find("raw_monkfish")
    val raw_shark = find("raw_shark")

    val shrimp = find("shrimp")
    val trout = find("trout")
    val salmon = find("salmon")
    val tuna = find("tuna")
    val lobster = find("lobster")
    val swordfish = find("swordfish")
    val monkfish = find("monkfish")
    val shark = find("shark")

    val bronze_bar = find("bronze_bar")
    val iron_bar = find("iron_bar")
    val steel_bar = find("steel_bar")
    val mithril_bar = find("mithril_bar")
    val adamantite_bar = find("adamantite_bar")
    val runite_bar = find("runite_bar")

    val arrow_shaft = find("arrow_shaft")
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

    val leather_gloves = find("leather_gloves")
    val leather_boots = find("leather_boots")
    val leather_cowl = find("leather_cowl")
    val leather_vambraces = find("leather_vambraces")
    val leather_armour = find("leather_armour")
    val leather_chaps = find("leather_chaps")
    val bow_string = find("bow_string")
    val sapphire = find("sapphire")
    val emerald = find("emerald")
    val ruby = find("ruby")
    val diamond = find("diamond")

    // Four-dose heads, the objs `PotionMixing` publishes. The cache names the strength potion
    // `strength4` and every other ladder `4dose<stem>`.
    val attack_potion = find("4dose1attack")
    val antipoison = find("4doseantipoison")
    val strength_potion = find("strength4")
    val restore_potion = find("4dosestatrestore")
    val energy_potion = find("4dose1energy")
    val defence_potion = find("4dose1defense")
    val prayer_potion = find("4doseprayerrestore")
    val super_attack_potion = find("4dose2attack")
    val super_energy_potion = find("4dose2energy")
    val super_strength_potion = find("4dose2strength")
    val super_restore_potion = find("4dose2restore")
    val super_defence_potion = find("4dose2defense")
    val ranging_potion = find("4doserangerspotion")
    val magic_potion = find("4dose1magic")
}
