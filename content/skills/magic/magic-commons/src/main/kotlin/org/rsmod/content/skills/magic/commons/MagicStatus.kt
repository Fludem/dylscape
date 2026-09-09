package org.rsmod.content.skills.magic.commons

import org.rsmod.annotations.InternalApi
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.game.entity.Player
import org.rsmod.game.type.timer.TimerType

/** Timer-backed spell buffs, readable from any module without a var of their own. */
@OptIn(InternalApi::class)
public object MagicStatus {
    /** Charge: god spells hit up to 30 while this runs. */
    public fun isCharged(player: Player): Boolean = player.hasSoftTimer(magic_timers.charge)

    /** Magic Imbue: combination runes need no talisman while this runs. */
    public fun isImbued(player: Player): Boolean = player.hasSoftTimer(magic_timers.imbue)

    public fun onVengeanceCooldown(player: Player): Boolean =
        player.hasSoftTimer(magic_timers.vengeance_cooldown)

    public fun Player.hasSoftTimer(timer: TimerType): Boolean =
        softTimerMap[timer.id.toShort()] != null
}
