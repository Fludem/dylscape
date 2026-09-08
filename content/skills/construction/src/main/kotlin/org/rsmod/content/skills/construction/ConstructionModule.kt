package org.rsmod.content.skills.construction

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.content.skills.construction.house.CharacterHouseApplier
import org.rsmod.content.skills.construction.house.CharacterHousePipeline
import org.rsmod.plugin.module.PluginModule

/**
 * Hooks house storage into the account pipeline.
 *
 * The migration that creates the table ships alongside this module, under
 * `resources/plugin/construction/migration`, which is one of the classpath locations Flyway is
 * pointed at.
 */
class ConstructionModule : PluginModule() {
    override fun bind() {
        bindInstance<CharacterHouseApplier>()
        addSetBinding<CharacterDataStage.Pipeline>(CharacterHousePipeline::class.java)
    }
}
