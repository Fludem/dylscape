package org.rsmod.content.areas.misc.tutorial

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.areas.misc.tutorial.configs.TutorialConstants
import org.rsmod.content.areas.misc.tutorial.configs.TutorialNpcs
import org.rsmod.content.areas.misc.tutorial.configs.TutorialObjs
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Classic Tutorial Island.
 *
 * The island is a linear course of instructors (see [TutorialStage]). A brand-new account is put on
 * the island at login; each instructor explains their room, hands out whatever the next task needs,
 * and lets the player through once they have actually done it. "Done" is measured by the relevant
 * skill's own experience going above zero — the player really chops, lights, fishes, cooks, mines,
 * smelts and smiths using the live skill modules, so nothing here re-implements or fakes a skill
 * step. Finishing the last instructor teleports the player to Lumbridge and takes the new-account
 * flag off, so the course never re-triggers.
 *
 * **What is faithful vs. deferred.** The progression, dialogue, item hand-outs, skill gating, hint
 * arrows and the Lumbridge finish are all real. Two things are not built here and are called out so
 * they are not mistaken for bugs:
 * - The **native progress-bar overlay** (interface 614) is not driven. It reads the `tutorial` varp
 *   through client scripts whose per-step encoding would have to be decoded out of the cache (the
 *   way the make-menu was); this uses the varp only server-side, and guides the player with
 *   dialogue and hint arrows instead.
 * - **Character design** (interface 679) is not shown; new accounts keep their default look.
 *
 * Every coordinate this module places anything at is a best-effort guess — the cache holds no npc
 * spawns, so the island's real tiles are not readable from it. See [TutorialConstants.START_COORD]
 * and `npcs.toml`. The progression itself does not depend on any of them.
 */
class TutorialIsland @Inject constructor(private val npcRepo: NpcRepository) : PluginScript() {
    private var Player.newAccount by boolVarBit(varbits.new_player_account)

    override fun ScriptContext.startup() {
        onPlayerLogin { player.onLogin() }
        onOpNpc1(TutorialNpcs.guide) { guide(it.npc) }
        onOpNpc1(TutorialNpcs.survival) { survival(it.npc) }
        onOpNpc1(TutorialNpcs.chef) { chef(it.npc) }
        onOpNpc1(TutorialNpcs.quest) { quest(it.npc) }
        onOpNpc1(TutorialNpcs.mining) { mining(it.npc) }
        onOpNpc1(TutorialNpcs.combat) { combat(it.npc) }
        onOpNpc1(TutorialNpcs.account) { account(it.npc) }
        onOpNpc1(TutorialNpcs.magic) { magic(it.npc) }
    }

    /** New accounts begin on the island; everyone else is left where they logged out. */
    private fun Player.onLogin() {
        if (!newAccount || !TutorialConstants.ROUTE_NEW_ACCOUNTS) {
            return
        }
        if (tutorialStage == TutorialStage.NOT_STARTED) {
            tutorialStage = TutorialStage.GUIDE
            coords = TutorialConstants.START_COORD
        }
    }

    // --- Instructors -----------------------------------------------------------------------------

    private suspend fun ProtectedAccess.guide(npc: Npc) {
        talk(npc) {
            if (player.tutorialStage.atLeast(TutorialStage.SURVIVAL_CHOP)) {
                chatNpc(happy, "You're doing well. Head north to the Survival Expert next.")
                return@talk
            }
            chatNpc(happy, "Welcome to the island of Gielinor! I'm here to set you on your way.")
            chatNpc(
                neutral,
                "As you journey, I and my colleagues will teach you what you need to know.",
            )
            chatPlayer(quiz, "Where should I go first?")
            chatNpc(happy, "Step through the door to the north and speak to the Survival Expert.")
            player.advanceTutorial(TutorialStage.SURVIVAL_CHOP)
        }
        hintNext(TutorialNpcs.survival)
    }

