package org.rsmod.content.skills.magic.utility.configs

import org.rsmod.api.type.refs.content.ContentReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.synth.SynthReferences
import org.rsmod.game.type.param.ParamType

public typealias utility_objs = UtilityObjs

public typealias utility_seqs = UtilitySeqs

public typealias utility_spotanims = UtilitySpotanims

public typealias utility_synths = UtilitySynths

public typealias utility_locs = UtilityLocs

public typealias utility_content = UtilityContent

public typealias utility_params = UtilityParams

/** Objs the utility spells consume or produce that `BaseObjs` does not already name. */
public object UtilityObjs : ObjReferences() {
    val cosmic_rune = find("cosmicrune")
    val astral_rune = find("astralrune")

    val banana = find("banana")
    val peach = find("peach")

    val unpowered_orb = find("stafforb")
    val air_orb = find("air_orb")
    val water_orb = find("water_orb")
    val earth_orb = find("earth_orb")
    val fire_orb = find("fire_orb")

    val saradomin_cape = find("saradomin_cape")
    val guthix_cape = find("guthix_cape")
    val zamorak_cape = find("zamorak_cape")

    val hunter_kit = find("dream_hunter_box")

    // Humidify
    val vial = find("vial_empty")
    val vial_of_water = find("vial_water")
    val bucket = find("bucket_empty")
    val bucket_of_water = find("bucket_water")
    val bowl = find("bowl_empty")
    val bowl_of_water = find("bowl_water")
    val jug = find("jug_empty")
    val jug_of_water = find("jug_water")
    val watering_can_empty = find("watering_can_0")
    val watering_can_1 = find("watering_can_1")
    val watering_can_2 = find("watering_can_2")
    val watering_can_3 = find("watering_can_3")
    val watering_can_4 = find("watering_can_4")
    val watering_can_5 = find("watering_can_5")
    val watering_can_6 = find("watering_can_6")
    val watering_can_7 = find("watering_can_7")
    val watering_can_full = find("watering_can_8")

    // Superglass Make, Spin Flax
    val seaweed = find("seaweed")
    val bucket_of_sand = find("bucket_sand")
    val molten_glass = find("molten_glass")
    val flax = find("flax")
    val bow_string = find("bow_string")

    // Tan Leather
    val cow_hide = find("cow_hide")
    val leather = find("leather")
    val green_dragonhide = find("dragonhide_green")
    val blue_dragonhide = find("dragonhide_blue")
    val red_dragonhide = find("dragonhide_red")
    val black_dragonhide = find("dragonhide_black")
    val green_dragon_leather = find("dragon_leather")
    val blue_dragon_leather = find("dragon_leather_blue")
    val red_dragon_leather = find("dragon_leather_red")
    val black_dragon_leather = find("dragon_leather_black")

    // Plank Make
    val logs = find("logs")
    val oak_logs = find("oak_logs")
    val teak_logs = find("teak_logs")
    val mahogany_logs = find("mahogany_logs")
    val plank = find("woodplank")
    val oak_plank = find("plank_oak")
    val teak_plank = find("plank_teak")
    val mahogany_plank = find("plank_mahogany")

    // String Jewellery
    val gold_amulet_u = find("unstrung_gold_amulet")
    val gold_amulet = find("strung_gold_amulet")
    val sapphire_amulet_u = find("unstrung_sapphire_amulet")
    val sapphire_amulet = find("strung_sapphire_amulet")
    val emerald_amulet_u = find("unstrung_emerald_amulet")
    val emerald_amulet = find("strung_emerald_amulet")
    val ruby_amulet_u = find("unstrung_ruby_amulet")
    val ruby_amulet = find("strung_ruby_amulet")
    val diamond_amulet_u = find("unstrung_diamond_amulet")
    val diamond_amulet = find("strung_diamond_amulet")
    val dragonstone_amulet_u = find("unstrung_dragonstone_amulet")
    val dragonstone_amulet = find("strung_dragonstone_amulet")
    val onyx_amulet_u = find("unstrung_onyx_amulet")
    val onyx_amulet = find("strung_onyx_amulet")
    val zenyte_amulet_u = find("unstrung_zenyte_amulet")
    val zenyte_amulet = find("zenyte_amulet")
    val opal_amulet_u = find("unstrung_opal_amulet")
    val opal_amulet = find("strung_opal_amulet")
    val jade_amulet_u = find("unstrung_jade_amulet")
    val jade_amulet = find("strung_jade_amulet")
    val topaz_amulet_u = find("unstrung_topaz_amulet")
    val topaz_amulet = find("strung_topaz_amulet")

