package org.rsmod.content.skills.slayer.scripts

import jakarta.inject.Inject
import org.rsmod.content.skills.slayer.data.SlayerTaskRepository
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Reads the slayer database out of the cache once, at boot.
 *
 * Kept as its own script rather than folded into a handler's `startup()`, matching
 * `MusicRepositoryScript`. Script start-up order is ClassGraph scan order and so is not fixed, but
 * nothing reads the repository during start-up - only from event handlers, which cannot run until
 * the game is ticking and every script has started.
 */
class SlayerRepositoryScript @Inject constructor(private val repo: SlayerTaskRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        repo.load()
    }
}
