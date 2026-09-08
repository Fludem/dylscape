package org.rsmod.content.skills.hunter.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Places the huntable creatures into their hunting grounds.
 *
 * **This does not run at boot.** `onPackMapTask` is invoked only by the Gradle `packCache` task,
 * and only with the server stopped. Until that is run the toml is inert and none of these creatures
 * exist, which is by far the most common reason a spawn set "doesn't work".
 *
 * See the toml for which grounds go where and what is still unverified.
 */
object HunterNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        // Resolved relative to *this* class's package, so the toml lives beside it in
        // `resources/org/rsmod/content/skills/hunter/map/`.
        resourceFile<HunterNpcSpawns>("npcs.toml")
    }
}
