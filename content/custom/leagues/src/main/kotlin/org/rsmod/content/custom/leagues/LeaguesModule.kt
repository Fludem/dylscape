package org.rsmod.content.custom.leagues

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.perks.PerkSource
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.content.custom.leagues.relics.CornerCutterXp
import org.rsmod.content.custom.leagues.relics.EquilibriumXp
import org.rsmod.content.custom.leagues.relics.RelicPerkSource
import org.rsmod.content.custom.leagues.tasks.CharacterLeagueTaskApplier
import org.rsmod.content.custom.leagues.tasks.CharacterLeagueTaskPipeline
import org.rsmod.plugin.module.PluginModule

class LeaguesModule : PluginModule() {
    override fun bind() {
        addSetBinding<PerkSource>(RelicPerkSource::class.java)
        addSetBinding<XpMod>(CornerCutterXp::class.java)
        addSetBinding<XpMod>(EquilibriumXp::class.java)

        // Task counters ride the account pipeline; the table's migration ships with this module.
        bindInstance<CharacterLeagueTaskApplier>()
        addSetBinding<CharacterDataStage.Pipeline>(CharacterLeagueTaskPipeline::class.java)
    }
}
