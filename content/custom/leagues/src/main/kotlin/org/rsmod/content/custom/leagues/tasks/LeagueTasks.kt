package org.rsmod.content.custom.leagues.tasks

import org.rsmod.api.config.refs.stats
import org.rsmod.content.custom.leagues.configs.league_task_objs
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Asgarnia
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Desert
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Fremennik
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Global
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Kandarin
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Karamja
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Misthalin
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Morytania
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Tirannwn
import org.rsmod.content.custom.leagues.tasks.LeagueTaskArea.Wilderness
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier.Easy
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier.Elite
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier.Hard
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier.Master
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier.Medium
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

/**
 * Every league task, in id order.
 *
 * **Declaration order is load-bearing.** A task's id is its position here, and that id is packed
 * into the cache (as `STRUCT_ID_BASE + id`), read by the client as the completion bit, and saved
 * under in the counters table. Add new tasks at the end of their tier group only if the tier groups
 * are not contiguous anymore - otherwise append at the very end. To retire a task, replace it with
 * [retired] so every id after it keeps its meaning.
 *
 * Every npc, obj and skill named here must exist on this server: kill tasks match npc display names
 * and are checked at boot, gear and products are typed references, and the visit boxes are checked
 * against the collision map by `LeagueTaskTableTest`.
 */
object LeagueTasks {
    private val objs
        get() = league_task_objs

    private val STRUNG_BOWS: Array<ObjType>
        get() =
            arrayOf(
                objs.shortbow,
                objs.longbow,
                objs.oak_shortbow,
                objs.oak_longbow,
                objs.willow_shortbow,
                objs.willow_longbow,
                objs.maple_shortbow,
                objs.maple_longbow,
                objs.yew_shortbow,
                objs.yew_longbow,
                objs.magic_shortbow,
                objs.magic_longbow,
            )

    private val BARROWS_BROTHERS =
        setOf(
            "Ahrim the Blighted",
            "Dharok the Wretched",
            "Guthan the Infested",
            "Karil the Tainted",
            "Torag the Corrupted",
            "Verac the Defiled",
        )

    private val DAGANNOTH_KINGS = setOf("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme")

    /** Every trainable skill, with its display name and its index in enum 2729. */
    private val SKILLS: List<Skill> =
        listOf(
            Skill(stats.attack, "Attack", 1),
            Skill(stats.strength, "Strength", 2),
            Skill(stats.ranged, "Ranged", 3),
            Skill(stats.magic, "Magic", 4),
            Skill(stats.defence, "Defence", 5),
            Skill(stats.hitpoints, "Hitpoints", 6),
            Skill(stats.prayer, "Prayer", 7),
            Skill(stats.agility, "Agility", 8),
            Skill(stats.herblore, "Herblore", 9),
            Skill(stats.thieving, "Thieving", 10),
            Skill(stats.crafting, "Crafting", 11),
            Skill(stats.mining, "Mining", 13),
            Skill(stats.smithing, "Smithing", 14),
            Skill(stats.fishing, "Fishing", 15),
            Skill(stats.cooking, "Cooking", 16),
            Skill(stats.firemaking, "Firemaking", 17),
            Skill(stats.woodcutting, "Woodcutting", 18),
            Skill(stats.fletching, "Fletching", 19),
            Skill(stats.slayer, "Slayer", 20),
            Skill(stats.farming, "Farming", 21),
            Skill(stats.construction, "Construction", 22),
            Skill(stats.hunter, "Hunter", 23),
        )

    private val ELITE_SKILLS =
        listOf(
            stats.attack,
            stats.strength,
            stats.defence,
            stats.ranged,
            stats.magic,
            stats.hitpoints,
            stats.mining,
            stats.woodcutting,
            stats.fishing,
            stats.cooking,
            stats.slayer,
            stats.thieving,
        )

    private val MASTER_SKILLS =
        listOf(stats.attack, stats.strength, stats.hitpoints, stats.ranged, stats.magic)

    // Initialisers run in textual order, so everything `declare()` reads sits above `all`.
    val all: List<LeagueTask> = TaskListBuilder().apply { declare() }.build()

    val byId: Map<Int, LeagueTask> = all.associateBy { it.id }

    fun find(idOrName: String): LeagueTask? {
        idOrName.toIntOrNull()?.let {
            return byId[it]
        }
        return all.firstOrNull { it.name.equals(idOrName, ignoreCase = true) }
            ?: all.firstOrNull { it.name.startsWith(idOrName, ignoreCase = true) }
    }

    /** Total points on offer across every task; the tier thresholds are set against this. */
    val totalPoints: Int = all.sumOf { it.points }

