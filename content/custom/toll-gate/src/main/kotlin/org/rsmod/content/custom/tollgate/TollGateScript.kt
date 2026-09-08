package org.rsmod.content.custom.tollgate

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.custom.tollgate.configs.tollgate_locs
import org.rsmod.content.custom.tollgate.configs.tollgate_npcs
import org.rsmod.content.custom.tollgate.configs.tollgate_varps
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.mesanim.UnpackedMesAnimType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The toll gate between Lumbridge and Al Kharid.
 *
 * `Open` on the gate, or `Talk-to` on a Border Guard, runs the toll conversation; `Pay-toll(10gp)`
 * skips straight to paying. Either way a successful payment walks the player up to the gate, swings
 * both leaves open, walks them through to the far side and shuts the gate behind them. Once Prince
 * Ali Rescue is complete the map resolves the gate to its `Open`-only variant and the guard waves
 * the player through for free.
 */
class TollGateScript
@Inject
constructor(private val locRepo: LocRepository, private val npcRepo: NpcRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(tollgate_locs.toll_left) { askToPass(gateLeaf(it.loc.coords)) }
        onOpLoc1(tollgate_locs.toll_right) { askToPass(gateLeaf(it.loc.coords)) }
        onOpLoc4(tollgate_locs.toll_left) { payToll(gateLeaf(it.loc.coords)) }
        onOpLoc4(tollgate_locs.toll_right) { payToll(gateLeaf(it.loc.coords)) }
        onOpLoc1(tollgate_locs.free_left) { passForFree(gateLeaf(it.loc.coords)) }
        onOpLoc1(tollgate_locs.free_right) { passForFree(gateLeaf(it.loc.coords)) }
        onOpNpc1(tollgate_npcs.borderguard_lumbridge) { talkToGuard(it.npc) }
        onOpNpc1(tollgate_npcs.borderguard_alkharid) { talkToGuard(it.npc) }
    }

    private suspend fun ProtectedAccess.talkToGuard(guard: Npc) {
        val leaf = nearestLeaf(guard.coords)
        if (isFriendOfAlKharid) {
            passForFree(leaf, guard)
        } else {
            askToPass(leaf, guard)
        }
    }

    private suspend fun ProtectedAccess.askToPass(leaf: LocInfo?, guard: Npc? = nearestGuard()) =
        guardDialogue(guard) {
            chatPlayer(quiz, "Can I come through this gate?")
            guardChat(neutral, "You must pay a toll of 10 gold coins to pass.")
            val choice =
                choice3(
                    "No thank you, I'll walk around.",
                    1,
                    "Who does my money go to?",
                    2,
                    "Yes, ok.",
                    3,
                )
            when (choice) {
                1 -> {
                    chatPlayer(neutral, "No, thank you. I'll walk around.")
                    guardChat(neutral, "Ok suit yourself.")
                }
                2 -> {
                    chatPlayer(quiz, "Who does my money go to?")
                    guardChat(neutral, "The money goes to the city of Al-Kharid.")
                }
                3 -> {
                    chatPlayer(happy, "Yes, ok.")
                    payAndPass(leaf)
                }
            }
        }

    private suspend fun ProtectedAccess.payToll(leaf: LocInfo?, guard: Npc? = nearestGuard()) =
        guardDialogue(guard) { payAndPass(leaf) }

    private suspend fun ProtectedAccess.passForFree(leaf: LocInfo?, guard: Npc? = nearestGuard()) =
        guardDialogue(guard) {
            chatPlayer(quiz, "Can I come through this gate?")
            guardChat(happy, "You may pass for free, you are a friend of Al-Kharid.")
            access.passThroughGate(leaf)
        }

    private suspend fun Dialogue.payAndPass(leaf: LocInfo?) {
        if (access.invTotal(access.inv, objs.coins) < TOLL) {
            chatPlayer(sad, "Oh dear, I don't actually seem to have enough money.")
            return
        }
        mesbox("You pay the guard.")
        // Re-check after the pause: the coins could have been dropped or banked while the message
        // box sat open, and the toll is only taken once the player actually goes through.
        if (access.invTotal(access.inv, objs.coins) < TOLL) {
            return
        }
        access.invDel(access.inv, objs.coins, TOLL)
        access.passThroughGate(leaf)
    }

    /**
     * Walks the player up to the gate, swings both leaves open, walks them through to the far side
     * of the wall and shuts the gate behind them.
     *
     * The approach comes first because the open leaves stand perpendicular to the wall on the
     * flanks of the passage: a player who is beside the gate rather than in front of it (talking to
     * a guard, say) would otherwise be fenced in by the very leaves that just opened for them.
     *
     * A `null` [leaf] means the gate is not on the map (already open, or a test world without it),
     * in which case there is nothing to walk through.
     */
    private suspend fun ProtectedAccess.passThroughGate(leaf: LocInfo?) {
        if (leaf == null) {
            return
        }
        val fromInside = isInside(leaf)
        val approach = approachTile(leaf, fromInside)
        if (!walkAndArrive(approach)) {
            return
        }

        val closed = listOfNotNull(leaf, partnerLeaf(leaf))
        val opened = closed.map { openLeaf(it) }
        walkAndArrive(crossingDest(leaf, fromInside))
        closed.forEach { locRepo.add(it, Int.MAX_VALUE) }
        opened.forEach { locRepo.del(it, Int.MAX_VALUE) }
    }

    /**
     * Walks to [dest] and returns once the player is stood there, or `false` if they did not make
     * it within a few extra cycles. `playerWalk` only waits for the straight-line distance, which
     * undershoots as soon as the route has to bend.
     */
    private suspend fun ProtectedAccess.walkAndArrive(dest: CoordGrid): Boolean {
        if (coords == dest) {
            return true
        }
        playerWalk(dest)
        var patience = ARRIVAL_PATIENCE
        while (coords != dest && patience-- > 0) {
            delay(1)
        }
        return coords == dest
    }

    /**
     * Replaces a closed leaf with its open counterpart and returns the open loc that was placed.
     */
    private fun openLeaf(leaf: LocInfo): LocInfo {
        val isLeft = leaf.id == tollgate_locs.closed_left.id
        val openType = if (isLeft) tollgate_locs.open_left else tollgate_locs.open_right
        // Same convention as the generic double doors: each leaf pivots on its own hinge, so the
        // two turn opposite ways and end up perpendicular to the wall on the same side.
        val openAngle = leaf.turnAngle(rotations = if (isLeft) 3 else 1)
        val openCoords = DoorTranslations.translateOpen(leaf.coords, leaf.shape, leaf.angle)
        // The durations are only a safety net for a walk that gets interrupted; the normal path
        // shuts the gate explicitly as soon as the player is through.
        locRepo.del(leaf, OPEN_SAFETY)
        return locRepo.add(openCoords, openType, OPEN_SAFETY, openAngle, leaf.shape)
    }

    /**
     * A straight wall loc sits on one edge of its tile, so the loc's own tile is the "inside" of
     * the wall line and the neighbour across that edge is the "outside". This reports which side
     * the player is currently on.
     */
    private fun ProtectedAccess.isInside(leaf: LocInfo): Boolean =
        when (leaf.angle) {
            LocAngle.West -> coords.x >= leaf.coords.x
            LocAngle.East -> coords.x <= leaf.coords.x
            LocAngle.North -> coords.z <= leaf.coords.z
            LocAngle.South -> coords.z >= leaf.coords.z
        }

    /** The tile directly in front of the leaf on the player's side of the wall. */
    private fun approachTile(leaf: LocInfo, fromInside: Boolean): CoordGrid =
        if (fromInside) leaf.coords else leaf.coords.step(leaf.angle, outward = 1)

    /** One tile clear of the wall on the far side, so the gate can shut behind the player. */
    private fun crossingDest(leaf: LocInfo, fromInside: Boolean): CoordGrid =
        if (fromInside) leaf.coords.step(leaf.angle, outward = 2)
        else leaf.coords.step(leaf.angle, outward = -1)

    /** Moves [outward] tiles across the wall edge a straight wall of [angle] sits on. */
    private fun CoordGrid.step(angle: LocAngle, outward: Int): CoordGrid =
        when (angle) {
            LocAngle.West -> translateX(-outward)
            LocAngle.East -> translateX(outward)
            LocAngle.North -> translateZ(outward)
            LocAngle.South -> translateZ(-outward)
        }

    /** The other closed leaf, which sits on the neighbouring tile along the wall's axis. */
    private fun partnerLeaf(leaf: LocInfo): LocInfo? {
        val partnerType =
            if (leaf.id == tollgate_locs.closed_left.id) {
                tollgate_locs.closed_right
            } else {
                tollgate_locs.closed_left
            }
        val alongWall =
            when (leaf.angle) {
                LocAngle.West,
                LocAngle.East -> listOf(leaf.coords.translateZ(1), leaf.coords.translateZ(-1))
                LocAngle.North,
                LocAngle.South -> listOf(leaf.coords.translateX(1), leaf.coords.translateX(-1))
            }
        return alongWall.firstNotNullOfOrNull { locRepo.findExact(it, partnerType) }
    }

    private fun gateLeaf(coords: CoordGrid): LocInfo? =
        locRepo.findExact(coords, tollgate_locs.closed_left)
            ?: locRepo.findExact(coords, tollgate_locs.closed_right)

    private fun nearestLeaf(from: CoordGrid): LocInfo? =
        locRepo
            .findAll(ZoneKey.from(from))
            .filter {
                it.id == tollgate_locs.closed_left.id || it.id == tollgate_locs.closed_right.id
            }
            .minByOrNull { it.coords.chebyshevDistance(from) }

    private fun ProtectedAccess.nearestGuard(): Npc? =
        npcRepo
            .findAll(ZoneKey.from(coords), zoneRadius = 1)
            .filter {
                it.id == tollgate_npcs.borderguard_lumbridge.id ||
                    it.id == tollgate_npcs.borderguard_alkharid.id
            }
            .minByOrNull { it.coords.chebyshevDistance(coords) }

    /**
     * Mirrors the gate multiloc: `princequest` values `0..99` still owe the toll, anything above
     * (the quest completes at 110) is a friend of Al Kharid.
     */
    private val ProtectedAccess.isFriendOfAlKharid: Boolean
        get() = vars[tollgate_varps.princequest] >= QUEST_COMPLETE

    /**
     * Runs a conversation with the nearest guard if one is stood at the gate, and falls back to the
     * guard's chat head without an npc otherwise, so the gate still works if a guard has been
     * killed or has wandered off.
     */
    private suspend fun ProtectedAccess.guardDialogue(
        guard: Npc?,
        conversation: suspend Dialogue.() -> Unit,
    ) {
        if (guard != null) {
            startDialogue(guard, conversation = conversation)
        } else {
            startDialogue(conversation)
        }
    }

    private suspend fun Dialogue.guardChat(mesanim: UnpackedMesAnimType, text: String) {
        if (npc != null) {
            chatNpc(mesanim, text)
        } else {
            chatNpcSpecific("Border Guard", tollgate_npcs.borderguard_lumbridge, mesanim, text)
        }
    }

    companion object {
        const val TOLL = 10
        const val QUEST_COMPLETE = 100

        /** Extra cycles to wait for the player to finish a leg of the walk before giving up. */
        const val ARRIVAL_PATIENCE = 6

        /** How long an opened gate lasts if the script that opened it never gets to close it. */
        const val OPEN_SAFETY = 50
    }
}
