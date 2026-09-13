package org.rsmod.content.custom.leagues.tasks

import jakarta.inject.Inject
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_timers
import org.rsmod.content.custom.leagues.relics.LeaguePointsSync
import org.rsmod.content.custom.leagues.relics.RelicUnlocked
import org.rsmod.content.custom.leagues.scripts.openTasksScreen
import org.rsmod.content.interfaces.levelup.LevelUpScript
import org.rsmod.content.skills.agility.scripts.CompletedLap
import org.rsmod.content.skills.cooking.scripts.CookedFood
import org.rsmod.content.skills.crafting.scripts.Crafted
import org.rsmod.content.skills.farming.scripts.Harvested
import org.rsmod.content.skills.firemaking.scripts.LitFire
import org.rsmod.content.skills.fishing.scripts.Fishing
import org.rsmod.content.skills.fletching.scripts.Fletched
import org.rsmod.content.skills.herblore.scripts.CleanedHerb
import org.rsmod.content.skills.herblore.scripts.MixedPotion
import org.rsmod.content.skills.hunter.scripts.HunterTrapScript
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.prayer.scripts.BuriedBones
import org.rsmod.content.skills.prayer.scripts.OfferedBones
import org.rsmod.content.skills.smithing.scripts.Smelted
import org.rsmod.content.skills.smithing.scripts.Smithed
import org.rsmod.content.skills.thieving.scripts.Pickpocketing
import org.rsmod.content.skills.thieving.scripts.StallThieving
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Turns what players do into task progress.
 *
 * Every hook here is an unbound event another module already publishes, so no skill knows about
 * leagues: kills come from `NpcDeathEvents.Killed`, level-ups from `LevelUpScript.StatLevelUp`,
 * gathers from each skill's own event, gear from `WearposChange` (which fires on unequip too, so
 * the worn slots are re-read), relics from `RelicUnlocked`. Visits are the one thing with no
 * event - the map carries no area data for towns - so they are a per-player soft timer that checks
 * the coordinate boxes still outstanding.
 *
 * Kill tasks name npcs by display name and are resolved against every npc type at startup, so a
 * variant added to the cache counts by itself and a misspelt name fails the boot.
 */