    private suspend fun ProtectedAccess.survival(npc: Npc) {
        talk(npc) {
            when {
                player.tutorialStage.isBefore(TutorialStage.SURVIVAL_CHOP) ->
                    chatNpc(neutral, "Speak to the Guide by the entrance first.")

                player.tutorialStage == TutorialStage.SURVIVAL_CHOP -> {
                    give(TutorialObjs.bronze_axe, "Take this axe.")
                    if (player.hasTrained(stats.woodcutting)) {
                        chatNpc(
                            happy,
                            "Good, you've felled a tree! Now light a fire with your tinderbox.",
                        )
                        give(TutorialObjs.tinderbox, "Here's a tinderbox for the fire.")
                        player.advanceTutorial(TutorialStage.SURVIVAL_FIRE)
                    } else {
                        chatNpc(neutral, "Click one of those trees to chop some logs.")
                    }
                }

                player.tutorialStage == TutorialStage.SURVIVAL_FIRE -> {
                    give(TutorialObjs.tinderbox, "Here's a tinderbox.")
                    if (player.hasTrained(stats.firemaking)) {
                        chatNpc(happy, "A fine fire. Now take this net and catch some shrimp.")
                        give(TutorialObjs.small_net, "A small fishing net.")
                        player.advanceTutorial(TutorialStage.SURVIVAL_FISH)
                    } else {
                        chatNpc(neutral, "Use your tinderbox on the logs to light a fire.")
                    }
                }

                player.tutorialStage == TutorialStage.SURVIVAL_FISH -> {
                    give(TutorialObjs.small_net, "A small fishing net.")
                    if (player.hasTrained(stats.fishing)) {
                        chatNpc(
                            happy,
                            "Well caught! Take your shrimp to the Master Chef through the door.",
                        )
                        player.advanceTutorial(TutorialStage.COOKING)
                    } else {
                        chatNpc(neutral, "Use the net on the fishing spot to catch some shrimp.")
                    }
                }

                else -> chatNpc(happy, "Off you go to the Master Chef, through the door.")
            }
        }
        if (player.tutorialStage == TutorialStage.COOKING) hintNext(TutorialNpcs.chef)
    }

    private suspend fun ProtectedAccess.chef(npc: Npc) {
        talk(npc) {
            when {
                player.tutorialStage.isBefore(TutorialStage.COOKING) ->
                    chatNpc(neutral, "The Survival Expert has more to teach you first.")

                player.tutorialStage == TutorialStage.COOKING -> {
                    if (player.hasTrained(stats.cooking)) {
                        chatNpc(happy, "Delicious! Head through to the Quest Guide next.")
                        player.advanceTutorial(TutorialStage.QUEST)
                    } else {
                        chatNpc(
                            happy,
                            "Cook your shrimp on the range over there, then talk to me again.",
                        )
                    }
                }

                else -> chatNpc(happy, "The Quest Guide is waiting through the next door.")
            }
        }
        if (player.tutorialStage == TutorialStage.QUEST) hintNext(TutorialNpcs.quest)
    }

    private suspend fun ProtectedAccess.quest(npc: Npc) {
        talk(npc) {
            if (player.tutorialStage.isBefore(TutorialStage.QUEST)) {
                chatNpc(neutral, "Finish with the Master Chef first.")
                return@talk
            }
            if (player.tutorialStage == TutorialStage.QUEST) {
                chatNpc(
                    happy,
                    "Quests are adventures set by the people of Gielinor. Check the Quest tab any time.",
                )
                chatNpc(neutral, "Down the ladder is the Mining Instructor. Off you go.")
                player.advanceTutorial(TutorialStage.MINING_MINE)
            } else {
                chatNpc(happy, "The Mining Instructor is down the ladder.")
            }
        }
        if (player.tutorialStage == TutorialStage.MINING_MINE) hintNext(TutorialNpcs.mining)
    }

    private suspend fun ProtectedAccess.mining(npc: Npc) {
        talk(npc) {
            when {
                player.tutorialStage.isBefore(TutorialStage.MINING_MINE) ->
                    chatNpc(neutral, "Speak to the Quest Guide upstairs first.")

                player.tutorialStage == TutorialStage.MINING_MINE -> {
                    give(TutorialObjs.bronze_pickaxe, "Take this pickaxe.")
                    if (player.hasTrained(stats.mining)) {
                        chatNpc(happy, "Good ore! Now smelt it into a bronze bar at the furnace.")
                        player.advanceTutorial(TutorialStage.MINING_SMELT)
                    } else {
                        chatNpc(neutral, "Mine the copper and tin rocks with your pickaxe.")
                    }
                }

                player.tutorialStage == TutorialStage.MINING_SMELT -> {
                    if (player.hasTrained(stats.smithing)) {
                        chatNpc(happy, "A fine bar. Now hammer it into a dagger on the anvil.")
                        give(TutorialObjs.hammer, "You'll want this hammer.")
                        player.advanceTutorial(TutorialStage.MINING_SMITH)
                    } else {
                        chatNpc(neutral, "Use the furnace to smelt your ore into a bronze bar.")
                    }
                }

                player.tutorialStage == TutorialStage.MINING_SMITH -> {
                    give(TutorialObjs.hammer, "Here's a hammer.")
                    if (TutorialObjs.bronze_dagger in player.inv) {
                        chatNpc(happy, "Well made! The Combat Instructor is through the gate.")
                        player.advanceTutorial(TutorialStage.COMBAT)
                    } else {
                        chatNpc(neutral, "Use the anvil with your bar to smith a bronze dagger.")
                    }
                }

                else -> chatNpc(happy, "The Combat Instructor is through the gate.")
            }
        }
        if (player.tutorialStage == TutorialStage.COMBAT) hintNext(TutorialNpcs.combat)
    }

