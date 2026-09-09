package org.rsmod.content.custom.teleports.configs

import org.rsmod.map.CoordGrid

/**
 * The coordinates behind [TeleportDestinations], split out so that the table can be extended
 * without touching the invariants and lookups that guard it.
 *
 * Provenance is marked per block: `spell` rows are copied from `params.spell_telecoord` in
 * `api/spells/.../configs/SpellObjs.kt`, `farm` rows from the cache-derived registry in
 * `content/skills/farming/.../data/FarmingPatches.kt`, and the rest are a tile beside a real loc
 * placement read out of `.data/cache/game`. `TeleportDestinationsTest` checks all of them against
 * the game's collision map, so a mistyped mapsquare fails the build rather than teleporting a
 * player into a wall via the `?: dest` fallback in the script.
 *
 * Dungeon rows land at the **surface entrance**, never inside. That keeps the "no destination is in
 * the Wilderness" invariant honest and lets the player walk in the normal way.
 */
internal object TeleportTable {
    val cities: List<TeleportDestination> =
        listOf(
            entry("lumbridge", "Lumbridge", CoordGrid(3221, 3218, 0)), // spell
            entry("varrock", "Varrock", CoordGrid(3213, 3424, 0)), // spell
            entry("grand_exchange", "Grand Exchange", CoordGrid(3164, 3486, 0)),
            entry("falador", "Falador", CoordGrid(2965, 3378, 0)), // spell
            entry("draynor", "Draynor Village", CoordGrid(3093, 3244, 0)),
            entry("edgeville", "Edgeville", CoordGrid(3087, 3496, 0)),
            entry("al_kharid", "Al Kharid", CoordGrid(3270, 3169, 0)),
            entry("port_sarim", "Port Sarim", CoordGrid(3045, 3234, 0)),
            entry("rimmington", "Rimmington", CoordGrid(2957, 3213, 0)),
            entry("camelot", "Camelot", CoordGrid(2757, 3478, 0)), // spell
            entry("seers_village", "Seers' Village", CoordGrid(2725, 3491, 0)),
            entry("catherby", "Catherby", CoordGrid(2801, 3449, 0)), // spell
            entry("ardougne", "Ardougne", CoordGrid(2661, 3302, 0)), // spell
            entry("west_ardougne", "West Ardougne", CoordGrid(2500, 3291, 0)), // spell
            entry("yanille", "Yanille", CoordGrid(2616, 3094, 0)),
            entry("canifis", "Canifis", CoordGrid(3510, 3482, 0)),
            entry("burthorpe", "Burthorpe", CoordGrid(2844, 3543, 0)),
            entry("rellekka", "Rellekka", CoordGrid(2650, 3676, 0)),
            entry("kourend", "Kourend Castle", CoordGrid(1641, 3673, 0)), // spell
            entry("civitas", "Civitas illa Fortis", CoordGrid(1681, 3133, 0)), // spell
        )

    val skilling: List<TeleportDestination> =
        listOf(
            entry("al_kharid_mine", "Al Kharid Mine", CoordGrid(3300, 3312, 0)),
            entry("mining_guild", "Mining Guild", CoordGrid(3018, 3339, 0)),
            entry("rimmington_mine", "Rimmington Mine", CoordGrid(2975, 3240, 0)),
            entry("fishing_guild", "Fishing Guild", CoordGrid(2599, 3420, 0)),
            entry("barbarian_fishing", "Barbarian Fishing", CoordGrid(2500, 3487, 0)),
            entry("catherby_fishing", "Catherby Fishing", CoordGrid(2844, 3430, 0)),
            entry("musa_point", "Musa Point", CoordGrid(2925, 3178, 0)),
            entry("draynor_willows", "Draynor Willows", CoordGrid(3087, 3234, 0)),
            entry("seers_yews", "Seers' Village Yews", CoordGrid(2706, 3465, 0)),
            entry("woodcutting_guild", "Woodcutting Guild", CoordGrid(1657, 3505, 0)),
            entry("edgeville_furnace", "Edgeville Furnace", CoordGrid(3108, 3498, 0)),
            entry("varrock_anvils", "Varrock Anvils", CoordGrid(3187, 3426, 0)),
            entry("crafting_guild", "Crafting Guild", CoordGrid(2933, 3290, 0)),
            entry("farming_guild", "Farming Guild", CoordGrid(1249, 3720, 0)),
            entry("catherby_patch", "Catherby Patch", CoordGrid(2809, 3463, 0)), // farm
            entry("falador_patch", "Falador Patch", CoordGrid(3055, 3307, 0)), // farm
            // Grace, who sells the graceful set for marks of grace. The Rogues' Den has no
            // entrance bound yet -- the Burthorpe trapdoor is unhandled -- so without this
            // row she stands in a room nobody can walk into. Level 1, not 0: the den is
            // underground but its floor is on level 1 in this cache.
            entry("rogues_den", "Rogues' Den (Grace)", CoordGrid(3050, 4963, 1)),
        )

    val dungeons: List<TeleportDestination> =
        listOf(
            entry("stronghold", "Stronghold of Security", CoordGrid(3081, 3421, 0)),
            entry("edgeville_dungeon", "Edgeville Dungeon", CoordGrid(3096, 3468, 0)),
            entry("taverley_dungeon", "Taverley Dungeon", CoordGrid(2884, 3397, 0)),
            entry("brimhaven_dungeon", "Brimhaven Dungeon", CoordGrid(2745, 3152, 0)),
            entry("lumbridge_caves", "Lumbridge Swamp Caves", CoordGrid(3169, 3172, 0)),
            entry("slayer_tower", "Slayer Tower", CoordGrid(3418, 3537, 0)),
            entry("fremennik_dungeon", "Fremennik Slayer Dungeon", CoordGrid(2797, 3613, 0)),
            entry("barrows", "Barrows", CoordGrid(3565, 3314, 0)), // spell
            entry("tzhaar", "TzHaar City", CoordGrid(2450, 5165, 0)),
            entry("waterbirth", "Waterbirth Island", CoordGrid(2546, 3756, 0)), // spell
            entry("ape_atoll", "Ape Atoll", CoordGrid(2797, 2798, 0)), // spell
            entry("kalphite_lair", "Kalphite Lair", CoordGrid(3227, 3108, 0)),
            // The God Wars entrance itself is a boulder crevice; Trollheim is the vetted spell
            // coordinate players actually approach it from.
            entry("trollheim", "Trollheim (God Wars)", CoordGrid(2890, 3679, 0)), // spell
        )

    private fun entry(key: String, label: String, dest: CoordGrid) =
        TeleportDestination(key, label, dest)
}
