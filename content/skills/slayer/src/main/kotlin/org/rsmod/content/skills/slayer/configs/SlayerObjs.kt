@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.skills.slayer.configs

import org.rsmod.api.type.refs.obj.ObjReferences

internal typealias slayer_objs = SlayerObjs

/**
 * Slayer's own items.
 *
 * The protective gear is deliberately absent: `nose_peg`, `facemask`, `earmuffs`, `spiny_helmet`
 * and `reinforced_goggles` are already declared in `BaseObjs` and are reused from there rather than
 * redeclared. Note `spiny_helmet` resolves to `wallbeast_spike_helmet` - the cache never uses the
 * word "spiny".
 */
object SlayerObjs : ObjReferences() {
    /**
     * The brimstone key. Named after Konar in the cache, though here every master's task drops it.
     */
    val brimstone_key = find("konar_key")

    val enchanted_gem = find("slayer_gem")
    val witchwood_icon = find("witchwood_icon")
    val mirror_shield = find("slayer_mirror_shield")

    // Finishing items: these monsters cannot be killed by damage alone.
    val rock_hammer = find("slayer_rock_hammer")
    val bag_of_salt = find("slayer_bag_of_salt")
    val ice_cooler = find("slayer_icy_water")
    val fungicide = find("slayer_fungicide")
}