    private suspend fun ProtectedAccess.combat(npc: Npc) {
        talk(npc) {
            if (player.tutorialStage.isBefore(TutorialStage.COMBAT)) {
                chatNpc(neutral, "The Mining Instructor has more to show you first.")
                return@talk
            }
            if (player.tutorialStage == TutorialStage.COMBAT) {
                give(TutorialObjs.bronze_sword, "Take this sword")
                give(TutorialObjs.wooden_shield, "and this shield.")
                chatNpc(neutral, "Wield them, then attack one of the giant rats to practise.")
                chatNpc(happy, "When you're done, climb the ladder to the bank.")
                player.advanceTutorial(TutorialStage.BANK)
            } else {
                chatNpc(happy, "The bank is up the ladder.")
            }
        }
        if (player.tutorialStage == TutorialStage.BANK) hintNext(TutorialNpcs.account)
    }

    private suspend fun ProtectedAccess.account(npc: Npc) {
        talk(npc) {
            if (player.tutorialStage.isBefore(TutorialStage.BANK)) {
                chatNpc(neutral, "Train with the Combat Instructor first.")
                return@talk
            }
            if (player.tutorialStage == TutorialStage.BANK) {
                chatNpc(
                    happy,
                    "This is a bank. Your items are safe here and reachable from any bank in Gielinor.",
                )
                chatNpc(neutral, "Last stop: the Magic Instructor, just west.")
                player.advanceTutorial(TutorialStage.MAGIC)
            } else {
                chatNpc(happy, "The Magic Instructor is just to the west.")
            }
        }
        if (player.tutorialStage == TutorialStage.MAGIC) hintNext(TutorialNpcs.magic)
    }

    private suspend fun ProtectedAccess.magic(npc: Npc) {
        var finished = false
        talk(npc) {
            if (player.tutorialStage.isBefore(TutorialStage.MAGIC)) {
                chatNpc(neutral, "See the Account Guide at the bank first.")
                return@talk
            }
            if (player.tutorialStage == TutorialStage.MAGIC) {
                give(TutorialObjs.air_rune, "Some air runes")
                give(TutorialObjs.mind_rune, "and mind runes for your first spell.")
                chatNpc(
                    happy,
                    "That's everything! I'll send you on to the mainland now. Good luck!",
                )
                finished = true
            } else {
                chatNpc(happy, "You've finished the tutorial. Off you go!")
            }
        }
        if (finished) finish()
    }

    /** Ends the tutorial: to Lumbridge, clear the new-account flag, drop the hint arrow. */
    private fun ProtectedAccess.finish() {
        player.advanceTutorial(TutorialStage.COMPLETE)
        player.newAccount = false
        player.coords = TutorialConstants.LUMBRIDGE
        resetHintArrow()
        mes("Welcome to Gielinor.")
    }

    // --- Helpers ---------------------------------------------------------------------------------

    private suspend fun ProtectedAccess.talk(npc: Npc, block: suspend Dialogue.() -> Unit) {
        faceEntitySquare(npc)
        startDialogue(npc) { block() }
    }

    /** True once the player has earned any experience in [stat] — i.e. actually done the action. */
    private fun Player.hasTrained(stat: StatType): Boolean = statMap.getXP(stat) > 0

    /** Hands [obj] over if the player is not already carrying it, with a line of dialogue. */
    private suspend fun Dialogue.give(obj: ObjType, line: String) {
        if (obj in player.inv) {
            return
        }
        val added = player.invAdd(player.inv, obj)
        if (added.success) {
            objbox(obj, line)
        }
    }

    /**
     * Best-effort hint arrow over the nearest [type] instructor. Silent if none is spawned near.
     */
    private fun ProtectedAccess.hintNext(type: NpcType) {
        val npc = nearestNpc(player.coords, type) ?: return
        hintArrow(npc)
    }

    private fun nearestNpc(coords: CoordGrid, type: NpcType): Npc? =
        npcRepo
            .findAll(ZoneKey.from(coords), zoneRadius = 3)
            .filter { it.id == type.id }
            .minByOrNull { it.coords.chebyshevDistance(coords) }
}
