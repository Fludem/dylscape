package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.editors.obj.ObjEditor

/**
 * Relabels relic items whose vanilla ops no longer match what this server's relic does.
 *
 * An obj edit replaces the whole `iop` array whenever any entry is set, so every op the item keeps
 * - Destroy on iop5 included - is written out here, and the ones left null disappear.
 */
internal object LeagueObjEditor : ObjEditor() {
    init {
        // Clue Compass: vanilla's Current-step / Teleport / Last-destination are gone.
        edit(league_objs.clue_compass) {
            iop1 = "Open-keys"
            iop2 = "Check"
            iop5 = "Destroy"
        }
    }
}