    // Bake Pie (uncooked -> cooked)
    val uncooked_redberry_pie = find("uncooked_redberry_pie")
    val redberry_pie = find("redberry_pie")
    val uncooked_meat_pie = find("uncooked_meat_pie")
    val meat_pie = find("meat_pie")
    val uncooked_mud_pie = find("uncooked_mud_pie")
    val mud_pie = find("mud_pie")
    val uncooked_apple_pie = find("uncooked_apple_pie")
    val apple_pie = find("apple_pie")
    val uncooked_garden_pie = find("uncooked_garden_pie")
    val garden_pie = find("garden_pie")
    val uncooked_fish_pie = find("uncooked_fish_pie")
    val fish_pie = find("fish_pie")
    val uncooked_botanical_pie = find("uncooked_botanical_pie")
    val botanical_pie = find("botanical_pie")
    val uncooked_mushroom_pie = find("uncooked_mushroom_pie")
    val mushroom_pie = find("mushroom_pie")
    val uncooked_admiral_pie = find("uncooked_admiral_pie")
    val admiral_pie = find("admiral_pie")
    val uncooked_dragonfruit_pie = find("uncooked_dragonfruit_pie")
    val dragonfruit_pie = find("dragonfruit_pie")
    val uncooked_wild_pie = find("uncooked_wild_pie")
    val wild_pie = find("wild_pie")
    val uncooked_summer_pie = find("uncooked_summer_pie")
    val summer_pie = find("summer_pie")

    // Recharge Dragonstone
    val amulet_of_glory = find("amulet_of_glory")
    val amulet_of_glory_1 = find("amulet_of_glory_1")
    val amulet_of_glory_2 = find("amulet_of_glory_2")
    val amulet_of_glory_3 = find("amulet_of_glory_3")
    val amulet_of_glory_4 = find("amulet_of_glory_4")
    val ring_of_wealth = find("ring_of_wealth")
    val ring_of_wealth_1 = find("ring_of_wealth_1")
    val ring_of_wealth_2 = find("ring_of_wealth_2")
    val ring_of_wealth_3 = find("ring_of_wealth_3")
    val ring_of_wealth_4 = find("ring_of_wealth_4")
    val ring_of_wealth_5 = find("ring_of_wealth_5")
    val skills_necklace = find("jewl_necklace_of_skills")
    val skills_necklace_1 = find("jewl_necklace_of_skills_1")
    val skills_necklace_2 = find("jewl_necklace_of_skills_2")
    val skills_necklace_3 = find("jewl_necklace_of_skills_3")
    val skills_necklace_4 = find("jewl_necklace_of_skills_4")
    val combat_bracelet = find("jewl_bracelet_of_combat")
    val combat_bracelet_1 = find("jewl_bracelet_of_combat_1")
    val combat_bracelet_2 = find("jewl_bracelet_of_combat_2")
    val combat_bracelet_3 = find("jewl_bracelet_of_combat_3")
    val combat_bracelet_4 = find("jewl_bracelet_of_combat_4")

