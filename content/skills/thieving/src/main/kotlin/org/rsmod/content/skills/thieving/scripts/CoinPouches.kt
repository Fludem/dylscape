package org.rsmod.content.skills.thieving.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.content.skills.thieving.configs.PickpocketTarget
import org.rsmod.content.skills.thieving.configs.ThievingContent
import org.rsmod.content.skills.thieving.configs.ThievingObjs
import org.rsmod.content.skills.thieving.configs.ThievingRates
import org.rsmod.content.skills.thieving.configs.ThievingTargets
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Opening the coin pouches pickpocketing pays out.
 *
 * The options live on the objs' **inventory** ops rather than their ground ops, and they read `iop1
 * = Open-all`, `iop2 = Open`, `iop5 = Destroy` — so `onOpHeld1` is Open-all and `onOpHeld2` is
 * Open, which is the reverse of the order you would guess. Decoded, not assumed; see
 * `ThievingOpsDump`.
 *
 * Each pouch rolls its own value out of the tier's coin range, so opening a stack of 28 pays a
 * spread rather than 28 copies of one lucky roll.
 */
class CoinPouches @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ThievingContent.coin_pouch) { openPouches(it.type, invTotal(inv, it.type)) }
        onOpHeld2(ThievingContent.coin_pouch) { openPouches(it.type, count = 1) }
    }

    internal companion object {
        /**
         * OSRS stops you pickpocketing once you hold this many pouches. We open them instead.
         *
         * That inversion is the point: blocking at 28 is precisely the anti-AFK behaviour this
         * skill was asked to shed, and because coins stack too, auto-opening means the inventory
         * footprint never grows and the loop can genuinely run forever.
         */
        const val AUTO_OPEN_AT = 28

        /** Called after every successful pickpocket, so a session never stalls on a full stack. */
        fun ProtectedAccess.autoOpenPouchesIfFull(pouch: ObjType) {
            if (invTotal(inv, pouch) < AUTO_OPEN_AT) {
                return
            }
            openPouches(pouch, count = AUTO_OPEN_AT, auto = true)
        }

        fun ProtectedAccess.openPouches(pouch: ObjType, count: Int, auto: Boolean = false) {
            val opening = count.coerceAtMost(invTotal(inv, pouch))
            if (opening <= 0) {
                return
            }

            val coinRange = coinRangeFor(pouch)
            if (coinRange == null) {
                // A pouch tier with no rung on the ladder yet. Saying so beats silently deleting
                // the player's pouches for nothing.
                mes("You do not know what to do with this pouch yet.")
                return
            }

            // Delete first: it frees the slot the coins may need if the bag is otherwise full.
            val removed = invDel(inv, pouch, count = opening)
            if (!removed.success) {
                return
            }

            var coins = 0
            repeat(opening) { coins += random.of(coinRange) * ThievingRates.LOOT_RATE }
            invAdd(inv, ThievingObjs.coins, count = coins)
            soundSynth(synths.coins_jingle_1)

            when {
                auto -> spam("Your coin pouches are full, so you open them: $coins coins.")
                opening == 1 -> mes("You open the pouch and find $coins coins.")
                else -> mes("You open $opening coin pouches and find $coins coins.")
            }
        }

        /**
         * Maps a pouch back to the rung that pays it.
         *
         * Two rungs share the farmer pouch, so this takes the richest match rather than the first —
         * a master farmer's purse should not shrink because ordinary farmers drop the same pouch.
         */
        private fun coinRangeFor(pouch: ObjType): IntRange? =
            ThievingTargets.all.values
                .filter { it.pouch.id == pouch.id }
                .maxByOrNull(PickpocketTarget::level)
                ?.coins
    }
}
