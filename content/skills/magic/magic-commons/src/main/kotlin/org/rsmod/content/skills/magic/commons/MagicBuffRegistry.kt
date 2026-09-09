package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.game.entity.Player

/**
 * Per-session magic state that has no varp of its own: whether Vengeance is armed, and which book a
 * Lunar Spellbook Swap should return to.
 *
 * Session-scoped on purpose, the way `LastTeleportRegistry` is: none of it should survive a logout
 * (Vengeance does not in the official game either), and persisting it would mean minting varps.
 * Entries are dropped on `SessionStateEvent.Delete` by [scripts.MagicCommonsScript], never on
 * `Logout`, which fires before the account save.
 */
@Singleton
public class MagicBuffRegistry @Inject constructor() {
    private val buffs = HashMap<Player, Buffs>()

    public operator fun get(player: Player): Buffs = buffs.getOrPut(player) { Buffs() }

    public fun remove(player: Player) {
        buffs.remove(player)
    }

    public class Buffs {
        public var vengeance: Boolean = false
        public var spellbookSwapReturn: Spellbook? = null
    }
}
