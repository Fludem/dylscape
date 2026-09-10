package org.rsmod.content.custom.cityshops.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Puts every shopkeeper in [org.rsmod.content.custom.cityshops.ShopAssignments] on the map.
 *
 * Without this the module is inert. Npc spawns are authored, not read from the cache, and upstream
 * only ships Lumbridge's — so the shops here had correct stock and op bindings but a shopkeeper who
 * stood nowhere and could not be reached by anyone.
 *
 * Split per region so a bad coordinate is traceable to a town at a glance. Coordinates are the OSRS
 * spawn tiles; `ShopSpawnTest` walks [FILES] and asserts every one of them names a real npc, is on
 * a tile something can stand on, and belongs to a shop.
 *
 * A handful sit one tile off the shop's published location. Those are shopkeepers whose published
 * tile is the counter itself, which nothing can stand on; each was moved to the adjacent standable
 * tile the collision map allows, and the walkable-tile test is what found them.
 *
 * **Only the Gradle `packCache` task runs this.** A new spawn silently does nothing until the
 * packer is run with the server stopped.
 */
object CityShopNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        for (file in FILES) {
            resourceFile<CityShopNpcSpawns>(file)
        }
    }

    /**
     * Single source of truth: `ShopSpawnTest` enumerates this rather than keeping its own list, so
     * a file added here cannot escape the checks. Public rather than internal because the
     * integration source set compiles separately.
     */
    val FILES: List<String> =
        listOf(
            "alkharid.toml",
            "varrock.toml",
            "falador.toml",
            "portsarim.toml",
            "asgarnia.toml",
            "kandarin.toml",
            "karamja.toml",
            "misthalin.toml",
            "edgeville.toml",
        )
}
