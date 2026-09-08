package org.rsmod.content.areas.misc.tutorial.configs

import org.rsmod.api.type.editors.npc.NpcEditor

/**
 * Pins the instructors to the tiles `npcs.toml` spawns them on.
 *
 * Out of the cache every instructor keeps the default `Wander` behaviour with a non-zero range, so
 * they stroll away from their rooms and end up nowhere near where a player is sent to find them.
 * Setting `wanderRange = 0` makes the wander processor skip them entirely (it only wanders npcs
 * whose range is above zero), so each one stays put. This is a type edit, so it is applied by the
 * server's boot-time config sync — no `packCache` needed for it, unlike the spawns themselves.
 */
internal object TutorialNpcEditor : NpcEditor() {
    init {
        for (instructor in TutorialNpcs.all) {
            edit(instructor) { wanderRange = 0 }
        }
    }
}
