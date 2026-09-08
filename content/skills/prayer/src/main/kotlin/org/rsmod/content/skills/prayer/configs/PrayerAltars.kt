package org.rsmod.content.skills.prayer.configs

import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.type.loc.LocType

/**
 * The altars a player can recharge prayer points at.
 *
 * Every loc here carries `Pray-at` or `Pray` on **op1** in the cache already, so the script binds
 * `onOpLoc1` and no op editing is needed. The list is the set of altars that are reachable in the
 * open world; three families are deliberately excluded:
 * - POH altars (`poh_altar_*`), because player-owned houses are not implemented. They are the
 *   gilded altar, so when houses land they should be tagged here *with* an offering multiplier.
 * - Altars whose op1 is not a prayer at all - `archeus_altar_*` (Bind/Venerate), `cata_altar`
 *   (Teleport), `templefire_altar` (Use), `poh_altar_ancient` (Venerate).
 * - Quest altars that only offer `Search` (`restless_ghost_altar*`, `cave_temple_altar`).
 */
object PrayerAltarLocs : LocReferences() {
    val altar = find("altar")
    val altar_v2 = find("altarv2")
    val guthix_altar = find("guthix_altar")
    val monks_altar = find("monks_altar")
    val varrock_church_altar = find("fai_varrock_church_altar")
    val slepe_church_altar = find("slp_church_altar")
    val shayzien_church_altar = find("shayzien_church_altar")
    val elf_village_altar = find("elf_village_altar")
    val arceuus_altar = find("arceuus_altar")
    val hosidius_altar = find("hosidius_altar")
    val lovakengj_altar = find("lovakengj_altar")
    val hosidius_dungeon_altar = find("hosdun_altar_normal")
    val hosidius_dungeon_altar_cursed = find("hosdun_altar_cursed")
    val darkmeyer_altar = find("darkm_altar")
    val wilderness_hub_altar = find("wildy_hub_altar")
    val clan_hall_altar = find("clan_medieval_altar")
    val clan_wars_altar = find("clanwars_tournament_altar_spellbook")
    val dragon_slayer_guild_altar = find("ds2_guild_altar")
    val nature_grotto_altar = find("druidic_spirit_grotto_naturealtar")
    val regicide_temple_altar = find("regicide_voyage_temple_altar")
    val high_priest_temple_altar = find("contact_high_priest_temple_altar")
    val zaros_altar = find("dt_zaros_altar")
    val nex_zaros_altar = find("nex_zaros_altar")
    val xerician_altar = find("tox_xerician_altar")
    val castlewars_saradomin_altar = find("castlewars_altar_saradomin")
    val castlewars_zamorak_altar = find("castlewars_altar_zamorak")
    val godwars_saradomin_altar = find("godwars_dungeon_saradomin_altar01")
    val godwars_zamorak_altar = find("godwars_dungeon_zamorak_altar01")
    val godwars_armadyl_altar = find("godwars_dungeon_armadyl_altar01")
    val godwars_bandos_altar = find("godwars_dungeon_bandos_altar01")

    /**
     * The three Wilderness Chaos altars. These both recharge prayer *and* accept bone offerings, so
     * they are the only locs that get the offering params.
     */
    val chaos_altar = find("chaosaltar")
    val chaos_altar_trapped = find("trappedchaosaltar")
    val chaos_altar_thrantax = find("thrantaxaltar")
}

private typealias altars = PrayerAltarLocs

internal object PrayerAltarsEditor : LocEditor() {
    init {
        restore(altars.altar)
        restore(altars.altar_v2)
        restore(altars.guthix_altar)
        restore(altars.monks_altar)
        restore(altars.varrock_church_altar)
        restore(altars.slepe_church_altar)
        restore(altars.shayzien_church_altar)
        restore(altars.elf_village_altar)
        restore(altars.arceuus_altar)
        restore(altars.hosidius_altar)
        restore(altars.lovakengj_altar)
        restore(altars.hosidius_dungeon_altar)
        restore(altars.hosidius_dungeon_altar_cursed)
        restore(altars.darkmeyer_altar)
        restore(altars.wilderness_hub_altar)
        restore(altars.clan_hall_altar)
        restore(altars.clan_wars_altar)
        restore(altars.dragon_slayer_guild_altar)
        restore(altars.nature_grotto_altar)
        restore(altars.regicide_temple_altar)
        restore(altars.high_priest_temple_altar)
        restore(altars.zaros_altar)
        restore(altars.nex_zaros_altar)
        restore(altars.xerician_altar)
        restore(altars.castlewars_saradomin_altar)
        restore(altars.castlewars_zamorak_altar)
        restore(altars.godwars_saradomin_altar)
        restore(altars.godwars_zamorak_altar)
        restore(altars.godwars_armadyl_altar)
        restore(altars.godwars_bandos_altar)

        // 3.5x xp with a 50% chance the bone survives - an effective 7x, and the reason the Chaos
        // altar is worth the Wilderness trip.
        offering(altars.chaos_altar)
        offering(altars.chaos_altar_trapped)
        offering(altars.chaos_altar_thrantax)
    }

    /** Recharges prayer points on op1 but refuses bone offerings. */
    private fun restore(type: LocType) = edit(type) { contentGroup = PrayerContent.prayer_altar }

    private fun offering(type: LocType) =
        edit(type) {
            contentGroup = PrayerContent.prayer_altar
            param[PrayerParams.offer_xp_percent] = 350
            param[PrayerParams.offer_keep_percent] = 50
        }
}