    private fun TaskListBuilder.declare() {
        // ----------------------------------------------------------------------------- Easy
        levels(Easy, 20)
        total(Easy, 100)
        total(Easy, 250)
        combat(Easy, 20)

        kill(Easy, "Defeat 5 Chickens", 5, "Chicken", Misthalin)
        kill(Easy, "Defeat 5 Cows", 5, "Cow", Misthalin)
        kill(Easy, "Defeat 10 Goblins", 10, "Goblin", Misthalin)
        kill(Easy, "Defeat 5 Giant Rats", 5, "Giant rat", Misthalin)
        kill(Easy, "Defeat 5 Imps", 5, "Imp")
        kill(Easy, "Defeat 5 Scorpions", 5, "Scorpion")
        kill(Easy, "Defeat 5 Wolves", 5, "Wolf")
        kill(Easy, "Defeat 10 Skeletons", 10, "Skeleton")
        kill(Easy, "Defeat 10 Zombies", 10, "Zombie")
        kill(Easy, "Defeat 5 Ghosts", 5, "Ghost")
        kill(Easy, "Defeat 5 Guards", 5, "Guard")
        kill(Easy, "Defeat 5 Dark Wizards", 5, "Dark wizard", Misthalin)
        kill(Easy, "Defeat 5 Dwarves", 5, "Dwarf", Asgarnia)
        kill(Easy, "Defeat 5 Barbarians", 5, "Barbarian", Misthalin)
        kill(Easy, "Defeat 5 Bats", 5, "Bat")
        kill(Easy, "Defeat 5 Giant Frogs", 5, "Giant frog", Misthalin)

        gather(Easy, "Mine Some Ore", "Mine an ore of any kind.", GatherKind.Mine, 1)
        gather(Easy, "Mine 10 Iron Ore", "Mine 10 iron ore.", GatherKind.Mine, 10, objs.iron_ore)
        gather(Easy, "Chop Some Logs", "Cut logs from any tree.", GatherKind.Chop, 1)
        gather(Easy, "Chop 10 Oak Logs", "Cut 10 oak logs.", GatherKind.Chop, 10, objs.oak_logs)
        gather(Easy, "Catch a Fish", "Catch a fish of any kind.", GatherKind.Fish, 1)
        gather(Easy, "Catch 10 Trout", "Catch 10 raw trout.", GatherKind.Fish, 10, objs.raw_trout)
        gather(Easy, "Cook Some Food", "Cook any item successfully.", GatherKind.Cook, 1)
        gather(
            Easy,
            "Cook 10 Shrimp",
            "Cook 10 shrimp without burning them.",
            GatherKind.Cook,
            10,
            objs.shrimp,
        )
        gather(Easy, "Light a Fire", "Light a fire with a tinderbox.", GatherKind.Firemake, 1)
        gather(
            Easy,
            "Burn 10 Logs",
            "Light 10 fires from logs of any kind.",
            GatherKind.Firemake,
            10,
        )
        gather(Easy, "Bury Some Bones", "Bury bones of any kind.", GatherKind.BuryBones, 1)
        gather(Easy, "Bury 10 Bones", "Bury 10 bones of any kind.", GatherKind.BuryBones, 10)
        gather(
            Easy,
            "Offer Bones at an Altar",
            "Use bones on a gilded altar or the Chaos altar.",
            GatherKind.OfferBones,
            1,
        )
        gather(
            Easy,
            "Smelt a Bronze Bar",
            "Smelt a bronze bar at a furnace.",
            GatherKind.Smelt,
            1,
            objs.bronze_bar,
        )
        gather(
            Easy,
            "Smith a Bronze Dagger",
            "Smith a bronze dagger on an anvil.",
            GatherKind.Smith,
            1,
            objs.bronze_dagger,
        )
        gather(
            Easy,
            "Cut a Sapphire",
            "Cut an uncut sapphire with a chisel.",
            GatherKind.Craft,
            1,
            objs.sapphire,
        )
        gather(
            Easy,
            "Craft an Item",
            "Craft any item: leather, jewellery, spinning or gem cutting.",
            GatherKind.Craft,
            1,
        )
        gather(
            Easy,
            "Fletch an Item",
            "Fletch any item from logs, or attach arrow heads.",
            GatherKind.Fletch,
            1,
        )
        gather(Easy, "String a Bow", "String any unstrung bow.", GatherKind.Fletch, 1, *STRUNG_BOWS)
        gather(Easy, "Clean a Herb", "Clean a grimy herb of any kind.", GatherKind.CleanHerb, 1)
        gather(Easy, "Mix a Potion", "Mix any finished potion.", GatherKind.Potion, 1)
        gather(
            Easy,
            "Pickpocket an NPC",
            "Successfully pickpocket any NPC.",
            GatherKind.Pickpocket,
            1,
        )
        gather(
            Easy,
            "Pickpocket 10 Times",
            "Successfully pickpocket any NPC 10 times.",
            GatherKind.Pickpocket,
            10,
        )
        gather(Easy, "Steal from a Stall", "Steal from any market stall.", GatherKind.Stall, 1)
        gather(
            Easy,
            "Steal from a Stall 10 Times",
            "Steal from market stalls 10 times.",
            GatherKind.Stall,
            10,
        )
        gather(
            Easy,
            "Catch a Creature",
            "Catch any creature with a Hunter trap.",
            GatherKind.Hunt,
            1,
        )
        gather(
            Easy,
            "Catch 10 Creatures",
            "Catch 10 creatures with Hunter traps.",
            GatherKind.Hunt,
            10,
        )
        gather(
            Easy,
            "Harvest a Crop",
            "Harvest produce from any farming patch.",
            GatherKind.Harvest,
            1,
        )
        gather(
            Easy,
            "Complete a Rooftop Lap",
            "Complete a lap of any agility course.",
            GatherKind.AgilityLap,
            1,
        )

        equip(Easy, "Equip a Shortbow", "Equip a shortbow.", objs.shortbow)
        equip(Easy, "Equip Leather Armour", "Equip a leather body.", objs.leather_armour)
        equip(Easy, "Equip an Iron Scimitar", "Equip an iron scimitar.", objs.iron_scimitar)
        equip(Easy, "Equip an Amulet of Power", "Equip an amulet of power.", objs.amulet_of_power)

        relic(
            Easy,
            "Unlock a Relic",
            "Choose your first relic from the Relics menu.",
            minTier = null,
        )

        visit(
            Easy,
            "Visit Varrock",
            "Stand in Varrock's town square.",
            Box(3200, 3418, 3225, 3440),
            Misthalin,
        )
        visit(
            Easy,
            "Visit Falador Park",
            "Stroll through Falador park.",
            Box(2990, 3370, 3010, 3388),
            Asgarnia,
        )
        visit(
            Easy,
            "Visit Draynor Village",
            "Visit the bank in Draynor Village.",
            Box(3086, 3238, 3099, 3248),
            Misthalin,
        )
        visit(
            Easy,
            "Visit Al Kharid",
            "Visit the palace in Al Kharid.",
            Box(3280, 3160, 3305, 3180),
            Desert,
        )
        visit(
            Easy,
            "Visit Edgeville",
            "Visit the bank in Edgeville.",
            Box(3089, 3486, 3100, 3500),
            Misthalin,
        )
        visit(
            Easy,
            "Visit Barbarian Village",
            "Visit Barbarian Village.",
            Box(3072, 3412, 3092, 3437),
            Misthalin,
        )
        visit(
            Easy,
            "Visit Port Sarim",
            "Walk along the docks of Port Sarim.",
            Box(3010, 3195, 3050, 3230),
            Asgarnia,
        )
        visit(Easy, "Visit Rimmington", "Visit Rimmington.", Box(2945, 3200, 2970, 3225), Asgarnia)
        visit(
            Easy,
            "Visit the Lumbridge Swamp",
            "Wander into the Lumbridge Swamp.",
            Box(3180, 3160, 3225, 3190),
            Misthalin,
        )
        visit(
            Easy,
            "Visit the Grand Exchange",
            "Visit the Grand Exchange.",
            Box(3150, 3475, 3180, 3505),
            Misthalin,
        )
        visit(
            Easy,
            "Visit the Edgeville Monastery",
            "Visit the monastery west of Edgeville.",
            Box(3040, 3480, 3062, 3500),
            Asgarnia,
        )
        visit(
            Easy,
            "Visit Musa Point",
            "Set foot on Karamja at Musa Point.",
            Box(2900, 3145, 2930, 3175),
            Karamja,
        )

        // --------------------------------------------------------------------------- Medium
        levels(Medium, 40)
        total(Medium, 500)
        total(Medium, 750)
        combat(Medium, 50)

        kill(Medium, "Defeat 25 Hill Giants", 25, "Hill Giant")
        kill(Medium, "Defeat 25 Moss Giants", 25, "Moss giant")
        kill(Medium, "Defeat 25 Ice Giants", 25, "Ice giant")
        kill(Medium, "Defeat 25 Ogres", 25, "Ogre", Kandarin)
        kill(Medium, "Defeat 25 Mountain Trolls", 25, "Mountain troll", Asgarnia)
        kill(Medium, "Defeat 10 Cyclopes", 10, "Cyclops")
        kill(Medium, "Defeat 10 Lesser Demons", 10, "Lesser demon")
        kill(Medium, "Defeat 25 Black Knights", 25, "Black Knight", Asgarnia)
        kill(Medium, "Defeat 10 White Knights", 10, "White Knight", Asgarnia)
        kill(Medium, "Defeat 10 Hellhounds", 10, "Hellhound")
        kill(Medium, "Defeat 25 Ghouls", 25, "Ghoul", Morytania)
        kill(Medium, "Defeat 25 Jogres", 25, "Jogre", Karamja)
        kill(Medium, "Defeat 25 Kalphite Workers", 25, "Kalphite Worker", Desert)
        kill(Medium, "Defeat 10 Green Dragons", 10, "Green dragon", Wilderness)
        kill(Medium, "Defeat 25 Chaos Druids", 25, "Chaos druid")
        kill(Medium, "Defeat 25 Rogues", 25, "Rogue", Wilderness)
        kill(Medium, "Defeat 25 Pirates", 25, "Pirate")
        kill(Medium, "Defeat 25 Giant Spiders", 25, "Giant spider")
        kill(Medium, "Defeat 25 Ice Warriors", 25, "Ice warrior")
        kill(Medium, "Defeat 10 Mummies", 10, "Mummy", Desert)
        kill(Medium, "Defeat 10 Fire Giants", 10, "Fire giant")

        gather(
            Medium,
            "Mine 100 Iron Ore",
            "Mine 100 iron ore.",
            GatherKind.Mine,
            100,
            objs.iron_ore,
        )
        gather(Medium, "Mine 50 Coal", "Mine 50 coal.", GatherKind.Mine, 50, objs.coal)
        gather(
            Medium,
            "Chop 100 Willow Logs",
            "Cut 100 willow logs.",
            GatherKind.Chop,
            100,
            objs.willow_logs,
        )
        gather(
            Medium,
            "Chop 50 Maple Logs",
            "Cut 50 maple logs.",
            GatherKind.Chop,
            50,
            objs.maple_logs,
        )
        gather(
            Medium,
            "Catch 100 Trout or Salmon",
            "Catch 100 raw trout or salmon.",
            GatherKind.Fish,
            100,
            objs.raw_trout,
            objs.raw_salmon,
        )
        gather(
            Medium,
            "Catch 25 Lobsters",
            "Catch 25 raw lobsters.",
            GatherKind.Fish,
            25,
            objs.raw_lobster,
        )
        gather(Medium, "Cook 100 Items", "Cook 100 items successfully.", GatherKind.Cook, 100)
        gather(
            Medium,
            "Cook 25 Lobsters",
            "Cook 25 lobsters without burning them.",
            GatherKind.Cook,
            25,
            objs.lobster,
        )
        gather(
            Medium,
            "Burn 100 Logs",
            "Light 100 fires from logs of any kind.",
            GatherKind.Firemake,
            100,
        )
        gather(
            Medium,
            "Burn 50 Willow Logs",
            "Light 50 fires from willow logs.",
            GatherKind.Firemake,
            50,
            objs.willow_logs,
        )
        gather(Medium, "Bury 100 Bones", "Bury 100 bones of any kind.", GatherKind.BuryBones, 100)
        gather(
            Medium,
            "Bury 25 Big Bones",
            "Bury 25 big bones.",
            GatherKind.BuryBones,
            25,
            objs.big_bones,
        )
        gather(
            Medium,
            "Offer 25 Bones at an Altar",
            "Offer 25 bones at a gilded altar or the Chaos altar.",
            GatherKind.OfferBones,
            25,
        )
        gather(
            Medium,
            "Smelt 50 Steel Bars",
            "Smelt 50 steel bars.",
            GatherKind.Smelt,
            50,
            objs.steel_bar,
        )
        gather(Medium, "Smith 100 Items", "Smith 100 items on an anvil.", GatherKind.Smith, 100)
        gather(
            Medium,
            "Smith 10 Steel Platebodies",
            "Smith 10 steel platebodies.",
            GatherKind.Smith,
            10,
            objs.steel_platebody,
        )
        gather(Medium, "Craft 100 Items", "Craft 100 items of any kind.", GatherKind.Craft, 100)
        gather(Medium, "Fletch 100 Items", "Fletch 100 items of any kind.", GatherKind.Fletch, 100)
        gather(
            Medium,
            "String 50 Bows",
            "String 50 bows of any kind.",
            GatherKind.Fletch,
            50,
            *STRUNG_BOWS,
        )
        gather(Medium, "Clean 100 Herbs", "Clean 100 grimy herbs.", GatherKind.CleanHerb, 100)
        gather(Medium, "Mix 25 Potions", "Mix 25 finished potions.", GatherKind.Potion, 25)
        gather(
            Medium,
            "Pickpocket 100 Times",
            "Successfully pickpocket any NPC 100 times.",
            GatherKind.Pickpocket,
            100,
        )
        gather(
            Medium,
            "Steal from a Stall 100 Times",
            "Steal from market stalls 100 times.",
            GatherKind.Stall,
            100,
        )
        gather(
            Medium,
            "Catch 50 Creatures",
            "Catch 50 creatures with Hunter traps.",
            GatherKind.Hunt,
            50,
        )
        gather(
            Medium,
            "Harvest 25 Crops",
            "Harvest produce from farming patches 25 times.",
            GatherKind.Harvest,
            25,
        )
        gather(
            Medium,
            "Complete 25 Rooftop Laps",
            "Complete 25 laps of agility courses.",
            GatherKind.AgilityLap,
            25,
        )

        equip(
            Medium,
            "Equip a Mithril Platebody",
            "Equip a mithril platebody.",
            objs.mithril_platebody,
        )
        equip(Medium, "Equip a Rune Scimitar", "Equip a rune scimitar.", objs.rune_scimitar)
        equip(
            Medium,
            "Equip a Green D'hide Body",
            "Equip a green dragonhide body.",
            objs.green_dhide_body,
        )
        equip(Medium, "Equip a Mystic Hat", "Equip a mystic hat.", objs.mystic_hat)
        equip(Medium, "Equip an Amulet of Glory", "Equip an amulet of glory.", objs.amulet_of_glory)
        equip(Medium, "Equip a Magic Shortbow", "Equip a magic shortbow.", objs.magic_shortbow)

        relic(Medium, "Unlock a Tier 2 Relic", "Unlock a relic from the second tier.", minTier = 1)

        visit(Medium, "Visit Taverley", "Visit Taverley.", Box(2880, 3425, 2905, 3455), Asgarnia)
        visit(
            Medium,
            "Visit Catherby",
            "Visit the bank in Catherby.",
            Box(2800, 3430, 2825, 3448),
            Kandarin,
        )
        visit(
            Medium,
            "Visit Seers' Village",
            "Visit the bank in Seers' Village.",
            Box(2718, 3485, 2735, 3500),
            Kandarin,
        )
        visit(
            Medium,
            "Visit Ardougne",
            "Visit the market in East Ardougne.",
            Box(2650, 3298, 2672, 3322),
            Kandarin,
        )
        visit(Medium, "Visit Yanille", "Visit Yanille.", Box(2590, 3085, 2620, 3110), Kandarin)
        visit(Medium, "Visit Canifis", "Visit Canifis.", Box(3480, 3470, 3510, 3500), Morytania)
        visit(Medium, "Visit Burthorpe", "Visit Burthorpe.", Box(2885, 3530, 2915, 3560), Asgarnia)
        visit(
            Medium,
            "Visit the Shantay Pass",
            "Visit the Shantay Pass south of Al Kharid.",
            Box(3295, 3115, 3315, 3130),
            Desert,
        )
        visit(
            Medium,
            "Visit the Tree Gnome Stronghold",
            "Visit the Tree Gnome Stronghold.",
            Box(2430, 3410, 2490, 3470),
            Kandarin,
        )
        visit(Medium, "Visit Rellekka", "Visit Rellekka.", Box(2630, 3640, 2685, 3690), Fremennik)

        // ----------------------------------------------------------------------------- Hard
        levels(Hard, 60)
        total(Hard, 1000)
        total(Hard, 1250)
        combat(Hard, 80)

        kill(Hard, "Defeat 50 Greater Demons", 50, "Greater demon")
        kill(Hard, "Defeat 25 Black Demons", 25, "Black demon")
        kill(Hard, "Defeat 100 Fire Giants", 100, "Fire giant")
        kill(Hard, "Defeat 50 Blue Dragons", 50, "Blue dragon", Asgarnia)
        kill(Hard, "Defeat 25 Red Dragons", 25, "Red dragon")
        kill(Hard, "Defeat 10 Black Dragons", 10, "Black dragon")
        kill(Hard, "Defeat 100 Hellhounds", 100, "Hellhound")
        kill(Hard, "Defeat 10 Bronze Dragons", 10, "Bronze dragon")
        kill(Hard, "Defeat 25 Iron Dragons", 25, "Iron dragon")
        kill(Hard, "Defeat 10 Steel Dragons", 10, "Steel dragon")
        kill(Hard, "Defeat 100 Lesser Demons", 100, "Lesser demon")
        kill(Hard, "Defeat 50 Kalphite Soldiers", 50, "Kalphite Soldier", Desert)
        kill(Hard, "Defeat the Kalphite Queen", 1, "Kalphite Queen", Desert)
        kill(Hard, "Defeat a Barrows Brother", 1, BARROWS_BROTHERS, Morytania)
        kill(Hard, "Defeat a Dagannoth King", 1, DAGANNOTH_KINGS, Fremennik)
        kill(Hard, "Defeat Zulrah", 1, "Zulrah", Tirannwn)

        gather(
            Hard,
            "Mine 200 Mithril Ore",
            "Mine 200 mithril ore.",
            GatherKind.Mine,
            200,
            objs.mithril_ore,
        )
        gather(
            Hard,
            "Mine 100 Adamantite Ore",
            "Mine 100 adamantite ore.",
            GatherKind.Mine,
            100,
            objs.adamantite_ore,
        )
        gather(Hard, "Chop 250 Yew Logs", "Cut 250 yew logs.", GatherKind.Chop, 250, objs.yew_logs)
        gather(
            Hard,
            "Catch 250 Lobsters",
            "Catch 250 raw lobsters.",
            GatherKind.Fish,
            250,
            objs.raw_lobster,
        )
        gather(
            Hard,
            "Catch 100 Swordfish",
            "Catch 100 raw swordfish.",
            GatherKind.Fish,
            100,
            objs.raw_swordfish,
        )
        gather(Hard, "Cook 250 Items", "Cook 250 items successfully.", GatherKind.Cook, 250)
        gather(
            Hard,
            "Burn 250 Logs",
            "Light 250 fires from logs of any kind.",
            GatherKind.Firemake,
            250,
        )
        gather(
            Hard,
            "Bury 100 Big Bones",
            "Bury 100 big bones.",
            GatherKind.BuryBones,
            100,
            objs.big_bones,
        )
        gather(
            Hard,
            "Bury 25 Dragon Bones",
            "Bury 25 dragon bones.",
            GatherKind.BuryBones,
            25,
            objs.dragon_bones,
        )
        gather(
            Hard,
            "Offer 100 Bones at an Altar",
            "Offer 100 bones at a gilded altar or the Chaos altar.",
            GatherKind.OfferBones,
            100,
        )
        gather(
            Hard,
            "Smelt 100 Mithril Bars",
            "Smelt 100 mithril bars.",
            GatherKind.Smelt,
            100,
            objs.mithril_bar,
        )
        gather(Hard, "Smith 250 Items", "Smith 250 items on an anvil.", GatherKind.Smith, 250)
        gather(Hard, "Craft 250 Items", "Craft 250 items of any kind.", GatherKind.Craft, 250)
        gather(Hard, "Fletch 500 Items", "Fletch 500 items of any kind.", GatherKind.Fletch, 500)
        gather(
            Hard,
            "Fletch 100 Yew Longbows",
            "Cut or string 100 yew longbows.",
            GatherKind.Fletch,
            100,
            objs.unstrung_yew_longbow,
            objs.yew_longbow,
        )
        gather(Hard, "Clean 250 Herbs", "Clean 250 grimy herbs.", GatherKind.CleanHerb, 250)
        gather(Hard, "Mix 100 Potions", "Mix 100 finished potions.", GatherKind.Potion, 100)
        gather(
            Hard,
            "Pickpocket 250 Times",
            "Successfully pickpocket any NPC 250 times.",
            GatherKind.Pickpocket,
            250,
        )
        gather(
            Hard,
            "Steal from a Stall 250 Times",
            "Steal from market stalls 250 times.",
            GatherKind.Stall,
            250,
        )
        gather(
            Hard,
            "Catch 200 Creatures",
            "Catch 200 creatures with Hunter traps.",
            GatherKind.Hunt,
            200,
        )
        gather(
            Hard,
            "Harvest 100 Crops",
            "Harvest produce from farming patches 100 times.",
            GatherKind.Harvest,
            100,
        )
        gather(
            Hard,
            "Complete 100 Rooftop Laps",
            "Complete 100 laps of agility courses.",
            GatherKind.AgilityLap,
            100,
        )

        equip(Hard, "Equip a Rune Platebody", "Equip a rune platebody.", objs.rune_platebody)
        equip(Hard, "Equip a Dragon Scimitar", "Equip a dragon scimitar.", objs.dragon_scimitar)
        equip(
            Hard,
            "Equip a Black D'hide Body",
            "Equip a black dragonhide body.",
            objs.black_dhide_body,
        )
        equip(Hard, "Equip a Granite Maul", "Equip a granite maul.", objs.granite_maul)
        equip(Hard, "Equip a Dragon Dagger", "Equip a dragon dagger.", objs.dragon_dagger)
        equip(Hard, "Equip a Rune Crossbow", "Equip a rune crossbow.", objs.rune_crossbow)

        relic(Hard, "Unlock a Tier 4 Relic", "Unlock a relic from the fourth tier.", minTier = 3)

        visit(
            Hard,
            "Visit the Barrows",
            "Visit the Barrows mounds.",
            Box(3545, 3280, 3585, 3310),
            Morytania,
        )
        visit(
            Hard,
            "Enter the Wilderness",
            "Cross the Wilderness ditch.",
            Box(2944, 3525, 3392, 3600),
            Wilderness,
        )
        visit(Hard, "Visit Brimhaven", "Visit Brimhaven.", Box(2760, 3160, 2815, 3195), Karamja)
        visit(
            Hard,
            "Visit Shilo Village",
            "Visit Shilo Village.",
            Box(2825, 2940, 2875, 2985),
            Karamja,
        )
        visit(
            Hard,
            "Visit the Slayer Tower",
            "Enter the Slayer Tower.",
            Box(3405, 3530, 3455, 3580),
            Morytania,
        )
        visit(
            Hard,
            "Visit Zul-Andra",
            "Visit Zul-Andra, home of Zulrah's shrine.",
            Box(2185, 3040, 2225, 3075),
            Tirannwn,
        )

        // ---------------------------------------------------------------------------- Elite
        levels(Elite, 80, ELITE_SKILLS)
        total(Elite, 1500)
        total(Elite, 1750)
        combat(Elite, 100)

        kill(Elite, "Defeat 50 Dagannoth Kings", 50, DAGANNOTH_KINGS, Fremennik)
        kill(Elite, "Defeat Zulrah 25 Times", 25, "Zulrah", Tirannwn)
        kill(Elite, "Defeat 50 Barrows Brothers", 50, BARROWS_BROTHERS, Morytania)
        kill(Elite, "Defeat 200 Black Demons", 200, "Black demon")
        kill(Elite, "Defeat 50 Steel Dragons", 50, "Steel dragon")
        kill(Elite, "Defeat the Kalphite Queen 10 Times", 10, "Kalphite Queen", Desert)

        gather(
            Elite,
            "Mine 200 Runite Ore",
            "Mine 200 runite ore.",
            GatherKind.Mine,
            200,
            objs.runite_ore,
        )
        gather(
            Elite,
            "Chop 500 Magic Logs",
            "Cut 500 magic logs.",
            GatherKind.Chop,
            500,
            objs.magic_logs,
        )
        gather(
            Elite,
            "Catch 250 Sharks",
            "Catch 250 raw sharks.",
            GatherKind.Fish,
            250,
            objs.raw_shark,
        )
        gather(
            Elite,
            "Cook 250 Sharks",
            "Cook 250 sharks without burning them.",
            GatherKind.Cook,
            250,
            objs.shark,
        )
        gather(
            Elite,
            "Smelt 250 Runite Bars",
            "Smelt 250 runite bars.",
            GatherKind.Smelt,
            250,
            objs.runite_bar,
        )
        gather(
            Elite,
            "Fletch 250 Magic Longbows",
            "Cut or string 250 magic longbows.",
            GatherKind.Fletch,
            250,
            objs.unstrung_magic_longbow,
            objs.magic_longbow,
        )
        gather(Elite, "Mix 500 Potions", "Mix 500 finished potions.", GatherKind.Potion, 500)
        gather(
            Elite,
            "Bury 250 Dragon Bones",
            "Bury 250 dragon bones.",
            GatherKind.BuryBones,
            250,
            objs.dragon_bones,
        )
        gather(
            Elite,
            "Complete 250 Rooftop Laps",
            "Complete 250 laps of agility courses.",
            GatherKind.AgilityLap,
            250,
        )

        equip(Elite, "Equip a Dragon Platebody", "Equip a dragon platebody.", objs.dragon_platebody)
        equip(Elite, "Equip an Abyssal Whip", "Equip an abyssal whip.", objs.abyssal_whip)
        equip(Elite, "Equip a Toxic Blowpipe", "Equip a toxic blowpipe.", objs.toxic_blowpipe)
        equip(Elite, "Equip a Serpentine Helm", "Equip a serpentine helm.", objs.serpentine_helm)
        equip(Elite, "Equip a Fire Cape", "Equip a fire cape.", objs.fire_cape)
        equip(
            Elite,
            "Equip a Barrows Weapon",
            "Equip any of the six Barrows brothers' weapons.",
            objs.ahrims_staff,
            objs.dharoks_greataxe,
            objs.guthans_warspear,
            objs.karils_crossbow,
            objs.torags_hammers,
            objs.veracs_flail,
        )

        relic(Elite, "Unlock a Tier 6 Relic", "Unlock a relic from the sixth tier.", minTier = 5)

        visit(
            Elite,
            "Visit the TzHaar City",
            "Enter the TzHaar city beneath the Karamja volcano.",
            Box(2430, 5100, 2510, 5180),
            Karamja,
        )
        visit(
            Elite,
            "Visit the Deep Wilderness",
            "Reach the deep Wilderness, north of level 45.",
            Box(2944, 3700, 3392, 3900),
            Wilderness,
        )

        // --------------------------------------------------------------------------- Master
        levels(Master, 99, MASTER_SKILLS)
        total(Master, 2000)
        combat(Master, 126)

        kill(Master, "Defeat Zulrah 100 Times", 100, "Zulrah", Tirannwn)
        kill(Master, "Defeat 250 Dagannoth Kings", 250, DAGANNOTH_KINGS, Fremennik)
        kill(Master, "Defeat 200 Barrows Brothers", 200, BARROWS_BROTHERS, Morytania)
        kill(Master, "Defeat the Kalphite Queen 50 Times", 50, "Kalphite Queen", Desert)

        gather(
            Master,
            "Mine 1,000 Runite Ore",
            "Mine 1,000 runite ore.",
            GatherKind.Mine,
            1000,
            objs.runite_ore,
        )
        gather(
            Master,
            "Catch 1,000 Sharks",
            "Catch 1,000 raw sharks.",
            GatherKind.Fish,
            1000,
            objs.raw_shark,
        )
        gather(
            Master,
            "Complete 500 Rooftop Laps",
            "Complete 500 laps of agility courses.",
            GatherKind.AgilityLap,
            500,
        )

        relic(Master, "Unlock a Tier 7 Relic", "Unlock a relic from the seventh tier.", minTier = 6)
    }

