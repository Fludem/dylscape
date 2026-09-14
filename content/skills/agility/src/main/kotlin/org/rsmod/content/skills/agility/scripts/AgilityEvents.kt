package org.rsmod.content.skills.agility.scripts

import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** A full lap of [course] was completed and its lap bonus paid. */
data class CompletedLap(val player: Player, val course: AgilityCourse) : UnboundEvent
