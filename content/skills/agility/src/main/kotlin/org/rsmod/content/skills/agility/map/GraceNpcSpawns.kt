package org.rsmod.content.skills.agility.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Puts Grace on the map.
 *
 * Without this the shop is inert in exactly the way `city-shops` documents: correct stock, correct
 * op bindings, and a shopkeeper who stands nowhere. Npc spawns are authored rather than read from
 * the cache.
 *
 * **Only the Gradle `packCache` task runs this**, and only with the server stopped. Until it has
 * been run, Grace does not exist in the world however green the tests are.
 */
public object GraceNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<GraceNpcSpawns>(FILE)
    }

    public const val FILE: String = "npcs.toml"
}
