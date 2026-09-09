package org.rsmod.content.custom.barrows

/**
 * The six brothers, in the cache's own order.
 *
 * Everything per-brother hangs off this: the mound you dig, the crypt you land in, the sarcophagus
 * you search, the varbit that records the kill, and which of the three attack styles the fight
 * uses. Keeping them in one enum is what lets [BarrowsRun] and the scripts loop rather than repeat
 * themselves six times.
 */
enum class Brother(val displayName: String, val style: BarrowsStyle) {
    Ahrim("Ahrim the Blighted", BarrowsStyle.Magic),
    Dharok("Dharok the Wretched", BarrowsStyle.Melee),
    Guthan("Guthan the Infested", BarrowsStyle.Melee),
    Karil("Karil the Tainted", BarrowsStyle.Ranged),
    Torag("Torag the Corrupted", BarrowsStyle.Melee),
    Verac("Verac the Defiled", BarrowsStyle.Melee);

    companion object {
        val all: List<Brother> = entries
    }
}

/**
 * Which attack the brother makes.
 *
 * [Melee] is the engine's own path - `NvPCombatScript` binds `onDefaultAiOpPlayer2` and always
 * builds a `CombatAttack.NpcMelee` - so those four brothers need no attack script at all. [Magic]
 * and [Ranged] have no engine path and are rolled by hand in `BarrowsCombatScript`.
 */
enum class BarrowsStyle {
    Melee,
    Magic,
    Ranged,
}
