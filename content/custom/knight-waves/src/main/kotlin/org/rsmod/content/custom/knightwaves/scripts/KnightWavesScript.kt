package org.rsmod.content.custom.knightwaves.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.custom.knightwaves.KnightWavesProgress
import org.rsmod.content.custom.knightwaves.KnightWavesProgress.trialComplete
import org.rsmod.content.custom.knightwaves.KnightWavesProgress.wavesCleared
import org.rsmod.content.custom.knightwaves.KnightWavesTrial
import org.rsmod.content.custom.knightwaves.configs.knightwaves_npcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Knight Waves trial: six of Camelot's knights, one at a time, in a private copy of the
 * training grounds on the castle's top floor. Finishing it unlocks Chivalry and Piety.
 *
 * A deliberately small stand-in for King's Ransom, which does not exist in this codebase. It keeps
 * the part of that quest the prayers actually come from and drops the rest.
 *
 * ### Why the fight is not one coroutine
 *
 * The obvious shape - `protectedAccess.launch { for (wave in waves) { spawn(); awaitDeath() } }` -
 * cannot work. Protected access locks the player out of acting for as long as the block runs, so
 * they would stand there unable to fight back. Coroutines here only cover the moments the player is
 * a passenger: entering the arena, and the reward.
 *
 * The fight itself is driven by each knight's own `queues.death` handler. Registering one per npc
 * type replaces the default death script for those types - npc queues resolve type -> content group
 * -> default and stop at the first match - which is why [NpcDeath.deathNoDrops] has to be called by
 * hand here. Registering per type rather than tagging a shared content group is deliberate:
 * `contentGroup` holds a single value, so two editors claiming one npc conflict silently.
 */
class KnightWavesScript
@Inject
constructor(
    private val trial: KnightWavesTrial,
    private val protectedAccess: ProtectedAccessLauncher,
    private val players: PlayerList,
    private val death: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(knightwaves_npcs.squire) { startDialogue(it.npc) }
        for (knight in knightwaves_npcs.waves) {
            onNpcQueue(knight, queues.death) { knightDefeated() }
        }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc): Unit =
        startDialogue(npc) {
            when {
                player.trialComplete -> alreadyProven()
                player.wavesCleared > 0 -> offerResume()
                else -> offerTrial()
            }
        }

    private suspend fun Dialogue.alreadyProven() {
        chatNpc(happy, "You've bested every knight in the hall. Chivalry and Piety are yours.")
    }

    private suspend fun Dialogue.offerTrial() {
        chatNpc(
            neutral,
            "The knights train upstairs. Six of them, and they'll not go easy " +
                "on you. Best them all and you'll be taught the prayers they " +
                "swear by.",
        )
        chatPlayer(quiz, "What will I need?")
        chatNpc(
            neutral,
            "A steady arm and a good shield. And bring food - you'll get no " +
                "quarter between bouts.",
        )
        beginTrial()
    }

    private suspend fun Dialogue.offerResume() {
        val cleared = player.wavesCleared
        val remaining = KnightWavesProgress.TOTAL_WAVES - cleared
        chatNpc(
            neutral,
            "You've felled $cleared of them. $remaining still waiting, and " +
                "they've not forgotten you.",
        )
        beginTrial()
    }

    private suspend fun Dialogue.beginTrial() {
        val accepted = choice2("Take me to them.", true, "Not right now.", false)
        if (!accepted) {
            chatPlayer(neutral, "Not right now.")
            return
        }
        if (trial.enter(access) == null) {
            chatNpc(sad, "The hall is in use. Give it a moment and ask me again.")
        }
    }

    private suspend fun StandardNpcAccess.knightDefeated() {
        // Both are captured before the death sequence runs: it walks the npc, plays the death
        // animation and then despawns it, by which point neither is reliable.
        val arenaTile = npc.spawnCoords
        val hero = findHero(players)

        death.deathNoDrops(this)

        val player = hero ?: return
        // Region coordinates sit far outside the normal world, so proximity to the tile the knight
        // stood on is what says "this happened inside that player's own instance". It also rules
        // out a knight spawned loose in the world by an admin, which is nobody's trial.
        if (!player.isWithinDistance(arenaTile, SAME_ARENA_DISTANCE)) {
            return
        }
        protectedAccess.launch(player) { trial.knightDefeated(this, arenaTile) }
    }

    private companion object {
        /** The arena hall is 21 by 22 tiles, so anything further out is not in it. */
        const val SAME_ARENA_DISTANCE = 32
    }
}
