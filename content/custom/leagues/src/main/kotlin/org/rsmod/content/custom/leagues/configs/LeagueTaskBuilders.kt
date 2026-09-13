package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.builders.enums.EnumBuilder
import org.rsmod.api.type.builders.struct.StructBuilder
import org.rsmod.api.type.editors.struct.StructEditor
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.tasks.LeagueTasks
import org.rsmod.game.type.struct.StructType
import org.rsmod.game.type.util.ParamMapBuilder

/*
 * The task list as cache types. Builders only land in `.data/cache/game` on `./gradlew packCache`
 * (server stopped); the two editors below apply on any boot.
 *
 * The client draws each row from the task struct's params and gates a tier on the tier struct's
 * threshold, so this is the whole of what it needs: our structs, an enum listing them, the league
 * struct pointed at that enum, and the thresholds lowered to fit the points on offer.
 */

/** One struct per task, at `LeagueTask.STRUCT_ID_BASE + id` via `.local/struct.sym`. */
internal object LeagueTaskStructBuilder : StructBuilder() {
    init {
        for (task in LeagueTasks.all) {
            build(task.structName) {
                paramMap =
                    ParamMapBuilder()
                        .apply {
                            this[league_task_params.index] = task.id
                            this[league_task_params.name] = task.name
                            this[league_task_params.description] = task.description
                            this[league_task_params.tier] = task.tier.cacheValue
                            this[league_task_params.type] = task.type.cacheValue
                            this[league_task_params.area] = task.area.cacheValue
                            this[league_task_params.skill] = task.skill
                        }
                        .toParamMap()
            }
        }
    }
}

/**
 * `league_task_list`: task id -> task struct. `[proc,league_tasks_draw_list]` walks it by count, so
 * the keys must run `0 until size` with no gaps - which [LeagueTasks] guarantees by making the id
 * the declaration index.
 */
internal object LeagueTaskEnumBuilder : EnumBuilder() {
    init {
        build<Int, StructType>(LeagueTaskListEnum.NAME) {
            transmit = true
            for (task in LeagueTasks.all) {
                this[task.id] = task.structType
            }
        }
    }
}

/** Swaps the Raging Echoes task list (param 868) for ours. */
internal object LeagueTaskListEditor : StructEditor() {
    init {
        edit(league_task_structs.league) {
            param[league_task_params.tasks_enum] = LeagueTaskListEnum.type
        }
    }
}

/**
 * Lowers each relic tier's points threshold (param 877) to [Relic.TIER_POINTS]. The client greys
 * tiers out by this value, so the server and the client agree by construction.
 */
internal object LeagueTierThresholdEditor : StructEditor() {
    init {
        for ((tier, struct) in league_task_structs.tiers.withIndex()) {
            edit(struct) { param[league_task_params.tier_points] = Relic.TIER_POINTS[tier] }
        }
    }
}
