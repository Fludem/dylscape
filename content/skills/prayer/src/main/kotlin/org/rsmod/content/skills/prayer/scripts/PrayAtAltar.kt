package org.rsmod.content.skills.prayer.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.locParam
import org.rsmod.api.config.objXpParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.prayer.configs.PrayerContent
import org.rsmod.content.skills.prayer.configs.PrayerParams
import org.rsmod.content.skills.prayer.configs.PrayerSeqs
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The two things a player does at an altar: recharge prayer points, and - at a Chaos altar - offer
 * bones for several times the xp burying them would give.
 *
 * Both hang off the same `prayer_altar` content group. Which of the two an altar supports is
 * decided by [PrayerParams.offer_xp_percent]: it defaults to `100`, meaning "no better than
 * burying", and the offering path refuses anything that has not been raised above that. An obj or
 * loc can only belong to one content group, so this param is what lets one group cover both.
 */
class PrayAtAltar @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(PrayerContent.prayer_altar) { rechargePrayer() }
        onOpLocU(PrayerContent.prayer_altar, PrayerContent.prayer_bones) {
            offerBones(it.type, it.objType, it.invSlot)
        }
    }

    private suspend fun ProtectedAccess.rechargePrayer() {
        val basePrayer = statBase(stats.prayer)
        if (player.prayerLvl >= basePrayer) {
            mes("You already have full prayer points.")
            return
        }

        anim(PrayerSeqs.pray)
        mes("You pray to the gods...")
        delay(RECHARGE_TICKS)

        // `statHeal` caps at the base level and never lowers a level, so an overcharged prayer (a
        // holy wrench top-up, say) survives praying at an altar rather than being reset down.
        statHeal(stats.prayer, constant = basePrayer, percent = 0)
        resetAnim()
        mes("...and recharge your prayer.")
    }

    private suspend fun ProtectedAccess.offerBones(
        altar: UnpackedLocType,
        bones: UnpackedObjType,
        slot: Int,
    ) {
        val xpPercent = altar.offerXpPercent
        if (xpPercent <= NO_OFFERING_BONUS) {
            mes("This altar has no use for your bones.")
            return
        }

        anim(PrayerSeqs.bury)
        delay(OFFER_TICKS)

        // Roll the save *before* deleting: the Chaos altar's 50% is a chance to keep the bone
        // entirely, not a refund after the fact.
        val keptBones = random.of(1, 100) <= altar.offerKeepPercent
        if (!keptBones) {
            val deleted = invDel(inv, bones, count = 1, slot = slot)
            if (!deleted.success) {
                resetAnim()
                return
            }
        }

        val xp = bones.prayerXp * xpPercent / 100.0
        statAdvance(stats.prayer, xp * xpMods.get(player, stats.prayer))
        resetAnim()

        if (keptBones) {
            mes("The gods are pleased with your offering, and return your bones.")
        } else {
            mes("The gods are pleased with your offering.")
        }
    }

    companion object {
        private const val RECHARGE_TICKS = 3
        private const val OFFER_TICKS = 3

        /** The [PrayerParams.offer_xp_percent] default - an altar that only recharges. */
        private const val NO_OFFERING_BONUS = 100

        val UnpackedObjType.prayerXp: Double by objXpParam(params.skill_xp)
        val UnpackedLocType.offerXpPercent: Int by locParam(PrayerParams.offer_xp_percent)
        val UnpackedLocType.offerKeepPercent: Int by locParam(PrayerParams.offer_keep_percent)
    }
}
