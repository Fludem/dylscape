package org.rsmod.content.skills.farming

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.content.skills.farming.data.CharacterFarmingApplier
import org.rsmod.content.skills.farming.data.CharacterFarmingPipeline
import org.rsmod.plugin.module.PluginModule

/**
 * Hooks patch storage into the account pipeline.
 *
 * The migration that creates the table ships alongside this module, under
 * `resources/plugin/farming/migration`, one of the classpath locations Flyway is pointed at.
 */
class FarmingModule : PluginModule() {
    override fun bind() {
        bindInstance<CharacterFarmingApplier>()
        addSetBinding<CharacterDataStage.Pipeline>(CharacterFarmingPipeline::class.java)
    }
}