    // Enchant: bases
    val sapphire_ring = find("sapphire_ring")
    val sapphire_necklace = find("sapphire_necklace")
    val sapphire_bracelet = find("jewl_sapphire_bracelet")
    val opal_ring = find("opal_ring")
    val opal_necklace = find("opal_necklace")
    val opal_bracelet = find("opal_bracelet")
    val emerald_ring = find("emerald_ring")
    val emerald_necklace = find("emerald_necklace")
    val emerald_bracelet = find("jewl_emerald_bracelet")
    val jade_ring = find("jade_ring")
    val jade_necklace = find("jade_necklace")
    val jade_bracelet = find("jade_bracelet")
    val ruby_ring = find("ruby_ring")
    val ruby_necklace = find("ruby_necklace")
    val ruby_bracelet = find("jewl_ruby_bracelet")
    val topaz_ring = find("topaz_ring")
    val topaz_necklace = find("topaz_necklace")
    val topaz_bracelet = find("topaz_bracelet")
    val diamond_ring = find("diamond_ring")
    val diamond_necklace = find("diamond_necklace")
    val diamond_bracelet = find("jewl_diamond_bracelet")
    val dragonstone_ring = find("dragonstone_ring")
    val dragonstone_necklace = find("dragonstone_necklace")
    val dragonstone_bracelet = find("jewl_dragonstone_bracelet")
    val onyx_ring = find("onyx_ring")
    val onyx_necklace = find("onyx_necklace")
    val onyx_bracelet = find("jewl_onyx_bracelet")
    val zenyte_ring = find("zenyte_ring")
    val zenyte_necklace = find("zenyte_necklace")
    val zenyte_bracelet = find("zenyte_bracelet")

    // Enchant: products
    val ring_of_recoil = find("ring_of_recoil")
    val games_necklace = find("necklace_of_minigames_8")
    val amulet_of_magic = find("amulet_of_magic")
    val bracelet_of_clay = find("jewl_bracelet_of_clay")
    val ring_of_pursuit = find("ring_of_pursuit")
    val dodgy_necklace = find("dodgy_necklace")
    val amulet_of_bounty = find("amulet_of_bounty")
    val expeditious_bracelet = find("expeditious_bracelet")
    val ring_of_dueling = find("ring_of_dueling_8")
    val binding_necklace = find("magic_emerald_necklace")
    val amulet_of_defence = find("amulet_of_defence")
    val castle_wars_bracelet = find("jewl_castlewars_bracelet3")
    val ring_of_returning = find("ring_of_returning_5")
    val necklace_of_passage = find("necklace_of_passage_5")
    val amulet_of_chemistry = find("amulet_of_chemistry")
    val flamtaer_bracelet = find("flamtaer_bracelet")
    val ring_of_forging = find("ring_of_forging")
    val digsite_pendant = find("necklace_of_digsite_5")
    val amulet_of_strength = find("amulet_of_strength")
    val inoculation_bracelet = find("jewl_bracelet_of_innoculation")
    val efaritays_aid = find("vampyre_ring")
    val necklace_of_faith = find("necklace_of_faith")
    val burning_amulet = find("burning_amulet_5")
    val bracelet_of_slaughter = find("bracelet_of_slaughter")
    val ring_of_life = find("ring_of_life")
    val phoenix_necklace = find("jewl_necklace_of_phoenix")
    val amulet_of_power = find("amulet_of_power")
    val abyssal_bracelet = find("jewl_runerunning_bracelet_5")
    val ring_of_stone = find("enchanted_onyx_ring")
    val berserker_necklace = find("jewl_beserker_necklace")
    val amulet_of_fury = find("enchanted_onyx_amulet")
    val regen_bracelet = find("jewl_bracelet_regen")
    val ring_of_suffering = find("zenyte_ring_enchanted")
    val necklace_of_anguish = find("zenyte_necklace_enchanted")
    val amulet_of_torture = find("zenyte_amulet_enchanted")
    val tormented_bracelet = find("zenyte_bracelet_enchanted")
}

public object UtilitySeqs : SeqReferences() {
    val low_alchemy = find("human_castlowlvlalchemy")
    val high_alchemy = find("human_casthighlvlalchemy")
    val superheat = find("human_castsuperheatitem")
    val bones_to_food = find("human_castbonestobananas")
    val charge_orb = find("human_castchargeorb")
    val enchant_amulet_1 = find("human_enchantamuletlvl1")
    val enchant_amulet_2 = find("human_enchantamuletlvl2")
    val enchant_amulet_3 = find("human_enchantamuletlvl3")
    val enchant_ring = find("human_cast_enchantring")
    val charge = find("human_casting")
    val vengeance = find("vengeance_spell_anim_nostalling")
    val humidify = find("dream_player_humidify_spell")
    val plank_make = find("dream_player_make_plank_spell")
    val hunter_kit = find("dream_player_hunter_box_spell")
    val bake_pie = find("quest_lunar_bakepie")
    val fertile_soil = find("quest_lunar_spell_raining_fertile_soil")
    val cure_plant = find("quest_lunar_cure_plant")
    /** The generic Lunar cast, used where the cache names nothing more specific. */
    val lunar_cast = find("lunar_human_magic_summon2")
}

