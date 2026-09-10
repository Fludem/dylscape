package org.rsmod.content.custom.leagues

import org.rsmod.api.perks.PerkSource
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.content.custom.leagues.relics.CornerCutterXp
import org.rsmod.content.custom.leagues.relics.EquilibriumXp
import org.rsmod.content.custom.leagues.relics.RelicPerkSource
import org.rsmod.plugin.module.PluginModule

class LeaguesModule : PluginModule() {
    override fun bind() {
        addSetBinding<PerkSource>(RelicPerkSource::class.java)
        addSetBinding<XpMod>(CornerCutterXp::class.java)
        addSetBinding<XpMod>(EquilibriumXp::class.java)
    }
}