    private data class Skill(val stat: StatType, val label: String, val index: Int)

    private class TaskListBuilder {
        private val tasks = ArrayList<LeagueTask>()

        fun build(): List<LeagueTask> = tasks.toList()

        private fun add(
            name: String,
            description: String,
            tier: LeagueTaskTier,
            trigger: Trigger,
            type: LeagueTaskType,
            area: LeagueTaskArea = Global,
            skill: Int = 0,
        ) {
            val id = tasks.size
            require(tasks.none { it.name.equals(name, ignoreCase = true) }) {
                "Duplicate task name: $name"
            }
            tasks += LeagueTask(id, name, description, tier, trigger, type, area, skill)
        }

        /** Holds an id for a task that no longer exists, so later ids keep their meaning. */
        @Suppress("unused")
        fun retired(name: String) {
            add(
                "Retired: $name",
                "This task has been removed.",
                Easy,
                Trigger.TotalLevel(Int.MAX_VALUE),
                LeagueTaskType.Other,
            )
        }

        fun levels(tier: LeagueTaskTier, level: Int, only: List<StatType>? = null) {
            for (skill in SKILLS) {
                if (only != null && skill.stat !in only) {
                    continue
                }
                add(
                    "Reach Level $level ${skill.label}",
                    "Reach level $level in ${skill.label}.",
                    tier,
                    Trigger.SkillLevel(skill.stat, level),
                    LeagueTaskType.Skill,
                    skill = skill.index,
                )
            }
        }