class LeagueTaskScript
@Inject
constructor(
    private val progress: LeagueTaskProgress,
    private val points: LeaguePointsSync,
    private val protectedAccess: ProtectedAccessLauncher,
    private val npcTypes: NpcTypeList,
    private val statTypes: StatTypeList,
) : PluginScript() {
    private val levelTasks = LeagueTasks.all.filter { it.trigger !is Trigger.Gather }
    private val gatherTasks: Map<GatherKind, List<LeagueTask>> =
        LeagueTasks.all
            .filter { it.trigger is Trigger.Gather }
            .groupBy { (it.trigger as Trigger.Gather).kind }
    private val equipTasks = LeagueTasks.all.filter { it.trigger is Trigger.Equip }
    private val relicTasks = LeagueTasks.all.filter { it.trigger is Trigger.UnlockRelic }
    private val visitTasks = LeagueTasks.all.filter { it.trigger is Trigger.VisitArea }
    private val killTasksByNpc = HashMap<Int, MutableList<LeagueTask>>()

    override fun ScriptContext.startup() {
        resolveKillTasks()

        onPlayerLogin {
            player.softTimer(league_timers.task_visit, VISIT_INTERVAL)
            checkLevels(player)
        }
        onPlayerSoftTimer(league_timers.task_visit) { checkVisits(player) }
        onEvent<SessionStateEvent.Delete> { progress.forget(player) }
        onEvent<LeagueTaskCompleted> { points.sync(player) }

        onEvent<LevelUpScript.StatLevelUp> { checkLevels(player) }
        onEvent<NpcDeathEvents.Killed> { killer?.let { killed(it, npc.id) } }
        onEvent<HeldEquipEvents.WearposChange> { checkEquip(player) }
        onEvent<RelicUnlocked> { unlockedRelic(player, relic.tier) }

        onEvent<Mining.MinedOre> { gathered(player, GatherKind.Mine, product) }
        onEvent<Woodcutting.CutLogs> { gathered(player, GatherKind.Chop, product) }
        onEvent<Fishing.CaughtFish> { gathered(player, GatherKind.Fish, fish) }
        onEvent<CookedFood> {
            if (!burnt) {
                gathered(player, GatherKind.Cook, product)
            }
        }
        onEvent<HunterTrapScript.CaughtCreature> { gathered(player, GatherKind.Hunt, product) }
        onEvent<Pickpocketing.Pickpocketed> { gathered(player, GatherKind.Pickpocket, loot) }
        onEvent<StallThieving.StoleFromStall> { gathered(player, GatherKind.Stall, product) }
        onEvent<Smithed> { gathered(player, GatherKind.Smith, product, count) }
        onEvent<Smelted> { gathered(player, GatherKind.Smelt, bar) }
        onEvent<Crafted> { gathered(player, GatherKind.Craft, product, count) }
        onEvent<Fletched> { gathered(player, GatherKind.Fletch, product, count) }
        onEvent<LitFire> { gathered(player, GatherKind.Firemake, logs) }
        onEvent<MixedPotion> { gathered(player, GatherKind.Potion, product) }
        onEvent<CleanedHerb> { gathered(player, GatherKind.CleanHerb, herb, count) }
        onEvent<CompletedLap> { gathered(player, GatherKind.AgilityLap, null) }
        onEvent<Harvested> { gathered(player, GatherKind.Harvest, produce) }
        onEvent<BuriedBones> { gathered(player, GatherKind.BuryBones, bones) }
        onEvent<OfferedBones> { gathered(player, GatherKind.OfferBones, bones) }

        // Rows expand and collapse on the client; the press reaches us only because the list is
        // armed so the client does not reject it.
        onIfOverlayButton(league_components.tasks_list) {}

        onCommand("tasks") {
            modLevel = modlevels.admin
            desc = "Open the league tasks screen"
            cheat(::openScreen)
        }
        onCommand("taskcomplete") {
            modLevel = modlevels.admin
            desc = "Complete a league task (::taskcomplete <id|name>)"
            cheat(::completeTask)
        }
        onCommand("taskprogress") {
            modLevel = modlevels.admin
            desc = "Show a league task's progress (::taskprogress <id|name>)"
            cheat(::showProgress)
        }
        onCommand("taskreset") {
            modLevel = modlevels.admin
            desc = "Clear every league task and its counters"
            cheat(::resetTasks)
        }
    }

    private fun resolveKillTasks() {
        val byName = HashMap<String, MutableList<LeagueTask>>()
        for (task in LeagueTasks.all) {
            val trigger = task.trigger as? Trigger.KillNpc ?: continue
            for (name in trigger.names) {
                byName.getOrPut(name.lowercase()) { ArrayList() } += task
            }
        }
        val unmatched = byName.keys.toMutableSet()
        for (type in npcTypes.types.values) {
            val tasks = byName[type.name.lowercase()] ?: continue
            unmatched -= type.name.lowercase()
            killTasksByNpc.getOrPut(type.id) { ArrayList() } += tasks
        }
        check(unmatched.isEmpty()) { "League kill tasks name npcs that do not exist: $unmatched" }
    }

    private fun killed(killer: Player, npcId: Int) {
        val tasks = killTasksByNpc[npcId] ?: return
        for (task in tasks) {
            progress.advance(killer, task)
        }
    }

    private fun gathered(player: Player, kind: GatherKind, product: ObjType?, count: Int = 1) {
        val tasks = gatherTasks[kind] ?: return
        for (task in tasks) {
            val trigger = task.trigger as Trigger.Gather
            if (trigger.products.isNotEmpty()) {
                if (product == null || trigger.products.none { it.id == product.id }) {
                    continue
                }
            }
            progress.advance(player, task, count)
        }
    }

    private fun checkLevels(player: Player) {
        var total = -1
        for (task in levelTasks) {
            val met =
                when (val trigger = task.trigger) {
                    is Trigger.SkillLevel -> player.statBase(trigger.stat) >= trigger.level
                    is Trigger.TotalLevel -> {
                        if (total < 0) {
                            total = totalLevel(player)
                        }
                        total >= trigger.level
                    }
                    is Trigger.CombatLevel -> player.combatLevel >= trigger.level
                    else -> false
                }
            if (met && !progress.isDone(player, task)) {
                progress.complete(player, task)
            }
        }
    }

    private fun totalLevel(player: Player): Int =
        statTypes.values.filterNot { it.unreleased }.sumOf { player.statBase(it) }

    private fun checkEquip(player: Player) {
        for (task in equipTasks) {
            if (progress.isDone(player, task)) {
                continue
            }
            val trigger = task.trigger as Trigger.Equip
            if (trigger.objs.any { it in player.worn }) {
                progress.complete(player, task)
            }
        }
    }

    private fun unlockedRelic(player: Player, tier: Int) {
        for (task in relicTasks) {
            val trigger = task.trigger as Trigger.UnlockRelic
            val minTier = trigger.minTier
            if (minTier == null || tier >= minTier) {
                progress.complete(player, task)
            }
        }
    }

    private fun checkVisits(player: Player) {
        val coords = player.coords
        for (task in visitTasks) {
            if (progress.isDone(player, task)) {
                continue
            }
            val trigger = task.trigger as Trigger.VisitArea
            if (trigger.box.contains(coords)) {
                progress.complete(player, task)
            }
        }
    }

    private fun openScreen(cheat: Cheat) {
        if (!protectedAccess.launch(cheat.player) { openTasksScreen() }) {
            cheat.player.mes("You are busy right now.")
        }
    }

    private fun completeTask(cheat: Cheat) =
        with(cheat) {
            val task = findTask(args) ?: return
            if (!progress.complete(player, task)) {
                player.mes("${task.name} is already complete.")
            }
        }

    private fun showProgress(cheat: Cheat) =
        with(cheat) {
            if (args.isEmpty()) {
                val done = progress.completedCount(player)
                player.mes(
                    "League tasks: $done/${LeagueTasks.all.size} complete, " +
                        "${points.points(player)} points."
                )
                return
            }
            val task = findTask(args) ?: return
            val state =
                when {
                    progress.isDone(player, task) -> "complete"
                    task.counted -> "${progress.count(player, task)}/${task.target}"
                    else -> "incomplete"
                }
            player.mes("Task ${task.id} '${task.name}' (${task.tier}, ${task.points} pts): $state")
        }

    private fun resetTasks(cheat: Cheat) =
        with(cheat) {
            progress.reset(player)
            points.sync(player)
            player.mes("Cleared every league task; you have ${points.points(player)} points.")
        }

    private fun Cheat.findTask(args: List<String>): LeagueTask? {
        val query = args.joinToString(" ").trim()
        if (query.isEmpty()) {
            player.mes("Give a task id or name, e.g. ::taskcomplete 3 or ::taskcomplete Chop Some")
            return null
        }
        val task = LeagueTasks.find(query)
        if (task == null) {
            player.mes("No league task matches '$query'.")
        }
        return task
    }

    private companion object {
        /** Ticks between visit checks; three seconds is well under how long anyone lingers. */
        private const val VISIT_INTERVAL = 5
    }
}

/** Whether the launcher could run [action]; mirrors `RelicScreenScript`'s helper. */
private fun ProtectedAccessLauncher.launch(
    player: Player,
    action: suspend ProtectedAccess.() -> Unit,
): Boolean = launch(player) { action() }
