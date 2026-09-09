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
import org.rsmod.content.areas.misc.tutorial.configs.TutorialKitObjs
import org.rsmod.content.areas.misc.tutorial.configs.TutorialNpcs
import org.rsmod.content.areas.misc.tutorial.configs.TutorialObjs
import org.rsmod.events.EventBus
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
 * Every coordinate this module places anything at is read out of the cache — the island's collision
 * map and loc set pin each room, even though npc spawns themselves are server-authored. See
 * [TutorialConstants.START_COORD] and `npcs.toml`, and `TutorialMapTest`, which asserts every one
 * of them is a tile something can actually stand on.
 *
 * The island's own scenery is bound by this module too, in [TutorialDoors] (the doors and the
 * survival-to-kitchen gate) and [TutorialScenery] (the two ladders into the mining cave, and the
 * bank booth). The skill locs are tagged by the skills that own them: the trees in woodcutting, the
 * copper and tin in mining, the fishing spot in fishing, the range in cooking, and the furnace in
 * smithing.
 */
class TutorialIsland
@Inject
constructor(private val npcRepo: NpcRepository, private val eventBus: EventBus) : PluginScript() {
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
        onOpNpc1(TutorialNpcs.prayer) { prayer(it.npc) }
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
            // First thing a brand-new account sees, as on live. The Guide can reopen it.
            TutorialCharacterDesign.openDesign(this, eventBus)
        }
    }

    // --- Instructors -----------------------------------------------------------------------------

    private suspend fun ProtectedAccess.guide(npc: Npc) {
        var reopenDesign = false
        talk(npc) {
            if (player.tutorialStage.atLeast(TutorialStage.SURVIVAL_CHOP)) {
                chatNpc(happy, "You're doing well. Head north to the Survival Expert next.")
                reopenDesign = true
                return@talk
            }
            chatNpc(happy, "Welcome to the island of Gielinor! I'm here to set you on your way.")
            chatNpc(
                neutral,
                "As you journey, I and my colleagues will teach you what you need to know.",
            )
            chatPlayer(quiz, "Where should I go first?")
            chatNpc(happy, "Step through the door to the north and speak to the Survival Expert.")
            chatNpc(neutral, "Talk to me again if you'd like to change your appearance.")
            player.advanceTutorial(TutorialStage.SURVIVAL_CHOP)
        }
        if (reopenDesign) {
            TutorialCharacterDesign.openDesign(player, eventBus)
            return
        }
        hintNext("survival", TutorialNpcs.survival)
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
                        chatNpc(happy, "Well caught! Now let's cook them.")
                        chatNpc(
                            neutral,
                            "Use your raw shrimps on the fire you lit to cook yourself a meal.",
                        )
                        player.advanceTutorial(TutorialStage.SURVIVAL_COOK)
                    } else {
                        chatNpc(neutral, "Use the net on the fishing spot to catch some shrimp.")
                    }
                }

                player.tutorialStage == TutorialStage.SURVIVAL_COOK -> {
                    if (player.hasTrained(stats.cooking)) {
                        chatNpc(happy, "Nicely cooked. Food like that will keep you alive.")
                        chatNpc(
                            neutral,
                            "That's all from me. Head west through the gate to the Master Chef.",
                        )
                        player.advanceTutorial(TutorialStage.COOKING)
                    } else {
                        chatNpc(
                            neutral,
                            "Use your raw shrimps on your fire. If it burnt out, light another.",
                        )
                    }
                }

                else -> chatNpc(happy, "Off you go to the Master Chef, west through the gate.")
            }
        }
        if (player.tutorialStage == TutorialStage.COOKING) hintNext("chef", TutorialNpcs.chef)
    }

    private suspend fun ProtectedAccess.chef(npc: Npc) {
        talk(npc) {
            when {
                player.tutorialStage.isBefore(TutorialStage.COOKING) ->
                    chatNpc(neutral, "The Survival Expert has more to teach you first.")

                player.tutorialStage == TutorialStage.COOKING -> {
                    // The shrimp were cooked on the Survival Expert's fire, so cooking xp is
                    // already above zero by now and cannot gate this step. The Chef's lesson is
                    // bread, as it is in the live game, and the loaf itself is the proof.
                    val started =
                        TutorialObjs.bread_dough in player.inv || TutorialObjs.bread in player.inv
                    if (!started) {
                        give(TutorialObjs.newbie_pot_flour, "Here's a pot of flour")
                        give(TutorialObjs.bucket_water, "and a bucket of water.")
                    }
                    if (TutorialObjs.bread in player.inv) {
                        chatNpc(happy, "Perfect bread! Head through to the Quest Guide next.")
                        player.advanceTutorial(TutorialStage.QUEST)
                    } else if (TutorialObjs.bread_dough in player.inv) {
                        chatNpc(neutral, "Now bake that dough on my range, and talk to me again.")
                    } else {
                        chatNpc(
                            happy,
                            "Use the water on the flour to make dough, then bake it on my range.",
                        )
                    }
                }

                else -> chatNpc(happy, "The Quest Guide is waiting through the next door.")
            }
        }
        if (player.tutorialStage == TutorialStage.QUEST) hintNext("quest", TutorialNpcs.quest)
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
        if (player.tutorialStage == TutorialStage.MINING_MINE) hintNext("ladder_down")
    }

    private suspend fun ProtectedAccess.mining(npc: Npc) {
        talk(npc) {
            when {
                player.tutorialStage.isBefore(TutorialStage.MINING_MINE) ->
                    chatNpc(neutral, "Speak to the Quest Guide upstairs first.")

                player.tutorialStage == TutorialStage.MINING_MINE -> {
                    give(TutorialObjs.bronze_pickaxe, "Take this pickaxe.")
                    val copper = TutorialObjs.copper_ore in player.inv
                    val tin = TutorialObjs.tin_ore in player.inv
                    if (copper && tin) {
                        chatNpc(happy, "That's both ores. Bronze is made from copper and tin.")
                        chatNpc(neutral, "Smelt them together into a bronze bar at the furnace.")
                        player.advanceTutorial(TutorialStage.MINING_SMELT)
                    } else if (copper) {
                        chatNpc(neutral, "Good, that's the copper. Now mine a tin ore as well.")
                    } else if (tin) {
                        chatNpc(neutral, "Good, that's the tin. Now mine a copper ore as well.")
                    } else {
                        chatNpc(
                            neutral,
                            "Bronze needs two ores: mine a copper rock and a tin rock.",
                        )
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
        if (player.tutorialStage == TutorialStage.COMBAT) hintNext("combat", TutorialNpcs.combat)
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
                // Any melee style trains one of these three, so this is "you actually fought",
                // not "you fought the way I happened to expect". Hitpoints is no good as a check:
                // a new account already has 1,154 xp in it from starting at level 10.
                if (player.hasFought()) {
                    chatNpc(happy, "Well fought! You're ready to leave the cave.")
                    chatNpc(neutral, "Climb the ladder in the north-east corner to reach the bank.")
                    player.advanceTutorial(TutorialStage.BANK)
                } else {
                    chatNpc(neutral, "Wield them, then attack one of the giant rats to practise.")
                    chatNpc(happy, "Come back to me once you've killed one.")
                }
            } else {
                chatNpc(happy, "The bank is up the ladder to the north-east.")
            }
        }
        if (player.tutorialStage == TutorialStage.BANK) hintNext("ladder_up")
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
                chatNpc(neutral, "Through the door and south is a chapel. Speak to Brother Brace.")
                player.advanceTutorial(TutorialStage.PRAYER)
            } else {
                chatNpc(happy, "Brother Brace is in the chapel, through the door and south.")
            }
        }
        if (player.tutorialStage == TutorialStage.PRAYER) hintNext("prayer", TutorialNpcs.prayer)
    }

    /** Brother Brace: prayer and the friends list. A talk-through step, as it is on live. */
    private suspend fun ProtectedAccess.prayer(npc: Npc) {
        talk(npc) {
            if (player.tutorialStage.isBefore(TutorialStage.PRAYER)) {
                chatNpc(neutral, "See the Account Guide at the bank first.")
                return@talk
            }
            if (player.tutorialStage == TutorialStage.PRAYER) {
                chatNpc(
                    happy,
                    "Welcome, friend. This is a chapel, and that altar restores your Prayer.",
                )
                chatNpc(
                    neutral,
                    "Prayers drain as you use them. Bury bones to raise the skill itself.",
                )
                chatNpc(neutral, "Now off to the Magic Instructor, out the far door and east.")
                player.advanceTutorial(TutorialStage.MAGIC)
            } else {
                chatNpc(happy, "The Magic Instructor is out the far door and east.")
            }
        }
        if (player.tutorialStage == TutorialStage.MAGIC) hintNext("magic", TutorialNpcs.magic)
    }

    private suspend fun ProtectedAccess.magic(npc: Npc) {
        var finished = false
        talk(npc) {
            if (player.tutorialStage.isBefore(TutorialStage.MAGIC)) {
                chatNpc(neutral, "Speak to Brother Brace in the chapel first.")
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

    /**
     * Ends the tutorial: bank the starter kit, teleport to Lumbridge, clear the new-account flag
     * and drop the hint arrow.
     *
     * The kit is banked before the flag is cleared, so the one thing that decides whether a player
     * gets it is the same thing that decides whether they were on the island at all. [finish] is
     * only ever reached from the Magic Instructor's last line, and [TutorialStage.COMPLETE] gates
     * that line, so it cannot be run twice.
     */
    private fun ProtectedAccess.finish() {
        player.advanceTutorial(TutorialStage.COMPLETE)
        grantStarterKit()
        player.newAccount = false
        player.coords = TutorialConstants.LUMBRIDGE
        resetHintArrow()
        mes("Welcome to Gielinor.")
    }

    /**
     * Puts [TutorialKitObjs.all] in the player's bank.
     *
     * `strict = false` so a stack that would overflow is capped rather than rejected outright; a
     * brand-new bank is empty, so in practice every entry lands whole.
     */
    private fun ProtectedAccess.grantStarterKit() {
        for ((obj, count) in TutorialKitObjs.all) {
            player.invAdd(bank, obj, count, strict = false)
        }
        mes("A starter kit is waiting for you in the bank.")
    }

    // --- Helpers ---------------------------------------------------------------------------------

    private suspend fun ProtectedAccess.talk(npc: Npc, block: suspend Dialogue.() -> Unit) {
        faceEntitySquare(npc)
        startDialogue(npc) { block() }
    }

    /** True once the player has earned any experience in [stat] — i.e. actually done the action. */
    private fun Player.hasTrained(stat: StatType): Boolean = statMap.getXP(stat) > 0

    /**
     * True once the player has landed a hit in any melee style.
     *
     * Attack, strength and defence are checked together because the three accurate/aggressive/
     * defensive styles each train a different one, and controlled trains all three -- keying off
     * just one would gate the step on the player having picked a particular stance.
     */
    private fun Player.hasFought(): Boolean =
        hasTrained(stats.attack) || hasTrained(stats.strength) || hasTrained(stats.defence)

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
     * Points the hint arrow at [key]'s tile in [TutorialConstants.HINT_TILES].
     *
     * If the named npc happens to be loaded nearby the arrow is put on the npc itself, so it tracks
     * them if they move; otherwise it falls back to the tile, which is the case that matters --
     * every instructor sends the player somewhere too far away for the npc to be loaded yet.
     */
    private fun ProtectedAccess.hintNext(key: String, type: NpcType? = null) {
        val coords = TutorialConstants.HINT_TILES[key] ?: return
        val npc = type?.let { nearestNpc(coords, it) }
        if (npc != null) hintArrow(npc) else hintArrow(coords)
    }

    private fun nearestNpc(coords: CoordGrid, type: NpcType): Npc? =
        npcRepo
            .findAll(ZoneKey.from(coords), zoneRadius = 3)
            .filter { it.id == type.id }
            .minByOrNull { it.coords.chebyshevDistance(coords) }
}
