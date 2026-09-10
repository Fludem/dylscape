package org.rsmod.content.custom.leagues.relics

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/*
 * A player's relics live entirely in the vanilla `league_relic_selection_<tier>` varbits, which
 * are `Perm` and so persist with the account. Nothing is cached server-side, so the varbit is the
 * one source of truth and a repick switches the old relic's effects off by itself.
 */

/** The relic picked in [tier], or `null` when the tier has no pick yet. */
fun Player.relicIn(tier: Int): Relic? {
    val varbit = league_varbits[tier] ?: return null
    return Relic.of(tier, vars[varbit])
}

/** Whether [relic] is this player's pick for its tier *and* is one we have built. */
fun Player.hasRelic(relic: Relic): Boolean {
    if (!relic.implemented) {
        return false
    }
    val varbit = league_varbits[relic.tier] ?: return false
    return vars[varbit] == relic.slot
}

/** Every relic this player has picked, lowest tier first. */
val Player.pickedRelics: List<Relic>
    get() = (0 until Relic.TIER_COUNT).mapNotNull { relicIn(it) }

/** Writes (and transmits) [relic] as this player's pick for its tier. */
fun Player.setRelic(relic: Relic) {
    val varbit = league_varbits[relic.tier] ?: return
    VarPlayerIntMapSetter.set(this, varbit, relic.slot)
}

/** Clears [tier]'s pick. */
fun Player.clearRelic(tier: Int) {
    val varbit = league_varbits[tier] ?: return
    VarPlayerIntMapSetter.set(this, varbit, 0)
}

/** Whether [obj] is carried or worn - the relic items only work while on the player. */
fun Player.carries(obj: ObjType): Boolean = obj in inv || obj in worn
