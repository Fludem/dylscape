package org.rsmod.content.custom.leagues.relics

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/*
 * A player's relics live entirely in the vanilla `league_relic_selection_<tier>` varbits, which
 * are `Perm` and so persist with the account. Nothing is cached server-side, so the varbit is the
 * one source of truth and a repick switches the old relic's effects off by itself.
 *
 * The Reloaded relic's extra pick lives the same way, in `league_relic_selection_other_<tier>`.
 */

/** The relic picked in [tier], or `null` when the tier has no pick yet. */
fun Player.relicIn(tier: Int): Relic? {
    val varbit = league_varbits[tier] ?: return null
    return Relic.of(tier, vars[varbit])
}

/**
 * Whether [relic] is active for this player: it is one we have built, and it is either their pick
 * for its tier or the relic their Reloaded pick copies.
 */
fun Player.hasRelic(relic: Relic): Boolean {
    if (!relic.implemented) {
        return false
    }
    if (relicIn(relic.tier) == relic) {
        return true
    }
    return relic.tier < Relic.Reloaded.tier && reloadedRelic == relic
}

/**
 * The lower-tier relic this player's Reloaded pick copies. `null` when they have none - including
 * when they chose one and have since swapped Reloaded away.
 */
val Player.reloadedRelic: Relic?
    get() {
        if (relicIn(Relic.Reloaded.tier) != Relic.Reloaded) {
            return null
        }
        return storedReloadedRelic
    }

/** The stored Reloaded pick, whether or not Reloaded is still this player's tier-4 relic. */
val Player.storedReloadedRelic: Relic?
    get() {
        for (tier in 0 until Relic.Reloaded.tier) {
            val slot = vars[league_varbits.relic_selection_other[tier]]
            if (slot != 0) {
                return Relic.of(tier, slot)
            }
        }
        return null
    }

/** Every relic that is active for this player, lowest tier first, then any Reloaded pick. */
val Player.pickedRelics: List<Relic>
    get() = (0 until Relic.TIER_COUNT).mapNotNull { relicIn(it) } + listOfNotNull(reloadedRelic)

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

/** Makes [relic] this player's Reloaded pick, replacing any earlier one. */
fun Player.setReloadedRelic(relic: Relic) {
    require(relic.tier < Relic.Reloaded.tier) { "Reloaded only copies lower tiers: $relic" }
    clearReloadedRelic()
    VarPlayerIntMapSetter.set(this, league_varbits.relic_selection_other[relic.tier], relic.slot)
}

/** Clears the Reloaded pick, so the grid stops shading it as active. */
fun Player.clearReloadedRelic() {
    for (varbit in league_varbits.relic_selection_other) {
        if (vars[varbit] != 0) {
            VarPlayerIntMapSetter.set(this, varbit, 0)
        }
    }
}

/** Whether [obj] is carried or worn - the relic items only work while on the player. */
fun Player.carries(obj: ObjType): Boolean = obj in inv || obj in worn
