package org.rsmod.content.custom.skillingtasks

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.plugin.module.PluginModule

class SkillingTasksModule : PluginModule() {
    override fun bind() {
        // Task state rides the account pipeline; the table's migration ships with this module.
        bindInstance<CharacterSkillingTaskApplier>()
        addSetBinding<CharacterDataStage.Pipeline>(CharacterSkillingTaskPipeline::class.java)
    }
}
