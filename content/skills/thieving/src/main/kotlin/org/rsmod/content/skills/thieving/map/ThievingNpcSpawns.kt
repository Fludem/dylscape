package org.rsmod.content.skills.thieving.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Places the mid and high tiers of the pickpocket ladder.
 *
 * **This does not run at boot.** `onPackMapTask` is invoked only by the Gradle `packCache` task,
 * and only with the server stopped. Until that is run the toml is inert and none of these npcs
 * exist, which is by far the most common reason a spawn set "doesn't work".
 *
 * See the toml for which clusters go where and why.
 */
object ThievingNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        // Resolved relative to *this* class's package, so the toml lives beside it in
        // `resources/org/rsmod/content/skills/thieving/map/`. `ThievingConfigTest` reads it back
        // through the same class, so the two can never disagree about where it is.
        resourceFile<ThievingNpcSpawns>("npcs.toml")
    }
}
