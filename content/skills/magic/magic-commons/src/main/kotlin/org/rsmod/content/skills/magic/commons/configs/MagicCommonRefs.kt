package org.rsmod.content.skills.magic.commons.configs

import org.rsmod.api.type.refs.enums.EnumReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.obj.ObjType

public typealias magic_enums = MagicEnums

public typealias magic_timers = MagicTimers

public typealias magic_queues = MagicQueues

/**
 * Enum 1981 maps a spellbook index to the enum listing that book's spells, exactly the way
 * `MagicSpellRegistry` reads it. Declared again here because upstream keeps its copy `internal`.
 */
public object MagicEnums : EnumReferences() {
    val spellbooks: EnumType<Int, EnumType<Int, ObjType>> = find("spellbooks")
}

/**
 * Server-only timers, hand-added to `.data/symbols/.local/timer.sym` (ids 1011+). Timers have no
 * cache encoder, so a `find` here is only a name lookup and nothing needs `packCache`.
 */
public object MagicTimers : TimerReferences() {
    /** Player: soft timer, so it expires even while the player is access-protected. Npc: timer. */
    val frozen = find("magic_frozen")
    val freeze_immunity = find("magic_freeze_immunity")
    val vengeance_cooldown = find("magic_vengeance_cooldown")
    val charge = find("magic_charge")
    val imbue = find("magic_imbue")
    val spellbook_swap = find("magic_spellbook_swap")
}

public object MagicQueues : QueueReferences() {
    /** Carries a [org.rsmod.content.skills.magic.commons.SpellImpact] to the tick a hit lands. */
    val spell_impact = find("magic_spell_impact")
}
