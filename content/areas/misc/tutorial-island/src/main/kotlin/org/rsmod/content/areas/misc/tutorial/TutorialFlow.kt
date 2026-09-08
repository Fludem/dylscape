package org.rsmod.content.areas.misc.tutorial

import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.areas.misc.tutorial.configs.TutorialVarps
import org.rsmod.game.entity.Player

/** The player's saved tutorial progress, as the raw `tutorial` varp value. */
var Player.tutorialValue: Int by intVarp(TutorialVarps.tutorial)

/** The player's tutorial progress as a [TutorialStage]. */
var Player.tutorialStage: TutorialStage
    get() = TutorialStage.of(tutorialValue)
    set(value) {
        tutorialValue = value.value
    }

/**
 * Moves the player forward to [stage], but never backwards — a player who has already passed this
 * point (by re-reading an instructor's dialogue, say) keeps their place.
 */
fun Player.advanceTutorial(stage: TutorialStage) {
    if (tutorialStage.isBefore(stage)) {
        tutorialStage = stage
    }
}
