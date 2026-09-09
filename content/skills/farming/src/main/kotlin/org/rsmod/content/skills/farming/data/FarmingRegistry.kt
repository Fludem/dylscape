package org.rsmod.content.skills.farming.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

/**
 * Every player's patches, keyed by the patch's primary loc id.
 *
 * Patches are per-player: two people standing in the same allotment see their own crops, exactly as
 * in the real game, because the appearance is driven by a varbit the server sets on each of them
 * individually.
 */
@Singleton
public class FarmingRegistry @Inject constructor() {
    private val players = HashMap<Player, PlayerPatches>()

    public operator fun get(player: Player): PlayerPatches =
        players.getOrPut(player) { PlayerPatches() }

    public fun put(player: Player, patches: PlayerPatches) {
        players[player] = patches
    }

    public fun remove(player: Player) {
        players.remove(player)
    }

    public fun contains(player: Player): Boolean = player in players
}

/** One player's patches. Absent keys are untouched patches, which is the overwhelming majority. */
public class PlayerPatches {
    private val states = HashMap<Int, PatchState>()

    public val touched: Map<Int, PatchState>
        get() = states

    /**
     * The stored state for a patch, creating it on demand.
     *
     * A patch nobody has touched still needs a [PatchState] the moment it is looked at, because
     * even "how overgrown is it" is answered from [PatchState.clearedAt]. A brand new one has a
     * `clearedAt` of 0, which is far enough in the past that the patch reads as fully weedy -- the
     * same thing a player sees walking up to an untended farm in the real game.
     */
    public operator fun get(patch: FarmingPatch): PatchState =
        states.getOrPut(patch.key) { PatchState() }

    public fun peek(key: Int): PatchState? = states[key]

    public fun set(key: Int, state: PatchState) {
        states[key] = state
    }
}

/**
 * Reads and writes a player's patches as one line of text, in the same spirit as the house grid.
 *
 * ```
 * 8550,5318,1757376000000,0,1,0,0,0,1;8150,5295,1757370000000,0,0,0,0,0,0
 * ```
 *
 * is a composted allotment of potatoes with one life spent and a herb patch of ranarrs. A patch
 * whose fields will not parse is dropped rather than thrown on: losing one patch is recoverable,
 * failing to log in is not.
 */
public object PatchCodec {
    private const val PATCH_SEPARATOR = ';'
    private const val FIELD_SEPARATOR = ','
    private const val FLAG_CHECKED = 0x1
    private const val FLAG_FELLED = 0x2

    public fun encode(patches: PlayerPatches): String = buildString {
        for ((key, state) in patches.touched) {
            if (state.empty && state.clearedAt == 0L) {
                continue
            }
            if (isNotEmpty()) {
                append(PATCH_SEPARATOR)
            }
            val flags =
                (if (state.checked) FLAG_CHECKED else 0) or (if (state.felled) FLAG_FELLED else 0)
            append(key).append(FIELD_SEPARATOR)
            append(state.seedId).append(FIELD_SEPARATOR)
            append(state.plantedAt).append(FIELD_SEPARATOR)
            append(state.clearedAt).append(FIELD_SEPARATOR)
            append(state.livesUsed).append(FIELD_SEPARATOR)
            append(state.picked).append(FIELD_SEPARATOR)
            append(state.regrewAt).append(FIELD_SEPARATOR)
            append(flags).append(FIELD_SEPARATOR)
            append(state.compost.ordinal)
        }
    }

    public fun decode(encoded: String): PlayerPatches {
        val patches = PlayerPatches()
        for (entry in encoded.split(PATCH_SEPARATOR)) {
            if (entry.isBlank()) continue
            val fields = entry.split(FIELD_SEPARATOR)
            if (fields.size < 9) continue
            val key = fields[0].toIntOrNull() ?: continue
            val state =
                PatchState().apply {
                    seedId = fields[1].toIntOrNull() ?: return@apply
                    plantedAt = fields[2].toLongOrNull() ?: 0L
                    clearedAt = fields[3].toLongOrNull() ?: 0L
                    livesUsed = fields[4].toIntOrNull() ?: 0
                    picked = fields[5].toIntOrNull() ?: 0
                    regrewAt = fields[6].toLongOrNull() ?: 0L
                    val flags = fields[7].toIntOrNull() ?: 0
                    checked = flags and FLAG_CHECKED != 0
                    felled = flags and FLAG_FELLED != 0
                    compost = CompostTier.from(fields[8].toIntOrNull() ?: 0)
                }
            patches.set(key, state)
        }
        return patches
    }
}
