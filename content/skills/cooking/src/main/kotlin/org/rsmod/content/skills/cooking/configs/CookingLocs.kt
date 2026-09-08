package org.rsmod.content.skills.cooking.configs

import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences

/**
 * Every range, stove, oven and cooker a player can cook at, plus the two fire locs.
 *
 * **Decoded, not assumed.** The rev 233 cache has 51 locs carrying a `Cook` op. Forty-two of them
 * are ordinary ranges with `Cook` on op1 and are all listed here, whatever their display name
 * ("Range", "Cooking range", "Stove", "Oven", "Clay oven", "Cooker", "Gnome cooker", "Cooking pot",
 * "Cooking stove", "Simple stove"). The nine left out are deliberate:
 * - `gauntlet_range` and `gauntlet_range_hm` belong to the Gauntlet minigame;
 * - `poh_stove_7`, `poh_stove_7_kettle` and `poh_stove_7_pot` are player-owned-house furniture with
 *   `Cook` on op2 and no house system to sit in;
 * - `tempoross_shrine_fire` carries `Cook-at`, a minigame verb;
 * - the two `poh_jewellery_box_*_cooking_guild` locs merely teleport to the Cooking Guild.
 *
 * `carnilleanrange` ("Range", but `Inspect` on op1) and `range_noop` (no ops at all) are quest and
 * scenery lookalikes and are not tagged either.
 *
 * Two fires are tagged. `fire` (26185) is the one firemaking lights: it carries **no ops**, so the
 * only way to cook on it is to use food on it, exactly as in the live game. `fire_cook` (43475) is
 * a "Fire" with `Cook` on op1, and is bound so that it works if it is ever placed.
 *
 * `newbierange` is Tutorial Island's range and `tut2_range` its newer twin.
 */
object CookingLocs : LocReferences() {
    val cooks_quest_range = find("cooksquestrange")
    val fai_varrock_range = find("fai_varrock_range")
    val fai_varrock_range_with_pipe = find("fai_varrock_range_with_pipe")
    val fai_varrock_posh_stove = find("fai_varrock_posh_stove")
    val elf_village_range = find("elf_village_range")
    val rimmington_poor_range = find("rimmington_poor_range")
    val newbie_range = find("newbierange")
    val elid_clay_oven = find("elid_clay_oven")
    val fairy_range = find("fairy_range")
    val dave_stove = find("100_dave_stove")
    val swan_stove = find("swan_stove")
    val ahoy_range = find("ahoy_range")
    val lunar_moonclan_cooker_small = find("lunar_moonclan_cooker_small")
    val lunar_pirate_cooking_range = find("lunar_pirate_cooking_range")
    val aluft_gnome_cooker = find("aluft_gnome_cooker")
    val iznot_clay_range = find("iznot_clay_range")
    val sorceress_range = find("sorceress_range")
    val dorgesh_cooking_range1 = find("dorgesh_cooking_range1")
    val dorgesh_cooking_range2 = find("dorgesh_cooking_range2")
    val kr_sin_range = find("kr_sin_range")
    val range = find("range")
    val hos_cooking_range = find("hos_cooking_range")
    val hos_cooking_range_02 = find("hos_cooking_range_02")
    val piscarilius_range = find("piscarilius_range")
    val fossil_range_built = find("fossil_range_built")
    val ds2_guild_cooking_range = find("ds2_guild_cooking_range")
    val slp_poor_stove = find("slp_poor_stove")
    val slp_stove = find("slp_stove")
    val tut2_range = find("tut2_range")
    val darkm_poor_range = find("darkm_poor_range")
    val darkm_middle_range = find("darkm_middle_range")
    val darkm_rich_range = find("darkm_rich_range")
    val con_contract_falador_south_range_fixed = find("con_contract_falador_south_range_fixed")
    val range_lassar01_default01_shadow = find("range_lassar01_default01_shadow")
    val range_lassar01_default01 = find("range_lassar01_default01")
    val mah3_base_range = find("mah3_base_range")
    val pmoon_range = find("pmoon_range")
    val cam_torum_stove = find("cam_torum_stove")
    val fortis_stove = find("fortis_stove")
    val luc2_mov_kitchen_range = find("luc2_mov_kitchen_range")
    val furnace_clay01_twilight = find("furnace_clay01_twilight")
    val stove_clay01_talkasti01 = find("stove_clay01_talkasti01")

    val fire = find("fire")
    val fire_cook = find("fire_cook")

    val ranges =
        listOf(
            cooks_quest_range,
            fai_varrock_range,
            fai_varrock_range_with_pipe,
            fai_varrock_posh_stove,
            elf_village_range,
            rimmington_poor_range,
            newbie_range,
            elid_clay_oven,
            fairy_range,
            dave_stove,
            swan_stove,
            ahoy_range,
            lunar_moonclan_cooker_small,
            lunar_pirate_cooking_range,
            aluft_gnome_cooker,
            iznot_clay_range,
            sorceress_range,
            dorgesh_cooking_range1,
            dorgesh_cooking_range2,
            kr_sin_range,
            range,
            hos_cooking_range,
            hos_cooking_range_02,
            piscarilius_range,
            fossil_range_built,
            ds2_guild_cooking_range,
            slp_poor_stove,
            slp_stove,
            tut2_range,
            darkm_poor_range,
            darkm_middle_range,
            darkm_rich_range,
            con_contract_falador_south_range_fixed,
            range_lassar01_default01_shadow,
            range_lassar01_default01,
            mah3_base_range,
            pmoon_range,
            cam_torum_stove,
            fortis_stove,
            luc2_mov_kitchen_range,
            furnace_clay01_twilight,
            stove_clay01_talkasti01,
        )

    val fires = listOf(fire, fire_cook)
}

internal object CookingLocsEditor : LocEditor() {
    init {
        for (type in CookingLocs.ranges) {
            edit(type) { contentGroup = CookingContent.cooking_range }
        }
        for (type in CookingLocs.fires) {
            edit(type) { contentGroup = CookingContent.cooking_fire }
        }
    }
}
