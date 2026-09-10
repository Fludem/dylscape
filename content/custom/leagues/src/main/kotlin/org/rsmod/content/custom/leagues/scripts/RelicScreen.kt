package org.rsmod.content.custom.leagues.scripts

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.custom.leagues.relics.LeaguePointsSync
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.carries
import org.rsmod.content.custom.leagues.relics.pickedRelics
import org.rsmod.game.type.obj.ObjTypeList

/**
 * Opening the relics screen, shared by the journal's leagues tab and the admin command.
 *
 * Opening is also when a player's points catch up with their total level, and when a relic item
 * they have lost is handed back - the vanilla text says a lost relic item "can be retrieved from
 * the Sage", and this server has no Sage.
 */
@Singleton
class RelicScreen
@Inject
constructor(
    private val points: LeaguePointsSync,
    private val objRepo: ObjRepository,
    private val objTypes: ObjTypeList,
) {
    fun open(access: ProtectedAccess) {
        points.sync(access.player)
        for (relic in access.player.pickedRelics) {
            grantItem(access, relic)
        }
        access.openRelicsScreen()
    }

    /**
     * Gives [relic]'s item unless the player already owns one: inventory first, then the bank, then
     * the floor. Returns `true` when an item was handed over.
     */
    fun grantItem(access: ProtectedAccess, relic: Relic): Boolean =
        with(access) {
            val item = relic.item ?: return false
            if (player.carries(item) || item in bank) {
                return false
            }
            when {
                !inv.isFull() -> invAdd(inv, item)
                !bank.isFull() -> {
                    invAdd(bank, item)
                    mes("Your ${objTypes[item].name} has been sent to your bank.")
                }
                else -> invAddOrDrop(objRepo, item)
            }
            return true
        }
}
