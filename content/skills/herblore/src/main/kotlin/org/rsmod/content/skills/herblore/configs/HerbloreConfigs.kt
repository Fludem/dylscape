package org.rsmod.content.skills.herblore.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences

/**
 * Every obj Herblore consumes or produces, except the dosed potions themselves.
 *
 * The doses are not here. There are five hundred of them and they follow a strict naming
 * convention, so [PotionFamily] resolves them from a family stem inside its own `ObjReferences`
 * init rather than repeating the table as several hundred `val`s. That is the same trade
 * `ConsumableFamily` makes, and for the same reason: an unknown name is a hard boot failure that
 * names the typo either way.
 *
 * Nothing here is in `BaseObjs` - it holds `knife` and `chisel` and almost nothing else - so
 * Herblore cannot write `objs.vial_water` the way Fletching writes `objs.knife`.
 *
 * Four spellings below are the cache's own and not typos. `.data/symbols/obj.sym` is the authority,
 * never the wiki:
 * - grimy herbs are `unidentified_*`, never `grimy_*`; the only three `grimy_*` objs in the cache
 *   belong to the Chambers of Xeric
 * - the *unfinished* marrentill potion carries two r's (`marrentillvial`) while the clean and grimy
 *   forms carry one (`marentill`, `unidentified_marentill`)
 * - potato cactus is `cactus_potato`, reversed
 * - the araxyte venom sack ends in `ck`
 */
object HerbloreObjs : ObjReferences() {
    // Tools and vessels.
    val pestle_and_mortar = find("pestle_and_mortar")
    val vial_water = find("vial_water")
    val vial_empty = find("vial_empty")
    val vial_blood = find("vial_blood")
    val vial_coconut_milk = find("vial_coconut_milk")

    // Grimy herbs. `unidentified_*`, not `grimy_*`.
    val unidentified_guam = find("unidentified_guam")
    val unidentified_marentill = find("unidentified_marentill")
    val unidentified_tarromin = find("unidentified_tarromin")
    val unidentified_harralander = find("unidentified_harralander")
    val unidentified_ranarr = find("unidentified_ranarr")
    val unidentified_toadflax = find("unidentified_toadflax")
    val unidentified_irit = find("unidentified_irit")
    val unidentified_avantoe = find("unidentified_avantoe")
    val unidentified_kwuarm = find("unidentified_kwuarm")
    val unidentified_huasca = find("unidentified_huasca")
    val unidentified_snapdragon = find("unidentified_snapdragon")
    val unidentified_cadantine = find("unidentified_cadantine")
    val unidentified_lantadyme = find("unidentified_lantadyme")
    val unidentified_dwarf_weed = find("unidentified_dwarf_weed")
    val unidentified_torstol = find("unidentified_torstol")

    // Clean herbs. Three carry a suffix and the rest are bare.
    val guam_leaf = find("guam_leaf")
    val marentill = find("marentill")
    val tarromin = find("tarromin")
    val harralander = find("harralander")
    val ranarr_weed = find("ranarr_weed")
    val toadflax = find("toadflax")
    val irit_leaf = find("irit_leaf")
    val avantoe = find("avantoe")
    val kwuarm = find("kwuarm")
    val huasca = find("huasca")
    val snapdragon = find("snapdragon")
    val cadantine = find("cadantine")
    val lantadyme = find("lantadyme")
    val dwarf_weed = find("dwarf_weed")
    val torstol = find("torstol")

    // Unfinished potions. Note `marrentillvial`: two r's, unlike the herb.
    val guamvial = find("guamvial")
    val marrentillvial = find("marrentillvial")
    val tarrominvial = find("tarrominvial")
    val harralandervial = find("harralandervial")
    val ranarrvial = find("ranarrvial")
    val toadflaxvial = find("toadflaxvial")
    val iritvial = find("iritvial")
    val avantoevial = find("avantoevial")
    val kwuarmvial = find("kwuarmvial")
    val huascavial = find("huascavial")
    val snapdragonvial = find("snapdragonvial")
    val cadantinevial = find("cadantinevial")
    val lantadymevial = find("lantadymevial")
    val dwarfweedvial = find("dwarfweedvial")
    val torstolvial = find("torstolvial")

    /**
     * Cadantine mixed into a vial of blood rather than water, for bastion and battlemage.
     *
     * The only unfinished potion whose base is not water, which is why it sits apart from the
     * fifteen above and why the unfinished table carries the base obj per row.
     */
    val cadantine_bloodvial = find("cadantine_bloodvial")

    /** Coconut-milk unfinished potions. The two antidotes, and nothing else. */
    val unfinished_antidote_plus = find("unfinished_antidote+")
    val unfinished_antidote_plusplus = find("unfinished_antidote++")

    // Secondaries, in roughly the order a player meets them.
    val eye_of_newt = find("eye_of_newt")
    val unicorn_horn = find("unicorn_horn")
    val unicorn_horn_dust = find("unicorn_horn_dust")
    val limpwurt_root = find("limpwurt_root")
    val red_spiders_eggs = find("red_spiders_eggs")
    val chocolate_bar = find("chocolate_bar")
    val chocolate_dust = find("chocolate_dust")
    val white_berries = find("white_berries")
    val toads_legs = find("toads_legs")
    val desert_goat_horn = find("desert_goat_horn")
    val ground_desert_goat_horn = find("ground_desert_goat_horn")
    val snape_grass = find("snape_grass")
    val mortmyremushroom = find("mortmyremushroom")
    val huntingbeast_sabreteeth = find("huntingbeast_sabreteeth")
    val huntingbeast_sabreteeth_dust = find("huntingbeast_sabreteeth_dust")
    val aldarium = find("aldarium")
    val blue_dragon_scale = find("blue_dragon_scale")
    val dragon_scale_dust = find("dragon_scale_dust")
    val yew_roots = find("yew_roots")
    val magic_roots = find("magic_roots")
    val wine_of_zamorak = find("wine_of_zamorak")
    val cactus_potato = find("cactus_potato")
    val jangerberries = find("jangerberries")
    val bird_nest_empty = find("bird_nest_empty")
    val crushed_bird_nest = find("crushed_bird_nest")
    val demonic_tallow = find("demonic_tallow")
    val nihil_dust = find("nihil_dust")
    val lily_of_the_sands = find("lily_of_the_sands")
    val lava_shard = find("lava_shard")
    val crushed_dragon_bones = find("crushed_dragon_bones")
    val ancient_essence = find("ancient_essence")
    val snakeboss_scale = find("snakeboss_scale")
    val araxyte_venom_sack = find("araxyte_venom_sack")
    val amylase = find("amylase")
    val sote_crystal_dust = find("sote_crystal_dust")
    val ashes = find("ashes")
}

object HerbloreSeqs : SeqReferences() {
    /** 363. Tipping a herb or a secondary into a vial. */
    val mix = find("human_herbing_vial")

    /** 364. The pestle and mortar. */
    val grind = find("human_herbing_grind")

    /**
     * 3284. Raising a vial to the mouth.
     *
     * The consumables module drinks with `human_drink_rum` (1194), which raises a tankard. This is
     * the only vial-shaped drink animation in the cache, and a potion is a vial.
     */
    val drink = find("human_drink_from_vial_cadava")
}

object HerbloreTimers : TimerReferences() {
    /**
     * Clears `varbits.stamina_active` when the last dose runs out.
     *
     * A *soft* timer on purpose: it is a cosmetic-and-mechanical buff, not a queued action, so it
     * should keep running while the player is doing something else and should not survive a logout
     * the way a hard timer would.
     */
    val stamina_expire = find("herblore_stamina_expire")
}
