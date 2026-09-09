package org.rsmod.api.account.character.main

import jakarta.inject.Inject
import java.time.LocalDateTime
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.AccountMode
import org.rsmod.game.entity.player.XpRateTier
import org.rsmod.game.type.mod.ModLevelTypeList
import org.rsmod.map.CoordGrid

public class CharacterAccountApplier
@Inject
constructor(private val modLevelTypes: ModLevelTypeList) :
    CharacterDataStage.Applier<CharacterAccountData> {
    override fun apply(player: Player, data: CharacterAccountData) {
        player.accountId = data.accountId
        player.characterId = data.characterId

        val accountHash = (data.accountId.toLong() shl 32) or data.realm.toLong()
        val userHash = data.loginName.hashCode().toLong()
        player.userId = data.characterId.toLong()
        player.accountHash = accountHash
        player.userHash = userHash

        val uuid = data.characterId.toLong()
        player.uuid = uuid
        player.observerUUID = uuid

        val device = data.knownDevice
        player.lastKnownDevice = device
        player.members = data.members
        player.username = data.loginName
        player.displayName = data.displayName ?: ""
        player.coords = CoordGrid(data.coordX, data.coordZ, data.coordLevel)
        player.runEnergy = data.runEnergy
        player.accountMode = AccountMode.of(data.accountMode)
        player.xpRateTier = XpRateTier.of(data.xpRateTier)
        // The tier is authoritative: regenerating `xpRate` from it here means the two can never
        // drift apart. `xp_rate_in_hundreds` remains the fallback for characters that predate the
        // chooser, and for an admin override on an account that never picked a tier.
        player.xpRate = player.xpRateTier?.xpRate ?: data.xpRate
        player.lastLogin = LocalDateTime.now()
        player.vars.backing.putAll(data.varps)
        player.assignModLevel(data.modLevel)
    }

    private fun Player.assignModLevel(modLevelName: String?) {
        val match =
            if (modLevelName != null) {
                modLevelTypes.values.firstOrNull { it.internalName == modLevelName }
            } else {
                null
            }
        modLevel = match ?: modLevelTypes.default()
    }
}
