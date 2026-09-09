package org.rsmod.content.skills.magic.commons.scripts

import jakarta.inject.Inject
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerSoftQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.skills.magic.commons.MagicBuffRegistry
import org.rsmod.content.skills.magic.commons.SpellImpact
import org.rsmod.content.skills.magic.commons.SpellbookSwitcher
import org.rsmod.content.skills.magic.commons.configs.magic_queues
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Delivers queued spell impacts, expires the swap timer, and forgets players who log out. */
class MagicCommonsScript
@Inject
constructor(
    private val players: PlayerList,
    private val npcs: NpcList,
    private val buffs: MagicBuffRegistry,
    private val switcher: SpellbookSwitcher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftQueueWithArgs<SpellImpact>(magic_queues.spell_impact) {
            val target = args.resolve(players, npcs) ?: return@onPlayerSoftQueueWithArgs
            args.effect(target)
        }
        onPlayerSoftTimer(magic_timers.spellbook_swap) { switcher.endSpellbookSwap(player) }
        // `Delete`, not `Logout`: the latter fires before the account save.
        onEvent<SessionStateEvent.Delete> { buffs.remove(player) }
    }
}