        fun total(tier: LeagueTaskTier, level: Int) =
            add(
                "Reach Total Level ${"%,d".format(level)}",
                "Reach a total level of ${"%,d".format(level)}.",
                tier,
                Trigger.TotalLevel(level),
                LeagueTaskType.Skill,
            )

        fun combat(tier: LeagueTaskTier, level: Int) =
            add(
                "Reach Combat Level $level",
                "Reach combat level $level.",
                tier,
                Trigger.CombatLevel(level),
                LeagueTaskType.Combat,
            )

        fun kill(
            tier: LeagueTaskTier,
            name: String,
            count: Int,
            npc: String,
            area: LeagueTaskArea = Global,
        ) = kill(tier, name, count, setOf(npc), area)

        fun kill(
            tier: LeagueTaskTier,
            name: String,
            count: Int,
            npcs: Set<String>,
            area: LeagueTaskArea = Global,
        ) {
            val who = if (npcs.size == 1) npcs.first() else npcs.joinToString(", ")
            val description =
                if (count == 1) "Defeat $who." else "Defeat $count of the following: $who."
            add(name, description, tier, Trigger.KillNpc(npcs, count), LeagueTaskType.Combat, area)
        }

        fun gather(
            tier: LeagueTaskTier,
            name: String,
            description: String,
            kind: GatherKind,
            count: Int,
            vararg products: ObjType,
        ) =
            add(
                name,
                description,
                tier,
                Trigger.Gather(kind, products.toSet(), count),
                LeagueTaskType.Skill,
                skill = kind.skill,
            )

        fun equip(tier: LeagueTaskTier, name: String, description: String, vararg objs: ObjType) =
            add(name, description, tier, Trigger.Equip(objs.toSet()), LeagueTaskType.Other)

        fun relic(tier: LeagueTaskTier, name: String, description: String, minTier: Int?) =
            add(name, description, tier, Trigger.UnlockRelic(minTier), LeagueTaskType.Achievement)

        fun visit(
            tier: LeagueTaskTier,
            name: String,
            description: String,
            box: Box,
            area: LeagueTaskArea,
        ) = add(name, description, tier, Trigger.VisitArea(box), LeagueTaskType.Other, area)
    }
}