public object UtilitySpotanims : SpotanimReferences() {
    val low_alchemy = find("lowlvlalchemy_casting")
    val high_alchemy = find("highlvlalchemy_casting")
    val superheat = find("superheatitem_casting")
    val bones_to_food = find("bonestobananas_casting")
    val charge_water_orb = find("chargewaterorb_casting")
    val charge_air_orb = find("chargewindorb_casting")
    val charge_earth_orb = find("chargeearthorb_casting")
    val charge_fire_orb = find("chargefireorb_casting")
    val enchant_amulet_1 = find("enchant_amulet_lvl1")
    val enchant_amulet_2 = find("enchant_amulet_lvl2")
    val enchant_amulet_3 = find("enchant_amulet_lvl3")
    val enchant_amulet_4 = find("enchant_amulet2_lvl4")
    val enchant_amulet_5 = find("enchant_amulet2_lvl5")
    val enchant_amulet_6 = find("enchant_amulet2_lvl6")
    val enchant_ring = find("enchant_ring")
    val vengeance = find("quest_lunar_spellbook_vengeance_spot_anim")
    val humidify = find("dream_humidify_spell_spotanim")
    val superglass = find("quest_lunar_magic_superglass")
    val spin_flax = find("quest_lunar_magic_flax_strim")
    val string_jewellery = find("quest_lunar_spellbook_string_jewelery_spot_anim")
    val tan_leather = find("lunar_tan_leather")
    val plank_make = find("dream_plank_make_spell_spotanim")
    val bake_pie = find("quest_lunar_spellbook_bake_pie_spot_anim")
    val magic_imbue = find("quest_lunar_spellbook_magic_embue_spot_anim")
    val fertile_soil = find("quest_lunar_spellbook_fertile_soil_spot_anim")
    val cure_plant = find("quest_lunar_cure_plant_spot")
    val recharge_dragonstone = find("lunar_enchant_dragonstone")
}

/** No Lunar spell has a named sound in this cache, so the Lunar casts are silent on purpose. */
public object UtilitySynths : SynthReferences() {
    val low_alchemy = find("low_alchemy")
    val high_alchemy = find("high_alchemy")
    val superheat = find("superheat_all")
    val bones_to_food = find("bones_to_bananas_all")
    val charge_air_orb = find("charge_air_orb")
    val charge_water_orb = find("charge_water_orb")
    val charge_earth_orb = find("charge_earth_orb")
    val charge_fire_orb = find("charge_fire_orb")
    val enchant_sapphire_amulet = find("enchant_sapphire_amulet")
    val enchant_sapphire_ring = find("enchant_sapphire_ring")
    val enchant_emerald_amulet = find("enchant_emerald_amulet")
    val enchant_emerald_ring = find("enchant_emerald_ring")
    val enchant_ruby_amulet = find("enchant_ruby_amulet")
    val enchant_ruby_ring = find("enchant_ruby_ring")
    val enchant_diamond_amulet = find("enchant_diamond_amulet")
    val enchant_diamond_ring = find("enchant_diamond_ring")
    val enchant_dragon_amulet = find("enchant_dragon_amulet")
    val enchant_dragon_ring = find("enchant_dragon_ring")
    val enchant_onyx_amulet = find("enchant_onyx_amulet")
    val enchant_onyx_ring = find("enchant_onyx_ring")
}

public object UtilityLocs : LocReferences() {
    val obelisk_air = find("obelisk_air")
    val obelisk_water = find("obelisk_water")
    val obelisk_earth = find("obelisk_earth")
    val obelisk_fire = find("obelisk_fire")
}

/** The prayer module's group of buriable bones, which is also what Bones to Bananas converts. */
public object UtilityContent : ContentReferences() {
    val bones = find("prayer_bones")
}

/** Cache spell params `BaseParams` leaves out. `spell_desc` is the tooltip text. */
public object UtilityParams : ParamReferences() {
    val spell_desc: ParamType<String> = find("spell_desc")
}
