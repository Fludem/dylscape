package org.rsmod.content.skills.firemaking.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.content.ContentReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType

object FiremakingContent : ContentReferences() {
    val firemaking_logs = find("firemaking_logs")
}

internal object FiremakingObjs : ObjReferences() {
    val ashes = find("ashes")
}

internal object FiremakingLocs : LocReferences() {
    /** The generic burning fire loc every lit log turns into. */
    val fire = find("fire")
}

internal object FiremakingSeqs : SeqReferences() {
    val light_fire = find("human_createfire")
}

internal object FiremakingQueues : QueueReferences() {
    /**
     * One light attempt. Weak, so walking away cancels it: the attempts used to run behind `delay`,
     * which left the player unable to move until the logs caught.
     */
    val light = find("firemaking_light")
}

object FiremakingParams : ParamReferences() {
    /**
     * Success weights at level 1 and level 99, interpolated by `statRandom` exactly as the mining
     * and woodcutting enums are. Firemaking varies by log rather than by tool, so these sit
     * directly on the log instead of in a per-tool enum.
     */
    val rate_low = find<Int>("firemaking_rate_low")
    val rate_high = find<Int>("firemaking_rate_high")

    /** How long the resulting fire burns before collapsing into ashes, in ticks. */
    val burn_ticks = find<Int>("firemaking_burn_ticks")
}

internal object FiremakingParamBuilder : ParamBuilder() {
    init {
        build<Int>("firemaking_rate_low")
        build<Int>("firemaking_rate_high")
        build<Int>("firemaking_burn_ticks")
    }
}

/**
 * Levels and XP are the real OSRS values. Success weights and burn durations are **tuned rather
 * than scraped**: they are correctly ordered (regular logs light almost immediately at high levels,
 * magic and redwood stay slow) and in the right band, but an individual number may be off live.
 */
internal object FiremakingLogsEditor : ObjEditor() {
    init {
        logs(objs.logs, level = 1, xp = 40.0, low = 60, high = 200, burn = 100)
        logs(objs.achey_tree_logs, level = 1, xp = 40.0, low = 60, high = 200, burn = 100)
        logs(objs.oak_logs, level = 15, xp = 60.0, low = 50, high = 190, burn = 150)
        logs(objs.willow_logs, level = 30, xp = 90.0, low = 45, high = 180, burn = 200)
        logs(objs.teak_logs, level = 35, xp = 105.0, low = 43, high = 175, burn = 200)
        logs(objs.arctic_pine_logs, level = 42, xp = 125.0, low = 40, high = 170, burn = 250)
        logs(objs.maple_logs, level = 45, xp = 135.0, low = 38, high = 165, burn = 250)
        logs(objs.mahogany_logs, level = 50, xp = 157.5, low = 36, high = 160, burn = 300)
        logs(objs.juniper_logs, level = 55, xp = 175.0, low = 34, high = 155, burn = 300)
        logs(objs.yew_logs, level = 60, xp = 202.5, low = 30, high = 150, burn = 350)
        logs(objs.magic_logs, level = 75, xp = 303.8, low = 25, high = 140, burn = 400)
        logs(objs.redwood_logs, level = 90, xp = 350.0, low = 20, high = 130, burn = 450)
    }

    private fun logs(type: ObjType, level: Int, xp: Double, low: Int, high: Int, burn: Int) {
        edit(type) {
            contentGroup = FiremakingContent.firemaking_logs
            param[params.levelrequire] = level
            param[params.skill_xp] = PlayerStatMap.toFineXP(xp).toInt()
            param[FiremakingParams.rate_low] = low
            param[FiremakingParams.rate_high] = high
            param[FiremakingParams.burn_ticks] = burn
        }
    }
}
