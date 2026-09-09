package org.rsmod.content.custom.worldspawns.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Every npc spawn outside the areas this server authors by hand, bridged from GregHib/void's world
 * data by `tools/npc-spawns/generate.py`.
 *
 * **This does not run at boot.** `onPackMapTask` is invoked only by the Gradle `packCache` task,
 * and only with the server stopped, so the tomls beside this file are inert until the packer runs.
 *
 * The 32 tomls are generated. Fix a bad spawn in the generator, not here.
 */
object WorldNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<WorldNpcSpawns>("area_asgarnia.toml")
        resourceFile<WorldNpcSpawns>("area_fremennik_province.toml")
        resourceFile<WorldNpcSpawns>("area_kandarin.toml")
        resourceFile<WorldNpcSpawns>("area_karamja.toml")
        resourceFile<WorldNpcSpawns>("area_kharidian_desert.toml")
        resourceFile<WorldNpcSpawns>("area_misthalin.toml")
        resourceFile<WorldNpcSpawns>("area_morytania.toml")
        resourceFile<WorldNpcSpawns>("area_realm.toml")
        resourceFile<WorldNpcSpawns>("area_tirannwn.toml")
        resourceFile<WorldNpcSpawns>("area_troll_country.toml")
        resourceFile<WorldNpcSpawns>("area_wilderness.toml")
        resourceFile<WorldNpcSpawns>("entity_npc.toml")
        resourceFile<WorldNpcSpawns>("minigame_barbarian_assault.toml")
        resourceFile<WorldNpcSpawns>("minigame_blast_furnace.toml")
        resourceFile<WorldNpcSpawns>("minigame_bounty_hunter.toml")
        resourceFile<WorldNpcSpawns>("minigame_castle_wars.toml")
        resourceFile<WorldNpcSpawns>("minigame_champions_challenge.toml")
        resourceFile<WorldNpcSpawns>("minigame_duel_arena.toml")
        resourceFile<WorldNpcSpawns>("minigame_fishing_trawler.toml")
        resourceFile<WorldNpcSpawns>("minigame_mage_training_arena.toml")
        resourceFile<WorldNpcSpawns>("minigame_pest_control.toml")
        resourceFile<WorldNpcSpawns>("minigame_puro_puro.toml")
        resourceFile<WorldNpcSpawns>("minigame_pyramid_plunder.toml")
        resourceFile<WorldNpcSpawns>("minigame_shades_of_mortton.toml")
        resourceFile<WorldNpcSpawns>("minigame_sorceresss_garden.toml")
        resourceFile<WorldNpcSpawns>("minigame_temple_trekking.toml")
        resourceFile<WorldNpcSpawns>("minigame_trouble_brewing.toml")
        resourceFile<WorldNpcSpawns>("minigame_warriors_guild.toml")
        resourceFile<WorldNpcSpawns>("quest_free.toml")
        resourceFile<WorldNpcSpawns>("quest_members.toml")
        resourceFile<WorldNpcSpawns>("skill_runecrafting.toml")
        resourceFile<WorldNpcSpawns>("social_trade.toml")
    }
}
