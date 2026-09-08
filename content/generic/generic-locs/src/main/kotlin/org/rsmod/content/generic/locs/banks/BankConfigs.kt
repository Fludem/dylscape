package org.rsmod.content.generic.locs.banks

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.type.loc.LocType

internal typealias bank_locs = BankLocs

object BankLocs : LocReferences() {
    /**
     * Every loc whose op2 is "Bank" (booths, counters and bank tables), decoded from the rev 233
     * cache. Multiloc parents that carry the ops themselves are listed alongside their children.
     */
    val booths: List<LocType> =
        listOf(
                "bankbooth",
                "bankbooth_multi",
                "bankbooth_deadman",
                "bankbooth_end_left",
                "bankbooth_end_right",
                "bankbooth_ap1",
                "aide_bankbooth",
                "aide_bankbooth_multi",
                "aide_bankbooth_deadman",
                "fai_varrock_bankbooth",
                "fai_varrock_bankbooth_multi",
                "fai_varrock_bankbooth_deadman",
                "fai_falador_bankbooth",
                "fai_falador_bankbooth_multi",
                "fai_falador_bankbooth_deadman",
                "kr_bankbooth",
                "kr_bankbooth_multi",
                "kr_bankbooth_deadman",
                "ahoy_bankbooth",
                "ahoy_bankbooth_multi",
                "ahoy_bankbooth_deadman",
                "pest_bankbooth",
                "pest_bankbooth_multi",
                "pest_bankbooth_deadman",
                "contact_bank_booth",
                "contact_bank_booth_multi",
                "contact_bank_booth_deadman",
                "exchange_bank_wall_bank",
                "exchange_bank_wall_bank_3ops",
                "dwarf_keldagrim_bankbooth",
                "elid_bankbooth",
                "fever_bankbooth",
                "lunar_moonclan_bankbooth",
                "elf_village_bankcounter",
                "dorgesh_bank_booth",
                "canafis_bankbooth",
                "tob_surface_bankbooth",
                "prif_bankbooth_open",
                "darkm_bankbooth",
                "gim_island_bankbooth",
                "banktable_breakroute_bankable",
                "piscarilius_bank_booth_01",
                "piscarilius_bank_booth_02",
                "piscarilius_bank_booth_03",
                "piscarilius_bank_booth_04",
                "archeeus_bank_booth_open_01",
                "archeeus_bank_booth_open_02",
                "archeeus_bank_booth_open_03",
                "archeeus_bank_booth_open_04",
                "lova_bank_booth_01",
                "lova_bank_booth_02",
                "lova_bank_booth_03",
                "lova_bank_booth_04",
                "cam_torum_bank_table",
                "cam_torum_bank_table02",
                "table_bank01_civitas01",
                "table_bank01_civitas02",
                "booth_bank01_talkasti01",
            )
            .map { find(it) }

    /** Every bank chest whose op1 is "Use" or "Bank". */
    val chests: List<LocType> =
        listOf(
                "thbankchest",
                "castlewars_bankchest",
                "champions_bankchest",
                "diary_guild_bankchest",
                "barbassault_bank_chest",
                "fris_bank_chest_open",
                "blast_bank_chest",
                "fairy_chest_bank",
                "lovakengj_bank_chest_orange",
                "lovakengj_bank_chest_grey",
                "mm_desk_bankchest",
                "wcguild_bankchest",
                "tox_bank_chest",
                "tzhaar_bank_chest",
                "fossil_bank_chest_built",
                "fossil_volcano_bank_chest",
                "brimstone_bankchest",
                "soul_wars_bankchest",
                "tempoross_lobby_bank_chest",
                "bim_ruins_bankchest",
                "gotr_bankchest",
                "bh_bankchest",
                "raids_bank_chest_lobby_working",
                "colosseum_bank",
                "magictraining_bankchest",
                "duel_chestopen",
                "wint_bankchest",
                "gim_bank_chest",
                "fortis_bank_chest",
                "fortis_bank_chest_small",
                "bank_buffalo",
                "toa_bank_camel",
                "toa_bank_camel_named",
                "burgh_bankbooth_repaired",
                "hundred_goodchest_base",
                "deadman_arena_bank_chest",
            )
            .map { find(it) }
}

internal object BankLocEditor : LocEditor() {
    init {
        for (booth in bank_locs.booths) {
            edit(booth) { contentGroup = content.bank_booth }
        }
        for (chest in bank_locs.chests) {
            edit(chest) { contentGroup = content.bank_chest }
        }
    }
}
