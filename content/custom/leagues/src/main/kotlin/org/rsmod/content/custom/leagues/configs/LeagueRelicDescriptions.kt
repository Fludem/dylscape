package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.editors.struct.StructEditor
import org.rsmod.content.custom.leagues.relics.Relic

/**
 * Rewrites every relic's description (param 880) so the client's expanded view says what *this*
 * server's relic does - never a vanilla effect we have not built.
 *
 * Built relics list exactly their implemented effects; the rest say "Coming soon", and the server
 * refuses to unlock them. Struct edits apply on a normal boot, with no `packCache`.
 */
internal object LeagueRelicDescriptions : StructEditor() {
    init {
        for (relic in Relic.entries) {
            edit(relic.struct) { param[league_params.relic_description] = describe(relic) }
        }
    }

    private fun describe(relic: Relic): String =
        when (relic) {
            Relic.PowerMiner ->
                lines(
                    "Your Echo pickaxe works as a crystal pickaxe with no Mining requirement.",
                    BREAK,
                    "While you carry or wield it:",
                    "A failed swing gets a separate 50% chance to succeed.",
                    "Ore goes straight to your bank while it has room.",
                    "A rock gives 4 ores before it depletes.",
                )
            Relic.Lumberjack ->
                lines(
                    "Your Echo axe works as a crystal axe with no Woodcutting requirement.",
                    BREAK,
                    "While you carry or wield it:",
                    "A failed chop gets a separate 50% chance to succeed.",
                    "Logs go straight to your bank while it has room.",
                    BREAK,
                    "You never fail to light a fire.",
                )
            Relic.AnimalWrangler ->
                lines(
                    "Your Echo harpoon stands in for every fishing tool: nets, rods, cages and " +
                        "harpoons.",
                    BREAK,
                    "While you carry or wield it:",
                    "A failed catch gets a separate 50% chance to succeed.",
                    "You fish one tick faster.",
                    "Fish go straight to your bank while it has room.",
                    BREAK,
                    "You never burn food.",
                )
            Relic.CornerCutter ->
                lines(
                    "You gain 25% more Agility experience.",
                    "Every Mark of grace comes with 10,000 coins.",
                    "Every 10 ticks you spend running in Sage's greaves grants a quarter of a " +
                        "Small XP lamp's worth of Agility experience for your Agility level, " +
                        "multiplied by your xp rate.",
                )
            Relic.BankHeist ->
                lines(
                    "Your Banker's briefcase teleports you to any of Gielinor's major banks.",
                    "Last-destination takes you back to the bank you visited last.",
                    "It ignores wilderness teleport restrictions.",
                )
            Relic.GoldenGod ->
                "You gain the following benefits to the High and Low Alchemy spells:" +
                    "<br><br>" +
                    lines(
                        "The spells have no rune cost or level requirement.",
                        "Items give 15% more gold, and have a 65% chance to not be consumed.",
                    )
            Relic.SlayerMaster ->
                lines(
                    "Every slayer monster counts as your task for Slayer experience, whatever " +
                        "you have been assigned."
                )
            Relic.TotalRecall ->
                lines(
                    "Your Crystal of echoes can save your tile, alongside your Hitpoints, " +
                        "Prayer and Special Attack energy.",
                    "Teleport-back returns you there and restores those stats to what they were.",
                    "It ignores wilderness teleport restrictions.",
                )
            Relic.BankersNote ->
                lines(
                    "Use any item on your Banker's note to note or un-note every one you carry.",
                    "Activate repeats that on the last item you used, and the Quantity options " +
                        "set how many it converts at once.",
                )
            Relic.Grimoire ->
                lines(
                    "Your Arcane grimoire swaps freely between the standard, Ancient and Lunar " +
                        "spellbooks.",
                    "Previous Spellbook swaps straight back to the book you were on.",
                )
            Relic.LastStand ->
                "You gain the Last Stand ability, which works as follows:" +
                    "<br><br>" +
                    lines(
                        "If damage would reduce you to 0 hp, you are reduced to 1 hp instead.",
                        "Your combat stats are boosted to 255, then drain quickly back to their " +
                            "base level + 15.",
                        "For the next 16 ticks you cannot be reduced below 1 hp.",
                    ) +
                    "<br><br>Once used, it cannot be used again until you die or 3 minutes " +
                    "have passed."
            Relic.FriendlyForager ->
                lines(
                    "While your Forager's pouch is in your inventory or equipped, every ore, " +
                        "log, fish and hunter catch has a 1 in 3 chance to find a grimy herb, " +
                        "sent to your bank.",
                    "The herb is any your Herblore level can clean.",
                    BREAK,
                    "Cleaning a herb cleans every herb of that kind you carry.",
                    "Mixing a potion has a 50% chance to keep its secondary ingredient.",
                )
            Relic.DodgyDeals ->
                "You gain the following benefits to Thieving:" +
                    "<br><br>" +
                    lines(
                        "Pickpocketing never fails.",
                        "Pickpocketing an npc also robs every npc of the same kind within 5 " +
                            "tiles, with full loot and experience for each.",
                        "Stalls give double loot.",
                    )
            Relic.ProductionMaster ->
                "When you make items in bulk, every item is made in a single action with full " +
                    "experience:" +
                    "<br><br>" +
                    lines(
                        "Cooking, smelting and smithing.",
                        "Cutting logs, stringing bows, making crossbows and attaching arrows.",
                        "Cutting gems, casting jewellery, crafting leather and spinning.",
                        "Mixing potions.",
                    )
            Relic.ClueCompass ->
                "This server has no clue scrolls, so your Clue compass finds people instead:" +
                    "<br><br>" +
                    lines(
                        "Teleport takes you beside any player who is online.",
                        "Last-destination takes you to the last player you found, wherever " +
                            "they are now.",
                        "Current-step takes you back to where you set off from.",
                        "It works at any Wilderness level.",
                    )
            Relic.FairysFlight ->
                lines(
                    "When you open the clue chest in Edgeville bank, you choose which tier of " +
                        "reward casket to open, whatever key you use.",
                    BREAK,
                    "Your Fairy mushroom teleports you to any of Gielinor's farms, from anywhere.",
                    "Last-destination takes you back to the farm you visited last.",
                )
            Relic.PocketKingdom ->
                "This server has no Miscellania, so your kingdom simply pays tribute:" +
                    "<br><br>" +
                    lines(
                        "Every 10 minutes you are logged in, a delivery arrives in your bank.",
                        "It holds 50 each of the best logs, ore and raw fish you could gather, " +
                            "10 of the best grimy herb you could clean, and 25,000 coins.",
                        "If your bank is full, the delivery waits: Collect takes it noted.",
                    )
            Relic.Overgrown ->
                lines(
                    "Your Leprechaun's vault opens your bank from anywhere.",
                    BREAK,
                    "You gain the following benefits to Farming:",
                    "Seeds have a 75% chance not to be used when planted.",
                    "Crops you plant start half grown.",
                    "Each harvest has an 80% chance not to use up a life.",
                )
            Relic.Guardian ->
                lines(
                    "Your Guardian horn summons a Guardian that follows you for 30 minutes.",
                    "Every 4 ticks it strikes whatever you are attacking, for up to 10 + a " +
                        "third of your highest combat level, and it never misses.",
                    "Its hits count as yours, for kills, loot and Slayer.",
                    "Dismiss sends it away; it also leaves if you die or log out.",
                )
            Relic.Reloaded ->
                lines(
                    "Choose one extra relic from any tier below this one. It works exactly as if " +
                        "you had picked it.",
                    "While you hold Reloaded, choosing a relic from a lower tier sets your " +
                        "Reloaded pick instead of swapping that tier's relic.",
                    "Changing your Reloaded pick costs 2,500,000 coins.",
                )
            Relic.Equilibrium ->
                "You gain the following benefits to experience in every skill, combat included:" +
                    "<br><br>" +
                    lines(
                        "Your lowest skill gains triple experience.",
                        "Any skill below your average level gains double experience.",
                        "Every other skill gains 10% more experience.",
                    )
            Relic.TreasureArbiter ->
                "You gain the following benefits when killing monsters:" +
                    "<br><br>" +
                    lines(
                        "Every monster with a drop table drops its loot twice over.",
                        "Every drop that can be noted drops noted.",
                    )
            Relic.Specialist ->
                "You gain the following benefits to special attacks:" +
                    "<br><br>" +
                    lines(
                        "Special attacks cost at most 20% special attack energy.",
                        "Special attacks have double accuracy.",
                        "Special attack energy regenerates twice as fast.",
                        "Killing any monster restores 15% special attack energy.",
                    )
            else -> COMING_SOON
        }

    /** Formats [entries] the way the vanilla descriptions are written: one "- " line each. */
    private fun lines(vararg entries: String): String =
        entries.joinToString("<br>") { if (it == BREAK) "" else "- $it" }

    private const val BREAK = "\u0000"

    private const val COMING_SOON =
        "<col=ff0000>Coming soon</col><br><br>This relic is not on the server yet, so it cannot " +
            "be unlocked."
}
