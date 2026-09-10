package org.rsmod.content.custom.leagues

import jakarta.inject.Inject
import org.rsmod.content.custom.leagues.relics.LeaguePointsSync
import org.rsmod.content.custom.leagues.scripts.RelicScreen
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Everything the leagues suites pull out of the game injector.
 *
 * `Perks` and `XpModifiers` are deliberately absent: the test injector installs no plugin modules,
 * so `LeaguesModule`'s set bindings never reach it. Suites that need them build them directly from
 * `RelicPerkSource`, `CornerCutterXp` and `EquilibriumXp`.
 */
class LeagueTestDeps
@Inject
constructor(
    val screen: RelicScreen,
    val points: LeaguePointsSync,
    val collision: CollisionFlagMap,
    val statTypes: StatTypeList,
)
