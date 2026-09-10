package org.rsmod.content.custom.teleports.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations
import org.rsmod.game.entity.Player

/**
 * The last place each online player teleported to, backing the panel's "Previous" button.
 *
 * Session-scoped on purpose: this resets on logout, in exchange for needing no cache work at all.
 * Persisting it would mean a `VarpBuilder`, the first `.data/symbols/.local/varp.sym` in the repo,
 * an int id per destination, and a `packCache` with the server stopped -- a lot of moving parts for
 * one convenience button.
 *
 * Entries are dropped on [org.rsmod.game.entity.player.SessionStateEvent.Delete]; without that the
 * map would pin a [Player] forever.
 *
 * The destination's stable key is stored rather than the object, so that a destination removed from
 * the table degrades to `null` -- "no previous destination" -- which the panel already handles.
 */
@Singleton
public class LastTeleportRegistry @Inject constructor() {
    private val keys = HashMap<Player, String>()

    public operator fun get(player: Player): TeleportDestination? =
        keys[player]?.let(TeleportDestinations::get)

    public fun set(player: Player, destination: TeleportDestination) {
        keys[player] = destination.key
    }

    public fun remove(player: Player) {
        keys.remove(player)
    }
}
